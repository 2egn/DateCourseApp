package com.datecourse.service;

import com.datecourse.model.UserSession;
import com.datecourse.db.MemberRepository;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

class UserContextTest {
    @Test void preloadAndRepeatedReadsShareOneQueryIncludingEmptyResults() throws Exception {
        var profiles = new AtomicInteger();
        var courses = new AtomicInteger();
        try (var context = new UserContext(new UserSession(1,"회원"), () -> {
            profiles.incrementAndGet(); return new MemberRepository.Profile("a@test","회원",null);
        }, () -> { courses.incrementAndGet(); return List.of(); })) {
            context.preload();
            assertEquals("회원", context.profile().get(5, TimeUnit.SECONDS).name());
            assertTrue(context.courses().get(5, TimeUnit.SECONDS).isEmpty());
            assertSame(context.profile(), context.profile());
            assertSame(context.courses(), context.courses());
            assertEquals(1, profiles.get()); assertEquals(1, courses.get());
            context.invalidateCourses();
            context.courses().get(5, TimeUnit.SECONDS);
            assertEquals(2, courses.get()); assertEquals(1, profiles.get());
        }
    }

    @Test void inFlightRequestsAreSharedAndOldReadsCannotReplaceAnInvalidatedCache() throws Exception {
        var calls = new AtomicInteger();
        var started = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        try (var context = new UserContext(new UserSession(1,"회원"), () -> {
            int call = calls.incrementAndGet();
            if (call == 1) { started.countDown(); release.await(5,TimeUnit.SECONDS); }
            return new MemberRepository.Profile("a@test","버전"+call,null);
        }, null)) {
            var old = context.profile();
            assertTrue(started.await(5,TimeUnit.SECONDS));
            assertSame(old, context.profile());
            context.invalidateProfile();
            var fresh = context.profile();
            assertEquals("버전2", fresh.get(5,TimeUnit.SECONDS).name());
            release.countDown();old.get(5,TimeUnit.SECONDS);
            assertSame(fresh, context.profile());
            assertEquals("버전2", context.profile().join().name());
        } finally { release.countDown(); }
    }

    @Test void failedReadCanRetryAndLogoutStopsReuseAcrossAccounts() throws Exception {
        var calls = new AtomicInteger();
        var context = new UserContext(new UserSession(1,"첫 회원"), null, () -> {
            if (calls.incrementAndGet() == 1) throw new IllegalStateException("일시적 실패");
            return List.of();
        });
        var failed = context.courses();
        assertThrows(CompletionException.class, failed::join);
        // Wait for the completion observer to release the failed entry, without another query.
        failed.handle((v,e) -> null).get(5,TimeUnit.SECONDS);
        assertTrue(context.courses().get(5,TimeUnit.SECONDS).isEmpty());
        assertEquals(2,calls.get());
        context.close();
        assertThrows(CompletionException.class, () -> context.courses().join());
        assertEquals(2,calls.get());
        try (var next = new UserContext(new UserSession(2,"다음 회원"), null, List::of)) {
            assertTrue(next.courses().get(5,TimeUnit.SECONDS).isEmpty());
            assertEquals(2,next.session().memberId());
        }
    }
    @Test void previewReadsShareCacheAndInvalidationAndLogoutStopReuse() throws Exception {
        var plan=new com.datecourse.model.RoutePlan(35,129,2,1000,"",List.of());
        var saved=new com.datecourse.model.SavedCourse(1,7,plan);var calls=new AtomicInteger();
        try(var context=new UserContext(new UserSession(7,"owner"),null,List::of)) {
            java.util.concurrent.Callable<java.util.Optional<byte[]>> loader=()->{calls.incrementAndGet();return java.util.Optional.empty();};
            var first=context.preview(saved,loader);assertTrue(first.get(5,TimeUnit.SECONDS).isEmpty());
            assertSame(first,context.preview(saved,loader));assertEquals(1,calls.get());
            var renamed=new com.datecourse.model.SavedCourse(1,7,new com.datecourse.model.RoutePlan(35,129,2,1000,"새 이름",List.of()));
            assertSame(first,context.preview(renamed,loader));
            context.invalidateCourses();context.preview(saved,loader).get(5,TimeUnit.SECONDS);assertEquals(2,calls.get());
            assertThrows(CompletionException.class,()->context.preview(new com.datecourse.model.SavedCourse(1,8,plan),loader).join());
            context.close();assertThrows(CompletionException.class,()->context.preview(saved,loader).join());
        }
    }
    @Test void previewErrorsCanRetryAndOldInFlightResultCannotReplaceNewSnapshot() throws Exception {
        var plan=new com.datecourse.model.RoutePlan(35,129,2,1000,"",List.of());
        var saved=new com.datecourse.model.SavedCourse(1,7,plan);var started=new CountDownLatch(1);var release=new CountDownLatch(1);
        try(var context=new UserContext(new UserSession(7,"owner"),null,List::of)) {
            var attempts=new AtomicInteger();
            java.util.concurrent.Callable<java.util.Optional<byte[]>> loader=()->{
                if(attempts.incrementAndGet()==1)throw new java.io.IOException("temporary failure");
                started.countDown();release.await(5,TimeUnit.SECONDS);return java.util.Optional.of(new byte[]{1});
            };
            var failed=context.preview(saved,loader);
            assertThrows(CompletionException.class,failed::join);
            var old=context.preview(saved,loader);
            assertTrue(started.await(5,TimeUnit.SECONDS));context.invalidateCourses();
            var fresh=context.preview(saved,()->java.util.Optional.of(new byte[]{2}));
            assertArrayEquals(new byte[]{2},fresh.get(5,TimeUnit.SECONDS).orElseThrow());
            release.countDown();old.get(5,TimeUnit.SECONDS);
            assertSame(fresh,context.preview(saved,()->{fail("must reuse fresh cache");return java.util.Optional.empty();}));
        } finally {release.countDown();}
    }
    @Test void previewCacheRetainsRecentEntriesAndBoundsBlobMemory() throws Exception {
        var plan=new com.datecourse.model.RoutePlan(35,129,2,1000,"",List.of());
        try(var context=new UserContext(new UserSession(7,"owner"),null,List::of)) {
            var first=new com.datecourse.model.SavedCourse(1,7,plan);
            var kept=context.preview(first,()->java.util.Optional.of(new byte[]{1}));kept.get(5,TimeUnit.SECONDS);
            for(long id=2;id<=12;id++)context.preview(new com.datecourse.model.SavedCourse(id,7,plan),java.util.Optional::empty).get(5,TimeUnit.SECONDS);
            assertSame(kept,context.preview(first,()->{fail("recent entry must be retained");return java.util.Optional.empty();}));
            context.preview(new com.datecourse.model.SavedCourse(13,7,plan),java.util.Optional::empty).get(5,TimeUnit.SECONDS);
            var reloads=new AtomicInteger();
            context.preview(new com.datecourse.model.SavedCourse(2,7,plan),()->{reloads.incrementAndGet();return java.util.Optional.empty();}).get(5,TimeUnit.SECONDS);
            assertEquals(1,reloads.get());assertSame(kept,context.preview(first,java.util.Optional::empty));
        }
    }
}
