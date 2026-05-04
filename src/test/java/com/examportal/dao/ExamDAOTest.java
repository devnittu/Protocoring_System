package com.examportal.dao;

import com.examportal.model.Exam;
import org.junit.jupiter.api.*;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/** Integration-style ExamDAO tests using H2 in-memory database. */
class ExamDAOTest {

    private static Connection connection;
    private ExamDAO dao;

    @BeforeAll
    static void setUpDatabase() throws Exception {
        Class.forName("org.h2.Driver");
        connection = DriverManager.getConnection(
            "jdbc:h2:mem:examtestdb;DB_CLOSE_DELAY=-1;MODE=MySQL", "sa", "");

        try (Statement st = connection.createStatement()) {
            st.execute("""
                CREATE TABLE IF NOT EXISTS users (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    email VARCHAR(255) NOT NULL UNIQUE,
                    password_hash VARCHAR(255) NOT NULL,
                    full_name VARCHAR(255) NOT NULL,
                    role VARCHAR(20) NOT NULL DEFAULT 'ADMIN',
                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    is_active BOOLEAN NOT NULL DEFAULT TRUE,
                    last_login TIMESTAMP NULL,
                    session_token VARCHAR(36) NULL
                )
            """);
            st.execute("INSERT INTO users (email,password_hash,full_name,role) VALUES ('admin@t.com','hash','Admin','ADMIN')");
            st.execute("""
                CREATE TABLE IF NOT EXISTS exams (
                    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
                    title               VARCHAR(255) NOT NULL,
                    description         TEXT NULL,
                    created_by          BIGINT NOT NULL,
                    duration_minutes    INT NOT NULL DEFAULT 60,
                    total_marks         INT NOT NULL DEFAULT 100,
                    pass_percentage     DOUBLE NOT NULL DEFAULT 40.0,
                    start_time          TIMESTAMP NULL,
                    end_time            TIMESTAMP NULL,
                    is_published        BOOLEAN NOT NULL DEFAULT FALSE,
                    randomise_questions BOOLEAN NOT NULL DEFAULT TRUE,
                    created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                )
            """);
        }
        TestDatabaseConfig2.initH2("jdbc:h2:mem:examtestdb;DB_CLOSE_DELAY=-1;MODE=MySQL");
    }

    @BeforeEach
    void setUp() throws Exception {
        dao = new ExamDAO();
        try (Statement st = connection.createStatement()) {
            st.execute("DELETE FROM exams");
        }
    }

    @AfterAll
    static void closeDatabase() throws Exception {
        if (connection != null && !connection.isClosed()) connection.close();
    }

    private Exam makeExam(String title) {
        return new Exam(null, title, "Desc", 1L, 60, 100, 40.0,
                LocalDateTime.now(), LocalDateTime.now().plusDays(30), false, true, null);
    }

    @Test
    @DisplayName("insert exam → findById returns same exam")
    void insertAndFindById() {
        Exam saved = dao.insert(makeExam("Java Fundamentals"));
        assertNotNull(saved.id());
        Optional<Exam> found = dao.findById(saved.id());
        assertTrue(found.isPresent());
        assertEquals("Java Fundamentals", found.get().title());
    }

    @Test
    @DisplayName("update exam → changes persisted")
    void update_changesPersisted() {
        Exam saved = dao.insert(makeExam("Original Title"));
        Exam updated = new Exam(saved.id(), "Updated Title", "New Desc", 1L,
                90, 50, 60.0, saved.startTime(), saved.endTime(), false, true, null);
        dao.update(updated);
        Optional<Exam> found = dao.findById(saved.id());
        assertTrue(found.isPresent());
        assertEquals("Updated Title", found.get().title());
        assertEquals(90, found.get().durationMinutes());
    }

    @Test
    @DisplayName("delete exam → findById returns empty")
    void delete_findByIdReturnsEmpty() {
        Exam saved = dao.insert(makeExam("To Delete"));
        dao.delete(saved.id());
        assertTrue(dao.findById(saved.id()).isEmpty());
    }

    @Test
    @DisplayName("setPublished → is_published flag updated")
    void setPublished_updatesFlag() {
        Exam saved = dao.insert(makeExam("Pub Test"));
        assertFalse(dao.findById(saved.id()).get().isPublished());
        dao.setPublished(saved.id(), true);
        assertTrue(dao.findById(saved.id()).get().isPublished());
    }

    @Test
    @DisplayName("findByCreator → returns only that creator's exams")
    void findByCreator_returnsOnlyThatCreatorsExams() {
        dao.insert(new Exam(null,"E1","D",1L,60,100,40,null,null,false,true,null));
        dao.insert(new Exam(null,"E2","D",1L,60,100,40,null,null,false,true,null));
        List<Exam> found = dao.findByCreator(1L);
        assertEquals(2, found.size());
        found.forEach(e -> assertEquals(1L, e.createdBy()));
    }
}
