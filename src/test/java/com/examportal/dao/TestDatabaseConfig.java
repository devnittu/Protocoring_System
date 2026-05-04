package com.examportal.dao;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.examportal.config.DatabaseConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;

/** Initialises a HikariCP pool pointed at the H2 in-memory test database. */
public final class TestDatabaseConfig {

    private static final Logger log = LoggerFactory.getLogger(TestDatabaseConfig.class);

    private TestDatabaseConfig() {}

    public static void initH2() {
        try {
            HikariConfig cfg = new HikariConfig();
            cfg.setJdbcUrl("jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;MODE=MySQL");
            cfg.setUsername("sa");
            cfg.setPassword("");
            cfg.setMaximumPoolSize(5);
            cfg.setPoolName("H2-Test-Pool");

            HikariDataSource ds = new HikariDataSource(cfg);

            // Inject into DatabaseConfig via reflection
            Field field = DatabaseConfig.class.getDeclaredField("dataSource");
            field.setAccessible(true);
            field.set(null, ds);
            log.info("H2 test database pool initialised.");
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialise H2 test pool", e);
        }
    }
}
