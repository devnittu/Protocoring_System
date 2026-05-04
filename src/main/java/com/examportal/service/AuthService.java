package com.examportal.service;

import com.examportal.dao.IUserDAO;
import com.examportal.model.User;
import com.examportal.util.BCryptUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Handles user registration, login, session management, and logout. */
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private final IUserDAO userDAO;

    public AuthService(IUserDAO userDAO) {
        this.userDAO = userDAO;
    }

    /**
     * Authenticates a user with email and plaintext password.
     * Issues a new session token, invalidating any existing one.
     *
     * @throws AuthException          if email not found, password wrong, or account inactive
     */
    public User login(String email, String plainPassword) {
        if (email == null || email.isBlank())         throw new AuthException("Email is required.");
        if (plainPassword == null || plainPassword.isBlank()) throw new AuthException("Password is required.");

        User user = userDAO.findByEmail(email.trim().toLowerCase())
                .orElseThrow(() -> new AuthException("Invalid email or password."));

        if (!user.isActive()) throw new AuthException("Account is deactivated. Contact admin.");

        if (!BCryptUtil.verify(plainPassword, user.passwordHash())) {
            throw new AuthException("Invalid email or password.");
        }

        // Issue a fresh session token (invalidates any previous session)
        String token = UUID.randomUUID().toString();
        userDAO.updateSessionToken(user.id(), token);
        userDAO.updateLastLogin(user.id());

        User loggedIn = user.withSessionToken(token);
        SessionManager.setCurrentUser(loggedIn);
        log.info("User logged in: {} [{}]", loggedIn.email(), loggedIn.role());
        return loggedIn;
    }

    /**
     * Registers a new student account.
     *
     * @throws DuplicateUserException if email already exists
     * @throws AuthException          if validation fails
     */
    public User register(String email, String fullName, String plainPassword) {
        if (email == null || email.isBlank())         throw new AuthException("Email is required.");
        if (fullName == null || fullName.isBlank())   throw new AuthException("Full name is required.");
        if (plainPassword == null || plainPassword.length() < 8)
            throw new AuthException("Password must be at least 8 characters.");

        String normalizedEmail = email.trim().toLowerCase();
        if (userDAO.findByEmail(normalizedEmail).isPresent()) {
            throw new DuplicateUserException(normalizedEmail);
        }

        String hash = BCryptUtil.hash(plainPassword);
        User newUser = new User(null, normalizedEmail, hash, fullName.trim(),
                User.Role.STUDENT, null, true, null, null);
        User saved = userDAO.insert(newUser);
        log.info("New student registered: {}", saved.email());
        return saved;
    }

    /**
     * Logs out the current user — clears session token in DB and in memory.
     */
    public void logout() {
        User current = SessionManager.getCurrentUser();
        if (current != null) {
            userDAO.updateSessionToken(current.id(), null);
            log.info("User logged out: {}", current.email());
        }
        SessionManager.clear();
    }

    /**
     * Validates a session token against the database.
     *
     * @return the User if token is valid
     * @throws AuthException if token is invalid or expired
     */
    public User validateSession(String token) {
        if (token == null || token.isBlank()) throw new AuthException("No session token.");
        return userDAO.findBySessionToken(token)
                .orElseThrow(() -> new AuthException("Session expired. Please log in again."));
    }

    /** Returns all student accounts. */
    public List<User> getAllStudents() {
        return userDAO.findAllStudents();
    }

    /** Returns all users. */
    public List<User> getAllUsers() {
        return userDAO.findAll();
    }

    /** Toggles active status for a user account. */
    public void setUserActive(Long userId, boolean active) {
        userDAO.setActive(userId, active);
    }

    /** Finds a user by id. */
    public Optional<User> findById(Long id) {
        return userDAO.findById(id);
    }

    /** Deletes a user account permanently. */
    public void deleteUser(Long userId) {
        userDAO.delete(userId);
        log.info("User account deleted: id={}", userId);
    }
}
