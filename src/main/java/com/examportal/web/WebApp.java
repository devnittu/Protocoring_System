package com.examportal.web;

import com.examportal.config.DatabaseConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Standalone web-server entry point — no JavaFX required.
 * Run with: mvn compile exec:java -Dexec.mainClass=com.examportal.web.WebApp
 * Then open: http://localhost:8080
 */
public class WebApp {

    private static final Logger log = LoggerFactory.getLogger(WebApp.class);

    public static void main(String[] args) {
        try {
            DatabaseConfig.initialise();
            ApiServer.start();
            // Keep alive
            Thread.currentThread().join();
        } catch (Exception e) {
            log.error("Failed to start web server", e);
            System.exit(1);
        }
    }
}
