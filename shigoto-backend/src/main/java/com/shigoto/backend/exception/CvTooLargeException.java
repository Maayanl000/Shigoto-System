package com.shigoto.backend.exception;

/**
 * Signals cv too large failures to the API exception layer.
 */
public class CvTooLargeException extends RuntimeException {
    /**
     * Creates an exception describing a CV upload that exceeds the permitted size.
     * @param message the exception or response message
     */
    public CvTooLargeException(String message) {
        super(message);
    }
}
