package com.examportal.web.handler;

import com.examportal.model.*;
import com.examportal.service.*;
import com.examportal.web.ApiServer;
import com.examportal.web.JsonUtil;
import com.examportal.web.SessionStore;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;

public class AdminHandler implements HttpHandler {
    private final ExamService   examService;
    private final AuthService   authService;
    private final ResultService resultService;

    public AdminHandler(ExamService e, AuthService a, ResultService r) {
        examService = e; authService = a; resultService = r;
    }

    @Override
    public void handle(HttpExchange ex) throws IOException {
        if (ApiServer.method(ex, "OPTIONS")) { ApiServer.cors(ex); return; }
        if (!SessionStore.isAdmin(ApiServer.bearerToken(ex))) {
            ApiServer.error(ex, 403, "Admin only"); return;
        }
        String path   = ex.getRequestURI().getPath();
        String method = ex.getRequestMethod().toUpperCase();
        String[] parts = path.split("/"); // /api/admin/{resource}/{id?}

        try {
            String resource = parts.length > 3 ? parts[3] : "";
            String idStr    = parts.length > 4 ? parts[4] : null;

            switch (resource) {
                case "stats"    -> handleStats(ex);
                case "exams"    -> handleExams(ex, method, idStr);
                case "students" -> handleStudents(ex, method, idStr, parts);
                case "logs"     -> ApiServer.ok(ex, resultService.getAllLogs());
                default         -> ApiServer.error(ex, 404, "Not found");
            }
        } catch (Exception e) {
            ApiServer.error(ex, 500, e.getMessage());
        }
    }

    private void handleStats(HttpExchange ex) throws IOException {
        ApiServer.ok(ex, Map.of(
            "totalExams",    examService.getAllExams().size(),
            "totalStudents", authService.getAllStudents().size(),
            "attemptsToday", resultService.getAttemptsToday(),
            "flaggedSessions", resultService.getFlaggedSessions()
        ));
    }

    private void handleExams(HttpExchange ex, String method, String idStr) throws IOException {
        switch (method) {
            case "GET"    -> ApiServer.ok(ex, examService.getAllExams());
            case "POST"   -> { Exam e = parseExam(ex, null); ApiServer.ok(ex, examService.createExam(e)); }
            case "PUT"    -> { Long id = Long.parseLong(idStr); Exam e = parseExam(ex, id); examService.updateExam(e); ApiServer.noContent(ex); }
            case "DELETE" -> { examService.deleteExam(Long.parseLong(idStr)); ApiServer.noContent(ex); }
            default       -> ApiServer.error(ex, 405, "Method not allowed");
        }
    }

    private Exam parseExam(HttpExchange ex, Long id) throws IOException {
        Map<?,?> b = JsonUtil.fromJson(ApiServer.body(ex), Map.class);
        User admin = SessionStore.get(ApiServer.bearerToken(ex));
        return new Exam(id,
            (String)b.get("title"), (String)b.get("description"),
            admin.id(),
            ((Number)b.get("durationMinutes")).intValue(),
            ((Number)b.get("totalMarks")).intValue(),
            ((Number)b.get("passPercentage")).doubleValue(),
            LocalDateTime.now(), LocalDateTime.now().plusDays(30),
            Boolean.TRUE.equals(b.get("isPublished")),
            !Boolean.FALSE.equals(b.get("randomiseQuestions")),
            null
        );
    }

    private void handleStudents(HttpExchange ex, String method, String idStr, String[] parts) throws IOException {
        if ("GET".equals(method)) { ApiServer.ok(ex, authService.getAllStudents()); return; }
        if ("PATCH".equals(method) && parts.length > 5 && "toggle".equals(parts[5])) {
            Long id = Long.parseLong(idStr);
            User u = authService.getAllStudents().stream().filter(s -> s.id().equals(id)).findFirst().orElseThrow();
            authService.setUserActive(id, !u.isActive());
            ApiServer.noContent(ex);
        }
    }
}
