package com.shigoto.backend.exception;

/**
 * Signals cv storage failures to the API exception layer.
 */
public class CvStorageException extends RuntimeException {
    /**
     * Creates a CV storage exception with the user-safe message and underlying filesystem failure.
     * @param message the exception or response message
     * @param cause the underlying failure
     */
    public CvStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
