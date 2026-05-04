package com.examportal.ui;

import com.examportal.AppServices;
import com.examportal.controller.ExamController;
import com.examportal.controller.ProctorController;
import com.examportal.model.*;
import com.examportal.proctor.ProctorEvent;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.event.Event;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.*;

/** Full-screen timed exam view with proctor overlay and keyboard lockdown. */
public class ExamView {

    private static final Map<Long, Character> answers = new LinkedHashMap<>();
    private static List<Question> questions;
    private static int currentIndex = 0;
    private static Attempt attempt;
    private static Exam exam;
    private static User student;
    private static Stage stage;
    private static ExamController examCtrl;
    private static ProctorController proctorCtrl;
    private static Label timerLabel;
    private static Label proctorStatusDot;
    private static Label proctorStatusText;
    private static VBox questionSidebar;
    private static BorderPane mainLayout;
    private static Timeline countdownTimeline;
    private static long remainingSeconds;
    private static StackPane rootStack;

    public static Scene build(Stage st, Exam e, User u) {
        stage = st; exam = e; student = u;
        answers.clear(); currentIndex = 0;
        examCtrl    = new ExamController(AppServices.exam(), AppServices.evaluator());
        proctorCtrl = new ProctorController(AppServices.proctor());

        try {
            attempt   = examCtrl.startAttempt(student.id(), exam.id());
            questions = examCtrl.getQuestionsForAttempt(exam.id(), exam.randomiseQuestions());
        } catch (Exception ex) {
            new Alert(Alert.AlertType.ERROR, ex.getMessage(), ButtonType.OK).showAndWait();
            return StudentDashboardView.build(stage, student, AppServices.auth());
        }
        remainingSeconds = examCtrl.getRemainingSeconds(attempt, exam);

        mainLayout = new BorderPane();
        mainLayout.getStyleClass().add("exam-root");
        mainLayout.setTop(buildTopBar());
        mainLayout.setLeft(buildSidebarScroll());
        mainLayout.setCenter(buildQuestionPanel());
        mainLayout.setBottom(buildBottomBar());

        rootStack = new StackPane(mainLayout);
        Scene scene = new Scene(rootStack, 1280, 780);
        scene.getStylesheets().addAll(
            ExamView.class.getResource("/css/app.css").toExternalForm(),
            ExamView.class.getResource("/css/exam.css").toExternalForm()
        );

        // Security lockdown
        scene.addEventFilter(ContextMenuEvent.CONTEXT_MENU_REQUESTED, Event::consume);
        scene.addEventFilter(KeyEvent.KEY_PRESSED, ev -> {
            if (new KeyCodeCombination(KeyCode.C, KeyCombination.CONTROL_DOWN).match(ev) ||
                new KeyCodeCombination(KeyCode.V, KeyCombination.CONTROL_DOWN).match(ev) ||
                new KeyCodeCombination(KeyCode.X, KeyCombination.CONTROL_DOWN).match(ev) ||
                new KeyCodeCombination(KeyCode.A, KeyCombination.CONTROL_DOWN).match(ev)) {
                ev.consume();
                logEvent(ActivityLog.EventType.COPY_ATTEMPT, ActivityLog.Severity.WARN, "Keyboard shortcut blocked");
            }
        });

        startCountdown();
        startProctor();
        logEvent(ActivityLog.EventType.EXAM_STARTED, ActivityLog.Severity.INFO, "Exam started");
        return scene;
    }

