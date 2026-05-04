package com.examportal.util;

import org.mindrot.jbcrypt.BCrypt;

/** Utility class for BCrypt password hashing and verification. */
public final class BCryptUtil {

    private static final int WORK_FACTOR = 12;

    private BCryptUtil() {}

    /** Hashes a plaintext password with BCrypt work factor 12. */
    public static String hash(String plainPassword) {
        if (plainPassword == null || plainPassword.isEmpty())
            throw new IllegalArgumentException("Password must not be null or empty.");
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt(WORK_FACTOR));
    }

    /** Verifies a plaintext password against a BCrypt hash. */
    public static boolean verify(String plainPassword, String hash) {
        if (plainPassword == null || hash == null) return false;
        try {
            return BCrypt.checkpw(plainPassword, hash);
        } catch (Exception e) {
            return false;
        }
    }
}
