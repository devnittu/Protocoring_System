package com.examportal.ui;

import com.examportal.controller.LoginController;
import com.examportal.service.AuthException;
import javafx.animation.*;
import javafx.geometry.*;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.*;
import javafx.scene.shape.*;
import javafx.util.Duration;

/**
 * Premium split-screen login — left branded hero panel, right dark form card.
 */
public class LoginView {

    public static Scene build(LoginController controller) {
        HBox root = new HBox();
        root.getStyleClass().add("login-root");

        // ── LEFT: Hero Panel ─────────────────────────────────────────────────
        VBox hero = buildHeroPanel();
        HBox.setHgrow(hero, Priority.SOMETIMES);

        // ── RIGHT: Form Panel ─────────────────────────────────────────────────
        StackPane formPanel = new StackPane();
        formPanel.getStyleClass().add("login-form-panel");
        HBox.setHgrow(formPanel, Priority.ALWAYS);

        VBox card = buildFormCard(controller);
        StackPane.setAlignment(card, Pos.CENTER);
        formPanel.getChildren().add(card);

        root.getChildren().addAll(hero, formPanel);

        // Entrance animation on card
        card.setOpacity(0);
        card.setTranslateY(20);
        FadeTransition fade = new FadeTransition(Duration.millis(600), card);
        fade.setFromValue(0); fade.setToValue(1);
        TranslateTransition slide = new TranslateTransition(Duration.millis(600), card);
        slide.setFromY(20); slide.setToY(0);
        slide.setInterpolator(Interpolator.EASE_OUT);
        new ParallelTransition(fade, slide).play();

        Scene scene = new Scene(root, 1200, 760);
        scene.getStylesheets().add(LoginView.class.getResource("/css/app.css").toExternalForm());
        return scene;
    }

    // ── Hero Panel ────────────────────────────────────────────────────────────
    private static VBox buildHeroPanel() {
        VBox hero = new VBox(28);
        hero.getStyleClass().add("login-hero");
        hero.setAlignment(Pos.CENTER);
        hero.setPadding(new Insets(60));
        hero.setPrefWidth(460);
        hero.setMaxWidth(460);

        Label icon  = new Label("🎓"); icon.getStyleClass().add("login-hero-icon");
        Label title = new Label("ExamPortal"); title.getStyleClass().add("login-hero-title");
        Label sub   = new Label("The most secure way to\nconduct online examinations.");
        sub.getStyleClass().add("login-hero-sub");
        sub.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

        // Feature list
        VBox features = new VBox(14,
            featureRow("🔒", "BCrypt-secured student accounts"),
            featureRow("📷", "AI webcam proctoring with face detection"),
            featureRow("⏱", "Live countdown with auto-submission"),
            featureRow("📊", "Instant scored results & CSV export"),
            featureRow("🛡", "Keyboard lockdown & tab-switch detection")
        );
        features.setAlignment(Pos.CENTER_LEFT);

        // Decorative divider
        Separator divider = new Separator();
        divider.setStyle("-fx-background-color: rgba(255,255,255,0.2);");

        // Bottom badge
        Label versionBadge = new Label("v1.0 · Java 21 · JavaFX 21 · MySQL 8");
        versionBadge.setStyle("-fx-font-size:11;-fx-text-fill:rgba(255,255,255,0.4);");

        hero.getChildren().addAll(icon, title, sub, divider, features, versionBadge);
        return hero;
    }

