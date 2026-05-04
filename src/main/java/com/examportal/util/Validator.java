package com.examportal.util;

/** Input validation helpers for forms and service parameters. */
public final class Validator {

    private Validator() {}

    private static final String EMAIL_REGEX = "^[\\w.+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}$";

    /** Returns true if the string is null or blank. */
    public static boolean isBlank(String s) { return s == null || s.isBlank(); }

    /** Returns true if the email matches a basic RFC-compliant pattern. */
    public static boolean isValidEmail(String email) {
        return email != null && email.matches(EMAIL_REGEX);
    }

    /** Returns true if password meets minimum requirements (8+ chars, 1 upper, 1 digit). */
    public static boolean isStrongPassword(String password) {
        if (password == null || password.length() < 8) return false;
        boolean hasUpper  = password.chars().anyMatch(Character::isUpperCase);
        boolean hasDigit  = password.chars().anyMatch(Character::isDigit);
        return hasUpper && hasDigit;
    }

    /** Throws IllegalArgumentException with the given message if condition is false. */
    public static void require(boolean condition, String message) {
        if (!condition) throw new IllegalArgumentException(message);
    }
}
