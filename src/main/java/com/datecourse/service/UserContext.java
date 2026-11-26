package com.datecourse.service;

import com.datecourse.model.SavedCourse;
import com.datecourse.model.UserSession;
import com.datecourse.db.MemberRepository;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

/**
 * - 로그인한 회원의 식별값, 프로필 및 저장 코스 캐시 관리용 클래스
 * - 한 로그인 세션에서 메모리를 공유하며 DB 연결 풀은 소유하지 않음
 */
public final class UserContext implements AutoCloseable {
    private final UserSession session;
    private final CachedValue<MemberRepository.Profile> profile;
    private final CachedValue<List<SavedCourse>> courses;
    private record PreviewKey(long courseId, String fingerprint) {}
    private static final int MAX_PREVIEW_CACHE_ENTRIES = 12;
    private final java.util.Map<PreviewKey, CachedValue<java.util.Optional<byte[]>>> previews =
            new java.util.LinkedHashMap<>(16, 0.75f, true) {
                @Override protected boolean removeEldestEntry(java.util.Map.Entry<PreviewKey, CachedValue<java.util.Optional<byte[]>>> eldest) {
                    return size() > MAX_PREVIEW_CACHE_ENTRIES;
                }
            };
    private final java.util.Set<Long> preparedMaps = new java.util.HashSet<>();
    private volatile boolean closed;

    public UserContext(UserSession session, Callable<MemberRepository.Profile> profileLoader,
                       Callable<List<SavedCourse>> courseLoader) {
        this.session = Objects.requireNonNull(session);
        profile = new CachedValue<>(profileLoader);
        courses = new CachedValue<>(courseLoader == null ? null : () -> List.copyOf(courseLoader.call()));
    }

    public UserSession session() { return session; }
    public boolean isClosed() { return closed; }
    public CompletableFuture<MemberRepository.Profile> profile() { return profile.get(); }
    public CompletableFuture<List<SavedCourse>> courses() { return courses.get(); }
    public void invalidateProfile() { profile.invalidate(); }
    // 저장 목록 변경 시 목록·이미지·지도 준비 상태를 함께 무효화
    public synchronized void invalidateCourses() { courses.invalidate(); previews.clear(); preparedMaps.clear(); }

    // 코스 번호와 내용 지문이 같은 미리보기 조회를 로그인 세션에서 재사용
    public synchronized CompletableFuture<java.util.Optional<byte[]>> preview(
            SavedCourse course, Callable<java.util.Optional<byte[]>> loader) {
        if (closed || course.memberId() != session.memberId())
            return CompletableFuture.failedFuture(new IllegalStateException("현재 회원의 미리보기만 조회할 수 있어요."));
        var key = new PreviewKey(course.courseId(), CoursePreviewImage.fingerprint(course.plan()));
        return previews.computeIfAbsent(key, ignored -> new CachedValue<>(loader)).get();
    }

    /** 지도 준비 여부는 로그인 세션 안에서만 코스별로 기억한다. */
    public synchronized boolean isMapPrepared(SavedCourse course) {
        return !closed && course.memberId() == session.memberId() && preparedMaps.contains(course.courseId());
    }

    /** 닫힌 세션이나 다른 회원의 코스에는 준비 상태를 기록하지 않는다. */
    public synchronized boolean markMapPrepared(SavedCourse course) {
        if (closed || course.memberId() != session.memberId()) return false;
        preparedMaps.add(course.courseId());
        return true;
    }

    /** 화면 이동을 막거나 로딩창을 띄우지 않고 회원·코스 정보를 미리 조회하는 함수 */
    public void preload() {
        if (profile.loader != null) profile();
        if (courses.loader != null) courses();
    }

    @Override public synchronized void close() {
        closed = true;
        profile.invalidate();
        courses.invalidate();
        previews.clear();
        preparedMaps.clear();
    }

    private final class CachedValue<T> {
        private final Callable<T> loader;
        private CompletableFuture<T> current;
        private CachedValue(Callable<T> loader) { this.loader = loader; }

        synchronized CompletableFuture<T> get() {
            if (closed) return CompletableFuture.failedFuture(new IllegalStateException("로그아웃한 세션이에요."));
            if (loader == null) return CompletableFuture.failedFuture(new IllegalStateException("조회 연결이 없어요."));
            if (current != null && !current.isCompletedExceptionally()) return current;
            CompletableFuture<T> request = CompletableFuture.supplyAsync(() -> {
                try { return loader.call(); }
                catch (Exception failure) { throw new CompletionException(failure); }
            });
            current = request;
            request.whenComplete((value, failure) -> {
                if (failure != null) synchronized (this) {
                    if (current == request) current = null; // A failed read must be retryable.
                }
            });
            return request;
        }

        synchronized void invalidate() { current = null; }
    }
}
