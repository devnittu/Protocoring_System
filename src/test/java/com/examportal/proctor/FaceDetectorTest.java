package com.examportal.proctor;

import com.examportal.model.ActivityLog;
import org.bytedeco.opencv.opencv_core.Mat;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

/** Unit tests for FaceDetector covering face count classification and gaze logic. */
class FaceDetectorTest {

    private FaceDetector detector;

    @BeforeEach
    void setUp() {
        // 0.30 gaze deviation threshold
        detector = new FaceDetector(0.30);
    }

    @AfterEach
    void tearDown() {}

    @Test
    @DisplayName("Null frame → count == 0 (FACE_ABSENT)")
    void nullFrame_returnsZeroCount() {
        FaceDetector.FaceResult result = detector.detect(null);
        assertEquals(0, result.count());
        assertFalse(result.gazeAway());
    }

    @Test
    @DisplayName("Empty Mat frame → count == 0 (FACE_ABSENT)")
    void emptyFrame_returnsZeroCount() {
        Mat empty = new Mat();
        FaceDetector.FaceResult result = detector.detect(empty);
        assertEquals(0, result.count());
        empty.release();
    }

    @Test
    @DisplayName("Event classifier: count == 0 → FACE_ABSENT CRITICAL")
    void zeroFaces_mapToFaceAbsentCritical() {
        // Simulate the ProctorEngine mapping logic
        FaceDetector.FaceResult result = new FaceDetector.FaceResult(0, false);
        ActivityLog.EventType eventType = classifyEventType(result);
        ActivityLog.Severity  severity  = classifySeverity(result);
        assertEquals(ActivityLog.EventType.FACE_ABSENT, eventType);
        assertEquals(ActivityLog.Severity.CRITICAL, severity);
    }

    @Test
    @DisplayName("Event classifier: count == 1, no gaze → no event (OK)")
    void oneFaceNoGaze_noEvent() {
        FaceDetector.FaceResult result = new FaceDetector.FaceResult(1, false);
        assertNull(classifyEventType(result));
    }

    @Test
    @DisplayName("Event classifier: count == 1, gaze away → GAZE_AWAY WARN")
    void oneFaceGazeAway_gazeAwayWarn() {
        FaceDetector.FaceResult result = new FaceDetector.FaceResult(1, true);
        assertEquals(ActivityLog.EventType.GAZE_AWAY, classifyEventType(result));
        assertEquals(ActivityLog.Severity.WARN, classifySeverity(result));
    }

    @Test
    @DisplayName("Event classifier: count == 2 → MULTI_FACE CRITICAL")
    void twoFaces_multiFaceCritical() {
        FaceDetector.FaceResult result = new FaceDetector.FaceResult(2, false);
        assertEquals(ActivityLog.EventType.MULTI_FACE, classifyEventType(result));
        assertEquals(ActivityLog.Severity.CRITICAL, classifySeverity(result));
    }

    @Test
    @DisplayName("Event classifier: count == 3 → MULTI_FACE CRITICAL")
    void threeFaces_multiFaceCritical() {
        FaceDetector.FaceResult result = new FaceDetector.FaceResult(3, false);
        assertEquals(ActivityLog.EventType.MULTI_FACE, classifyEventType(result));
        assertEquals(ActivityLog.Severity.CRITICAL, classifySeverity(result));
    }

    // ── Helpers replicating ProctorEngine classification logic ────────────────

    private ActivityLog.EventType classifyEventType(FaceDetector.FaceResult r) {
        if (r.count() == 0)    return ActivityLog.EventType.FACE_ABSENT;
        if (r.count() >= 2)    return ActivityLog.EventType.MULTI_FACE;
        if (r.gazeAway())      return ActivityLog.EventType.GAZE_AWAY;
        return null;
    }

    private ActivityLog.Severity classifySeverity(FaceDetector.FaceResult r) {
        if (r.count() == 0 || r.count() >= 2) return ActivityLog.Severity.CRITICAL;
        if (r.gazeAway())                      return ActivityLog.Severity.WARN;
        return ActivityLog.Severity.INFO;
    }
}
