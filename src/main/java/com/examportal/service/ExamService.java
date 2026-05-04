package com.examportal.service;

import com.examportal.dao.IExamDAO;
import com.examportal.dao.IQuestionDAO;
import com.examportal.dao.IAttemptDAO;
import com.examportal.model.Attempt;
import com.examportal.model.Exam;
import com.examportal.model.Question;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/** Manages exam lifecycle: retrieval, question ordering, attempt creation, and time enforcement. */
public class ExamService {

    private static final Logger log = LoggerFactory.getLogger(ExamService.class);

    private final IExamDAO     examDAO;
    private final IQuestionDAO questionDAO;
    private final IAttemptDAO  attemptDAO;

    public ExamService(IExamDAO examDAO, IQuestionDAO questionDAO, IAttemptDAO attemptDAO) {
        this.examDAO     = examDAO;
        this.questionDAO = questionDAO;
        this.attemptDAO  = attemptDAO;
    }

    /** Returns all exams (admin view). */
    public List<Exam> getAllExams() { return examDAO.findAll(); }

    /** Returns all published exams available to students. */
    public List<Exam> getPublishedExams() { return examDAO.findPublished(); }

    /** Returns a single exam by id. */
    public Optional<Exam> findExamById(Long id) { return examDAO.findById(id); }

    /** Returns exams created by a specific admin. */
    public List<Exam> getExamsByCreator(Long adminId) { return examDAO.findByCreator(adminId); }

    /**
     * Creates a new exam record.
     * @throws IllegalArgumentException if validation fails
     */
    public Exam createExam(Exam exam) {
        validateExam(exam);
        Exam saved = examDAO.insert(exam);
        log.info("Exam created: id={}, title={}", saved.id(), saved.title());
        return saved;
    }

    /** Updates an existing exam. */
    public void updateExam(Exam exam) {
        validateExam(exam);
        examDAO.update(exam);
        log.info("Exam updated: id={}", exam.id());
    }

    /** Deletes an exam and all its questions (cascade). */
    public void deleteExam(Long examId) {
        examDAO.delete(examId);
        log.info("Exam deleted: id={}", examId);
    }

    /** Publishes or unpublishes an exam. */
    public void setPublished(Long examId, boolean published) {
        examDAO.setPublished(examId, published);
    }

    /** Returns questions for an exam, optionally shuffled per-attempt. */
    public List<Question> getQuestionsForAttempt(Long examId, boolean randomise) {
        List<Question> questions = new ArrayList<>(questionDAO.findByExamId(examId));
        if (randomise) Collections.shuffle(questions);
        return questions;
    }

    /** Returns ordered questions for a given exam (admin view). */
    public List<Question> getQuestionsOrdered(Long examId) {
        return questionDAO.findByExamId(examId);
    }

    /**
     * Starts a new attempt for a student on an exam.
     * Prevents duplicate in-progress attempts.
     */
    public Attempt startAttempt(Long studentId, Long examId) {
        Exam exam = examDAO.findById(examId)
                .orElseThrow(() -> new IllegalArgumentException("Exam not found: " + examId));

        if (!exam.isAvailableNow()) {
            throw new IllegalStateException("Exam is not currently available.");
        }

        // If there's already an in-progress attempt, RESUME it (don't block re-entry)
        var existing = attemptDAO.findInProgressByStudentAndExam(studentId, examId);
        if (existing.isPresent()) {
            log.info("Resuming in-progress attempt id={} for student={}", existing.get().id(), studentId);
            return existing.get();
        }

        Attempt newAttempt = new Attempt(null, studentId, examId,
                LocalDateTime.now(), null, null, null, null, Attempt.Status.IN_PROGRESS);
        Attempt saved = attemptDAO.insert(newAttempt);
        log.info("Attempt started: studentId={}, examId={}, attemptId={}", studentId, examId, saved.id());
        return saved;
    }

    /** Returns the remaining seconds for a given attempt. Returns 0 if timed out. */
    public long getRemainingSeconds(Attempt attempt, Exam exam) {
        long elapsed = java.time.Duration.between(attempt.startedAt(), LocalDateTime.now()).toSeconds();
        long total   = (long) exam.durationMinutes() * 60L;
        return Math.max(0, total - elapsed);
    }

    /** Question CRUD */
    public Question addQuestion(Question question) {
        Question saved = questionDAO.insert(question);
        log.info("Question added: id={}, examId={}", saved.id(), saved.examId());
        return saved;
    }

    public void updateQuestion(Question question) { questionDAO.update(question); }

    public void deleteQuestion(Long questionId) { questionDAO.delete(questionId); }

    public Optional<Question> findQuestionById(Long id) { return questionDAO.findById(id); }

    private void validateExam(Exam exam) {
        if (exam.title() == null || exam.title().isBlank())
            throw new IllegalArgumentException("Exam title is required.");
        if (exam.durationMinutes() <= 0)
            throw new IllegalArgumentException("Duration must be positive.");
        if (exam.totalMarks() <= 0)
            throw new IllegalArgumentException("Total marks must be positive.");
        if (exam.passPercentage() < 0 || exam.passPercentage() > 100)
            throw new IllegalArgumentException("Pass percentage must be between 0 and 100.");
    }
}
