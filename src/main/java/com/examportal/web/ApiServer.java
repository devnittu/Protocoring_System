package com.examportal.web;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import com.examportal.config.DatabaseConfig;
import com.examportal.dao.*;
import com.examportal.service.*;
import com.examportal.web.handler.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;

/** Starts the embedded HTTP server on port 8080 and wires all routes. */
public class ApiServer {

    private static final Logger log = LoggerFactory.getLogger(ApiServer.class);
    private static final int PORT = 8080;

    public static void start() throws IOException {
        // Wire services
        IUserDAO     userDAO     = new UserDAO();
        IExamDAO     examDAO     = new ExamDAO();
        IQuestionDAO questionDAO = new QuestionDAO();
        IAttemptDAO  attemptDAO  = new AttemptDAO();
        IResponseDAO responseDAO = new ResponseDAO();
        IActivityLogDAO logDAO   = new ActivityLogDAO();

        AuthService      authService   = new AuthService(userDAO);
        ExamService      examService   = new ExamService(examDAO, questionDAO, attemptDAO);
        EvaluatorService evalService   = new EvaluatorService(attemptDAO, responseDAO);
        ResultService    resultService = new ResultService(attemptDAO, responseDAO, logDAO);

        // Handlers
        AuthHandler    authH  = new AuthHandler(authService);
        ExamHandler    examH  = new ExamHandler(examService, authService);
        AttemptHandler attH   = new AttemptHandler(examService, evalService, resultService);
        AdminHandler   adminH = new AdminHandler(examService, authService, resultService);
        StaticHandler  staticH= new StaticHandler();

        // HTTP server
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
        server.setExecutor(Executors.newFixedThreadPool(20));

        // Routes
        server.createContext("/api/auth",    authH);
        server.createContext("/api/exams",   examH);
        server.createContext("/api/attempts",attH);
        server.createContext("/api/admin",   adminH);
        server.createContext("/",            staticH);   // catch-all for static files

        server.start();
        log.info("╔══════════════════════════════════════════╗");
        log.info("║  ExamPortal Web Server started           ║");
        log.info("║  http://localhost:{}                   ║", PORT);
        log.info("╚══════════════════════════════════════════╝");
    }

    // ── Shared response helpers ───────────────────────────────────────────────

    public static void json(HttpExchange ex, int status, Object body) throws IOException {
        addCors(ex);
        byte[] bytes = JsonUtil.toJson(body).getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        ex.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(bytes); }
    }

    public static void ok(HttpExchange ex, Object body) throws IOException { json(ex, 200, body); }

    public static void error(HttpExchange ex, int status, String msg) throws IOException {
        json(ex, status, java.util.Map.of("error", msg));
    }

    public static void noContent(HttpExchange ex) throws IOException {
        addCors(ex);
        ex.sendResponseHeaders(204, -1);
    }

    public static void cors(HttpExchange ex) throws IOException {
        addCors(ex);
        ex.sendResponseHeaders(204, -1);
    }

    private static void addCors(HttpExchange ex) {
        ex.getResponseHeaders().set("Access-Control-Allow-Origin",  "*");
        ex.getResponseHeaders().set("Access-Control-Allow-Methods", "GET,POST,PUT,DELETE,PATCH,OPTIONS");
        ex.getResponseHeaders().set("Access-Control-Allow-Headers", "Authorization,Content-Type");
    }

    /** Extracts Bearer token from Authorization header. */
    public static String bearerToken(HttpExchange ex) {
        String auth = ex.getRequestHeaders().getFirst("Authorization");
        if (auth != null && auth.startsWith("Bearer ")) return auth.substring(7);
        return null;
    }

    /** Reads request body as string. */
    public static String body(HttpExchange ex) throws IOException {
        return new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
    }

    /** Returns true if method matches. */
    public static boolean method(HttpExchange ex, String m) {
        return ex.getRequestMethod().equalsIgnoreCase(m);
    }
}
