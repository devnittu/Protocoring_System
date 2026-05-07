package com.examportal.proctor;

import com.examportal.config.AppConfig;
import com.examportal.model.ActivityLog;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_videoio.VideoCapture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.*;

import static org.bytedeco.opencv.global.opencv_imgcodecs.imwrite;

/**
 * Captures webcam frames at a fixed interval, saves JPEGs, and enqueues ProctorEvents
 * for the ProctorEngine to process asynchronously.
 */
public class FrameCapture {

    private static final Logger log = LoggerFactory.getLogger(FrameCapture.class);
    private static final DateTimeFormatter TS_FMT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private final Long                      attemptId;
    private final Long                      studentId;
    private final BlockingQueue<ProctorEvent> eventQueue;
    private final FaceDetector              faceDetector;
    private final String                    saveDir;
    private final long                      intervalMs;

    private ScheduledExecutorService scheduler;
    private VideoCapture             capture;

    public FrameCapture(Long attemptId, Long studentId,
                        BlockingQueue<ProctorEvent> eventQueue,
                        FaceDetector faceDetector) {
        AppConfig cfg  = AppConfig.getInstance();
        this.attemptId   = attemptId;
        this.studentId   = studentId;
        this.eventQueue  = eventQueue;
        this.faceDetector = faceDetector;
        this.saveDir     = cfg.getProctorSaveDir() + "/" + attemptId;
        this.intervalMs  = cfg.getProctorFrameInterval();
    }

    /** Starts the scheduled frame capture on a daemon thread. */
    public void start() {
        // Ensure save directory exists
        new File(saveDir).mkdirs();

        capture   = new VideoCapture(0);
        if (!capture.isOpened()) {
            log.warn("Webcam not available — proctoring will simulate FACE_ABSENT events.");
        }

        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "FrameCapture-Thread");
            t.setDaemon(true);
            return t;
        });

        scheduler.scheduleAtFixedRate(this::captureAndEnqueue,
                0, intervalMs, TimeUnit.MILLISECONDS);
        log.info("FrameCapture started — intervalMs={}, saveDir={}", intervalMs, saveDir);
    }

    /** Stops capture and releases the webcam. */
    public void stop() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdownNow();
        }
        if (capture != null && capture.isOpened()) {
            capture.release();
        }
        log.info("FrameCapture stopped.");
    }

    private void captureAndEnqueue() {
        try {
            if (capture == null || !capture.isOpened()) {
                // No webcam — generate synthetic FACE_ABSENT event
                ProctorEvent event = new ProctorEvent(attemptId, studentId,
                        ActivityLog.EventType.FACE_ABSENT, ActivityLog.Severity.CRITICAL,
                        "Webcam unavailable", null);
                eventQueue.offer(event, 1, TimeUnit.SECONDS);
                return;
            }

            Mat frame = new Mat();
            if (!capture.read(frame) || frame.empty()) {
                log.warn("Empty frame captured.");
                return;
            }

            String ts       = LocalDateTime.now().format(TS_FMT);
            String filePath = saveDir + "/" + ts + ".jpg";
            imwrite(filePath, frame);

            FaceDetector.FaceResult result = faceDetector.detect(frame);
            ProctorEvent event = classifyResult(result, filePath);
            if (event != null) {
                eventQueue.offer(event, 1, TimeUnit.SECONDS);
            }

            frame.release();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            log.error("Error during frame capture", e);
        }
    }

    private ProctorEvent classifyResult(FaceDetector.FaceResult result, String framePath) {
        if (result.count() == 0) {
            return new ProctorEvent(attemptId, studentId,
                    ActivityLog.EventType.FACE_ABSENT, ActivityLog.Severity.CRITICAL,
                    "No face detected in frame", framePath);
        }
        if (result.count() >= 2) {
            return new ProctorEvent(attemptId, studentId,
                    ActivityLog.EventType.MULTI_FACE, ActivityLog.Severity.CRITICAL,
                    result.count() + " faces detected", framePath);
        }
        if (result.gazeAway()) {
            return new ProctorEvent(attemptId, studentId,
                    ActivityLog.EventType.GAZE_AWAY, ActivityLog.Severity.WARN,
                    "Gaze deviation detected", framePath);
        }
        return null; // All clear
    }
}
