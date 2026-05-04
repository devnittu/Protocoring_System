package com.examportal.controller;

import com.examportal.model.User;
import com.examportal.service.AuthService;
import com.examportal.service.SessionManager;
import com.examportal.ui.AdminDashboardView;
import com.examportal.ui.StudentDashboardView;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Thin controller delegating login and registration to AuthService, then routing to the correct dashboard. */
public class LoginController {

    private static final Logger log = LoggerFactory.getLogger(LoginController.class);

    private final AuthService authService;
    private final Stage       primaryStage;

    public LoginController(AuthService authService, Stage primaryStage) {
        this.authService  = authService;
        this.primaryStage = primaryStage;
    }

    /**
     * Attempts to authenticate the user and navigates to the appropriate dashboard.
     *
     * @param email     raw email string from form
     * @param password  raw password string from form
     * @throws com.examportal.service.AuthException on failure
     */
    public void login(String email, String password) {
        User user = authService.login(email, password);
        log.info("Login success for {} [{}]", user.email(), user.role());
        navigateToDashboard(user);
    }

    /**
     * Registers a new student and automatically logs them in.
     */
    public void register(String email, String fullName, String password) {
        authService.register(email, fullName, password);
        User user = authService.login(email, password);
        navigateToDashboard(user);
    }

    /** Logs out the current user and returns to the login screen. */
    public void logout() {
        authService.logout();
        com.examportal.App.showLogin(primaryStage);
    }

    private void navigateToDashboard(User user) {
        if (user.isAdmin()) {
            Scene scene = AdminDashboardView.build(primaryStage, authService);
            primaryStage.setScene(scene);
            primaryStage.setTitle("Admin Dashboard — ExamPortal");
        } else {
            Scene scene = StudentDashboardView.build(primaryStage, user, authService);
            primaryStage.setScene(scene);
            primaryStage.setTitle("Student Dashboard — ExamPortal");
        }
    }
}
