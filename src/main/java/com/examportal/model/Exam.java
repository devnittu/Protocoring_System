package com.examportal.model;

import java.time.LocalDateTime;

/** Immutable record representing an exam created by an admin. */
public record Exam(
        Long          id,
        String        title,
        String        description,
        Long          createdBy,
        int           durationMinutes,
        int           totalMarks,
        double        passPercentage,
        LocalDateTime startTime,
        LocalDateTime endTime,
        boolean       isPublished,
        boolean       randomiseQuestions,
        LocalDateTime createdAt
) {
    /** Returns true if this exam is currently within its scheduled window. */
    public boolean isAvailableNow() {
        LocalDateTime now = LocalDateTime.now();
        if (startTime != null && now.isBefore(startTime)) return false;
        if (endTime   != null && now.isAfter(endTime))    return false;
        return isPublished;
    }
}
