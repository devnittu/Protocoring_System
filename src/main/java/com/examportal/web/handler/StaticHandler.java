package com.examportal.web.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.*;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/** Serves static web files from the /web classpath directory. */
public class StaticHandler implements HttpHandler {

    private static final Map<String, String> MIME = Map.of(
        "html", "text/html; charset=utf-8",
        "css",  "text/css",
        "js",   "application/javascript",
        "json", "application/json",
        "png",  "image/png",
        "ico",  "image/x-icon",
        "svg",  "image/svg+xml"
    );

    @Override
    public void handle(HttpExchange ex) throws IOException {
        String path = ex.getRequestURI().getPath();

        // SPA-style: route /student, /exam, /result, /admin → their HTML files
        if (path.equals("/") || path.equals("/login") || path.isEmpty()) path = "/index.html";
        else if (!path.contains(".")) path = path + ".html"; // /student → /student.html

        String resourcePath = "/web" + path;
        InputStream is = StaticHandler.class.getResourceAsStream(resourcePath);

        if (is == null) {
            // Fallback to index.html for SPA navigation
            is = StaticHandler.class.getResourceAsStream("/web/index.html");
            path = "/index.html";
        }

        String ext  = path.contains(".") ? path.substring(path.lastIndexOf('.') + 1) : "html";
        String mime = MIME.getOrDefault(ext, "application/octet-stream");

        byte[] bytes = is.readAllBytes();
        ex.getResponseHeaders().set("Content-Type", mime);
        ex.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(bytes); }
    }
}
