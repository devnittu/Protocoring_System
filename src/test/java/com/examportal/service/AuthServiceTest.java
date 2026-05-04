package com.examportal.service;

import com.examportal.dao.IUserDAO;
import com.examportal.model.User;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mindrot.jbcrypt.BCrypt;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Unit tests for AuthService covering login, register, logout, and session validation. */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private IUserDAO userDAO;
    private AuthService authService;

    private static final String RAW_PASSWORD = "Secret@1234";
    private static final String HASHED       = BCrypt.hashpw(RAW_PASSWORD, BCrypt.gensalt(4));

    private User makeUser(Long id, String email, String hash, boolean active) {
        return new User(id, email, hash, "Test User", User.Role.STUDENT,
                java.time.LocalDateTime.now(), active, null, null);
    }

    @BeforeEach
    void setUp() {
        authService = new AuthService(userDAO);
        SessionManager.clear();
    }

    @AfterEach
    void tearDown() {
        SessionManager.clear();
    }

    // ── Login ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Login with correct credentials returns a valid session token")
    void login_correctCredentials_returnsSessionToken() {
        User stored = makeUser(1L, "alice@test.com", HASHED, true);
        when(userDAO.findByEmail("alice@test.com")).thenReturn(Optional.of(stored));
        doNothing().when(userDAO).updateSessionToken(anyLong(), anyString());
        doNothing().when(userDAO).updateLastLogin(anyLong());
        when(userDAO.findById(1L)).thenReturn(Optional.of(stored.withSessionToken("tok-001")));

        User result = authService.login("alice@test.com", RAW_PASSWORD);

        assertNotNull(result);
        assertNotNull(result.sessionToken());
        assertEquals("alice@test.com", result.email());
        verify(userDAO, times(1)).updateSessionToken(eq(1L), anyString());
    }

    @Test
    @DisplayName("Login with wrong password throws AuthException")
    void login_wrongPassword_throwsAuthException() {
        User stored = makeUser(1L, "alice@test.com", HASHED, true);
        when(userDAO.findByEmail("alice@test.com")).thenReturn(Optional.of(stored));

        AuthException ex = assertThrows(AuthException.class,
                () -> authService.login("alice@test.com", "WrongPassword1"));
        assertTrue(ex.getMessage().toLowerCase().contains("invalid"));
    }

    @Test
    @DisplayName("Login with non-existent email throws AuthException")
    void login_unknownEmail_throwsAuthException() {
        when(userDAO.findByEmail(anyString())).thenReturn(Optional.empty());

        assertThrows(AuthException.class,
                () -> authService.login("nobody@test.com", RAW_PASSWORD));
    }

    @Test
    @DisplayName("Login with inactive account throws AuthException")
    void login_inactiveAccount_throwsAuthException() {
        User inactive = makeUser(2L, "bob@test.com", HASHED, false);
        when(userDAO.findByEmail("bob@test.com")).thenReturn(Optional.of(inactive));

        AuthException ex = assertThrows(AuthException.class,
                () -> authService.login("bob@test.com", RAW_PASSWORD));
        assertTrue(ex.getMessage().toLowerCase().contains("deactivated"));
    }

    @Test
    @DisplayName("Login when already logged in invalidates old session and issues new one")
    void login_alreadyLoggedIn_invalidatesOldToken() {
        User stored = makeUser(1L, "alice@test.com", HASHED, true);
        SessionManager.setCurrentUser(stored.withSessionToken("old-token"));
        when(userDAO.findByEmail("alice@test.com")).thenReturn(Optional.of(stored));
        doNothing().when(userDAO).updateSessionToken(anyLong(), anyString());
        doNothing().when(userDAO).updateLastLogin(anyLong());
        when(userDAO.findById(1L)).thenReturn(Optional.of(stored.withSessionToken("new-token")));

        User result = authService.login("alice@test.com", RAW_PASSWORD);

        assertNotEquals("old-token", result.sessionToken());
        verify(userDAO, times(1)).updateSessionToken(eq(1L), anyString());
    }

    // ── Register ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Register with duplicate email throws DuplicateUserException")
    void register_duplicateEmail_throwsDuplicateUserException() {
        User existing = makeUser(3L, "carol@test.com", HASHED, true);
        when(userDAO.findByEmail("carol@test.com")).thenReturn(Optional.of(existing));

        assertThrows(DuplicateUserException.class,
                () -> authService.register("carol@test.com", "Carol", RAW_PASSWORD));
    }

    @Test
    @DisplayName("Register with short password throws AuthException")
    void register_shortPassword_throwsAuthException() {
        assertThrows(AuthException.class,
                () -> authService.register("new@test.com", "New User", "abc"));
    }

    @Test
    @DisplayName("Register with valid details inserts new user")
    void register_validDetails_insertsUser() {
        when(userDAO.findByEmail("new@test.com")).thenReturn(Optional.empty());
        User saved = makeUser(10L, "new@test.com", HASHED, true);
        when(userDAO.insert(any(User.class))).thenReturn(saved);

        User result = authService.register("new@test.com", "New User", RAW_PASSWORD);

        assertEquals("new@test.com", result.email());
        verify(userDAO, times(1)).insert(any(User.class));
    }

    // ── Logout / Validate ─────────────────────────────────────────────────────

    @Test
    @DisplayName("Logout clears session token in DB and memory")
    void logout_clearsSession() {
        User u = makeUser(1L, "alice@test.com", HASHED, true).withSessionToken("tok");
        SessionManager.setCurrentUser(u);
        doNothing().when(userDAO).updateSessionToken(anyLong(), isNull());

        authService.logout();

        assertNull(SessionManager.getCurrentUser());
        verify(userDAO, times(1)).updateSessionToken(1L, null);
    }

    @Test
    @DisplayName("Validate session with invalid token throws AuthException")
    void validateSession_invalidToken_throwsAuthException() {
        when(userDAO.findBySessionToken("bad-token")).thenReturn(Optional.empty());

        assertThrows(AuthException.class,
                () -> authService.validateSession("bad-token"));
    }
}
