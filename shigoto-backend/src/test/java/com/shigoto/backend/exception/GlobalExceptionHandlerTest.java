package com.shigoto.backend.exception;

import org.junit.jupiter.api.Test;
import org.springframework.dao.OptimisticLockingFailureException;
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

    @Test
    void optimisticLockingFailureReturnsConflictWithRefreshMessage() {
        var response = handler.handleOptimisticLockingFailure(
                new OptimisticLockingFailureException("stale write"));

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals(409, response.getBody().status());
        assertEquals("This record was updated by another user. Refresh and try again.",
                response.getBody().message());
    }
}
