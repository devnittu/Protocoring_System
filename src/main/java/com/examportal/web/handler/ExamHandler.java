package com.examportal.web.handler;

import com.examportal.model.Exam;
import com.examportal.model.Question;
import com.examportal.model.User;
import com.examportal.service.AuthService;
import com.examportal.service.ExamService;
import com.examportal.web.ApiServer;
import com.examportal.web.JsonUtil;
import com.examportal.web.SessionStore;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class ExamHandler implements HttpHandler {

    private final ExamService examService;
    private final AuthService authService;

    public ExamHandler(ExamService e, AuthService a) { examService = e; authService = a; }

    @Override
    public void handle(HttpExchange ex) throws IOException {
        if (ApiServer.method(ex, "OPTIONS")) { ApiServer.cors(ex); return; }
        User user = SessionStore.get(ApiServer.bearerToken(ex));
        if (user == null) { ApiServer.error(ex, 401, "Unauthorised"); return; }

        String path = ex.getRequestURI().getPath();
        // /api/exams           → list
        // /api/exams/{id}      → get one
        // /api/exams/{id}/questions → questions
        String[] parts = path.split("/"); // ["","api","exams","{id}","questions"]

        try {
            if (parts.length == 3 && ApiServer.method(ex, "GET")) {
                // GET /api/exams
                List<Exam> exams = user.isAdmin()
                    ? examService.getAllExams()
                    : examService.getPublishedExams().stream()
                        .filter(Exam::isAvailableNow).toList();
                ApiServer.ok(ex, exams);

            } else if (parts.length == 4 && ApiServer.method(ex, "GET")) {
                // GET /api/exams/{id}
                Long id = Long.parseLong(parts[3]);
                Exam exam = examService.findExamById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Exam not found"));
                ApiServer.ok(ex, exam);

            } else if (parts.length == 5 && "questions".equals(parts[4]) && ApiServer.method(ex, "GET")) {
                // GET /api/exams/{id}/questions
                Long id = Long.parseLong(parts[3]);
                Exam exam = examService.findExamById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Exam not found"));
                List<Question> qs = examService.getQuestionsForAttempt(id, exam.randomiseQuestions());
                // Map to frontend-friendly DTO (field names the JS expects)
                List<Map<String,Object>> dtos = qs.stream().map(q -> Map.<String,Object>of(
                    "id",            q.id(),
                    "questionText",  q.body(),
                    "optionA",       q.optionA(),
                    "optionB",       q.optionB(),
                    "optionC",       q.optionC(),
                    "optionD",       q.optionD() != null ? q.optionD() : "",
                    "correctOption", String.valueOf(q.correctOption()),
                    "marks",         q.marks(),
                    "difficulty",    q.difficulty().name(),
                    "topic",         q.topic() != null ? q.topic() : ""
                )).toList();
                ApiServer.ok(ex, dtos);

            } else {
                ApiServer.error(ex, 404, "Not found");
            }
        } catch (NumberFormatException nfe) {
            ApiServer.error(ex, 400, "Invalid ID");
        } catch (IllegalArgumentException iae) {
            ApiServer.error(ex, 404, iae.getMessage());
        } catch (Exception e) {
            ApiServer.error(ex, 500, e.getMessage());
        }
    }
}
