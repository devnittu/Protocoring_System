package com.examportal.service;

import com.examportal.dao.IAttemptDAO;
import com.examportal.dao.IResponseDAO;
import com.examportal.model.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Unit tests for EvaluatorService covering scoring, pass/fail, and partial answers. */
@ExtendWith(MockitoExtension.class)
class EvaluatorServiceTest {

    @Mock private IAttemptDAO  attemptDAO;
    @Mock private IResponseDAO responseDAO;
    private EvaluatorService evaluator;

    private static Exam exam;
    private static List<Question> questions;

    @BeforeEach
    void setUp() {
        evaluator = new EvaluatorService(attemptDAO, responseDAO);
        exam = new Exam(1L,"Test","Desc",1L,60,10,50.0,
                LocalDateTime.now(), LocalDateTime.now().plusDays(1), true, false, LocalDateTime.now());
        questions = List.of(
            makeQ(1L, 'A', 2), makeQ(2L, 'B', 2), makeQ(3L, 'C', 2),
            makeQ(4L, 'D', 2), makeQ(5L, 'A', 2)
        );
    }

    @AfterEach
    void tearDown() {}

    private Question makeQ(Long id, char correct, int marks) {
        return new Question(id, 1L, "Q"+id, "A","B","C","D",
                correct, marks, Question.Difficulty.EASY,"Topic", id.intValue());
    }

    private Attempt makeFinalAttempt(double score, double pct, boolean passed) {
        return new Attempt(99L, 1L, 1L, LocalDateTime.now(), LocalDateTime.now(),
                score, pct, passed, Attempt.Status.SUBMITTED);
    }

    // ── computeScore ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("All correct answers → score == totalMarks")
    void allCorrect_scoreEqualsTotalMarks() {
        Map<Long, Character> answers = Map.of(1L,'A', 2L,'B', 3L,'C', 4L,'D', 5L,'A');
        double score = evaluator.computeScore(questions, answers);
        assertEquals(10.0, score, 0.001);
    }

    @Test
    @DisplayName("All wrong answers → score == 0")
    void allWrong_scoreIsZero() {
        Map<Long, Character> answers = Map.of(1L,'D', 2L,'A', 3L,'B', 4L,'C', 5L,'D');
        double score = evaluator.computeScore(questions, answers);
        assertEquals(0.0, score, 0.001);
    }

    @Test
    @DisplayName("Partial answers → correct weighted score")
    void partialAnswers_weightedScore() {
        // Q1(A=correct,2marks), Q3(C=correct,2marks) only
        Map<Long, Character> answers = Map.of(1L,'A', 2L,'A', 3L,'C', 4L,'A', 5L,'D');
        double score = evaluator.computeScore(questions, answers);
        assertEquals(4.0, score, 0.001);
    }

    @Test
    @DisplayName("No answers → score == 0")
    void noAnswers_scoreIsZero() {
        double score = evaluator.computeScore(questions, Map.of());
        assertEquals(0.0, score, 0.001);
    }

    // ── isPassed ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("score >= passPercentage → passed == true")
    void aboveThreshold_passed() {
        assertTrue(evaluator.isPassed(50.0, 50.0));
        assertTrue(evaluator.isPassed(75.0, 50.0));
    }

    @Test
    @DisplayName("score < passPercentage → passed == false")
    void belowThreshold_failed() {
        assertFalse(evaluator.isPassed(49.9, 50.0));
        assertFalse(evaluator.isPassed(0.0, 50.0));
    }

    // ── computePercentage ─────────────────────────────────────────────────────

    @Test
    @DisplayName("Percentage computed correctly from score and totalMarks")
    void percentage_computedCorrectly() {
        assertEquals(50.0, evaluator.computePercentage(5.0, 10), 0.001);
        assertEquals(100.0, evaluator.computePercentage(10.0, 10), 0.001);
        assertEquals(0.0, evaluator.computePercentage(0.0, 10), 0.001);
    }

    @Test
    @DisplayName("computePercentage with zero totalMarks returns 0 without exception")
    void percentage_zeroTotalMarks_returnsZero() {
        assertEquals(0.0, evaluator.computePercentage(5.0, 0), 0.001);
    }

    // ── evaluate (integration-style with mocks) ────────────────────────────────

    @Test
    @DisplayName("evaluate: all correct → finalise called with full score and passed=true")
    void evaluate_allCorrect_finalisesWithPass() {
        Map<Long, Character> answers = Map.of(1L,'A', 2L,'B', 3L,'C', 4L,'D', 5L,'A');
        Attempt finalAttempt = makeFinalAttempt(10.0, 100.0, true);
        doNothing().when(responseDAO).upsert(any(Response.class));
        doNothing().when(attemptDAO).finalise(anyLong(), anyDouble(), anyDouble(), anyBoolean(), any());
        when(attemptDAO.findById(99L)).thenReturn(java.util.Optional.of(finalAttempt));

        Attempt result = evaluator.evaluate(99L, questions, answers, exam, Attempt.Status.SUBMITTED);

        assertTrue(result.passed());
        assertEquals(10.0, result.score(), 0.001);
        verify(attemptDAO).finalise(eq(99L), eq(10.0), eq(100.0), eq(true), eq(Attempt.Status.SUBMITTED));
    }

    @Test
    @DisplayName("evaluate: all wrong → finalise called with score=0 and passed=false")
    void evaluate_allWrong_finalisesWithFail() {
        Map<Long, Character> answers = Map.of(1L,'D', 2L,'A', 3L,'B', 4L,'C', 5L,'D');
        Attempt finalAttempt = makeFinalAttempt(0.0, 0.0, false);
        doNothing().when(responseDAO).upsert(any(Response.class));
        doNothing().when(attemptDAO).finalise(anyLong(), anyDouble(), anyDouble(), anyBoolean(), any());
        when(attemptDAO.findById(99L)).thenReturn(java.util.Optional.of(finalAttempt));

        Attempt result = evaluator.evaluate(99L, questions, answers, exam, Attempt.Status.SUBMITTED);

        assertFalse(result.passed());
        verify(attemptDAO).finalise(eq(99L), eq(0.0), eq(0.0), eq(false), eq(Attempt.Status.SUBMITTED));
    }
}
