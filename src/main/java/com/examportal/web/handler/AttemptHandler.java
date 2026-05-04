package com.examportal.web.handler;

import com.examportal.model.*;
import com.examportal.service.*;
import com.examportal.web.ApiServer;
import com.examportal.web.JsonUtil;
import com.examportal.web.SessionStore;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.util.*;

/**
 * Handles:
 *   POST /api/attempts/start
 *   POST /api/attempts/{id}/submit
 *   GET  /api/attempts/{id}/result
 */
public class AttemptHandler implements HttpHandler {

    private final ExamService      examService;
    private final EvaluatorService evalService;
    private final ResultService    resultService;

    public AttemptHandler(ExamService e, EvaluatorService ev, ResultService r) {
        examService = e; evalService = ev; resultService = r;
    }

    @Override
    public void handle(HttpExchange ex) throws IOException {
        if (ApiServer.method(ex, "OPTIONS")) { ApiServer.cors(ex); return; }
        User user = SessionStore.get(ApiServer.bearerToken(ex));
        if (user == null) { ApiServer.error(ex, 401, "Unauthorised"); return; }

        String path   = ex.getRequestURI().getPath();
        String method = ex.getRequestMethod().toUpperCase();

        try {
            // GET /api/attempts/my
            if (path.endsWith("/my") && "GET".equals(method)) {
                handleHistory(ex, user);
                return;
            }
            // POST /api/attempts/start
            if (path.endsWith("/start") && "POST".equals(method)) {
                handleStart(ex, user);
                return;
            }

            // Paths with attempt ID: /api/attempts/{id}/...
            String[] parts = path.split("/");
            // ["", "api", "attempts", "{id}", "submit"|"result"]
            if (parts.length == 5) {
                long attemptId = Long.parseLong(parts[3]);
                String action  = parts[4];
                if ("submit".equals(action) && "POST".equals(method)) {
                    handleSubmit(ex, user, attemptId);
                } else if ("result".equals(action) && "GET".equals(method)) {
                    handleResult(ex, user, attemptId);
                } else {
                    ApiServer.error(ex, 404, "Not found");
                }
            } else {
                ApiServer.error(ex, 404, "Not found");
            }
        } catch (NumberFormatException nfe) {
            ApiServer.error(ex, 400, "Invalid attempt ID");
        } catch (Exception e) {
            e.printStackTrace();
            ApiServer.error(ex, 500, e.getMessage() != null ? e.getMessage() : "Internal error");
        }
    }

    // ── History ───────────────────────────────────────────────────────────────
    private void handleHistory(HttpExchange ex, User user) throws IOException {
        List<Attempt> attempts = resultService.getAttemptsByStudent(user.id());
        // Enrich each attempt with exam title
        List<Map<String, Object>> dtos = attempts.stream().map(a -> {
            String examTitle = examService.findExamById(a.examId())
                .map(e -> e.title()).orElse("Unknown Exam");
            Map<String, Object> m = new java.util.LinkedHashMap<>();
            m.put("id",          a.id());
            m.put("examId",      a.examId());
            m.put("examTitle",   examTitle);
            m.put("startedAt",   a.startedAt());
            m.put("submittedAt", a.submittedAt());
            m.put("score",       a.score());
            m.put("percentage",  a.percentage());
            m.put("passed",      a.passed());
            m.put("status",      a.status().name());
            return m;
        }).toList();
        ApiServer.ok(ex, dtos);
    }

    // ── Start ─────────────────────────────────────────────────────────────────
    private void handleStart(HttpExchange ex, User user) throws IOException {
        Map<?,?> body   = JsonUtil.fromJson(ApiServer.body(ex), Map.class);
        Object rawId    = body.get("examId");
        if (rawId == null) { ApiServer.error(ex, 400, "examId required"); return; }
        // Gson deserialises JSON numbers as Double by default
        Long examId;
        if (rawId instanceof Number n)  examId = n.longValue();
        else if (rawId instanceof String s) examId = Long.parseLong(s);
        else { ApiServer.error(ex, 400, "examId must be a number, got " + rawId.getClass().getSimpleName()); return; }
        Attempt attempt = examService.startAttempt(user.id(), examId);
        ApiServer.ok(ex, attempt);
    }

    // ── Submit ────────────────────────────────────────────────────────────────
    private void handleSubmit(HttpExchange ex, User user, long attemptId) throws IOException {
        Map<?,?> body = JsonUtil.fromJson(ApiServer.body(ex), Map.class);

        @SuppressWarnings("unchecked")
        Map<String,String> rawAnswers = body.get("answers") instanceof Map
            ? (Map<String,String>) body.get("answers") : Map.of();

        boolean force = Boolean.TRUE.equals(body.get("force"));

        // Build answers map {questionId → option char}
        Map<Long, Character> answers = new LinkedHashMap<>();
        rawAnswers.forEach((k, v) -> {
            if (v != null && !v.isEmpty())
                answers.put(Long.parseLong(k), v.charAt(0));
        });

        // Use Long.equals() — not == — to avoid boxed-Long cache miss for IDs > 127
        Attempt attempt = resultService.getAttemptsByStudent(user.id()).stream()
            .filter(a -> a.id() != null && a.id().equals(attemptId))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Attempt not found: " + attemptId));

        Exam exam = examService.findExamById(attempt.examId())
            .orElseThrow(() -> new IllegalArgumentException("Exam not found"));

        List<Question> questions = examService.getQuestionsOrdered(exam.id());

        Attempt.Status status = force ? Attempt.Status.FORCE_SUBMITTED : Attempt.Status.SUBMITTED;
        Attempt finalAttempt  = evalService.evaluate(attemptId, questions, answers, exam, status);
        ApiServer.ok(ex, finalAttempt);
    }

    // ── Result ────────────────────────────────────────────────────────────────
    private void handleResult(HttpExchange ex, User user, long attemptId) throws IOException {
        Attempt attempt = resultService.getAttemptsByStudent(user.id()).stream()
            .filter(a -> a.id() != null && a.id().equals(attemptId))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Attempt not found: " + attemptId));

        Exam exam = examService.findExamById(attempt.examId())
            .orElseThrow(() -> new IllegalArgumentException("Exam not found"));

        List<Question> rawQs   = examService.getQuestionsOrdered(exam.id());
        List<Response> responses = resultService.getResponsesForAttempt(attemptId);
        List<ActivityLog> logs = resultService.getLogsForAttempt(attemptId);

        // Build frontend-friendly question DTOs
        List<Map<String,Object>> questions = rawQs.stream().map(q -> Map.<String,Object>of(
            "id",            q.id(),
            "questionText",  q.body(),
            "optionA",       q.optionA(),
            "optionB",       q.optionB(),
            "optionC",       q.optionC(),
            "optionD",       q.optionD() != null ? q.optionD() : "",
            "correctOption", String.valueOf(q.correctOption()),
            "marks",         q.marks(),
            "difficulty",    q.difficulty().name()
        )).toList();

        // Build frontend-friendly response DTOs
        List<Map<String,Object>> respDtos = responses.stream().map(r -> {
            Map<String,Object> m = new LinkedHashMap<>();
            m.put("questionId",     r.questionId());
            m.put("selectedOption", r.chosenOption() != null ? String.valueOf(r.chosenOption()) : null);
            m.put("isCorrect",      r.isCorrect());
            return m;
        }).toList();

        ApiServer.ok(ex, Map.of(
            "attempt",  attempt,
            "exam",     exam,
            "questions", questions,
            "responses", respDtos,
            "logs",     logs
        ));
    }
}
