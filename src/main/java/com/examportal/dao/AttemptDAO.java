package com.examportal.dao;

import com.examportal.config.DatabaseConfig;
import com.examportal.model.Attempt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** JDBC implementation of IAttemptDAO using HikariCP connection pool. */
public class AttemptDAO implements IAttemptDAO {

    private static final Logger log = LoggerFactory.getLogger(AttemptDAO.class);

    private static final String INSERT =
        "INSERT INTO attempts (student_id, exam_id, started_at, status) VALUES (?, ?, NOW(), 'IN_PROGRESS')";
    private static final String FIND_BY_ID =
        "SELECT * FROM attempts WHERE id = ?";
    private static final String FIND_BY_STUDENT =
        "SELECT * FROM attempts WHERE student_id = ? ORDER BY started_at DESC";
    private static final String FIND_BY_EXAM =
        "SELECT * FROM attempts WHERE exam_id = ? ORDER BY started_at DESC";
    private static final String FIND_IN_PROGRESS =
        "SELECT * FROM attempts WHERE student_id = ? AND exam_id = ? AND status = 'IN_PROGRESS' LIMIT 1";
    private static final String UPDATE_STATUS =
        "UPDATE attempts SET status = ? WHERE id = ?";
    private static final String FINALISE =
        """
        UPDATE attempts
        SET score=?, percentage=?, passed=?, status=?, submitted_at=NOW()
        WHERE id=?
        """;
    private static final String COUNT_TODAY =
        "SELECT COUNT(*) FROM attempts WHERE DATE(started_at) = CURDATE()";
    private static final String COUNT_FLAGGED =
        """
        SELECT COUNT(DISTINCT attempt_id) FROM activity_logs
        WHERE severity = 'CRITICAL'
        """;

    @Override
    public Attempt insert(Attempt attempt) {
        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(INSERT, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, attempt.studentId());
            ps.setLong(2, attempt.examId());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return findById(rs.getLong(1)).orElseThrow();
            }
        } catch (SQLException e) {
            log.error("insert attempt failed", e);
            throw new RuntimeException("Failed to insert attempt", e);
        }
        throw new RuntimeException("Insert attempt returned no generated key");
    }

    @Override
    public Optional<Attempt> findById(Long id) {
        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(FIND_BY_ID)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) {
            log.error("findById attempt failed", e);
            throw new RuntimeException(e);
        }
        return Optional.empty();
    }

    @Override
    public List<Attempt> findByStudentId(Long studentId) {
        List<Attempt> list = new ArrayList<>();
        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(FIND_BY_STUDENT)) {
            ps.setLong(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            log.error("findByStudentId attempt failed", e);
            throw new RuntimeException(e);
        }
        return list;
    }

    @Override
    public List<Attempt> findByExamId(Long examId) {
        List<Attempt> list = new ArrayList<>();
        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(FIND_BY_EXAM)) {
            ps.setLong(1, examId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            log.error("findByExamId attempt failed", e);
            throw new RuntimeException(e);
        }
        return list;
    }

    @Override
    public Optional<Attempt> findInProgressByStudentAndExam(Long studentId, Long examId) {
        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(FIND_IN_PROGRESS)) {
            ps.setLong(1, studentId);
            ps.setLong(2, examId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) {
            log.error("findInProgress attempt failed", e);
            throw new RuntimeException(e);
        }
        return Optional.empty();
    }

    @Override
    public void updateStatus(Long attemptId, Attempt.Status status) {
        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(UPDATE_STATUS)) {
            ps.setString(1, status.name());
            ps.setLong(2, attemptId);
            ps.executeUpdate();
        } catch (SQLException e) {
            log.error("updateStatus attempt failed", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void finalise(Long attemptId, double score, double percentage, boolean passed, Attempt.Status status) {
        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(FINALISE)) {
            ps.setDouble(1, score);
            ps.setDouble(2, percentage);
            ps.setBoolean(3, passed);
            ps.setString(4, status.name());
            ps.setLong(5, attemptId);
            ps.executeUpdate();
        } catch (SQLException e) {
            log.error("finalise attempt failed", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public long countAttemptsToday() {
        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(COUNT_TODAY);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getLong(1);
        } catch (SQLException e) {
            log.error("countAttemptsToday failed", e);
            throw new RuntimeException(e);
        }
        return 0L;
    }

    @Override
    public long countFlaggedSessions() {
        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(COUNT_FLAGGED);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getLong(1);
        } catch (SQLException e) {
            log.error("countFlaggedSessions failed", e);
            throw new RuntimeException(e);
        }
        return 0L;
    }

    private Attempt mapRow(ResultSet rs) throws SQLException {
        Timestamp startedAt   = rs.getTimestamp("started_at");
        Timestamp submittedAt = rs.getTimestamp("submitted_at");
        double score = rs.getDouble("score");   boolean scoreNull = rs.wasNull();
        double pct   = rs.getDouble("percentage"); boolean pctNull = rs.wasNull();
        // MySQL 8 JDBC returns Boolean for BOOLEAN columns — use getBoolean+wasNull
        boolean passedVal = rs.getBoolean("passed"); boolean passedNull = rs.wasNull();
        return new Attempt(
            rs.getLong("id"),
            rs.getLong("student_id"),
            rs.getLong("exam_id"),
            startedAt   != null ? startedAt.toLocalDateTime()  : LocalDateTime.now(),
            submittedAt != null ? submittedAt.toLocalDateTime() : null,
            scoreNull  ? null : score,
            pctNull    ? null : pct,
            passedNull ? null : passedVal,
            Attempt.Status.valueOf(rs.getString("status"))
        );
    }
}
