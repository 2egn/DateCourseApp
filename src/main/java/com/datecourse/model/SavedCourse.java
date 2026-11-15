package com.datecourse.model;

/** DB identity stays separate from the RoutePlan used by the editor. */
public record SavedCourse(long courseId, long memberId, RoutePlan plan, java.time.Instant updatedAt) {
    public SavedCourse(long courseId, long memberId, RoutePlan plan) { this(courseId, memberId, plan, null); }
}
