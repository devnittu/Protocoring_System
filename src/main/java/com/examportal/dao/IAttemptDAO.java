package com.examportal.dao;

import com.examportal.model.Attempt;

import java.util.List;
import java.util.Optional;

/** Contract for all Attempt persistence operations. */
public interface IAttemptDAO {
    Attempt           insert(Attempt attempt);
    Optional<Attempt> findById(Long id);
    List<Attempt>     findByStudentId(Long studentId);
    List<Attempt>     findByExamId(Long examId);
    Optional<Attempt> findInProgressByStudentAndExam(Long studentId, Long examId);
    void              updateStatus(Long attemptId, Attempt.Status status);
    void              finalise(Long attemptId, double score, double percentage, boolean passed, Attempt.Status status);
    long              countAttemptsToday();
    long              countFlaggedSessions();
}
