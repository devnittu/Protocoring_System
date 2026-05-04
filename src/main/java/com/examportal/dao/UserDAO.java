package com.examportal.dao;

import com.examportal.config.DatabaseConfig;
import com.examportal.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** JDBC implementation of IUserDAO using HikariCP connection pool. */
public class UserDAO implements IUserDAO {

    private static final Logger log = LoggerFactory.getLogger(UserDAO.class);

    private static final String INSERT =
        "INSERT INTO users (email, password_hash, full_name, role, is_active) VALUES (?, ?, ?, ?, ?)";
    private static final String FIND_BY_ID =
        "SELECT * FROM users WHERE id = ?";
    private static final String FIND_BY_EMAIL =
        "SELECT * FROM users WHERE email = ?";
    private static final String FIND_BY_TOKEN =
        "SELECT * FROM users WHERE session_token = ?";
    private static final String FIND_ALL_STUDENTS =
        "SELECT * FROM users WHERE role = 'STUDENT' ORDER BY full_name";
    private static final String FIND_ALL =
        "SELECT * FROM users ORDER BY role, full_name";
    private static final String UPDATE_TOKEN =
        "UPDATE users SET session_token = ? WHERE id = ?";
    private static final String UPDATE_LAST_LOGIN =
        "UPDATE users SET last_login = NOW() WHERE id = ?";
    private static final String UPDATE_PASSWORD =
        "UPDATE users SET password_hash = ? WHERE id = ?";
    private static final String SET_ACTIVE =
        "UPDATE users SET is_active = ? WHERE id = ?";
    private static final String DELETE =
        "DELETE FROM users WHERE id = ?";

    @Override
    public User insert(User user) {
        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(INSERT, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, user.email());
            ps.setString(2, user.passwordHash());
            ps.setString(3, user.fullName());
            ps.setString(4, user.role().name());
            ps.setBoolean(5, user.isActive());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return findById(rs.getLong(1)).orElseThrow();
                }
            }
        } catch (SQLException e) {
            log.error("insert user failed", e);
            throw new RuntimeException("Failed to insert user", e);
        }
        throw new RuntimeException("Insert user returned no generated key");
    }

    @Override
    public Optional<User> findById(Long id) {
        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(FIND_BY_ID)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) {
            log.error("findById failed", e);
            throw new RuntimeException(e);
        }
        return Optional.empty();
    }

    @Override
    public Optional<User> findByEmail(String email) {
        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(FIND_BY_EMAIL)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) {
            log.error("findByEmail failed", e);
            throw new RuntimeException(e);
        }
        return Optional.empty();
    }

    @Override
    public Optional<User> findBySessionToken(String token) {
        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(FIND_BY_TOKEN)) {
            ps.setString(1, token);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) {
            log.error("findBySessionToken failed", e);
            throw new RuntimeException(e);
        }
        return Optional.empty();
    }

    @Override
    public List<User> findAllStudents() {
        List<User> list = new ArrayList<>();
        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(FIND_ALL_STUDENTS);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            log.error("findAllStudents failed", e);
            throw new RuntimeException(e);
        }
        return list;
    }

    @Override
    public List<User> findAll() {
        List<User> list = new ArrayList<>();
        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(FIND_ALL);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            log.error("findAll failed", e);
            throw new RuntimeException(e);
        }
        return list;
    }

    @Override
    public void updateSessionToken(Long userId, String token) {
        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(UPDATE_TOKEN)) {
            if (token == null) ps.setNull(1, Types.VARCHAR);
            else               ps.setString(1, token);
            ps.setLong(2, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            log.error("updateSessionToken failed", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void updateLastLogin(Long userId) {
        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(UPDATE_LAST_LOGIN)) {
            ps.setLong(1, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            log.error("updateLastLogin failed", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void updatePassword(Long userId, String passwordHash) {
        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(UPDATE_PASSWORD)) {
            ps.setString(1, passwordHash);
            ps.setLong(2, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            log.error("updatePassword failed", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void setActive(Long userId, boolean active) {
        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(SET_ACTIVE)) {
            ps.setBoolean(1, active);
            ps.setLong(2, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            log.error("setActive failed", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void delete(Long userId) {
        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(DELETE)) {
            ps.setLong(1, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            log.error("delete user failed", e);
            throw new RuntimeException(e);
        }
    }

    private User mapRow(ResultSet rs) throws SQLException {
        Timestamp lastLogin = rs.getTimestamp("last_login");
        Timestamp createdAt = rs.getTimestamp("created_at");
        return new User(
            rs.getLong("id"),
            rs.getString("email"),
            rs.getString("password_hash"),
            rs.getString("full_name"),
            User.Role.valueOf(rs.getString("role")),
            createdAt != null ? createdAt.toLocalDateTime() : LocalDateTime.now(),
            rs.getBoolean("is_active"),
            lastLogin != null ? lastLogin.toLocalDateTime() : null,
            rs.getString("session_token")
        );
    }
}
