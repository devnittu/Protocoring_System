package com.examportal.controller;

import com.examportal.model.Attempt;
import com.examportal.model.Exam;
import com.examportal.model.Question;
import com.examportal.service.EvaluatorService;
import com.examportal.service.ExamService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/** Thin controller managing exam start, answer recording, submission, and timeout. */
public class ExamController {

    private static final Logger log = LoggerFactory.getLogger(ExamController.class);

    private final ExamService      examService;
    private final EvaluatorService evaluatorService;

    public ExamController(ExamService examService, EvaluatorService evaluatorService) {
        this.examService      = examService;
        this.evaluatorService = evaluatorService;
    }

    /** Starts a new attempt for the student on the given exam. */
    public Attempt startAttempt(Long studentId, Long examId) {
        return examService.startAttempt(studentId, examId);
    }

    /** Returns questions for the attempt (potentially shuffled). */
    public List<Question> getQuestionsForAttempt(Long examId, boolean randomise) {
        return examService.getQuestionsForAttempt(examId, randomise);
    }

    /** Returns remaining time in seconds for an attempt. */
    public long getRemainingSeconds(Attempt attempt, Exam exam) {
        return examService.getRemainingSeconds(attempt, exam);
    }

    /**
     * Evaluates responses and finalises the attempt as SUBMITTED.
     */
    public Attempt submitAttempt(Long attemptId, List<Question> questions,
                                 Map<Long, Character> answers, Exam exam) {
        log.info("Student submitting attemptId={}", attemptId);
        return evaluatorService.evaluate(attemptId, questions, answers, exam, Attempt.Status.SUBMITTED);
    }

    /**
     * Force-submits the attempt (timeout or proctoring violation).
     */
    public Attempt forceSubmitAttempt(Long attemptId, List<Question> questions,
                                      Map<Long, Character> answers, Exam exam) {
        log.warn("Force-submitting attemptId={}", attemptId);
        return evaluatorService.evaluate(attemptId, questions, answers, exam, Attempt.Status.FORCE_SUBMITTED);
    }

    /** Loads an exam by id. */
    public Exam getExam(Long examId) {
        return examService.findExamById(examId)
                .orElseThrow(() -> new IllegalArgumentException("Exam not found: " + examId));
    }
}
