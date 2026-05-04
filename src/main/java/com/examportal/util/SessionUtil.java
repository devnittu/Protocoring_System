package com.examportal.util;

import com.examportal.service.SessionManager;
import com.examportal.model.User;

/** Utility methods for session and access control checks. */
public final class SessionUtil {

    private SessionUtil() {}

    /** Returns the current user or throws if not logged in. */
    public static User requireLogin() {
        User u = SessionManager.getCurrentUser();
        if (u == null) throw new IllegalStateException("No active session. Please log in.");
        return u;
    }

    /** Throws if the current user is not an ADMIN. */
    public static void requireAdmin() {
        User u = requireLogin();
        if (!u.isAdmin()) throw new SecurityException("Admin access required.");
    }

    /** Throws if the current user is not a STUDENT. */
    public static void requireStudent() {
        User u = requireLogin();
        if (!u.isStudent()) throw new SecurityException("Student access required.");
    }
}
