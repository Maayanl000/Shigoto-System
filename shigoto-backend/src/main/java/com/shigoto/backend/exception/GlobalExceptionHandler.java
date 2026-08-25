package com.shigoto.backend.exception;

import com.shigoto.backend.dto.ErrorResponseDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import java.time.LocalDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // הפונקציה הזו תופסת את כל שגיאות ה-ResourceNotFoundException במערכת
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponseDTO> handleResourceNotFound(ResourceNotFoundException ex) {

        ErrorResponseDTO errorResponse = new ErrorResponseDTO(
                ex.getMessage(),
                HttpStatus.NOT_FOUND.value(), // מחזיר 404
                LocalDateTime.now()
        );

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponseDTO> handleBadRequest(IllegalArgumentException ex) {
        return buildErrorResponse(ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(DuplicateApplicationException.class)
    public ResponseEntity<ErrorResponseDTO> handleConflict(DuplicateApplicationException ex) {
        return buildErrorResponse(ex.getMessage(), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(DuplicateEmailException.class)
    public ResponseEntity<ErrorResponseDTO> handleDuplicateEmail(DuplicateEmailException ex) {
        return buildErrorResponse(ex.getMessage(), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponseDTO> handleAuthentication(AuthenticationException ex) {
        return buildErrorResponse("Invalid email or password", HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponseDTO> handleAccessDenied(AccessDeniedException ex) {
        return buildErrorResponse("Access denied", HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponseDTO> handleMaxUploadSize(MaxUploadSizeExceededException ex) {
        return buildErrorResponse("CV file must not exceed 5 MB", HttpStatus.PAYLOAD_TOO_LARGE);
    }

    @ExceptionHandler(CvTooLargeException.class)
    public ResponseEntity<ErrorResponseDTO> handleCvTooLarge(CvTooLargeException ex) {
        return buildErrorResponse("CV file must not exceed 5 MB", HttpStatus.PAYLOAD_TOO_LARGE);
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<ErrorResponseDTO> handleMissingMultipartPart(MissingServletRequestPartException ex) {
        return buildErrorResponse("CV file is required", HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<ErrorResponseDTO> handleMultipart(MultipartException ex) {
        return buildErrorResponse("The multipart upload request is invalid", HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(CvStorageException.class)
    public ResponseEntity<ErrorResponseDTO> handleCvStorage(CvStorageException ex) {
        return buildErrorResponse("CV storage operation failed", HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private ResponseEntity<ErrorResponseDTO> buildErrorResponse(String message, HttpStatus status) {
        ErrorResponseDTO errorResponse = new ErrorResponseDTO(
                message,
                status.value(),
                LocalDateTime.now()
        );
        return ResponseEntity.status(status).body(errorResponse);
    }

    // אפשר להוסיף כאן בעתיד עוד פונקציות שיתפסו שגיאות אחרות!
}
