package com.examportal.model;

import java.time.LocalDateTime;

/** Immutable record representing a proctoring or behavioural event during an exam attempt. */
public record ActivityLog(
        Long          id,
        Long          attemptId,
        Long          studentId,
        EventType     eventType,
        Severity      severity,
        String        detail,
        String        framePath,
        LocalDateTime loggedAt
) {
    public enum EventType {
        TAB_SWITCH, WINDOW_BLUR, INACTIVITY_60S,
        FACE_ABSENT, MULTI_FACE, GAZE_AWAY,
        EXAM_STARTED, EXAM_SUBMITTED,
        EXAM_FORCE_SUBMITTED, COPY_ATTEMPT
    }

    public enum Severity { INFO, WARN, CRITICAL }

    /** Returns true if this is a CRITICAL severity event. */
    public boolean isCritical() { return severity == Severity.CRITICAL; }
}
