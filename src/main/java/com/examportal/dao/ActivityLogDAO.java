package com.examportal.dao;

import com.examportal.config.DatabaseConfig;
import com.examportal.model.ActivityLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** JDBC implementation of IActivityLogDAO using HikariCP connection pool. */
public class ActivityLogDAO implements IActivityLogDAO {

    private static final Logger log = LoggerFactory.getLogger(ActivityLogDAO.class);

    private static final String INSERT =
        """
        INSERT INTO activity_logs (attempt_id, student_id, event_type, severity, detail, frame_path, logged_at)
        VALUES (?, ?, ?, ?, ?, ?, NOW())
        """;
    private static final String FIND_BY_ATTEMPT =
        "SELECT * FROM activity_logs WHERE attempt_id = ? ORDER BY logged_at";
    private static final String FIND_BY_STUDENT =
        "SELECT * FROM activity_logs WHERE student_id = ? ORDER BY logged_at DESC";
    private static final String FIND_ALL =
        "SELECT * FROM activity_logs ORDER BY logged_at DESC LIMIT 1000";
    private static final String COUNT_CRITICAL =
        "SELECT COUNT(*) FROM activity_logs WHERE attempt_id = ? AND severity = 'CRITICAL'";
    private static final String COUNT_BY_EVENT =
        "SELECT event_type, COUNT(*) AS cnt FROM activity_logs WHERE attempt_id = ? GROUP BY event_type";

    @Override
    public ActivityLog insert(ActivityLog entry) {
        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(INSERT, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, entry.attemptId());
            ps.setLong(2, entry.studentId());
            ps.setString(3, entry.eventType().name());
            ps.setString(4, entry.severity().name());
            if (entry.detail() != null) ps.setString(5, entry.detail());
            else                        ps.setNull(5, Types.VARCHAR);
            if (entry.framePath() != null) ps.setString(6, entry.framePath());
            else                           ps.setNull(6, Types.VARCHAR);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    long id = rs.getLong(1);
                    return new ActivityLog(id, entry.attemptId(), entry.studentId(),
                        entry.eventType(), entry.severity(), entry.detail(),
                        entry.framePath(), LocalDateTime.now());
                }
            }
        } catch (SQLException e) {
            log.error("insert activityLog failed", e);
            throw new RuntimeException("Failed to insert activity log", e);
        }
        throw new RuntimeException("Insert activityLog returned no generated key");
    }

    @Override
    public List<ActivityLog> findByAttemptId(Long attemptId) {
        List<ActivityLog> list = new ArrayList<>();
        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(FIND_BY_ATTEMPT)) {
            ps.setLong(1, attemptId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            log.error("findByAttemptId log failed", e);
            throw new RuntimeException(e);
        }
        return list;
    }

    @Override
    public List<ActivityLog> findByStudentId(Long studentId) {
        List<ActivityLog> list = new ArrayList<>();
        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(FIND_BY_STUDENT)) {
            ps.setLong(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            log.error("findByStudentId log failed", e);
            throw new RuntimeException(e);
        }
        return list;
    }

    @Override
    public List<ActivityLog> findAll() {
        List<ActivityLog> list = new ArrayList<>();
        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(FIND_ALL);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            log.error("findAll logs failed", e);
            throw new RuntimeException(e);
        }
        return list;
    }

    @Override
    public long countCriticalByAttemptId(Long attemptId) {
        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(COUNT_CRITICAL)) {
            ps.setLong(1, attemptId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getLong(1);
            }
        } catch (SQLException e) {
            log.error("countCritical log failed", e);
            throw new RuntimeException(e);
        }
        return 0L;
    }

    @Override
    public Map<String, Long> countByEventTypeForAttempt(Long attemptId) {
        Map<String, Long> map = new LinkedHashMap<>();
        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(COUNT_BY_EVENT)) {
            ps.setLong(1, attemptId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    map.put(rs.getString("event_type"), rs.getLong("cnt"));
                }
            }
        } catch (SQLException e) {
            log.error("countByEventType log failed", e);
            throw new RuntimeException(e);
        }
        return map;
    }

    private ActivityLog mapRow(ResultSet rs) throws SQLException {
        Timestamp ts = rs.getTimestamp("logged_at");
        return new ActivityLog(
            rs.getLong("id"),
            rs.getLong("attempt_id"),
            rs.getLong("student_id"),
            ActivityLog.EventType.valueOf(rs.getString("event_type")),
            ActivityLog.Severity.valueOf(rs.getString("severity")),
            rs.getString("detail"),
            rs.getString("frame_path"),
            ts != null ? ts.toLocalDateTime() : LocalDateTime.now()
        );
    }
}
