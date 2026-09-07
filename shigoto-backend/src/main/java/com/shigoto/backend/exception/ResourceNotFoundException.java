package com.shigoto.backend.exception;

/**
 * Signals resource not found failures to the API exception layer.
 */
public class ResourceNotFoundException extends RuntimeException {
    /**
     * Creates an exception identifying a requested domain resource that could not be found.
     * @param message the exception or response message
     */
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
