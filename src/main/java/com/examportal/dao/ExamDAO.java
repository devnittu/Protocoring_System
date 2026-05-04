package com.examportal.dao;

import com.examportal.config.DatabaseConfig;
import com.examportal.model.Exam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** JDBC implementation of IExamDAO using HikariCP connection pool. */
public class ExamDAO implements IExamDAO {

    private static final Logger log = LoggerFactory.getLogger(ExamDAO.class);

    private static final String INSERT =
        """
        INSERT INTO exams (title, description, created_by, duration_minutes,
            total_marks, pass_percentage, start_time, end_time,
            is_published, randomise_questions)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;
    private static final String FIND_BY_ID    = "SELECT * FROM exams WHERE id = ?";
    private static final String FIND_ALL      = "SELECT * FROM exams ORDER BY created_at DESC";
    private static final String FIND_PUBLISHED = "SELECT * FROM exams WHERE is_published = 1 ORDER BY start_time";
    private static final String FIND_BY_CREATOR = "SELECT * FROM exams WHERE created_by = ? ORDER BY created_at DESC";
    private static final String UPDATE =
        """
        UPDATE exams SET title=?, description=?, duration_minutes=?,
            total_marks=?, pass_percentage=?, start_time=?, end_time=?,
            is_published=?, randomise_questions=?
        WHERE id=?
        """;
    private static final String DELETE         = "DELETE FROM exams WHERE id = ?";
    private static final String SET_PUBLISHED  = "UPDATE exams SET is_published = ? WHERE id = ?";

    @Override
    public Exam insert(Exam exam) {
        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(INSERT, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, exam.title());
            ps.setString(2, exam.description());
            ps.setLong(3, exam.createdBy());
            ps.setInt(4, exam.durationMinutes());
            ps.setInt(5, exam.totalMarks());
            ps.setDouble(6, exam.passPercentage());
            ps.setTimestamp(7, exam.startTime() != null ? Timestamp.valueOf(exam.startTime()) : null);
            ps.setTimestamp(8, exam.endTime()   != null ? Timestamp.valueOf(exam.endTime())   : null);
            ps.setBoolean(9, exam.isPublished());
            ps.setBoolean(10, exam.randomiseQuestions());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return findById(rs.getLong(1)).orElseThrow();
            }
        } catch (SQLException e) {
            log.error("insert exam failed", e);
            throw new RuntimeException("Failed to insert exam", e);
        }
        throw new RuntimeException("Insert exam returned no generated key");
    }

    @Override
    public Optional<Exam> findById(Long id) {
        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(FIND_BY_ID)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) {
            log.error("findById exam failed", e);
            throw new RuntimeException(e);
        }
        return Optional.empty();
    }

    @Override
    public List<Exam> findAll() {
        List<Exam> list = new ArrayList<>();
        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(FIND_ALL);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            log.error("findAll exams failed", e);
            throw new RuntimeException(e);
        }
        return list;
    }

    @Override
    public List<Exam> findPublished() {
        List<Exam> list = new ArrayList<>();
        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(FIND_PUBLISHED);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            log.error("findPublished exams failed", e);
            throw new RuntimeException(e);
        }
        return list;
    }

    @Override
    public List<Exam> findByCreator(Long adminId) {
        List<Exam> list = new ArrayList<>();
        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(FIND_BY_CREATOR)) {
            ps.setLong(1, adminId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            log.error("findByCreator exams failed", e);
            throw new RuntimeException(e);
        }
        return list;
    }

    @Override
    public void update(Exam exam) {
        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(UPDATE)) {
            ps.setString(1, exam.title());
            ps.setString(2, exam.description());
            ps.setInt(3, exam.durationMinutes());
            ps.setInt(4, exam.totalMarks());
            ps.setDouble(5, exam.passPercentage());
            ps.setTimestamp(6, exam.startTime() != null ? Timestamp.valueOf(exam.startTime()) : null);
            ps.setTimestamp(7, exam.endTime()   != null ? Timestamp.valueOf(exam.endTime())   : null);
            ps.setBoolean(8, exam.isPublished());
            ps.setBoolean(9, exam.randomiseQuestions());
            ps.setLong(10, exam.id());
            ps.executeUpdate();
        } catch (SQLException e) {
            log.error("update exam failed", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void delete(Long examId) {
        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(DELETE)) {
            ps.setLong(1, examId);
            ps.executeUpdate();
        } catch (SQLException e) {
            log.error("delete exam failed", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void setPublished(Long examId, boolean published) {
        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(SET_PUBLISHED)) {
            ps.setBoolean(1, published);
            ps.setLong(2, examId);
            ps.executeUpdate();
        } catch (SQLException e) {
            log.error("setPublished exam failed", e);
            throw new RuntimeException(e);
        }
    }

    private Exam mapRow(ResultSet rs) throws SQLException {
        Timestamp st = rs.getTimestamp("start_time");
        Timestamp et = rs.getTimestamp("end_time");
        Timestamp ca = rs.getTimestamp("created_at");
        return new Exam(
            rs.getLong("id"),
            rs.getString("title"),
            rs.getString("description"),
            rs.getLong("created_by"),
            rs.getInt("duration_minutes"),
            rs.getInt("total_marks"),
            rs.getDouble("pass_percentage"),
            st != null ? st.toLocalDateTime() : null,
            et != null ? et.toLocalDateTime() : null,
            rs.getBoolean("is_published"),
            rs.getBoolean("randomise_questions"),
            ca != null ? ca.toLocalDateTime() : LocalDateTime.now()
        );
    }
}
