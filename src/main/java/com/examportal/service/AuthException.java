package com.examportal.service;

/** Thrown when authentication fails (wrong password or inactive account). */
public class AuthException extends RuntimeException {
    public AuthException(String message) { super(message); }
}
