package com.thundercore.erp.common.exception;

import com.thundercore.erp.common.dto.ApiResponse;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
/**
 * GlobalExceptionHandler converts framework and domain exceptions into the
 * shared ApiResponse envelope.
 *
 * <p>This keeps controller code focused on business operations while clients
 * receive consistent status codes and message fields for validation,
 * authentication, authorization, and database constraint failures.</p>
 */
public class GlobalExceptionHandler {

    @ExceptionHandler(AuthenticationException.class)
    /** Returns 401 when a request lacks valid authentication. */
    public ResponseEntity<ApiResponse<String>> handleAuthenticationException(AuthenticationException ex) {
        return new ResponseEntity<>(ApiResponse.error("Authentication required"), HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(AccessDeniedException.class)
    /** Returns 403 when the authenticated user lacks the required role. */
    public ResponseEntity<ApiResponse<String>> handleAccessDeniedException(AccessDeniedException ex) {
        return new ResponseEntity<>(ApiResponse.error("You do not have permission to perform this action"), HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    /** Returns 409 for unique-key or relational database conflicts. */
    public ResponseEntity<ApiResponse<String>> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        return new ResponseEntity<>(ApiResponse.error("Request conflicts with existing data or database constraints"), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    /** Returns 400 for invalid enum values or explicit domain validation. */
    public ResponseEntity<ApiResponse<String>> handleIllegalArgumentException(IllegalArgumentException ex) {
        return new ResponseEntity<>(ApiResponse.error(ex.getMessage()), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(RuntimeException.class)
    /** Returns 400 for domain lookup failures surfaced as runtime exceptions. */
    public ResponseEntity<ApiResponse<String>> handleRuntimeException(RuntimeException ex) {
        return new ResponseEntity<>(ApiResponse.error(ex.getMessage()), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    /** Returns 500 for unexpected errors that escape narrower handlers. */
    public ResponseEntity<ApiResponse<String>> handleGlobalException(Exception ex) {
        return new ResponseEntity<>(ApiResponse.error("An unexpected error occurred: " + ex.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    /** Returns field-level validation messages for DTO/entity validation errors. */
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error -> 
            errors.put(error.getField(), error.getDefaultMessage()));
        return new ResponseEntity<>(new ApiResponse<>(false, "Validation failed", errors), HttpStatus.BAD_REQUEST);
    }
}
