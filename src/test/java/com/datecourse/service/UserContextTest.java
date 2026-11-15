package com.datecourse.service;

import kr.ac.university.localdb.UserSession;
import kr.ac.university.localdb.db.MemberRepository;
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
}
