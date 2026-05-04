package com.examportal.service;

import com.examportal.dao.IActivityLogDAO;
import com.examportal.model.ActivityLog;
import com.examportal.proctor.ProctorEngine;
import com.examportal.proctor.ProctorEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.function.Consumer;

/** Service facade that starts/stops the ProctorEngine and routes events to the UI. */
public class ProctorService {

    private static final Logger log = LoggerFactory.getLogger(ProctorService.class);

    private final IActivityLogDAO activityLogDAO;
    private ProctorEngine         engine;

    public ProctorService(IActivityLogDAO activityLogDAO) {
        this.activityLogDAO = activityLogDAO;
    }

    /**
     * Starts the proctoring engine for a given attempt.
     *
     * @param attemptId     the active attempt
     * @param studentId     the student being proctored
     * @param onEvent       UI callback invoked via Platform.runLater on each ProctorEvent
     * @param onForceSubmit UI callback invoked when CRITICAL threshold is exceeded
     */
    public void start(Long attemptId, Long studentId,
                      Consumer<ProctorEvent> onEvent,
                      Runnable onForceSubmit) {
        if (engine != null) stop();
        engine = new ProctorEngine(attemptId, studentId, activityLogDAO, onEvent, onForceSubmit);
        engine.start();
        log.info("ProctorService started for attemptId={}", attemptId);
    }

    /** Stops the proctoring engine cleanly. */
    public void stop() {
        if (engine != null) {
            engine.stop();
            engine = null;
            log.info("ProctorService stopped.");
        }
    }

    /** Logs a behavioural event (tab switch, copy attempt, etc.) directly. */
    public void logEvent(Long attemptId, Long studentId,
                         ActivityLog.EventType type, ActivityLog.Severity severity,
                         String detail) {
        ActivityLog entry = new ActivityLog(
            null, attemptId, studentId, type, severity, detail, null, null
        );
        activityLogDAO.insert(entry);
    }

    /** Returns all logs for a given attempt. */
    public List<ActivityLog> getLogs(Long attemptId) {
        return activityLogDAO.findByAttemptId(attemptId);
    }
}
