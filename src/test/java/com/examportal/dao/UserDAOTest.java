package com.examportal.dao;

import com.examportal.model.User;
import org.junit.jupiter.api.*;

import java.sql.*;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration-style DAO tests for UserDAO using H2 in-memory database.
 * H2 is auto-configured via a test-only DatabaseConfig override.
 */
class UserDAOTest {

    private static Connection connection;
    private UserDAO dao;

    @BeforeAll
    static void setUpDatabase() throws Exception {
        Class.forName("org.h2.Driver");
        connection = DriverManager.getConnection(
            "jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;MODE=MySQL", "sa", "");
        try (Statement st = connection.createStatement()) {
            st.execute("""
                CREATE TABLE IF NOT EXISTS users (
                    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
                    email         VARCHAR(255) NOT NULL UNIQUE,
                    password_hash VARCHAR(255) NOT NULL,
                    full_name     VARCHAR(255) NOT NULL,
                    role          VARCHAR(20)  NOT NULL DEFAULT 'STUDENT',
                    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    is_active     BOOLEAN      NOT NULL DEFAULT TRUE,
                    last_login    TIMESTAMP    NULL,
                    session_token VARCHAR(36)  NULL
                )
            """);
        }
        // Point DatabaseConfig to H2
        System.setProperty("test.db.url", "jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;MODE=MySQL");
        System.setProperty("test.db.user", "sa");
        System.setProperty("test.db.password", "");
        TestDatabaseConfig.initH2();
    }

    @BeforeEach
    void setUp() throws Exception {
        dao = new UserDAO();
        try (Statement st = connection.createStatement()) {
            st.execute("DELETE FROM users");
        }
    }

    @AfterEach
    void tearDown() {}

    @AfterAll
    static void closeDatabase() throws Exception {
        if (connection != null && !connection.isClosed()) connection.close();
    }

    @Test
    @DisplayName("insert → findById returns same user")
    void insertAndFindById() {
        User user = new User(null, "alice@test.com",
                "$2a$12$hash", "Alice", User.Role.STUDENT,
                null, true, null, null);
        User saved = dao.insert(user);
        assertNotNull(saved.id());
        Optional<User> found = dao.findById(saved.id());
        assertTrue(found.isPresent());
        assertEquals("alice@test.com", found.get().email());
        assertEquals("Alice", found.get().fullName());
    }

    @Test
    @DisplayName("findByEmail returns correct user")
    void findByEmail_returnsCorrectUser() {
        User user = new User(null, "bob@test.com", "$2a$hash", "Bob",
                User.Role.STUDENT, null, true, null, null);
        dao.insert(user);
        Optional<User> found = dao.findByEmail("bob@test.com");
        assertTrue(found.isPresent());
        assertEquals("bob@test.com", found.get().email());
    }

    @Test
    @DisplayName("updateSessionToken persists token correctly")
    void updateSessionToken_persistsToken() {
        User user = new User(null, "carol@test.com", "$2a$hash", "Carol",
                User.Role.STUDENT, null, true, null, null);
        User saved = dao.insert(user);
        dao.updateSessionToken(saved.id(), "my-token-abc");
        Optional<User> found = dao.findBySessionToken("my-token-abc");
        assertTrue(found.isPresent());
        assertEquals("carol@test.com", found.get().email());
    }

    @Test
    @DisplayName("setActive to false → findById shows inactive")
    void setActive_updatesFlag() {
        User user = new User(null, "dave@test.com", "$2a$hash", "Dave",
                User.Role.STUDENT, null, true, null, null);
        User saved = dao.insert(user);
        dao.setActive(saved.id(), false);
        Optional<User> found = dao.findById(saved.id());
        assertTrue(found.isPresent());
        assertFalse(found.get().isActive());
    }

    @Test
    @DisplayName("delete → findById returns empty")
    void delete_findByIdReturnsEmpty() {
        User user = new User(null, "eve@test.com", "$2a$hash", "Eve",
                User.Role.STUDENT, null, true, null, null);
        User saved = dao.insert(user);
        dao.delete(saved.id());
        assertTrue(dao.findById(saved.id()).isEmpty());
    }

    @Test
    @DisplayName("findAllStudents returns only STUDENT role users")
    void findAllStudents_returnsOnlyStudents() {
        dao.insert(new User(null,"s1@t.com","h","S1",User.Role.STUDENT,null,true,null,null));
        dao.insert(new User(null,"s2@t.com","h","S2",User.Role.STUDENT,null,true,null,null));
        dao.insert(new User(null,"a1@t.com","h","A1",User.Role.ADMIN,  null,true,null,null));
        List<User> students = dao.findAllStudents();
        assertEquals(2, students.size());
        students.forEach(u -> assertEquals(User.Role.STUDENT, u.role()));
    }
}
