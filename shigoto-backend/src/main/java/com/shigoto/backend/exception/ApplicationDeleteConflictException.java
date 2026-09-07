package com.shigoto.backend.exception;

/**
 * Signals application delete conflict failures to the API exception layer.
 */
public class ApplicationDeleteConflictException extends RuntimeException {
    /**
     * Creates an exception describing why an application cannot be deleted.
     * @param message the exception or response message
     */
    public ApplicationDeleteConflictException(String message) {
        super(message);
    }
}
