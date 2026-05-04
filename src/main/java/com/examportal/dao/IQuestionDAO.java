package com.examportal.dao;

import com.examportal.model.Question;

import java.util.List;
import java.util.Optional;

/** Contract for all Question persistence operations. */
public interface IQuestionDAO {
    Question           insert(Question question);
    Optional<Question> findById(Long id);
    List<Question>     findByExamId(Long examId);
    void               update(Question question);
    void               delete(Long questionId);
    void               deleteByExamId(Long examId);
    int                countByExamId(Long examId);
}