    private static HBox buildTopBar() {
        Label examTitle = new Label(exam.title());
        examTitle.getStyleClass().add("exam-title");
        timerLabel = new Label();
        timerLabel.getStyleClass().add("exam-timer");
        updateTimerLabel(remainingSeconds);
        proctorStatusDot  = new Label("●");
        proctorStatusDot.setStyle("-fx-text-fill:#16A34A;-fx-font-size:14;");
        proctorStatusText = new Label("Proctor: OK");
        proctorStatusText.getStyleClass().add("muted-text");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox bar = new HBox(16, examTitle, spacer, proctorStatusDot, proctorStatusText, timerLabel);
        bar.getStyleClass().add("exam-topbar");
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(12, 24, 12, 24));
        return bar;
    }

    private static ScrollPane buildSidebarScroll() {
        questionSidebar = new VBox(6);
        questionSidebar.setPadding(new Insets(16, 8, 16, 8));
        questionSidebar.getStyleClass().add("question-sidebar");
        for (int i = 0; i < questions.size(); i++) {
            final int idx = i;
            Button btn = new Button(String.valueOf(i + 1));
            btn.getStyleClass().add("q-nav-btn");
            btn.setOnAction(e -> { currentIndex = idx; mainLayout.setCenter(buildQuestionPanel()); refreshSidebar(); });
            questionSidebar.getChildren().add(btn);
        }
        refreshSidebar();
        ScrollPane sp = new ScrollPane(questionSidebar);
        sp.setPrefWidth(80); sp.setFitToWidth(true);
        sp.getStyleClass().add("transparent-scroll");
        return sp;
    }

    private static void refreshSidebar() {
        for (int i = 0; i < questionSidebar.getChildren().size(); i++) {
            if (questionSidebar.getChildren().get(i) instanceof Button btn) {
                btn.getStyleClass().removeAll("q-nav-active", "q-nav-answered");
                if (i == currentIndex) btn.getStyleClass().add("q-nav-active");
                else if (i < questions.size() && answers.containsKey(questions.get(i).id()))
                    btn.getStyleClass().add("q-nav-answered");
            }
        }
    }

    private static VBox buildQuestionPanel() {
        if (questions.isEmpty()) return new VBox(new Label("No questions."));
        Question q = questions.get(currentIndex);
        VBox panel = new VBox(20);
        panel.setPadding(new Insets(40));
        panel.getStyleClass().add("question-panel");
        Label meta = new Label("Question " + (currentIndex + 1) + " of " + questions.size()
            + "  ·  " + q.marks() + " mark(s)  ·  " + q.difficulty().name());
        meta.getStyleClass().add("muted-text");
        Label body = new Label(q.body());
        body.getStyleClass().add("question-body");
        body.setWrapText(true);
        ToggleGroup group = new ToggleGroup();
        VBox options = new VBox(12);
        Character selected = answers.get(q.id());
        for (char opt : new char[]{'A','B','C','D'}) {
            RadioButton rb = new RadioButton(opt + ".  " + q.getOptionText(opt));
            rb.setToggleGroup(group);
            rb.getStyleClass().add("option-radio");
            rb.setWrapText(true);
            if (selected != null && selected == opt) rb.setSelected(true);
            final char finalOpt = opt;
            rb.setOnAction(e -> { answers.put(q.id(), finalOpt); refreshSidebar(); });
            options.getChildren().add(rb);
        }
        panel.getChildren().addAll(meta, body, options);
        return panel;
    }

    private static HBox buildBottomBar() {
        Button prevBtn = new Button("◀ Previous");
        prevBtn.getStyleClass().add("btn-secondary");
        prevBtn.setOnAction(e -> {
            if (currentIndex > 0) { currentIndex--; mainLayout.setCenter(buildQuestionPanel()); refreshSidebar(); }
        });
        Button nextBtn = new Button("Next ▶");
        nextBtn.getStyleClass().add("btn-secondary");
        nextBtn.setOnAction(e -> {
            if (currentIndex < questions.size() - 1) { currentIndex++; mainLayout.setCenter(buildQuestionPanel()); refreshSidebar(); }
        });
        Button submitBtn = new Button("✓ Submit Exam");
        submitBtn.setId("submit-exam-btn");
        submitBtn.getStyleClass().add("btn-primary");
        submitBtn.setOnAction(e -> confirmAndSubmit());
        VBox webcam = new VBox(4);
        webcam.getStyleClass().add("webcam-box");
        webcam.setPrefSize(80, 60);
        webcam.setAlignment(Pos.CENTER);
        webcam.getChildren().addAll(new Label("📷"), proctorStatusDot);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox bar = new HBox(12, webcam, prevBtn, nextBtn, spacer, submitBtn);
        bar.getStyleClass().add("exam-bottom-bar");
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(12, 24, 12, 24));
        return bar;
    }

    private static void startCountdown() {
        countdownTimeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            remainingSeconds--;
            updateTimerLabel(remainingSeconds);
            if (remainingSeconds <= 0) { countdownTimeline.stop(); doForceSubmit("Time's up!"); }
        }));
        countdownTimeline.setCycleCount(Timeline.INDEFINITE);
        countdownTimeline.play();
    }

    private static void updateTimerLabel(long secs) {
        timerLabel.setText(String.format("⏱ %02d:%02d", secs / 60, secs % 60));
        if (secs < 300) timerLabel.setStyle("-fx-text-fill:#DC2626;-fx-font-weight:bold;");
        else            timerLabel.setStyle("");
    }

    private static void startProctor() {
        proctorCtrl.startProctoring(attempt.id(), student.id(), stage,
            event -> handleProctorEvent(event),
            () -> doForceSubmit("Too many proctoring violations — exam auto-submitted.")
        );
    }

    private static void handleProctorEvent(ProctorEvent event) {
        String color = switch (event.severity()) {
            case CRITICAL -> "#DC2626";
            case WARN     -> "#F59E0B";
            default       -> "#16A34A";
        };
        proctorStatusDot.setStyle("-fx-text-fill:" + color + ";-fx-font-size:14;");
        proctorStatusText.setText("Proctor: " + event.eventType().name().replace("_", " "));
        if (event.isCritical()) showCriticalOverlay(event);
    }

    private static void showCriticalOverlay(ProctorEvent event) {
        VBox overlay = new VBox(16);
        overlay.setAlignment(Pos.CENTER);
        overlay.setMaxSize(480, 280);
        overlay.setPadding(new Insets(40));
        overlay.setStyle("-fx-background-color:white;-fx-background-radius:12;" +
                "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.3),20,0,0,4);");
        Label icon   = new Label("⚠"); icon.setStyle("-fx-font-size:40;-fx-text-fill:#DC2626;");
        Label title  = new Label("Proctoring Alert — Exam Paused"); title.getStyleClass().add("panel-heading");
        Label detail = new Label(event.detail() != null ? event.detail() : event.eventType().name());
        detail.setWrapText(true); detail.setStyle("-fx-text-fill:#64748B;");
        Button ackBtn = new Button("I Understand — Resume Exam"); ackBtn.getStyleClass().add("btn-primary");
        overlay.getChildren().addAll(icon, title, detail, ackBtn);
        StackPane dimmer = new StackPane(overlay);
        dimmer.setStyle("-fx-background-color:rgba(0,0,0,0.55);");
        ackBtn.setOnAction(e -> rootStack.getChildren().remove(dimmer));
        rootStack.getChildren().add(dimmer);
    }

    private static void confirmAndSubmit() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Submit exam? Unanswered questions will be marked wrong.", ButtonType.YES, ButtonType.NO);
        confirm.showAndWait().filter(r -> r == ButtonType.YES).ifPresent(r -> doSubmit(Attempt.Status.SUBMITTED));
    }

    static void doForceSubmit(String reason) {
        Platform.runLater(() -> {
            new Alert(Alert.AlertType.WARNING, reason, ButtonType.OK).showAndWait();
            doSubmit(Attempt.Status.FORCE_SUBMITTED);
        });
    }

    private static void doSubmit(Attempt.Status status) {
        if (countdownTimeline != null) countdownTimeline.stop();
        proctorCtrl.stopProctoring(stage);
        Attempt finalAttempt = status == Attempt.Status.SUBMITTED
            ? examCtrl.submitAttempt(attempt.id(), questions, answers, exam)
            : examCtrl.forceSubmitAttempt(attempt.id(), questions, answers, exam);
        ActivityLog.EventType evType = status == Attempt.Status.SUBMITTED
            ? ActivityLog.EventType.EXAM_SUBMITTED : ActivityLog.EventType.EXAM_FORCE_SUBMITTED;
        logEvent(evType, ActivityLog.Severity.INFO, status.name());
        Scene result = ResultView.build(stage, finalAttempt, student);
        stage.setScene(result);
        stage.setTitle("Result — ExamPortal");
    }

    private static void logEvent(ActivityLog.EventType type, ActivityLog.Severity sev, String detail) {
        try { proctorCtrl.logBehaviourEvent(attempt.id(), student.id(), type, sev, detail); }
        catch (Exception ignored) {}
    }
}
