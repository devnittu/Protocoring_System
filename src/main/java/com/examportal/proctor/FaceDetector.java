package com.examportal.proctor;

import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.RectVector;
import org.bytedeco.opencv.opencv_objdetect.CascadeClassifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import static org.bytedeco.opencv.global.opencv_imgproc.cvtColor;
import static org.bytedeco.opencv.global.opencv_imgproc.COLOR_BGR2GRAY;

/** Loads the Haar cascade XML and runs face detection on individual frames. */
public class FaceDetector {

    private static final Logger log = LoggerFactory.getLogger(FaceDetector.class);

    private final CascadeClassifier classifier;
    private final double            gazeThreshold;

    /** Represents the result of analysing one frame. */
    public record FaceResult(int count, boolean gazeAway) {}

    public FaceDetector(double gazeDeviationThreshold) {
        this.gazeThreshold = gazeDeviationThreshold;
        this.classifier    = loadClassifier();
    }

    private CascadeClassifier loadClassifier() {
        try {
            // Extract the haar XML from classpath to a temp file so OpenCV can load it
            InputStream is = getClass().getClassLoader()
                    .getResourceAsStream("haarcascade_frontalface_default.xml");
            if (is == null) {
                log.warn("haarcascade_frontalface_default.xml not found on classpath — using empty classifier.");
                return new CascadeClassifier();
            }
            File tmp = File.createTempFile("haarcascade_", ".xml");
            tmp.deleteOnExit();
            Files.copy(is, tmp.toPath(), StandardCopyOption.REPLACE_EXISTING);
            CascadeClassifier cc = new CascadeClassifier(tmp.getAbsolutePath());
            if (cc.empty()) {
                log.warn("CascadeClassifier loaded but is empty.");
            }
            return cc;
        } catch (Exception e) {
            log.error("Failed to load CascadeClassifier", e);
            return new CascadeClassifier();
        }
    }

    /**
     * Analyses a frame and returns face count and gaze status.
     *
     * @param frame BGR Mat frame from OpenCV VideoCapture
     * @return FaceResult with face count and gaze deviation flag
     */
    public FaceResult detect(Mat frame) {
        if (frame == null || frame.empty()) {
            return new FaceResult(0, false);
        }

        if (classifier.empty()) {
            // Simulate: return 1 face, no gaze deviation (graceful degradation)
            return new FaceResult(1, false);
        }

        try (Mat gray = new Mat()) {
            cvtColor(frame, gray, COLOR_BGR2GRAY);

            RectVector faces = new RectVector();
            classifier.detectMultiScale(gray, faces, 1.1, 3, 0, new org.bytedeco.opencv.opencv_core.Size(30, 30),
                    new org.bytedeco.opencv.opencv_core.Size());

            int count = (int) faces.size();

            if (count == 0) {
                return new FaceResult(0, false);
            }

            // Gaze estimation: check if face centre deviates more than threshold from frame centre
            boolean gazeAway = false;
            if (count == 1) {
                org.bytedeco.opencv.opencv_core.Rect face = faces.get(0);
                double faceCentreX  = face.x() + face.width() / 2.0;
                double frameCentreX = frame.cols() / 2.0;
                double deviation    = Math.abs(faceCentreX - frameCentreX) / frame.cols();
                gazeAway = deviation > gazeThreshold;
            }

            return new FaceResult(count, gazeAway);
        }
    }
}
