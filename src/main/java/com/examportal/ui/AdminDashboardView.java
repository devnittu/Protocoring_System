package com.examportal.ui;

import com.examportal.AppServices;
import com.examportal.controller.AdminController;
import com.examportal.model.Exam;
import com.examportal.model.Question;
import com.examportal.model.User;
import com.examportal.service.AuthService;
import com.examportal.util.DateUtil;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/** Admin dashboard with sidebar navigation, stat cards, and CRUD panels. */
public class AdminDashboardView {

    public static Scene build(Stage stage, AuthService authService) {
        AdminController admin = new AdminController(
                AppServices.exam(), authService, AppServices.result());

        // ── Layout ───────────────────────────────────────────────────────────
        BorderPane root = new BorderPane();
        root.getStyleClass().add("dashboard-root");

        // Top bar
        HBox topBar = buildTopBar(stage, authService, admin);
        root.setTop(topBar);

        // Sidebar
        VBox sidebar = buildSidebar();
        root.setLeft(sidebar);

        // Content area (swapped by sidebar nav)
        StackPane content = new StackPane();
        content.getStyleClass().add("content-area");
        root.setCenter(content);

        // Default view: overview
        content.getChildren().setAll(buildOverview(admin));

        // Sidebar nav wiring
        for (javafx.scene.Node node : sidebar.getChildren()) {
            if (node instanceof Button btn) {
                btn.setOnAction(e -> {
                    sidebar.getChildren().forEach(n -> n.getStyleClass().remove("sidebar-active"));
                    btn.getStyleClass().add("sidebar-active");
                    switch (btn.getText()) {
                        case "📊  Overview"   -> content.getChildren().setAll(buildOverview(admin));
                        case "📝  Exams"      -> content.getChildren().setAll(buildExamsPanel(admin));
                        case "❓  Questions"  -> content.getChildren().setAll(buildQuestionsPanel(admin));
                        case "👥  Students"   -> content.getChildren().setAll(buildStudentsPanel(admin));
                        case "📋  Logs"       -> content.getChildren().setAll(buildLogsPanel());
                    }
                });
            }
        }

        Scene scene = new Scene(root, 1280, 780);
        scene.getStylesheets().add(AdminDashboardView.class.getResource("/css/app.css").toExternalForm());
        return scene;
    }

