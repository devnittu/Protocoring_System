package com.examportal.dao;

import com.examportal.model.Response;

import java.util.List;
import java.util.Optional;

/** Contract for all Response persistence operations. */
public interface IResponseDAO {
    Response           insert(Response response);
    Optional<Response> findById(Long id);
    List<Response>     findByAttemptId(Long attemptId);
    void               upsert(Response response);
    void               deleteByAttemptId(Long attemptId);
}
