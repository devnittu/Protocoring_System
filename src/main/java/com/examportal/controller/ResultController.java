package com.examportal.controller;

import com.examportal.model.ActivityLog;
import com.examportal.model.Attempt;
import com.examportal.model.Question;
import com.examportal.model.Response;
import com.examportal.service.ExamService;
import com.examportal.service.ResultService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/** Thin controller for fetching and exporting exam result data. */
public class ResultController {

    private static final Logger log = LoggerFactory.getLogger(ResultController.class);

    private final ResultService resultService;
    private final ExamService   examService;

    public ResultController(ResultService resultService, ExamService examService) {
        this.resultService = resultService;
        this.examService   = examService;
    }

    public List<Attempt>     getStudentAttempts(Long studentId)  { return resultService.getAttemptsByStudent(studentId); }
    public List<Response>    getResponses(Long attemptId)         { return resultService.getResponsesForAttempt(attemptId); }
    public List<ActivityLog> getLogs(Long attemptId)              { return resultService.getLogsForAttempt(attemptId); }
    public Map<String, Long> getEventSummary(Long attemptId)      { return resultService.getEventSummary(attemptId); }
    public long              getCriticalCount(Long attemptId)     { return resultService.getCriticalCount(attemptId); }
    public List<ActivityLog> getAllLogs()                         { return resultService.getAllLogs(); }

    /** Exports a result to CSV at the given file path. */
    public void exportCsv(Attempt attempt, Long examId, File dest) throws IOException {
        List<Question> questions = examService.getQuestionsOrdered(examId);
        List<Response> responses = resultService.getResponsesForAttempt(attempt.id());
        resultService.exportToCsv(attempt, questions, responses, dest);
        log.info("CSV exported: {}", dest.getAbsolutePath());
    }
}
