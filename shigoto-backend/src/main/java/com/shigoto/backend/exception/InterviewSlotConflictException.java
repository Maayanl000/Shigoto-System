package com.shigoto.backend.exception;

/**
 * Signals interview slot conflict failures to the API exception layer.
 */
public class InterviewSlotConflictException extends RuntimeException {
    /**
     * Creates an exception describing an interview scheduling conflict and its persistence cause.
     * @param message the exception or response message
     * @param cause the underlying failure
     */
    public InterviewSlotConflictException(String message, Throwable cause) {
        super(message, cause);
    }
}
