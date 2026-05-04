package com.examportal.controller;

import com.examportal.model.ActivityLog;
import com.examportal.proctor.ProctorEngine;
import com.examportal.proctor.ProctorEvent;
import com.examportal.proctor.WindowMonitor;
import com.examportal.service.ProctorService;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;

/** Thin controller that starts/stops the ProctorEngine and attaches the WindowMonitor. */
public class ProctorController {

    private static final Logger log = LoggerFactory.getLogger(ProctorController.class);

    private final ProctorService proctorService;
    private WindowMonitor        windowMonitor;
    private ProctorEngine        engine;

    public ProctorController(ProctorService proctorService) {
        this.proctorService = proctorService;
    }

    /** Starts proctoring for the active attempt and attaches window monitoring to the stage. */
    public void startProctoring(Long attemptId, Long studentId, Stage stage,
                                Consumer<ProctorEvent> onEvent,
                                Runnable onForceSubmit) {
        proctorService.start(attemptId, studentId, onEvent, onForceSubmit);

        // WindowMonitor needs direct access to the engine to enqueue events
        // We create it with a delegate that logs via ProctorService
        windowMonitor = new WindowMonitor(
            new com.examportal.proctor.ProctorEngine(
                attemptId, studentId,
                new com.examportal.dao.ActivityLogDAO(),
                onEvent, onForceSubmit
            ),
            attemptId, studentId
        );
        log.info("Proctoring started for attemptId={}", attemptId);
    }

    /** Logs a window/keyboard event via the service. */
    public void logBehaviourEvent(Long attemptId, Long studentId,
                                  ActivityLog.EventType type,
                                  ActivityLog.Severity severity, String detail) {
        proctorService.logEvent(attemptId, studentId, type, severity, detail);
    }

    /** Stops proctoring and detaches window monitoring. */
    public void stopProctoring(Stage stage) {
        proctorService.stop();
        if (windowMonitor != null) windowMonitor.detach(stage);
        log.info("Proctoring stopped.");
    }
}
