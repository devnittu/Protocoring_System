package com.examportal.dao;

import com.examportal.model.Exam;

import java.util.List;
import java.util.Optional;

/** Contract for all Exam persistence operations. */
public interface IExamDAO {
    Exam           insert(Exam exam);
    Optional<Exam> findById(Long id);
    List<Exam>     findAll();
    List<Exam>     findPublished();
    List<Exam>     findByCreator(Long adminId);
    void           update(Exam exam);
    void           delete(Long examId);
    void           setPublished(Long examId, boolean published);
}
