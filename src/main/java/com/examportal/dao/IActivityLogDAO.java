package com.examportal.dao;

import com.examportal.model.ActivityLog;

import java.util.List;
import java.util.Map;

/** Contract for all ActivityLog persistence operations. */
public interface IActivityLogDAO {
    ActivityLog        insert(ActivityLog log);
    List<ActivityLog>  findByAttemptId(Long attemptId);
    List<ActivityLog>  findByStudentId(Long studentId);
    List<ActivityLog>  findAll();
    long               countCriticalByAttemptId(Long attemptId);
    Map<String, Long>  countByEventTypeForAttempt(Long attemptId);
}
