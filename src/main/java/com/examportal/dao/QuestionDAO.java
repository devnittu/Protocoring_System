package com.examportal.dao;

import com.examportal.config.DatabaseConfig;
import com.examportal.model.Question;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** JDBC implementation of IQuestionDAO using HikariCP connection pool. */
public class QuestionDAO implements IQuestionDAO {

    private static final Logger log = LoggerFactory.getLogger(QuestionDAO.class);

    private static final String INSERT =
        """
        INSERT INTO questions (exam_id, body, option_a, option_b, option_c, option_d,
            correct_option, marks, difficulty, topic, position)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;
    private static final String FIND_BY_ID     = "SELECT * FROM questions WHERE id = ?";
    private static final String FIND_BY_EXAM   = "SELECT * FROM questions WHERE exam_id = ? ORDER BY position";
    private static final String UPDATE =
        """
        UPDATE questions SET body=?, option_a=?, option_b=?, option_c=?, option_d=?,
            correct_option=?, marks=?, difficulty=?, topic=?, position=?
        WHERE id=?
        """;
    private static final String DELETE           = "DELETE FROM questions WHERE id = ?";
    private static final String DELETE_BY_EXAM   = "DELETE FROM questions WHERE exam_id = ?";
    private static final String COUNT_BY_EXAM    = "SELECT COUNT(*) FROM questions WHERE exam_id = ?";

    @Override
    public Question insert(Question question) {
        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(INSERT, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, question.examId());
            ps.setString(2, question.body());
            ps.setString(3, question.optionA());
            ps.setString(4, question.optionB());
            ps.setString(5, question.optionC());
            ps.setString(6, question.optionD());
            ps.setString(7, String.valueOf(question.correctOption()));
            ps.setInt(8, question.marks());
            ps.setString(9, question.difficulty().name());
            ps.setString(10, question.topic());
            ps.setInt(11, question.position());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return findById(rs.getLong(1)).orElseThrow();
            }
        } catch (SQLException e) {
            log.error("insert question failed", e);
            throw new RuntimeException("Failed to insert question", e);
        }
        throw new RuntimeException("Insert question returned no generated key");
    }

    @Override
    public Optional<Question> findById(Long id) {
        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(FIND_BY_ID)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) {
            log.error("findById question failed", e);
            throw new RuntimeException(e);
        }
        return Optional.empty();
    }

    @Override
    public List<Question> findByExamId(Long examId) {
        List<Question> list = new ArrayList<>();
        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(FIND_BY_EXAM)) {
            ps.setLong(1, examId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            log.error("findByExamId questions failed", e);
            throw new RuntimeException(e);
        }
        return list;
    }

    @Override
    public void update(Question question) {
        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(UPDATE)) {
            ps.setString(1, question.body());
            ps.setString(2, question.optionA());
            ps.setString(3, question.optionB());
            ps.setString(4, question.optionC());
            ps.setString(5, question.optionD());
            ps.setString(6, String.valueOf(question.correctOption()));
            ps.setInt(7, question.marks());
            ps.setString(8, question.difficulty().name());
            ps.setString(9, question.topic());
            ps.setInt(10, question.position());
            ps.setLong(11, question.id());
            ps.executeUpdate();
        } catch (SQLException e) {
            log.error("update question failed", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void delete(Long questionId) {
        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(DELETE)) {
            ps.setLong(1, questionId);
            ps.executeUpdate();
        } catch (SQLException e) {
            log.error("delete question failed", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void deleteByExamId(Long examId) {
        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(DELETE_BY_EXAM)) {
            ps.setLong(1, examId);
            ps.executeUpdate();
        } catch (SQLException e) {
            log.error("deleteByExamId question failed", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public int countByExamId(Long examId) {
        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(COUNT_BY_EXAM)) {
            ps.setLong(1, examId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            log.error("countByExamId question failed", e);
            throw new RuntimeException(e);
        }
        return 0;
    }

    private Question mapRow(ResultSet rs) throws SQLException {
        String diffStr = rs.getString("difficulty");
        return new Question(
            rs.getLong("id"),
            rs.getLong("exam_id"),
            rs.getString("body"),
            rs.getString("option_a"),
            rs.getString("option_b"),
            rs.getString("option_c"),
            rs.getString("option_d"),
            rs.getString("correct_option").charAt(0),
            rs.getInt("marks"),
            Question.Difficulty.valueOf(diffStr),
            rs.getString("topic"),
            rs.getInt("position")
        );
    }
}