    private static HBox buildTopBar(Stage stage, AuthService authService, AdminController admin) {
        Label title = new Label("ExamPortal Admin");
        title.getStyleClass().add("topbar-title");

        User user = com.examportal.service.SessionManager.getCurrentUser();
        Label userLabel = new Label("👤 " + (user != null ? user.fullName() : "Admin"));
        userLabel.getStyleClass().add("topbar-user");

        Button logoutBtn = new Button("Logout");
        logoutBtn.getStyleClass().add("btn-danger-sm");
        logoutBtn.setOnAction(e -> {
            authService.logout();
            com.examportal.App.showLogin(stage);
            stage.setScene(stage.getScene());
        });

        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox bar = new HBox(16, title, spacer, userLabel, logoutBtn);
        bar.getStyleClass().add("topbar");
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(0, 24, 0, 24));
        return bar;
    }

    private static VBox buildSidebar() {
        VBox sb = new VBox(4);
        sb.getStyleClass().add("sidebar");
        sb.setPadding(new Insets(24, 0, 24, 0));

        String[] items = {"📊  Overview","📝  Exams","❓  Questions","👥  Students","📋  Logs"};
        for (String item : items) {
            Button btn = new Button(item);
            btn.getStyleClass().add("sidebar-item");
            btn.setMaxWidth(Double.MAX_VALUE);
            sb.getChildren().add(btn);
        }
        // Activate first
        if (!sb.getChildren().isEmpty())
            sb.getChildren().get(0).getStyleClass().add("sidebar-active");
        return sb;
    }

    // ── Overview ─────────────────────────────────────────────────────────────
    private static VBox buildOverview(AdminController admin) {
        VBox box = new VBox(24);
        box.setPadding(new Insets(32));
        box.getStyleClass().add("panel");

        Label heading = new Label("Dashboard Overview");
        heading.getStyleClass().add("panel-heading");

        long totalExams    = admin.getAllExams().size();
        long totalStudents = admin.getAllStudents().size();
        long today         = admin.getAttemptsToday();
        long flagged       = admin.getFlaggedSessions();

        HBox cards = new HBox(20,
                statCard("Total Exams",    String.valueOf(totalExams),    "📝", "--primary"),
                statCard("Students",        String.valueOf(totalStudents), "👥", "--success"),
                statCard("Attempts Today",  String.valueOf(today),         "📊", "--primary"),
                statCard("Flagged Sessions",String.valueOf(flagged),       "⚠", "--danger")
        );
        cards.setAlignment(Pos.CENTER_LEFT);

        box.getChildren().addAll(heading, cards);
        return box;
    }

    private static VBox statCard(String label, String value, String icon, String colorVar) {
        Label iconL  = new Label(icon);  iconL.setStyle("-fx-font-size:28;");
        Label valL   = new Label(value); valL.getStyleClass().add("stat-value");
        Label lblL   = new Label(label); lblL.getStyleClass().add("stat-label");
        VBox card = new VBox(6, iconL, valL, lblL);
        card.getStyleClass().add("stat-card");
        card.setPadding(new Insets(24));
        card.setMinWidth(160);
        return card;
    }

    // ── Exams Panel ───────────────────────────────────────────────────────────
    @SuppressWarnings("unchecked")
    private static VBox buildExamsPanel(AdminController admin) {
        VBox box = new VBox(16); box.setPadding(new Insets(32)); box.getStyleClass().add("panel");
        Label heading = new Label("Manage Exams"); heading.getStyleClass().add("panel-heading");

        TableView<Exam> table = new TableView<>();
        table.getStyleClass().add("data-table");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<Exam,String> colTitle = new TableColumn<>("Title");
        colTitle.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().title()));
        TableColumn<Exam,String> colDur = new TableColumn<>("Duration");
        colDur.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().durationMinutes() + " min"));
        TableColumn<Exam,String> colMarks = new TableColumn<>("Total Marks");
        colMarks.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().totalMarks())));
        TableColumn<Exam,String> colPub = new TableColumn<>("Published");
        colPub.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().isPublished() ? "✅ Yes" : "❌ No"));
        TableColumn<Exam,String> colStart = new TableColumn<>("Start");
        colStart.setCellValueFactory(c -> new SimpleStringProperty(DateUtil.format(c.getValue().startTime())));

        table.getColumns().addAll(colTitle, colDur, colMarks, colPub, colStart);

        Runnable refresh = () -> table.setItems(FXCollections.observableArrayList(admin.getAllExams()));
        refresh.run();

        Button addBtn = new Button("＋ New Exam"); addBtn.getStyleClass().add("btn-primary");
        addBtn.setOnAction(e -> showExamDialog(null, admin, refresh));

        Button editBtn = new Button("✏ Edit"); editBtn.getStyleClass().add("btn-secondary");
        editBtn.setOnAction(e -> {
            Exam sel = table.getSelectionModel().getSelectedItem();
            if (sel != null) showExamDialog(sel, admin, refresh);
        });

        Button delBtn = new Button("🗑 Delete"); delBtn.getStyleClass().add("btn-danger-sm");
        delBtn.setOnAction(e -> {
            Exam sel = table.getSelectionModel().getSelectedItem();
            if (sel != null) {
                Alert conf = new Alert(Alert.AlertType.CONFIRMATION,
                        "Delete exam '" + sel.title() + "'?", ButtonType.YES, ButtonType.NO);
                conf.showAndWait().filter(r -> r == ButtonType.YES).ifPresent(r -> {
                    admin.deleteExam(sel.id()); refresh.run();
                });
            }
        });

        Button pubBtn = new Button("Toggle Publish"); pubBtn.getStyleClass().add("btn-secondary");
        pubBtn.setOnAction(e -> {
            Exam sel = table.getSelectionModel().getSelectedItem();
            if (sel != null) { admin.setPublished(sel.id(), !sel.isPublished()); refresh.run(); }
        });

        HBox toolbar = new HBox(10, addBtn, editBtn, pubBtn, delBtn);
        VBox.setVgrow(table, Priority.ALWAYS);
        box.getChildren().addAll(heading, toolbar, table);
        return box;
    }

    private static void showExamDialog(Exam existing, AdminController admin, Runnable refresh) {
        Dialog<Void> dlg = new Dialog<>();
        dlg.setTitle(existing == null ? "New Exam" : "Edit Exam");

        TextField titleF = new TextField(existing != null ? existing.title() : "");
        titleF.setPromptText("Exam title");
        TextArea descF = new TextArea(existing != null ? existing.description() : "");
        descF.setPromptText("Description"); descF.setPrefRowCount(3);
        TextField durF  = new TextField(existing != null ? String.valueOf(existing.durationMinutes()) : "60");
        TextField markF = new TextField(existing != null ? String.valueOf(existing.totalMarks()) : "100");
        TextField passF = new TextField(existing != null ? String.valueOf(existing.passPercentage()) : "40");
        CheckBox randCb = new CheckBox("Randomise Questions");
        if (existing != null) randCb.setSelected(existing.randomiseQuestions());
        Label errL = new Label(); errL.setStyle("-fx-text-fill:#DC2626;");

        GridPane grid = new GridPane(); grid.setHgap(12); grid.setVgap(10);
        grid.setPadding(new Insets(16));
        grid.addRow(0, new Label("Title:"), titleF);
        grid.addRow(1, new Label("Description:"), descF);
        grid.addRow(2, new Label("Duration (min):"), durF);
        grid.addRow(3, new Label("Total Marks:"), markF);
        grid.addRow(4, new Label("Pass %:"), passF);
        grid.addRow(5, new Label(""), randCb);
        grid.add(errL, 0, 6, 2, 1);
        dlg.getDialogPane().setContent(grid);
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        Button okBtn = (Button) dlg.getDialogPane().lookupButton(ButtonType.OK);
        okBtn.setOnAction(e -> {
            try {
                User curr = com.examportal.service.SessionManager.getCurrentUser();
                Exam exam = new Exam(
                    existing != null ? existing.id() : null,
                    titleF.getText(), descF.getText(),
                    curr != null ? curr.id() : 1L,
                    Integer.parseInt(durF.getText()),
                    Integer.parseInt(markF.getText()),
                    Double.parseDouble(passF.getText()),
                    existing != null ? existing.startTime() : LocalDateTime.now(),
                    existing != null ? existing.endTime()   : LocalDateTime.now().plusDays(30),
                    existing != null && existing.isPublished(),
                    randCb.isSelected(),
                    null
                );
                if (existing == null) admin.createExam(exam); else admin.updateExam(exam);
                refresh.run(); dlg.close();
            } catch (Exception ex) { errL.setText(ex.getMessage()); e.consume(); }
        });
        dlg.showAndWait();
    }

    // ── Questions Panel ───────────────────────────────────────────────────────
    @SuppressWarnings("unchecked")
    private static VBox buildQuestionsPanel(AdminController admin) {
        VBox box = new VBox(16); box.setPadding(new Insets(32)); box.getStyleClass().add("panel");
        Label heading = new Label("Question Bank"); heading.getStyleClass().add("panel-heading");

        ComboBox<Exam> examCombo = new ComboBox<>();
        examCombo.setPromptText("Select an exam…");
        examCombo.setItems(FXCollections.observableArrayList(admin.getAllExams()));
        examCombo.setConverter(new javafx.util.StringConverter<>() {
            public String toString(Exam e)    { return e != null ? e.title() : ""; }
            public Exam   fromString(String s) { return null; }
        });

        TableView<Question> table = new TableView<>();
        table.getStyleClass().add("data-table");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<Question,String> colPos  = new TableColumn<>("#");
        colPos.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().position())));
        colPos.setMaxWidth(50);
        TableColumn<Question,String> colBody = new TableColumn<>("Question");
        colBody.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().body()));
        TableColumn<Question,String> colDiff = new TableColumn<>("Difficulty");
        colDiff.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().difficulty().name()));
        TableColumn<Question,String> colMark = new TableColumn<>("Marks");
        colMark.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().marks())));

        table.getColumns().addAll(colPos, colBody, colDiff, colMark);

        Runnable refresh = () -> {
            Exam sel = examCombo.getValue();
            if (sel != null) table.setItems(FXCollections.observableArrayList(admin.getQuestionsForExam(sel.id())));
        };
        examCombo.setOnAction(e -> refresh.run());

        Button addBtn = new Button("＋ Add Question"); addBtn.getStyleClass().add("btn-primary");
        addBtn.setOnAction(e -> {
            Exam sel = examCombo.getValue();
            if (sel == null) { showAlert("Select an exam first."); return; }
            showQuestionDialog(null, sel.id(), admin, refresh);
        });
        Button editBtn = new Button("✏ Edit"); editBtn.getStyleClass().add("btn-secondary");
        editBtn.setOnAction(e -> {
            Question q = table.getSelectionModel().getSelectedItem();
            if (q != null) showQuestionDialog(q, q.examId(), admin, refresh);
        });
        Button delBtn = new Button("🗑 Delete"); delBtn.getStyleClass().add("btn-danger-sm");
        delBtn.setOnAction(e -> {
            Question q = table.getSelectionModel().getSelectedItem();
            if (q != null) { admin.deleteQuestion(q.id()); refresh.run(); }
        });

        HBox toolbar = new HBox(10, new Label("Exam:"), examCombo, addBtn, editBtn, delBtn);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        VBox.setVgrow(table, Priority.ALWAYS);
        box.getChildren().addAll(heading, toolbar, table);
        return box;
    }

    private static void showQuestionDialog(Question existing, Long examId,
                                           AdminController admin, Runnable refresh) {
        Dialog<Void> dlg = new Dialog<>();
        dlg.setTitle(existing == null ? "Add Question" : "Edit Question");
        dlg.getDialogPane().setPrefWidth(600);

        TextArea bodyF = new TextArea(existing != null ? existing.body() : "");
        bodyF.setPromptText("Question text"); bodyF.setPrefRowCount(3);
        TextField optA = new TextField(existing != null ? existing.optionA() : ""); optA.setPromptText("Option A");
        TextField optB = new TextField(existing != null ? existing.optionB() : ""); optB.setPromptText("Option B");
        TextField optC = new TextField(existing != null ? existing.optionC() : ""); optC.setPromptText("Option C");
        TextField optD = new TextField(existing != null ? existing.optionD() : ""); optD.setPromptText("Option D");
        ComboBox<String> correct = new ComboBox<>(FXCollections.observableArrayList("A","B","C","D"));
        if (existing != null) correct.setValue(String.valueOf(existing.correctOption()));
        TextField marksF = new TextField(existing != null ? String.valueOf(existing.marks()) : "2");
        ComboBox<String> diff = new ComboBox<>(FXCollections.observableArrayList("EASY","MEDIUM","HARD"));
        if (existing != null) diff.setValue(existing.difficulty().name()); else diff.setValue("MEDIUM");
        TextField topicF = new TextField(existing != null ? existing.topic() : ""); topicF.setPromptText("Topic");
        TextField posF   = new TextField(existing != null ? String.valueOf(existing.position()) : "1");
        Label errL = new Label(); errL.setStyle("-fx-text-fill:#DC2626;");

        GridPane grid = new GridPane(); grid.setHgap(12); grid.setVgap(10); grid.setPadding(new Insets(16));
        grid.addRow(0, new Label("Question:"), bodyF);
        grid.addRow(1, new Label("Option A:"), optA); grid.addRow(2, new Label("Option B:"), optB);
        grid.addRow(3, new Label("Option C:"), optC); grid.addRow(4, new Label("Option D:"), optD);
        grid.addRow(5, new Label("Correct:"), correct);
        grid.addRow(6, new Label("Marks:"), marksF); grid.addRow(7, new Label("Difficulty:"), diff);
        grid.addRow(8, new Label("Topic:"), topicF); grid.addRow(9, new Label("Position:"), posF);
        grid.add(errL, 0, 10, 2, 1);
        dlg.getDialogPane().setContent(grid);
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        Button okBtn = (Button) dlg.getDialogPane().lookupButton(ButtonType.OK);
        okBtn.setOnAction(e -> {
            try {
                Question q = new Question(
                    existing != null ? existing.id() : null, examId,
                    bodyF.getText(), optA.getText(), optB.getText(), optC.getText(), optD.getText(),
                    correct.getValue() != null ? correct.getValue().charAt(0) : 'A',
                    Integer.parseInt(marksF.getText()),
                    Question.Difficulty.valueOf(diff.getValue()),
                    topicF.getText(), Integer.parseInt(posF.getText())
                );
                if (existing == null) admin.addQuestion(q); else admin.updateQuestion(q);
                refresh.run(); dlg.close();
            } catch (Exception ex) { errL.setText(ex.getMessage()); e.consume(); }
        });
        dlg.showAndWait();
    }

    // ── Students Panel ────────────────────────────────────────────────────────
    @SuppressWarnings("unchecked")
    private static VBox buildStudentsPanel(AdminController admin) {
        VBox box = new VBox(16); box.setPadding(new Insets(32)); box.getStyleClass().add("panel");
        Label heading = new Label("Student Management"); heading.getStyleClass().add("panel-heading");

        TableView<User> table = new TableView<>();
        table.getStyleClass().add("data-table");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<User,String> colName  = new TableColumn<>("Full Name");
        colName.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().fullName()));
        TableColumn<User,String> colEmail = new TableColumn<>("Email");
        colEmail.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().email()));
        TableColumn<User,String> colActive = new TableColumn<>("Active");
        colActive.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().isActive() ? "✅" : "❌"));
        TableColumn<User,String> colLogin = new TableColumn<>("Last Login");
        colLogin.setCellValueFactory(c -> new SimpleStringProperty(DateUtil.format(c.getValue().lastLogin())));

        table.getColumns().addAll(colName, colEmail, colActive, colLogin);
        Runnable refresh = () -> table.setItems(FXCollections.observableArrayList(admin.getAllStudents()));
        refresh.run();

        Button toggleBtn = new Button("Toggle Active"); toggleBtn.getStyleClass().add("btn-secondary");
        toggleBtn.setOnAction(e -> {
            User sel = table.getSelectionModel().getSelectedItem();
            if (sel != null) { admin.setUserActive(sel.id(), !sel.isActive()); refresh.run(); }
        });

        HBox toolbar = new HBox(10, toggleBtn);
        VBox.setVgrow(table, Priority.ALWAYS);
        box.getChildren().addAll(heading, toolbar, table);
        return box;
    }

    // ── Logs Panel ────────────────────────────────────────────────────────────
    @SuppressWarnings("unchecked")
    private static VBox buildLogsPanel() {
        VBox box = new VBox(16); box.setPadding(new Insets(32)); box.getStyleClass().add("panel");
        Label heading = new Label("Activity Logs"); heading.getStyleClass().add("panel-heading");

        TableView<com.examportal.model.ActivityLog> table = new TableView<>();
        table.getStyleClass().add("data-table");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<com.examportal.model.ActivityLog,String> colTime = new TableColumn<>("Time");
        colTime.setCellValueFactory(c -> new SimpleStringProperty(DateUtil.format(c.getValue().loggedAt())));
        TableColumn<com.examportal.model.ActivityLog,String> colEvt = new TableColumn<>("Event");
        colEvt.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().eventType().name()));
        TableColumn<com.examportal.model.ActivityLog,String> colSev = new TableColumn<>("Severity");
        colSev.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().severity().name()));
        TableColumn<com.examportal.model.ActivityLog,String> colDet = new TableColumn<>("Detail");
        colDet.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().detail() != null ? c.getValue().detail() : ""));

        table.getColumns().addAll(colTime, colEvt, colSev, colDet);
        table.setItems(FXCollections.observableArrayList(AppServices.result().getAllLogs()));
        VBox.setVgrow(table, Priority.ALWAYS);
        box.getChildren().addAll(heading, table);
        return box;
    }

    private static void showAlert(String msg) {
        new Alert(Alert.AlertType.WARNING, msg, ButtonType.OK).showAndWait();
    }
}
