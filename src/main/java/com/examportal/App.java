package com.examportal;

import com.examportal.config.DatabaseConfig;
import com.examportal.controller.LoginController;
import com.examportal.dao.*;
import com.examportal.service.*;
import com.examportal.ui.LoginView;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** JavaFX Application entry point — initialises the DB pool and shows the login screen. */
public class App extends Application {

    private static final Logger log = LoggerFactory.getLogger(App.class);
    private static Stage primaryStage;

    @Override
    public void start(Stage stage) {
        primaryStage = stage;
        stage.setTitle("ExamPortal — Secure Online Examination System");
        stage.setMinWidth(1100);
        stage.setMinHeight(720);
        stage.setOnCloseRequest(e -> shutdown());

        try {
            DatabaseConfig.initialise();
        } catch (Exception ex) {
            log.error("Failed to initialise database", ex);
            showError(stage, "Database connection failed:\n" + ex.getMessage());
            return;
        }

        showLogin(stage);
        stage.show();
    }

    /** Shows the login screen. Static so controllers can call it for logout. */
    public static void showLogin(Stage stage) {
        // Wire dependencies
        IUserDAO     userDAO     = new UserDAO();
        IExamDAO     examDAO     = new ExamDAO();
        IQuestionDAO questionDAO = new QuestionDAO();
        IAttemptDAO  attemptDAO  = new AttemptDAO();
        IResponseDAO responseDAO = new ResponseDAO();
        IActivityLogDAO logDAO   = new ActivityLogDAO();

        AuthService     authService     = new AuthService(userDAO);
        ExamService     examService     = new ExamService(examDAO, questionDAO, attemptDAO);
        EvaluatorService evaluatorService = new EvaluatorService(attemptDAO, responseDAO);
        ResultService   resultService   = new ResultService(attemptDAO, responseDAO, logDAO);
        ProctorService  proctorService  = new ProctorService(logDAO);

        LoginController loginController = new LoginController(authService, stage);

        Scene scene = LoginView.build(loginController);
        stage.setScene(scene);
        stage.setTitle("Login — ExamPortal");

        // Store services in app-level state for view wiring
        AppServices.init(authService, examService, evaluatorService, resultService, proctorService);
    }

    private void showError(Stage stage, String message) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                javafx.scene.control.Alert.AlertType.ERROR, message,
                javafx.scene.control.ButtonType.OK);
        alert.setTitle("Startup Error");
        alert.showAndWait();
        Platform.exit();
    }

    private void shutdown() {
        log.info("Shutting down application...");
        SessionManager.clear();
        DatabaseConfig.shutdown();
        Platform.exit();
    }

    public static Stage getPrimaryStage() { return primaryStage; }

    public static void main(String[] args) {
        launch(args);
    }
}
