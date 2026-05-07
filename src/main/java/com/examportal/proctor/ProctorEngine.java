package com.examportal.proctor;

import com.examportal.config.AppConfig;
import com.examportal.dao.IActivityLogDAO;
import com.examportal.model.ActivityLog;
import javafx.application.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;
import java.util.function.Consumer;

/**
 * Consumes ProctorEvents from the BlockingQueue, persists them to DB asynchronously,
 * updates the UI overlay via Platform.runLater, and triggers force-submit after 3 CRITICAL events.
 */
public class ProctorEngine {

    private static final Logger log = LoggerFactory.getLogger(ProctorEngine.class);

    private final Long                      attemptId;
    private final Long                      studentId;
    private final IActivityLogDAO           activityLogDAO;
    private final Consumer<ProctorEvent>    onEvent;
    private final Runnable                  onForceSubmit;
    private final int                       criticalThreshold;

    private final BlockingQueue<ProctorEvent> eventQueue   = new LinkedBlockingQueue<>(100);
    private final ExecutorService             dbWriter     = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "ProctorDB-Writer");
        t.setDaemon(true);
        return t;
    });

    private FrameCapture frameCapture;
    private FaceDetector faceDetector;
    private Thread       consumerThread;
    private volatile boolean running = false;
    private volatile int     criticalCount = 0;

    public ProctorEngine(Long attemptId, Long studentId,
                         IActivityLogDAO activityLogDAO,
                         Consumer<ProctorEvent> onEvent,
                         Runnable onForceSubmit) {
        this.attemptId        = attemptId;
        this.studentId        = studentId;
        this.activityLogDAO   = activityLogDAO;
        this.onEvent          = onEvent;
        this.onForceSubmit    = onForceSubmit;
        this.criticalThreshold = AppConfig.getInstance().getCriticalAutoSubmitCount();
    }

    /** Starts frame capture and event consumption. */
    public void start() {
        running      = true;
        criticalCount = 0;

        faceDetector = new FaceDetector(AppConfig.getInstance().getGazeDeviationThreshold());
        frameCapture = new FrameCapture(attemptId, studentId, eventQueue, faceDetector);
        frameCapture.start();

        consumerThread = new Thread(this::consumeEvents, "ProctorEngine-Consumer");
        consumerThread.setDaemon(true);
        consumerThread.start();

        log.info("ProctorEngine started for attemptId={}", attemptId);
    }

    /** Stops all proctoring threads cleanly. */
    public void stop() {
        running = false;
        if (frameCapture != null) frameCapture.stop();
        if (consumerThread != null) consumerThread.interrupt();
        dbWriter.shutdownNow();
        log.info("ProctorEngine stopped for attemptId={}", attemptId);
    }

    /** Enqueues an externally generated event (e.g. from WindowMonitor). */
    public void enqueueEvent(ProctorEvent event) {
        eventQueue.offer(event);
    }

    private void consumeEvents() {
        while (running && !Thread.currentThread().isInterrupted()) {
            try {
                ProctorEvent event = eventQueue.poll(2, TimeUnit.SECONDS);
                if (event == null) continue;

                // Persist to DB on the dedicated writer thread
                dbWriter.submit(() -> persistEvent(event));

                // Update UI on JavaFX thread
                Platform.runLater(() -> onEvent.accept(event));

                // Track CRITICAL events
                if (event.isCritical()) {
                    criticalCount++;
                    log.warn("CRITICAL event #{}: {} — attemptId={}", criticalCount, event.eventType(), attemptId);
                    if (criticalCount >= criticalThreshold) {
                        log.error("Force-submitting exam: {} CRITICAL events exceeded threshold", criticalCount);
                        Platform.runLater(onForceSubmit);
                        stop();
                        return;
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void persistEvent(ProctorEvent event) {
        try {
            ActivityLog entry = new ActivityLog(
                null, event.attemptId(), event.studentId(),
                event.eventType(), event.severity(),
                event.detail(), event.framePath(), null
            );
            activityLogDAO.insert(entry);
        } catch (Exception e) {
            log.error("Failed to persist ProctorEvent to DB", e);
        }
    }
}
