package com.shigoto.backend.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void serviceLevelOversizedCvReturnsPayloadTooLarge() {
        var response = handler.handleCvTooLarge(new CvTooLargeException("too large"));

        assertEquals(HttpStatus.PAYLOAD_TOO_LARGE, response.getStatusCode());
        assertEquals(413, response.getBody().status());
    }

    @Test
    void servletMultipartOverflowReturnsPayloadTooLarge() {
        var response = handler.handleMaxUploadSize(new MaxUploadSizeExceededException(5L * 1024 * 1024));

        assertEquals(HttpStatus.PAYLOAD_TOO_LARGE, response.getStatusCode());
        assertEquals(413, response.getBody().status());
    }
}
