package com.examportal.service;

import com.examportal.dao.IAttemptDAO;
import com.examportal.dao.IResponseDAO;
import com.examportal.model.Attempt;
import com.examportal.model.Exam;
import com.examportal.model.Question;
import com.examportal.model.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Evaluates exam responses, computes scores, and finalises attempt records. */
public class EvaluatorService {

    private static final Logger log = LoggerFactory.getLogger(EvaluatorService.class);

    private final IAttemptDAO  attemptDAO;
    private final IResponseDAO responseDAO;

    public EvaluatorService(IAttemptDAO attemptDAO, IResponseDAO responseDAO) {
        this.attemptDAO  = attemptDAO;
        this.responseDAO = responseDAO;
    }

    /**
     * Evaluates all responses for an attempt and finalises the attempt record.
     *
     * @param attemptId  the attempt being evaluated
     * @param questions  ordered list of questions in the exam
     * @param answers    map of questionId → chosen option char (may be null for skipped)
     * @param exam       the exam (for totalMarks and passPercentage)
     * @param status     SUBMITTED or FORCE_SUBMITTED
     * @return the finalised Attempt
     */
    public Attempt evaluate(Long attemptId, List<Question> questions,
                            Map<Long, Character> answers, Exam exam, Attempt.Status status) {

        Map<Long, Question> questionMap = questions.stream()
                .collect(Collectors.toMap(Question::id, Function.identity()));

        double scored = 0.0;

        for (Question q : questions) {
            Character chosen  = answers.get(q.id());
            boolean   correct = chosen != null && q.isCorrect(chosen);
            if (correct) scored += q.marks();

            Response response = new Response(
                null, attemptId, q.id(),
                chosen, correct, 0
            );
            responseDAO.upsert(response);
        }

        double totalMarks  = exam.totalMarks();
        double percentage  = totalMarks > 0 ? (scored / totalMarks) * 100.0 : 0.0;
        boolean passed     = percentage >= exam.passPercentage();

        attemptDAO.finalise(attemptId, scored, percentage, passed, status);
        log.info("Attempt {} evaluated — score={}/{}, pct={}%, passed={}",
                attemptId, scored, totalMarks, String.format("%.1f", percentage), passed);

        return attemptDAO.findById(attemptId).orElseThrow();
    }

    /**
     * Computes score from a map of questionId → chosen option without persisting.
     * Used for preview / dry-run.
     */
    public double computeScore(List<Question> questions, Map<Long, Character> answers) {
        double score = 0.0;
        for (Question q : questions) {
            Character chosen = answers.get(q.id());
            if (chosen != null && q.isCorrect(chosen)) score += q.marks();
        }
        return score;
    }

    /** Computes percentage from raw score. */
    public double computePercentage(double score, int totalMarks) {
        return totalMarks > 0 ? (score / totalMarks) * 100.0 : 0.0;
    }

    /** Determines pass/fail from percentage and exam threshold. */
    public boolean isPassed(double percentage, double passPercentage) {
        return percentage >= passPercentage;
    }
}
