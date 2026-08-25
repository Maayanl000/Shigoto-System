package com.shigoto.backend.exception;

public class CvTooLargeException extends RuntimeException {
    public CvTooLargeException(String message) {
        super(message);
    }
}
