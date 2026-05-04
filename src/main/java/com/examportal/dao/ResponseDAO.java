package com.examportal.dao;

import com.examportal.config.DatabaseConfig;
import com.examportal.model.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** JDBC implementation of IResponseDAO using HikariCP connection pool. */
public class ResponseDAO implements IResponseDAO {

    private static final Logger log = LoggerFactory.getLogger(ResponseDAO.class);

    private static final String INSERT =
        "INSERT INTO responses (attempt_id, question_id, chosen_option, is_correct, time_spent_seconds) VALUES (?, ?, ?, ?, ?)";
    private static final String FIND_BY_ID =
        "SELECT * FROM responses WHERE id = ?";
    private static final String FIND_BY_ATTEMPT =
        "SELECT * FROM responses WHERE attempt_id = ? ORDER BY question_id";
    private static final String UPSERT =
        """
        INSERT INTO responses (attempt_id, question_id, chosen_option, is_correct, time_spent_seconds)
        VALUES (?, ?, ?, ?, ?)
        ON DUPLICATE KEY UPDATE chosen_option=VALUES(chosen_option),
            is_correct=VALUES(is_correct), time_spent_seconds=VALUES(time_spent_seconds)
        """;
    private static final String DELETE_BY_ATTEMPT =
        "DELETE FROM responses WHERE attempt_id = ?";

    @Override
    public Response insert(Response response) {
        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(INSERT, Statement.RETURN_GENERATED_KEYS)) {
            bindResponse(ps, response);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return findById(rs.getLong(1)).orElseThrow();
            }
        } catch (SQLException e) {
            log.error("insert response failed", e);
            throw new RuntimeException("Failed to insert response", e);
        }
        throw new RuntimeException("Insert response returned no generated key");
    }

    @Override
    public Optional<Response> findById(Long id) {
        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(FIND_BY_ID)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) {
            log.error("findById response failed", e);
            throw new RuntimeException(e);
        }
        return Optional.empty();
    }

    @Override
    public List<Response> findByAttemptId(Long attemptId) {
        List<Response> list = new ArrayList<>();
        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(FIND_BY_ATTEMPT)) {
            ps.setLong(1, attemptId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            log.error("findByAttemptId response failed", e);
            throw new RuntimeException(e);
        }
        return list;
    }

    @Override
    public void upsert(Response response) {
        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(UPSERT)) {
            bindResponse(ps, response);
            ps.executeUpdate();
        } catch (SQLException e) {
            log.error("upsert response failed", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void deleteByAttemptId(Long attemptId) {
        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(DELETE_BY_ATTEMPT)) {
            ps.setLong(1, attemptId);
            ps.executeUpdate();
        } catch (SQLException e) {
            log.error("deleteByAttemptId response failed", e);
            throw new RuntimeException(e);
        }
    }

    private void bindResponse(PreparedStatement ps, Response r) throws SQLException {
        ps.setLong(1, r.attemptId());
        ps.setLong(2, r.questionId());
        if (r.chosenOption() != null) ps.setString(3, String.valueOf(r.chosenOption()));
        else                          ps.setNull(3, Types.CHAR);
        if (r.isCorrect() != null) ps.setBoolean(4, r.isCorrect());
        else                       ps.setNull(4, Types.BOOLEAN);
        ps.setInt(5, r.timeSpentSeconds());
    }

    private Response mapRow(ResultSet rs) throws SQLException {
        String chosen = rs.getString("chosen_option");
        Object correct = rs.getObject("is_correct");
        return new Response(
            rs.getLong("id"),
            rs.getLong("attempt_id"),
            rs.getLong("question_id"),
            chosen  != null ? chosen.charAt(0) : null,
            correct != null ? ((Number) correct).intValue() == 1 : null,
            rs.getInt("time_spent_seconds")
        );
    }
}
