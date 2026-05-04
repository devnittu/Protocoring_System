package com.examportal.dao;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.examportal.config.DatabaseConfig;

import java.lang.reflect.Field;

/** Secondary H2 pool initialiser for ExamDAOTest (different in-memory DB URL). */
public final class TestDatabaseConfig2 {

    private TestDatabaseConfig2() {}

    public static void initH2(String url) {
        try {
            HikariConfig cfg = new HikariConfig();
            cfg.setJdbcUrl(url);
            cfg.setUsername("sa");
            cfg.setPassword("");
            cfg.setMaximumPoolSize(5);
            cfg.setPoolName("H2-Exam-Test-Pool");
            HikariDataSource ds = new HikariDataSource(cfg);
            Field field = DatabaseConfig.class.getDeclaredField("dataSource");
            field.setAccessible(true);
            field.set(null, ds);
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialise H2 exam test pool", e);
        }
    }
}
