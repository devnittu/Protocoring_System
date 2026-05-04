package com.examportal.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/** Loads and exposes all application configuration from config.properties. */
public final class AppConfig {

    private static final Logger log = LoggerFactory.getLogger(AppConfig.class);
    private static volatile AppConfig instance;
    private final Properties props = new Properties();

    private AppConfig() {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("config.properties")) {
            if (is == null) throw new IllegalStateException("config.properties not found on classpath");
            props.load(is);
            log.info("config.properties loaded successfully.");
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load config.properties", e);
        }
    }

    public static AppConfig getInstance() {
        if (instance == null) {
            synchronized (AppConfig.class) {
                if (instance == null) instance = new AppConfig();
            }
        }
        return instance;
    }

    public String getDbUrl()                { return props.getProperty("db.url"); }
    public String getDbUser()               { return props.getProperty("db.user"); }
    public String getDbPassword()           { return props.getProperty("db.password"); }
    public int    getDbPoolSize()           { return Integer.parseInt(props.getProperty("db.pool.size", "10")); }
    public int    getDbPoolMinIdle()        { return Integer.parseInt(props.getProperty("db.pool.min.idle", "2")); }
    public long   getDbConnectionTimeout()  { return Long.parseLong(props.getProperty("db.pool.connection.timeout", "30000")); }
    public long   getDbIdleTimeout()        { return Long.parseLong(props.getProperty("db.pool.idle.timeout", "600000")); }
    public long   getDbMaxLifetime()        { return Long.parseLong(props.getProperty("db.pool.max.lifetime", "1800000")); }
    public String getOpencvLibPath()        { return props.getProperty("opencv.lib.path", ""); }
    public long   getProctorFrameInterval() { return Long.parseLong(props.getProperty("proctor.frame.interval.ms", "5000")); }
    public String getProctorSaveDir()       { return props.getProperty("proctor.save.dir", System.getProperty("java.io.tmpdir") + "/proctor"); }
    public double getGazeDeviationThreshold() { return Double.parseDouble(props.getProperty("proctor.gaze.deviation.threshold", "0.30")); }
    public int    getCriticalAutoSubmitCount() { return Integer.parseInt(props.getProperty("proctor.critical.auto.submit.count", "3")); }
    public int    getSessionTimeoutMinutes()   { return Integer.parseInt(props.getProperty("session.timeout.minutes", "120")); }
    public String getAppName()              { return props.getProperty("app.name", "ExamPortal"); }
    public String getAppVersion()           { return props.getProperty("app.version", "1.0.0"); }
}
