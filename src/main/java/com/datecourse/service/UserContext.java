package com.datecourse.service;

import com.datecourse.model.SavedCourse;
import kr.ac.university.localdb.UserSession;
import kr.ac.university.localdb.db.MemberRepository;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

/** One login's identity and shared, in-memory account data. Never owns the DB pool. */
public final class UserContext implements AutoCloseable {
    private final UserSession session;
    private final CachedValue<MemberRepository.Profile> profile;
    private final CachedValue<List<SavedCourse>> courses;
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
    public void invalidateCourses() { courses.invalidate(); }

    /** Background prefetch: it does not block navigation or display a loading overlay. */
    public void preload() {
        if (profile.loader != null) profile();
        if (courses.loader != null) courses();
    }

    @Override public void close() {
        closed = true;
        profile.invalidate();
        courses.invalidate();
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
