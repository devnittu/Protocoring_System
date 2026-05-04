-- ============================================================
-- Seed Data: Admin + 2 Students + 1 Exam + 10 Questions
-- ============================================================
USE exam_portal;

-- ============================================================
-- USERS
-- admin@exam.local / Admin@1234  (BCrypt hash)
-- student1@exam.local / Student@1234
-- student2@exam.local / Student@1234
-- ============================================================
INSERT INTO users (email, password_hash, full_name, role, is_active) VALUES
(
    'admin@exam.local',
    '$2a$12$KIXz7N9y1u9K.rU5QJpz5.E3N4SzD0kM9sU5wJ4hUy9bL2rVsj6gO',
    'System Administrator',
    'ADMIN',
    1
),
(
    'student1@exam.local',
    '$2a$12$6z4hQf0gBY7sR1kPbJ3W6e8NzF2aP.lT1mU7vK9dX3cE5hWjLi8IO',
    'Alice Johnson',
    'STUDENT',
    1
),
(
    'student2@exam.local',
    '$2a$12$6z4hQf0gBY7sR1kPbJ3W6e8NzF2aP.lT1mU7vK9dX3cE5hWjLi8IO',
    'Bob Williams',
    'STUDENT',
    1
);

-- ============================================================
-- EXAM
-- ============================================================
INSERT INTO exams (
    title, description, created_by, duration_minutes,
    total_marks, pass_percentage, start_time, end_time,
    is_published, randomise_questions
) VALUES (
    'Introduction to Computer Science',
    'A comprehensive exam covering fundamentals of computer science including algorithms, data structures, operating systems, and networking.',
    1,
    60,
    20,
    50.0,
    NOW(),
    DATE_ADD(NOW(), INTERVAL 30 DAY),
    1,
    1
);

-- ============================================================
-- QUESTIONS (exam_id = 1)
-- ============================================================
INSERT INTO questions (exam_id, body, option_a, option_b, option_c, option_d, correct_option, marks, difficulty, topic, position) VALUES
(1,
 'What is the time complexity of binary search on a sorted array?',
 'O(n)', 'O(log n)', 'O(n log n)', 'O(1)',
 'B', 2, 'EASY', 'Algorithms', 1),

(1,
 'Which data structure uses LIFO (Last In, First Out) ordering?',
 'Queue', 'Heap', 'Stack', 'Linked List',
 'C', 2, 'EASY', 'Data Structures', 2),

(1,
 'Which sorting algorithm has the best average-case time complexity?',
 'Bubble Sort', 'Selection Sort', 'Merge Sort', 'Insertion Sort',
 'C', 2, 'MEDIUM', 'Algorithms', 3),

(1,
 'In object-oriented programming, what is encapsulation?',
 'Inheriting properties from a parent class',
 'Bundling data and methods that operate on the data within one unit',
 'Creating multiple forms of the same method',
 'Hiding the implementation details and showing only functionality',
 'B', 2, 'MEDIUM', 'OOP Concepts', 4),

(1,
 'What does CPU stand for?',
 'Central Processing Unit',
 'Core Processing Utility',
 'Central Program Unit',
 'Computed Processing Unit',
 'A', 2, 'EASY', 'Computer Architecture', 5),

(1,
 'Which OSI layer is responsible for end-to-end communication and error recovery?',
 'Network Layer', 'Data Link Layer', 'Transport Layer', 'Session Layer',
 'C', 2, 'MEDIUM', 'Networking', 6),

(1,
 'What is a deadlock in operating systems?',
 'A situation where a process uses 100% CPU',
 'A situation where two or more processes are waiting indefinitely for each other to release resources',
 'A situation where memory allocation fails',
 'A situation where I/O operations block the CPU',
 'B', 2, 'HARD', 'Operating Systems', 7),

(1,
 'Which of the following is a non-volatile storage device?',
 'RAM', 'Cache', 'CPU Registers', 'SSD',
 'D', 2, 'EASY', 'Computer Hardware', 8),

(1,
 'What does SQL stand for?',
 'Structured Query Language',
 'Simple Query Language',
 'Standard Query Logic',
 'Structured Question Logic',
 'A', 2, 'EASY', 'Databases', 9),

(1,
 'Which design pattern ensures a class has only one instance and provides a global access point to it?',
 'Factory Pattern', 'Observer Pattern', 'Singleton Pattern', 'Decorator Pattern',
 'C', 2, 'HARD', 'Design Patterns', 10);
