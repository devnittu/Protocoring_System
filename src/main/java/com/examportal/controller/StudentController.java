package com.examportal.controller;

import com.examportal.model.Attempt;
import com.examportal.model.Exam;
import com.examportal.model.User;
import com.examportal.service.ExamService;
import com.examportal.service.ResultService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/** Thin student controller: loads available exams and past attempts. */
public class StudentController {

    private static final Logger log = LoggerFactory.getLogger(StudentController.class);

    private final ExamService   examService;
    private final ResultService resultService;

    public StudentController(ExamService examService, ResultService resultService) {
        this.examService   = examService;
        this.resultService = resultService;
    }

    /** Returns all currently available (published, in-window) exams. */
    public List<Exam> getAvailableExams() {
        return examService.getPublishedExams()
                .stream()
                .filter(Exam::isAvailableNow)
                .toList();
    }

    /** Returns all exam attempts for a student. */
    public List<Attempt> getMyAttempts(Long studentId) {
        return resultService.getAttemptsByStudent(studentId);
    }
}
