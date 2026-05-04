package com.examportal.service;

/** Thrown when a registration attempt uses an email that already exists. */
public class DuplicateUserException extends RuntimeException {
    public DuplicateUserException(String email) {
        super("A user with email '" + email + "' already exists.");
    }
}
