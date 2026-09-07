package com.shigoto.backend.exception;

/**
 * Signals duplicate email failures to the API exception layer.
 */
public class DuplicateEmailException extends RuntimeException {
    /**
     * Creates an exception describing an attempted registration with an existing email.
     * @param message the exception or response message
     */
    public DuplicateEmailException(String message) {
        super(message);
    }
}
