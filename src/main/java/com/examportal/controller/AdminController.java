package com.examportal.controller;

import com.examportal.model.Exam;
import com.examportal.model.Question;
import com.examportal.model.User;
import com.examportal.service.AuthService;
import com.examportal.service.ExamService;
import com.examportal.service.ResultService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/** Thin admin controller: delegates CRUD for exams, questions, and users to services. */
public class AdminController {

    private static final Logger log = LoggerFactory.getLogger(AdminController.class);

    private final ExamService   examService;
    private final AuthService   authService;
    private final ResultService resultService;

    public AdminController(ExamService examService, AuthService authService, ResultService resultService) {
        this.examService   = examService;
        this.authService   = authService;
        this.resultService = resultService;
    }

    // ── Exam management ──────────────────────────────────────────────────────

    public List<Exam> getAllExams()                       { return examService.getAllExams(); }
    public Exam createExam(Exam exam)                    { return examService.createExam(exam); }
    public void updateExam(Exam exam)                    { examService.updateExam(exam); }
    public void deleteExam(Long examId)                  { examService.deleteExam(examId); }
    public void setPublished(Long examId, boolean state) { examService.setPublished(examId, state); }

    // ── Question management ───────────────────────────────────────────────────

    public List<Question> getQuestionsForExam(Long examId) {
        return examService.getQuestionsOrdered(examId);
    }
    public Question addQuestion(Question q)    { return examService.addQuestion(q); }
    public void updateQuestion(Question q)     { examService.updateQuestion(q); }
    public void deleteQuestion(Long questionId){ examService.deleteQuestion(questionId); }

    // ── Student/User management ──────────────────────────────────────────────

    public List<User> getAllStudents()                         { return authService.getAllStudents(); }
    public void setUserActive(Long userId, boolean active)    { authService.setUserActive(userId, active); }
    public void deleteUser(Long userId)                       { authService.deleteUser(userId); }

    // ── Stats ────────────────────────────────────────────────────────────────

    public long getAttemptsToday()   { return resultService.getAttemptsToday(); }
    public long getFlaggedSessions() { return resultService.getFlaggedSessions(); }
}
