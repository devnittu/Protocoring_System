package com.examportal.service;

import com.examportal.model.User;

/** Holds the authenticated user for the current desktop session (single-user app context). */
public final class SessionManager {

    private static volatile User currentUser;

    private SessionManager() {}

    /** Sets the currently logged-in user. */
    public static synchronized void setCurrentUser(User user) {
        currentUser = user;
    }

    /** Returns the currently logged-in user, or null if not authenticated. */
    public static User getCurrentUser() {
        return currentUser;
    }

    /** Returns true if a user is currently authenticated. */
    public static boolean isLoggedIn() {
        return currentUser != null;
    }

    /** Clears the session (logout). */
    public static synchronized void clear() {
        currentUser = null;
    }
}
