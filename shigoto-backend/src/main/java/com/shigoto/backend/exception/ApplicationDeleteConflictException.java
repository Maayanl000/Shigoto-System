package com.shigoto.backend.exception;

public class ApplicationDeleteConflictException extends RuntimeException {
    public ApplicationDeleteConflictException(String message) {
        super(message);
    }
}
