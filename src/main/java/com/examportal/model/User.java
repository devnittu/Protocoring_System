package com.examportal.model;

import java.time.LocalDateTime;

/** Immutable record representing a user account in the system. */
public record User(
        Long          id,
        String        email,
        String        passwordHash,
        String        fullName,
        Role          role,
        LocalDateTime createdAt,
        boolean       isActive,
        LocalDateTime lastLogin,
        String        sessionToken
) {
    public enum Role { ADMIN, STUDENT }

    /** Returns true if this user has the ADMIN role. */
    public boolean isAdmin()   { return role == Role.ADMIN; }

    /** Returns true if this user has the STUDENT role. */
    public boolean isStudent() { return role == Role.STUDENT; }

    /** Returns a copy of this user with the session token replaced. */
    public User withSessionToken(String token) {
        return new User(id, email, passwordHash, fullName, role,
                        createdAt, isActive, lastLogin, token);
    }

    /** Returns a copy of this user with last login set. */
    public User withLastLogin(LocalDateTime dt) {
        return new User(id, email, passwordHash, fullName, role,
                        createdAt, isActive, dt, sessionToken);
    }
}
