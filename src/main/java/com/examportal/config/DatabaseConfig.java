package com.examportal.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/** Manages the HikariCP connection pool, providing pooled DB connections. */
public final class DatabaseConfig {

    private static final Logger log = LoggerFactory.getLogger(DatabaseConfig.class);
    private static volatile HikariDataSource dataSource;

    private DatabaseConfig() {}

    /** Initialises the pool using values from AppConfig. Call once at startup. */
    public static synchronized void initialise() {
        if (dataSource != null && !dataSource.isClosed()) {
            return;
        }
        AppConfig cfg = AppConfig.getInstance();

        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(cfg.getDbUrl());
        hikariConfig.setUsername(cfg.getDbUser());
        hikariConfig.setPassword(cfg.getDbPassword());
        hikariConfig.setMaximumPoolSize(cfg.getDbPoolSize());
        hikariConfig.setMinimumIdle(cfg.getDbPoolMinIdle());
        hikariConfig.setConnectionTimeout(cfg.getDbConnectionTimeout());
        hikariConfig.setIdleTimeout(cfg.getDbIdleTimeout());
        hikariConfig.setMaxLifetime(cfg.getDbMaxLifetime());
        hikariConfig.setPoolName("ExamPortal-Pool");
        hikariConfig.addDataSourceProperty("cachePrepStmts", "true");
        hikariConfig.addDataSourceProperty("prepStmtCacheSize", "250");
        hikariConfig.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

        dataSource = new HikariDataSource(hikariConfig);
        log.info("HikariCP pool initialised — url={}", cfg.getDbUrl());
    }

    /** Returns a {@link Connection} from the pool; caller must close it. */
    public static Connection getConnection() throws SQLException {
        if (dataSource == null || dataSource.isClosed()) {
            throw new IllegalStateException("DataSource not initialised. Call DatabaseConfig.initialise() first.");
        }
        return dataSource.getConnection();
    }

    /** Returns the raw DataSource for framework use. */
    public static DataSource getDataSource() {
        return dataSource;
    }

    /** Shuts down the connection pool gracefully. */
    public static void shutdown() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            log.info("HikariCP pool shut down.");
        }
    }
}
