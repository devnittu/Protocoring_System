package com.examportal.proctor;

import com.examportal.model.ActivityLog;

/** Immutable value object representing a single proctoring detection event. */
public record ProctorEvent(
        Long                      attemptId,
        Long                      studentId,
        ActivityLog.EventType     eventType,
        ActivityLog.Severity      severity,
        String                    detail,
        String                    framePath
) {
    /** Returns true if this event is CRITICAL severity. */
    public boolean isCritical() { return severity == ActivityLog.Severity.CRITICAL; }
}
