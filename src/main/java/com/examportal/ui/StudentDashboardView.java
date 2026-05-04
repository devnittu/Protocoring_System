package com.examportal.ui;

import com.examportal.AppServices;
import com.examportal.controller.StudentController;
import com.examportal.model.Attempt;
import com.examportal.model.Exam;
import com.examportal.model.User;
import com.examportal.service.AuthService;
import com.examportal.util.DateUtil;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.*;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.util.List;

/** Premium dark student dashboard with welcome hero, exam cards, and history table. */
public class StudentDashboardView {

    public static Scene build(Stage stage, User student, AuthService authService) {
        StudentController ctrl = new StudentController(AppServices.exam(), AppServices.result());

        BorderPane root = new BorderPane();
        root.getStyleClass().add("dashboard-root");
        root.setTop(buildTopBar(stage, student, authService));

        // Sidebar
        VBox sidebar = buildSidebar();
        root.setLeft(sidebar);

        StackPane content = new StackPane();
        content.getStyleClass().add("content-area");
        root.setCenter(content);

        // Default panel
        content.getChildren().setAll(buildExamsPanel(stage, ctrl, student));

        // Sidebar wiring
        Button examBtn    = (Button) sidebar.getChildren().get(1);
        Button historyBtn = (Button) sidebar.getChildren().get(2);

        examBtn.setOnAction(e -> {
            clearActive(sidebar); examBtn.getStyleClass().add("sidebar-active");
            content.getChildren().setAll(buildExamsPanel(stage, ctrl, student));
        });
        historyBtn.setOnAction(e -> {
            clearActive(sidebar); historyBtn.getStyleClass().add("sidebar-active");
            content.getChildren().setAll(buildHistoryPanel(stage, ctrl, student));
        });

        Scene scene = new Scene(root, 1280, 780);
        scene.getStylesheets().add(StudentDashboardView.class.getResource("/css/app.css").toExternalForm());
        return scene;
    }

    private static HBox buildTopBar(Stage stage, User student, AuthService authService) {
        Label logo  = new Label("EP"); logo.setStyle("-fx-background-color:#2563EB;-fx-text-fill:white;-fx-font-weight:bold;-fx-font-size:14;-fx-background-radius:8;-fx-padding:4 10;");
        Label title = new Label("ExamPortal"); title.getStyleClass().add("topbar-title");
        HBox brand  = new HBox(10, logo, title); brand.setAlignment(Pos.CENTER_LEFT);

        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);

        Label userLbl  = new Label("👤  " + student.fullName()); userLbl.getStyleClass().add("topbar-user");
        Label roleLbl  = new Label("STUDENT"); roleLbl.getStyleClass().add("badge-blue");

        Button logoutBtn = new Button("Sign Out"); logoutBtn.getStyleClass().add("btn-danger-sm");
        logoutBtn.setOnAction(e -> { authService.logout(); com.examportal.App.showLogin(stage); });

