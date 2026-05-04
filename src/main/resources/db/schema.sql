-- ============================================================
-- Secure Online Examination System — Database Schema
-- MySQL 8.0
-- ============================================================

CREATE DATABASE IF NOT EXISTS exam_portal
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE exam_portal;

-- ============================================================
-- TABLE: users
-- ============================================================
CREATE TABLE IF NOT EXISTS users (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    email          VARCHAR(255) NOT NULL,
    password_hash  VARCHAR(255) NOT NULL,
    full_name      VARCHAR(255) NOT NULL,
    role           ENUM('ADMIN','STUDENT') NOT NULL DEFAULT 'STUDENT',
    created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_active      TINYINT(1)   NOT NULL DEFAULT 1,
    last_login     DATETIME     NULL,
    session_token  VARCHAR(36)  NULL,
    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT uq_users_email UNIQUE (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- TABLE: exams
-- ============================================================
CREATE TABLE IF NOT EXISTS exams (
    id                   BIGINT       NOT NULL AUTO_INCREMENT,
    title                VARCHAR(255) NOT NULL,
    description          TEXT         NULL,
    created_by           BIGINT       NOT NULL,
    duration_minutes     INT          NOT NULL DEFAULT 60,
    total_marks          INT          NOT NULL DEFAULT 100,
    pass_percentage      DOUBLE       NOT NULL DEFAULT 40.0,
    start_time           DATETIME     NULL,
    end_time             DATETIME     NULL,
    is_published         TINYINT(1)   NOT NULL DEFAULT 0,
    randomise_questions  TINYINT(1)   NOT NULL DEFAULT 1,
    created_at           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_exams PRIMARY KEY (id),
    CONSTRAINT fk_exams_created_by FOREIGN KEY (created_by) REFERENCES users(id)
        ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- TABLE: questions
-- ============================================================
CREATE TABLE IF NOT EXISTS questions (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    exam_id         BIGINT       NOT NULL,
    body            TEXT         NOT NULL,
    option_a        VARCHAR(500) NOT NULL,
    option_b        VARCHAR(500) NOT NULL,
    option_c        VARCHAR(500) NOT NULL,
    option_d        VARCHAR(500) NOT NULL,
    correct_option  CHAR(1)      NOT NULL,
    marks           INT          NOT NULL DEFAULT 1,
    difficulty      ENUM('EASY','MEDIUM','HARD') NOT NULL DEFAULT 'MEDIUM',
    topic           VARCHAR(100) NULL,
    position        INT          NOT NULL DEFAULT 0,
    CONSTRAINT pk_questions PRIMARY KEY (id),
    CONSTRAINT fk_questions_exam FOREIGN KEY (exam_id) REFERENCES exams(id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT chk_correct_option CHECK (correct_option IN ('A','B','C','D'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- TABLE: attempts
-- ============================================================
CREATE TABLE IF NOT EXISTS attempts (
    id            BIGINT   NOT NULL AUTO_INCREMENT,
    student_id    BIGINT   NOT NULL,
    exam_id       BIGINT   NOT NULL,
    started_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    submitted_at  DATETIME NULL,
    score         DOUBLE   NULL,
    percentage    DOUBLE   NULL,
    passed        TINYINT(1) NULL,
    status        ENUM('IN_PROGRESS','SUBMITTED','FORCE_SUBMITTED','ABANDONED') NOT NULL DEFAULT 'IN_PROGRESS',
    CONSTRAINT pk_attempts PRIMARY KEY (id),
    CONSTRAINT fk_attempts_student FOREIGN KEY (student_id) REFERENCES users(id)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_attempts_exam FOREIGN KEY (exam_id) REFERENCES exams(id)
        ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- TABLE: responses
-- ============================================================
CREATE TABLE IF NOT EXISTS responses (
    id                  BIGINT   NOT NULL AUTO_INCREMENT,
    attempt_id          BIGINT   NOT NULL,
    question_id         BIGINT   NOT NULL,
    chosen_option       CHAR(1)  NULL,
    is_correct          TINYINT(1) NULL,
    time_spent_seconds  INT      NOT NULL DEFAULT 0,
    CONSTRAINT pk_responses PRIMARY KEY (id),
    CONSTRAINT fk_responses_attempt FOREIGN KEY (attempt_id) REFERENCES attempts(id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_responses_question FOREIGN KEY (question_id) REFERENCES questions(id)
        ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- TABLE: activity_logs
-- ============================================================
CREATE TABLE IF NOT EXISTS activity_logs (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    attempt_id   BIGINT       NOT NULL,
    student_id   BIGINT       NOT NULL,
    event_type   ENUM(
                     'TAB_SWITCH','WINDOW_BLUR','INACTIVITY_60S',
                     'FACE_ABSENT','MULTI_FACE','GAZE_AWAY',
                     'EXAM_STARTED','EXAM_SUBMITTED',
                     'EXAM_FORCE_SUBMITTED','COPY_ATTEMPT'
                 ) NOT NULL,
    severity     ENUM('INFO','WARN','CRITICAL') NOT NULL DEFAULT 'INFO',
    detail       VARCHAR(500) NULL,
    frame_path   VARCHAR(300) NULL,
    logged_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_activity_logs PRIMARY KEY (id),
    CONSTRAINT fk_logs_attempt FOREIGN KEY (attempt_id) REFERENCES attempts(id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_logs_student FOREIGN KEY (student_id) REFERENCES users(id)
        ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- INDEXES
-- ============================================================
CREATE INDEX idx_attempts_student   ON attempts(student_id);
CREATE INDEX idx_attempts_exam      ON attempts(exam_id);
CREATE INDEX idx_logs_attempt       ON activity_logs(attempt_id);
CREATE INDEX idx_logs_logged_at     ON activity_logs(logged_at);
CREATE INDEX idx_questions_exam_pos ON questions(exam_id, position);
