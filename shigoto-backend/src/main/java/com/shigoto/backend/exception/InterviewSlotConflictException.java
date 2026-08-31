package com.shigoto.backend.exception;

public class InterviewSlotConflictException extends RuntimeException {
    public InterviewSlotConflictException(String message, Throwable cause) {
        super(message, cause);
    }
}