        HBox bar = new HBox(16, brand, spacer, roleLbl, userLbl, logoutBtn);
        bar.getStyleClass().add("topbar");
        bar.setPadding(new Insets(0, 24, 0, 24));
        bar.setAlignment(Pos.CENTER_LEFT);
        return bar;
    }

    private static VBox buildSidebar() {
        VBox sb = new VBox(2);
        sb.getStyleClass().add("sidebar");
        sb.setPadding(new Insets(20, 0, 20, 0));

        Label nav = new Label("NAVIGATION"); nav.getStyleClass().add("sidebar-section-label");

        Button examBtn    = sidebarBtn("📝   Available Exams");
        Button historyBtn = sidebarBtn("📊   My Results");
        examBtn.getStyleClass().add("sidebar-active");

        sb.getChildren().addAll(nav, examBtn, historyBtn);
        return sb;
    }

    private static Button sidebarBtn(String label) {
        Button b = new Button(label);
        b.getStyleClass().add("sidebar-item");
        b.setMaxWidth(Double.MAX_VALUE);
        return b;
    }

    private static void clearActive(VBox sidebar) {
        sidebar.getChildren().forEach(n -> n.getStyleClass().remove("sidebar-active"));
    }

    // ── Available Exams ───────────────────────────────────────────────────────
    private static VBox buildExamsPanel(Stage stage, StudentController ctrl, User student) {
        VBox box = new VBox(24); box.setPadding(new Insets(36)); box.getStyleClass().add("panel");

        // Welcome hero
        VBox hero = new VBox(6);
        Label greet = new Label("Welcome back, " + student.fullName() + " 👋");
        greet.getStyleClass().add("welcome-title");
        Label sub = new Label("Choose an exam below to get started. Good luck!");
        sub.getStyleClass().add("welcome-sub");
        hero.getChildren().addAll(greet, sub);

        // Stat strip
        List<Attempt> attempts = AppServices.result().getAttemptsByStudent(student.id());
        long done = attempts.stream().filter(a -> a.status() != Attempt.Status.IN_PROGRESS).count();
        long passed = attempts.stream().filter(a -> Boolean.TRUE.equals(a.passed())).count();

        HBox stats = new HBox(16,
            miniStat("Total Attempts",  String.valueOf(done),   "#3B82F6"),
            miniStat("Passed",          String.valueOf(passed), "#22C55E"),
            miniStat("Available Exams", String.valueOf(ctrl.getAvailableExams().size()), "#A78BFA")
        );

        // Exam cards grid
        List<Exam> exams = ctrl.getAvailableExams();
        Label heading = new Label("📝  Available Exams");
        heading.setStyle("-fx-font-size:16;-fx-font-weight:bold;-fx-text-fill:#94A3B8;");

        FlowPane grid = new FlowPane(20, 20);
        grid.setPadding(new Insets(4, 0, 0, 0));

        if (exams.isEmpty()) {
            Label empty = new Label("No exams are currently available. Check back later.");
            empty.getStyleClass().add("muted-text");
            empty.setStyle("-fx-font-size:15;-fx-text-fill:#475569;-fx-padding:40;");
            grid.getChildren().add(empty);
        } else {
            for (Exam exam : exams) grid.getChildren().add(buildExamCard(stage, exam, student));
        }

        ScrollPane scroll = new ScrollPane(grid);
        scroll.setFitToWidth(true); scroll.getStyleClass().add("transparent-scroll");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        box.getChildren().addAll(hero, stats, new Region() {{ setPrefHeight(4); }}, heading, scroll);
        return box;
    }

    private static HBox miniStat(String label, String value, String color) {
        Label val = new Label(value);
        val.setStyle("-fx-font-size:26;-fx-font-weight:bold;-fx-text-fill:" + color + ";");
        Label lbl = new Label(label);
        lbl.setStyle("-fx-font-size:12;-fx-text-fill:#475569;-fx-font-weight:bold;");
        VBox box = new VBox(2, val, lbl);
        box.setPadding(new Insets(16, 24, 16, 24));
        box.setStyle("-fx-background-color:#1E293B;-fx-background-radius:12;-fx-border-color:#1E3A5F;-fx-border-radius:12;-fx-border-width:1;");
        return new HBox(box);
    }

    private static VBox buildExamCard(Stage stage, Exam exam, User student) {
        VBox card = new VBox(14);
        card.getStyleClass().add("exam-card");
        card.setPadding(new Insets(24));
        card.setPrefWidth(320);

        // Header row
        Label titleLbl  = new Label(exam.title()); titleLbl.getStyleClass().add("exam-card-title"); titleLbl.setWrapText(true);
        Label available = new Label("● Available"); available.getStyleClass().add("badge-success");

        // Meta pills
        HBox meta = new HBox(10,
            pill("⏱ " + exam.durationMinutes() + " min"),
            pill("📊 " + exam.totalMarks() + " marks"),
            pill("✅ " + (int) exam.passPercentage() + "% to pass")
        );

        Label dateLbl = new Label("📅 Opens: " + DateUtil.format(exam.startTime()));
        dateLbl.getStyleClass().add("muted-text");

        Region spacer = new Region(); VBox.setVgrow(spacer, Priority.ALWAYS);

        Button startBtn = new Button("Start Exam  →");
        startBtn.getStyleClass().add("btn-primary"); startBtn.setMaxWidth(Double.MAX_VALUE);
        startBtn.setOnAction(e -> {
            Scene examScene = ExamView.build(stage, exam, student);
            stage.setScene(examScene);
            stage.setTitle("Exam: " + exam.title() + " — ExamPortal");
        });

        card.getChildren().addAll(available, titleLbl, meta, dateLbl, spacer, startBtn);
        return card;
    }

    private static Label pill(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-background-color:#0F172A;-fx-text-fill:#64748B;-fx-font-size:11;-fx-font-weight:bold;-fx-padding:4 10;-fx-background-radius:20;-fx-border-color:#1E3A5F;-fx-border-radius:20;-fx-border-width:1;");
        return l;
    }

    // ── History Panel ─────────────────────────────────────────────────────────
    @SuppressWarnings("unchecked")
    private static VBox buildHistoryPanel(Stage stage, StudentController ctrl, User student) {
        VBox box = new VBox(16); box.setPadding(new Insets(36)); box.getStyleClass().add("panel");

        Label heading = new Label("My Exam Results"); heading.getStyleClass().add("panel-heading");
        Label sub = new Label("Your complete attempt history");
        sub.getStyleClass().add("muted-text");

        TableView<Attempt> table = new TableView<>();
        table.getStyleClass().add("data-table");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPlaceholder(new Label("No attempts yet.") {{ setStyle("-fx-text-fill:#475569;"); }});

        TableColumn<Attempt,String> colExam = col("Exam ID", c -> new SimpleStringProperty(String.valueOf(c.getValue().examId())));
        TableColumn<Attempt,String> colDate = col("Date", c -> new SimpleStringProperty(DateUtil.format(c.getValue().startedAt())));
        TableColumn<Attempt,String> colScore = col("Score", c ->
            new SimpleStringProperty(c.getValue().score() != null ? String.format("%.1f", c.getValue().score()) : "—"));
        TableColumn<Attempt,String> colPct = col("Percentage", c ->
            new SimpleStringProperty(c.getValue().percentage() != null ? String.format("%.1f%%", c.getValue().percentage()) : "—"));
        TableColumn<Attempt,String> colResult = col("Result", c -> {
            if (c.getValue().passed() == null) return new SimpleStringProperty("—");
            return new SimpleStringProperty(c.getValue().passed() ? "✅  PASS" : "❌  FAIL");
        });
        TableColumn<Attempt,String> colStatus = col("Status", c -> new SimpleStringProperty(c.getValue().status().name()));

        table.getColumns().addAll(colExam, colDate, colScore, colPct, colResult, colStatus);
        table.setItems(FXCollections.observableArrayList(ctrl.getMyAttempts(student.id())));
        VBox.setVgrow(table, Priority.ALWAYS);

        Button viewBtn = new Button("📄  View Detailed Result"); viewBtn.getStyleClass().add("btn-primary");
        viewBtn.setOnAction(e -> {
            Attempt sel = table.getSelectionModel().getSelectedItem();
            if (sel != null && sel.status() != Attempt.Status.IN_PROGRESS) {
                stage.setScene(ResultView.build(stage, sel, student));
                stage.setTitle("Result — ExamPortal");
            }
        });

        box.getChildren().addAll(heading, sub, new HBox(10, viewBtn), table);
        return box;
    }

    private static TableColumn<Attempt,String> col(String title, javafx.util.Callback<TableColumn.CellDataFeatures<Attempt,String>, javafx.beans.value.ObservableValue<String>> factory) {
        TableColumn<Attempt,String> c = new TableColumn<>(title);
        c.setCellValueFactory(factory);
        return c;
    }
}
