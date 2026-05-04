package com.examportal.service;

import com.examportal.dao.IActivityLogDAO;
import com.examportal.dao.IAttemptDAO;
import com.examportal.dao.IResponseDAO;
import com.examportal.model.ActivityLog;
import com.examportal.model.Attempt;
import com.examportal.model.Question;
import com.examportal.model.Response;
import com.examportal.util.CsvExporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/** Aggregates result data for display and export. */
public class ResultService {

    private static final Logger log = LoggerFactory.getLogger(ResultService.class);

    private final IAttemptDAO     attemptDAO;
    private final IResponseDAO    responseDAO;
    private final IActivityLogDAO activityLogDAO;

    public ResultService(IAttemptDAO attemptDAO, IResponseDAO responseDAO,
                         IActivityLogDAO activityLogDAO) {
        this.attemptDAO     = attemptDAO;
        this.responseDAO    = responseDAO;
        this.activityLogDAO = activityLogDAO;
    }

    /** Returns all attempts for a student. */
    public List<Attempt> getAttemptsByStudent(Long studentId) {
        return attemptDAO.findByStudentId(studentId);
    }

    /** Returns all attempts for an exam (admin view). */
    public List<Attempt> getAttemptsByExam(Long examId) {
        return attemptDAO.findByExamId(examId);
    }

    /** Returns per-question responses for an attempt. */
    public List<Response> getResponsesForAttempt(Long attemptId) {
        return responseDAO.findByAttemptId(attemptId);
    }

    /** Returns all activity log entries for an attempt. */
    public List<ActivityLog> getLogsForAttempt(Long attemptId) {
        return activityLogDAO.findByAttemptId(attemptId);
    }

    /** Returns a summary count of each event type for an attempt. */
    public Map<String, Long> getEventSummary(Long attemptId) {
        return activityLogDAO.countByEventTypeForAttempt(attemptId);
    }

    /** Returns the count of CRITICAL events for an attempt. */
    public long getCriticalCount(Long attemptId) {
        return activityLogDAO.countCriticalByAttemptId(attemptId);
    }

    /** Returns all activity logs (admin monitoring). */
    public List<ActivityLog> getAllLogs() {
        return activityLogDAO.findAll();
    }

    /** Stat card data. */
    public long getTotalExams(List<?> exams)    { return exams.size(); }
    public long getAttemptsToday()              { return attemptDAO.countAttemptsToday(); }
    public long getFlaggedSessions()            { return attemptDAO.countFlaggedSessions(); }

    /**
     * Exports an attempt's result to a CSV file.
     *
     * @param attempt    the attempt record
     * @param questions  the exam questions
     * @param responses  the student responses
     * @param destFile   target file path
     */
    public void exportToCsv(Attempt attempt, List<Question> questions,
                            List<Response> responses, File destFile) throws IOException {
        CsvExporter.export(attempt, questions, responses, destFile);
        log.info("Result exported to CSV: {}", destFile.getAbsolutePath());
    }
}
