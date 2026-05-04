package com.examportal.ui;

import com.examportal.AppServices;
import com.examportal.controller.ResultController;
import com.examportal.model.*;
import com.examportal.service.ExamService;
import com.examportal.util.DateUtil;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Arc;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Result screen with score circle, pass/fail badge, per-question table, proctor summary, and CSV export. */
public class ResultView {

    public static Scene build(Stage stage, Attempt attempt, User student) {
        ResultController ctrl = new ResultController(AppServices.result(), AppServices.exam());
        ExamService examSvc   = AppServices.exam();

        Exam exam = examSvc.findExamById(attempt.examId()).orElse(null);
        List<Response> responses = ctrl.getResponses(attempt.id());
        List<Question> questions = exam != null ? examSvc.getQuestionsOrdered(exam.id()) : List.of();
        Map<String, Long> eventSummary = ctrl.getEventSummary(attempt.id());

        BorderPane root = new BorderPane();
        root.getStyleClass().add("dashboard-root");
        root.setTop(buildTopBar(stage, student));

        ScrollPane scroll = new ScrollPane();
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("transparent-scroll");

        VBox content = new VBox(28);
        content.setPadding(new Insets(36));
        content.getStyleClass().add("panel");

        // ── Score section ────────────────────────────────────────────────────
        HBox scoreSection = buildScoreSection(attempt, exam);
        content.getChildren().add(scoreSection);

        // ── Proctor summary ──────────────────────────────────────────────────
        if (!eventSummary.isEmpty()) {
            VBox proctorBox = buildProctorSummary(eventSummary, ctrl.getCriticalCount(attempt.id()));
            content.getChildren().add(proctorBox);
        }

        // ── Per-question breakdown ────────────────────────────────────────────
        if (!questions.isEmpty()) {
            VBox breakdown = buildBreakdown(questions, responses);
            content.getChildren().add(breakdown);
        }

        // ── Export button ─────────────────────────────────────────────────────
        Button exportBtn = new Button("⬇ Export CSV");
        exportBtn.getStyleClass().add("btn-secondary");
        exportBtn.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.setTitle("Save Result CSV");
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files","*.csv"));
            fc.setInitialFileName("result_attempt_" + attempt.id() + ".csv");
            File file = fc.showSaveDialog(stage);
            if (file != null) {
                try { ctrl.exportCsv(attempt, attempt.examId(), file); }
                catch (Exception ex) {
                    new Alert(Alert.AlertType.ERROR, "Export failed: " + ex.getMessage(), ButtonType.OK).showAndWait();
                }
            }
        });

        Button backBtn = new Button("← Back to Dashboard");
        backBtn.getStyleClass().add("btn-secondary");
        backBtn.setOnAction(e -> {
            Scene dash = StudentDashboardView.build(stage, student, AppServices.auth());
            stage.setScene(dash); stage.setTitle("Student Dashboard — ExamPortal");
        });

        HBox btnRow = new HBox(12, backBtn, exportBtn);
        btnRow.setAlignment(Pos.CENTER_LEFT);
        content.getChildren().add(btnRow);

        scroll.setContent(content);
        root.setCenter(scroll);

        Scene scene = new Scene(root, 1280, 780);
        scene.getStylesheets().add(ResultView.class.getResource("/css/app.css").toExternalForm());
        return scene;
    }

    private static HBox buildTopBar(Stage stage, User student) {
        Label title = new Label("Exam Result"); title.getStyleClass().add("topbar-title");
        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
        Label userLbl = new Label("👤 " + student.fullName()); userLbl.getStyleClass().add("topbar-user");
        HBox bar = new HBox(16, title, spacer, userLbl);
        bar.getStyleClass().add("topbar");
        bar.setPadding(new Insets(0, 24, 0, 24));
        bar.setAlignment(Pos.CENTER_LEFT);
        return bar;
    }

    private static HBox buildScoreSection(Attempt attempt, Exam exam) {
        // Score circle
        double pct = attempt.percentage() != null ? attempt.percentage() : 0;
        boolean passed = Boolean.TRUE.equals(attempt.passed());

        StackPane circle = new StackPane();
        Circle bg  = new Circle(70); bg.setFill(Color.web("#F1F5F9"));
        Circle ring = new Circle(70); ring.setFill(Color.TRANSPARENT);
        ring.setStroke(passed ? Color.web("#16A34A") : Color.web("#DC2626"));
        ring.setStrokeWidth(8);
        Label pctLbl = new Label(String.format("%.1f%%", pct)); pctLbl.setStyle("-fx-font-size:22;-fx-font-weight:bold;");
        circle.getChildren().addAll(bg, ring, pctLbl);

        // Badge
        Label badge = new Label(passed ? "✅  PASS" : "❌  FAIL");
        badge.getStyleClass().add(passed ? "badge-success" : "badge-danger");
        badge.setStyle((passed ? "-fx-background-color:#DCFCE7;-fx-text-fill:#16A34A;" :
                "-fx-background-color:#FEE2E2;-fx-text-fill:#DC2626;") +
                "-fx-font-size:18;-fx-font-weight:bold;-fx-padding:8 20;-fx-background-radius:8;");

        // Metadata
        String scoreTxt = attempt.score() != null ? String.format("%.1f", attempt.score()) : "-";
        String totalTxt = exam  != null ? String.valueOf(exam.totalMarks()) : "-";
        VBox meta = new VBox(8,
            badge,
            new Label("Score: " + scoreTxt + " / " + totalTxt),
            new Label("Status: " + attempt.status().name()),
            new Label("Started: " + DateUtil.format(attempt.startedAt())),
            new Label("Submitted: " + DateUtil.format(attempt.submittedAt()))
        );
        meta.getChildren().forEach(n -> { if (n instanceof Label l) l.getStyleClass().add("muted-text"); });
        ((Label)meta.getChildren().get(0)).getStyleClass().clear(); // badge keeps its own style

        HBox section = new HBox(40, circle, meta);
        section.setAlignment(Pos.CENTER_LEFT);
        section.setPadding(new Insets(8));
        return section;
    }

    @SuppressWarnings("unchecked")
    private static VBox buildBreakdown(List<Question> questions, List<Response> responses) {
        Map<Long, Response> respMap = responses.stream()
                .collect(Collectors.toMap(Response::questionId, Function.identity()));

        VBox box = new VBox(12);
        Label heading = new Label("Per-Question Breakdown"); heading.getStyleClass().add("panel-heading");

        TableView<Question> table = new TableView<>();
        table.getStyleClass().add("data-table");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPrefHeight(300);

        TableColumn<Question,String> colNum = new TableColumn<>("#");
        colNum.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(questions.indexOf(c.getValue()) + 1)));
        colNum.setMaxWidth(50);
        TableColumn<Question,String> colQ = new TableColumn<>("Question");
        colQ.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().body()));
        TableColumn<Question,String> colYour = new TableColumn<>("Your Answer");
        colYour.setCellValueFactory(c -> {
            Response r = respMap.get(c.getValue().id());
            return new SimpleStringProperty(r != null && r.chosenOption() != null ? String.valueOf(r.chosenOption()) : "-");
        });
        TableColumn<Question,String> colCorrect = new TableColumn<>("Correct Answer");
        colCorrect.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().correctOption())));
        TableColumn<Question,String> colMarks = new TableColumn<>("Marks");
        colMarks.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().marks())));
        TableColumn<Question,String> colEarned = new TableColumn<>("Earned");
        colEarned.setCellValueFactory(c -> {
            Response r = respMap.get(c.getValue().id());
            boolean correct = r != null && Boolean.TRUE.equals(r.isCorrect());
            return new SimpleStringProperty(correct ? String.valueOf(c.getValue().marks()) : "0");
        });
        TableColumn<Question,String> colResult = new TableColumn<>("Result");
        colResult.setCellValueFactory(c -> {
            Response r = respMap.get(c.getValue().id());
            boolean correct = r != null && Boolean.TRUE.equals(r.isCorrect());
            return new SimpleStringProperty(correct ? "✅ Correct" : "❌ Wrong");
        });

        table.getColumns().addAll(colNum, colQ, colYour, colCorrect, colMarks, colEarned, colResult);
        table.setItems(FXCollections.observableArrayList(questions));
        box.getChildren().addAll(heading, table);
        return box;
    }

    private static VBox buildProctorSummary(Map<String, Long> summary, long criticalCount) {
        VBox box = new VBox(12);
        Label heading = new Label("Proctoring Summary"); heading.getStyleClass().add("panel-heading");
        Label critLbl = new Label("⚠ Critical Events: " + criticalCount);
        critLbl.setStyle(criticalCount > 0
            ? "-fx-text-fill:#DC2626;-fx-font-weight:bold;"
            : "-fx-text-fill:#16A34A;");

        GridPane grid = new GridPane(); grid.setHgap(24); grid.setVgap(8);
        int row = 0;
        for (Map.Entry<String, Long> entry : summary.entrySet()) {
            Label key = new Label(entry.getKey().replace("_", " "));
            key.getStyleClass().add("muted-text");
            Label val = new Label(String.valueOf(entry.getValue()));
            val.setStyle("-fx-font-weight:bold;");
            grid.addRow(row++, key, val);
        }
        box.getChildren().addAll(heading, critLbl, grid);
        return box;
    }
}
