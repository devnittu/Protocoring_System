package com.examportal.model;

/** Immutable record representing a student's answer to a single question. */
public record Response(
        Long    id,
        Long    attemptId,
        Long    questionId,
        Character chosenOption,
        Boolean isCorrect,
        int     timeSpentSeconds
) {}
