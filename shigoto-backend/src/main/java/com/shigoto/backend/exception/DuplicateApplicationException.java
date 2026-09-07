package com.shigoto.backend.exception;

/**
 * Signals duplicate application failures to the API exception layer.
 */
public class DuplicateApplicationException extends RuntimeException {

    /**
     * Creates an exception describing an attempted duplicate job application.
     * @param message the exception or response message
     */
    public DuplicateApplicationException(String message) {
        super(message);
    }
}
