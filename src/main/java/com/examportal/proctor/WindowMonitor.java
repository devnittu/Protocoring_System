package com.examportal.proctor;

import com.examportal.model.ActivityLog;
import javafx.beans.value.ChangeListener;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Monitors JavaFX window focus and detects inactivity,
 * enqueuing TAB_SWITCH and INACTIVITY_60S events to the ProctorEngine.
 */
public class WindowMonitor {

    private static final Logger log = LoggerFactory.getLogger(WindowMonitor.class);

    private final ProctorEngine         engine;
    private final Long                  attemptId;
    private final Long                  studentId;
    private       ChangeListener<Boolean> focusListener;
    private       Timer                 inactivityTimer;
    private volatile long               lastActivityMs = System.currentTimeMillis();

    public WindowMonitor(ProctorEngine engine, Long attemptId, Long studentId) {
        this.engine    = engine;
        this.attemptId = attemptId;
        this.studentId = studentId;
    }

    /** Attaches focus listener to the stage and starts the inactivity poll timer. */
    public void attach(Stage stage) {
        focusListener = (obs, wasFocused, isFocused) -> {
            if (!isFocused) {
                log.warn("Window lost focus — TAB_SWITCH/WINDOW_BLUR event");
                engine.enqueueEvent(new ProctorEvent(
                    attemptId, studentId,
                    ActivityLog.EventType.TAB_SWITCH,
                    ActivityLog.Severity.WARN,
                    "Window lost focus", null
                ));
            }
        };
        stage.focusedProperty().addListener(focusListener);

        // Inactivity poll: every 60 s
        inactivityTimer = new Timer("InactivityTimer", true);
        inactivityTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                long elapsedMs = System.currentTimeMillis() - lastActivityMs;
                if (elapsedMs >= 60_000) {
                    log.warn("Inactivity detected — {}ms since last activity", elapsedMs);
                    engine.enqueueEvent(new ProctorEvent(
                        attemptId, studentId,
                        ActivityLog.EventType.INACTIVITY_60S,
                        ActivityLog.Severity.WARN,
                        "No interaction for 60+ seconds", null
                    ));
                    resetActivity(); // reset so we don't spam
                }
            }
        }, 60_000, 60_000);

        log.info("WindowMonitor attached to stage.");
    }

    /** Call this whenever the student interacts (mouse move, key press, etc.). */
    public void resetActivity() {
        lastActivityMs = System.currentTimeMillis();
    }

    /** Detaches listeners and stops the timer. */
    public void detach(Stage stage) {
        if (focusListener != null) {
            stage.focusedProperty().removeListener(focusListener);
        }
        if (inactivityTimer != null) {
            inactivityTimer.cancel();
        }
        log.info("WindowMonitor detached.");
    }
}
