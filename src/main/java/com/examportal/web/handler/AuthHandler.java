package com.examportal.web.handler;

import com.examportal.model.User;
import com.examportal.service.AuthException;
import com.examportal.service.AuthService;
import com.examportal.web.ApiServer;
import com.examportal.web.JsonUtil;
import com.examportal.web.SessionStore;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.util.Map;

public class AuthHandler implements HttpHandler {
    private final AuthService authService;
    public AuthHandler(AuthService a) { this.authService = a; }

    @Override
    public void handle(HttpExchange ex) throws IOException {
        if (ApiServer.method(ex, "OPTIONS")) { ApiServer.cors(ex); return; }
        String path = ex.getRequestURI().getPath();

        try {
            if (path.endsWith("/login")    && ApiServer.method(ex, "POST"))  handleLogin(ex);
            else if (path.endsWith("/logout")   && ApiServer.method(ex, "POST"))  handleLogout(ex);
            else if (path.endsWith("/me")        && ApiServer.method(ex, "GET"))   handleMe(ex);
            else if (path.endsWith("/register")  && ApiServer.method(ex, "POST"))  handleRegister(ex);
            else ApiServer.error(ex, 404, "Not found");
        } catch (AuthException e) {
            ApiServer.error(ex, 401, e.getMessage());
        } catch (Exception e) {
            ApiServer.error(ex, 500, e.getMessage());
        }
    }

    private void handleLogin(HttpExchange ex) throws IOException {
        var body = JsonUtil.fromJson(ApiServer.body(ex), Map.class);
        String email    = (String) body.get("email");
        String password = (String) body.get("password");
        User user = authService.login(email, password);
        String token = SessionStore.create(user);
        ApiServer.ok(ex, Map.of("token", token, "user", Map.of(
            "id", user.id(), "email", user.email(),
            "fullName", user.fullName(), "role", user.role().name()
        )));
    }

    private void handleLogout(HttpExchange ex) throws IOException {
        SessionStore.remove(ApiServer.bearerToken(ex));
        ApiServer.noContent(ex);
    }

    private void handleMe(HttpExchange ex) throws IOException {
        User user = SessionStore.get(ApiServer.bearerToken(ex));
        if (user == null) { ApiServer.error(ex, 401, "Unauthorised"); return; }
        ApiServer.ok(ex, Map.of(
            "id", user.id(), "email", user.email(),
            "fullName", user.fullName(), "role", user.role().name()
        ));
    }

    private void handleRegister(HttpExchange ex) throws IOException {
        var body     = JsonUtil.fromJson(ApiServer.body(ex), Map.class);
        String email    = (String) body.get("email");
        String fullName = (String) body.get("fullName");
        String password = (String) body.get("password");
        User user = authService.register(email, fullName, password);
        ApiServer.ok(ex, Map.of("id", user.id(), "email", user.email(), "fullName", user.fullName()));
    }
}