    private static HBox featureRow(String emoji, String text) {
        Label em  = new Label(emoji); em.setStyle("-fx-font-size:16;");
        Label lbl = new Label(text);  lbl.getStyleClass().add("login-feature-item");
        HBox row  = new HBox(12, em, lbl);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    // ── Form Card ─────────────────────────────────────────────────────────────
    private static VBox buildFormCard(LoginController controller) {
        VBox card = new VBox(20);
        card.getStyleClass().add("login-card");
        card.setMaxWidth(400);
        card.setPadding(new Insets(44));
        card.setAlignment(Pos.TOP_LEFT);

        // Brand
        Label logo  = new Label("EP"); logo.getStyleClass().add("login-logo-badge");
        Label brand = new Label("ExamPortal"); brand.getStyleClass().add("login-brand");
        HBox brandRow = new HBox(14, logo, brand);
        brandRow.setAlignment(Pos.CENTER_LEFT);

        Label signInTitle = new Label("Sign in to your account");
        signInTitle.getStyleClass().add("login-section-title");
        Label signInSub = new Label("Enter your credentials below");
        signInSub.getStyleClass().add("login-section-sub");

        // Error banner
        Label errorLabel = new Label();
        errorLabel.getStyleClass().add("error-banner");
        errorLabel.setVisible(false); errorLabel.setManaged(false);
        errorLabel.setWrapText(true); errorLabel.setMaxWidth(Double.MAX_VALUE);

        // Email
        Label emailLbl = new Label("Email Address"); emailLbl.getStyleClass().add("field-label");
        TextField emailField = new TextField();
        emailField.setId("login-email");
        emailField.setPromptText("admin@exam.local");
        emailField.getStyleClass().add("field-input");
        emailField.setMaxWidth(Double.MAX_VALUE);

        // Password
        Label pwLbl = new Label("Password"); pwLbl.getStyleClass().add("field-label");
        PasswordField pwField = new PasswordField();
        pwField.setId("login-password");
        pwField.setPromptText("Enter your password");
        pwField.getStyleClass().add("field-input");

        TextField pwVisible = new TextField();
        pwVisible.getStyleClass().add("field-input");
        pwVisible.setVisible(false); pwVisible.setManaged(false);
        pwVisible.textProperty().bindBidirectional(pwField.textProperty());

        Button togglePw = new Button("Show");
        togglePw.getStyleClass().add("btn-link");
        togglePw.setCursor(Cursor.HAND);
        togglePw.setOnAction(e -> {
            boolean s = pwVisible.isVisible();
            pwVisible.setVisible(!s); pwVisible.setManaged(!s);
            pwField.setVisible(s); pwField.setManaged(s);
            togglePw.setText(s ? "Show" : "Hide");
        });

        StackPane pwStack = new StackPane(pwField, pwVisible);
        HBox.setHgrow(pwStack, Priority.ALWAYS);
        HBox pwRow = new HBox(8, pwStack, togglePw);
        pwRow.setAlignment(Pos.CENTER_LEFT);

        // Forgot password label (cosmetic)
        Label forgotLbl = new Label("Forgot password?");
        forgotLbl.getStyleClass().add("btn-link");
        forgotLbl.setCursor(Cursor.HAND);
        HBox forgotRow = new HBox(); forgotRow.setAlignment(Pos.CENTER_RIGHT);
        forgotRow.getChildren().add(forgotLbl);

        // Sign In button
        Button loginBtn = new Button("Sign In  →");
        loginBtn.setId("login-btn");
        loginBtn.getStyleClass().add("btn-primary");
        loginBtn.setMaxWidth(Double.MAX_VALUE);

        // Divider
        HBox divRow = new HBox(10);
        divRow.setAlignment(Pos.CENTER);
        Region d1 = new Region(); Region d2 = new Region();
        HBox.setHgrow(d1, Priority.ALWAYS); HBox.setHgrow(d2, Priority.ALWAYS);
        d1.setStyle("-fx-background-color:#1E3A5F;-fx-pref-height:1;");
        d2.setStyle("-fx-background-color:#1E3A5F;-fx-pref-height:1;");
        Label orLbl = new Label("OR"); orLbl.getStyleClass().add("muted-text");
        divRow.getChildren().addAll(d1, orLbl, d2);

        // Register link
        Label registerLink = new Label("New student? Create an account →");
        registerLink.getStyleClass().add("btn-link");
        registerLink.setCursor(Cursor.HAND);
        registerLink.setOnMouseClicked(e -> showRegisterDialog(controller));

        // Login action
        Runnable doLogin = () -> {
            errorLabel.setVisible(false); errorLabel.setManaged(false);
            loginBtn.setDisable(true); loginBtn.setText("Signing in…");
            try {
                String pw = pwField.isVisible() ? pwField.getText() : pwVisible.getText();
                controller.login(emailField.getText().trim(), pw);
            } catch (AuthException ex) {
                showError(errorLabel, ex.getMessage());
            } catch (Exception ex) {
                showError(errorLabel, "Unexpected error: " + ex.getMessage());
            } finally {
                loginBtn.setDisable(false); loginBtn.setText("Sign In  →");
            }
        };
        loginBtn.setOnAction(e -> doLogin.run());
        pwField.setOnAction(e -> doLogin.run());
        pwVisible.setOnAction(e -> doLogin.run());

        card.getChildren().addAll(
            brandRow, new Separator() {{ setStyle("-fx-opacity:0.3;"); }},
            signInTitle, signInSub,
            errorLabel,
            emailLbl, emailField,
            pwLbl, pwRow, forgotRow,
            loginBtn, divRow, registerLink
        );
        return card;
    }

    private static void showError(Label label, String msg) {
        label.setText("⚠  " + msg);
        label.setVisible(true); label.setManaged(true);
        FadeTransition ft = new FadeTransition(Duration.millis(300), label);
        ft.setFromValue(0); ft.setToValue(1); ft.play();
    }

    private static void showRegisterDialog(LoginController controller) {
        Dialog<Void> dlg = new Dialog<>();
        dlg.setTitle("Create Student Account");
        dlg.setHeaderText("New Student Registration");

        TextField nameF  = new TextField(); nameF.setPromptText("Full Name");
        TextField emailF = new TextField(); emailF.setPromptText("Email Address");
        PasswordField pwF = new PasswordField(); pwF.setPromptText("Min 8 chars, 1 uppercase, 1 digit");
        Label errL = new Label(); errL.setStyle("-fx-text-fill:#FCA5A5;-fx-font-size:12;");

        GridPane g = new GridPane(); g.setHgap(12); g.setVgap(10); g.setPadding(new Insets(16));
        g.addRow(0, new Label("Full Name"), nameF);
        g.addRow(1, new Label("Email"), emailF);
        g.addRow(2, new Label("Password"), pwF);
        g.add(errL, 0, 3, 2, 1);
        GridPane.setHgrow(nameF, Priority.ALWAYS);
        GridPane.setHgrow(emailF, Priority.ALWAYS);
        GridPane.setHgrow(pwF, Priority.ALWAYS);
        dlg.getDialogPane().setContent(g);
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        Button okBtn = (Button) dlg.getDialogPane().lookupButton(ButtonType.OK);
        okBtn.setText("Create Account");
        okBtn.setOnAction(e -> {
            try { controller.register(emailF.getText(), nameF.getText(), pwF.getText()); dlg.close(); }
            catch (Exception ex) { errL.setText(ex.getMessage()); e.consume(); }
        });
        dlg.showAndWait();
    }
}
