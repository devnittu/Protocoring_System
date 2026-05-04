package com.examportal.model;

import java.time.LocalDateTime;

/** Immutable record representing a student's attempt at an exam. */
public record Attempt(
        Long          id,
        Long          studentId,
        Long          examId,
        LocalDateTime startedAt,
        LocalDateTime submittedAt,
        Double        score,
        Double        percentage,
        Boolean       passed,
        Status        status
) {
    public enum Status { IN_PROGRESS, SUBMITTED, FORCE_SUBMITTED, ABANDONED }

    /** Returns true if this attempt is still in progress. */
    public boolean isInProgress() { return status == Status.IN_PROGRESS; }

    /** Returns true if this attempt was force-submitted due to proctoring violations. */
    public boolean wasForceSubmitted() { return status == Status.FORCE_SUBMITTED; }
}
