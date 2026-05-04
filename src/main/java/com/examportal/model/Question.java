package com.examportal.model;

/** Immutable record representing a single MCQ question in an exam. */
public record Question(
        Long       id,
        Long       examId,
        String     body,
        String     optionA,
        String     optionB,
        String     optionC,
        String     optionD,
        char       correctOption,
        int        marks,
        Difficulty difficulty,
        String     topic,
        int        position
) {
    public enum Difficulty { EASY, MEDIUM, HARD }

    /** Returns the text for a given option letter (A/B/C/D). */
    public String getOptionText(char option) {
        return switch (Character.toUpperCase(option)) {
            case 'A' -> optionA;
            case 'B' -> optionB;
            case 'C' -> optionC;
            case 'D' -> optionD;
            default  -> "";
        };
    }

    /** Returns true if the given answer matches the correct option. */
    public boolean isCorrect(char chosen) {
        return Character.toUpperCase(chosen) == Character.toUpperCase(correctOption);
    }
}
