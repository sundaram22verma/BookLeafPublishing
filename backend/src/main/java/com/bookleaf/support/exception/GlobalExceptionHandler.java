//package com.bookleaf.support.exception;
//
//import jakarta.servlet.http.HttpServletRequest;
//import jakarta.validation.ConstraintViolationException;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.dao.DataIntegrityViolationException;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//import org.springframework.http.converter.HttpMessageNotReadableException;
//import org.springframework.security.access.AccessDeniedException;
//import org.springframework.security.authentication.BadCredentialsException;
//import org.springframework.web.HttpRequestMethodNotSupportedException;
//import org.springframework.web.bind.MethodArgumentNotValidException;
//import org.springframework.web.bind.MissingServletRequestParameterException;
//import org.springframework.web.bind.annotation.ExceptionHandler;
//import org.springframework.web.bind.annotation.RestControllerAdvice;
//import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
//import org.springframework.web.servlet.resource.NoResourceFoundException;
//
//import java.util.LinkedHashMap;
//import java.util.Map;
//
//@Slf4j
//@RestControllerAdvice
//public class GlobalExceptionHandler {
//
//    @ExceptionHandler(ResourceNotFoundException.class)
//    public ResponseEntity<ErrorResponse> notFound(ResourceNotFoundException ex, HttpServletRequest req) {
//        return build(HttpStatus.NOT_FOUND, ex.getMessage(), req, null);
//    }
//
//    @ExceptionHandler(MethodArgumentNotValidException.class)
//    public ResponseEntity<ErrorResponse> validation(MethodArgumentNotValidException ex, HttpServletRequest req) {
//        Map<String, String> fields = new LinkedHashMap<>();
//        ex.getBindingResult().getFieldErrors()
//                .forEach(fe -> fields.putIfAbsent(fe.getField(), fe.getDefaultMessage() == null ? "invalid value" : fe.getDefaultMessage()));
//        return build(HttpStatus.BAD_REQUEST, "Validation failed", req, fields);
//    }
//
//    @ExceptionHandler(ConstraintViolationException.class)
//    public ResponseEntity<ErrorResponse> constraint(ConstraintViolationException ex, HttpServletRequest req) {
//        Map<String, String> fields = new LinkedHashMap<>();
//        ex.getConstraintViolations().forEach(v -> fields.putIfAbsent(v.getPropertyPath().toString(), v.getMessage()));
//        return build(HttpStatus.BAD_REQUEST, "Validation failed", req, fields);
//    }
//
//    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class,
//            MissingServletRequestParameterException.class})
//    public ResponseEntity<ErrorResponse> badInput(Exception ex, HttpServletRequest req) {
//        return build(HttpStatus.BAD_REQUEST, "Malformed request: missing, unparsable or invalid value (check enums and dates)", req, null);
//    }
//
//    @ExceptionHandler(IllegalArgumentException.class)
//    public ResponseEntity<ErrorResponse> illegalArgument(IllegalArgumentException ex, HttpServletRequest req) {
//        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), req, null);
//    }
//
//    @ExceptionHandler({IllegalStateException.class, DataIntegrityViolationException.class})
//    public ResponseEntity<ErrorResponse> conflict(Exception ex, HttpServletRequest req) {
//        String msg = ex instanceof IllegalStateException ? ex.getMessage() : "The request conflicts with existing data";
//        return build(HttpStatus.CONFLICT, msg, req, null);
//    }
//
//    @ExceptionHandler(BadCredentialsException.class)
//    public ResponseEntity<ErrorResponse> badCredentials(BadCredentialsException ex, HttpServletRequest req) {
//        return build(HttpStatus.UNAUTHORIZED, "Invalid email or password", req, null);
//    }
//
//    @ExceptionHandler(AccessDeniedException.class)
//    public ResponseEntity<ErrorResponse> denied(AccessDeniedException ex, HttpServletRequest req) {
//        return build(HttpStatus.FORBIDDEN, "You do not have permission to perform this action", req, null);
//    }
//
//    @ExceptionHandler(AiUnavailableException.class)
//    public ResponseEntity<ErrorResponse> aiDown(AiUnavailableException ex, HttpServletRequest req) {
//        return build(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage(), req, null);
//    }
//
//    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
//    public ResponseEntity<ErrorResponse> methodNotAllowed(HttpRequestMethodNotSupportedException ex, HttpServletRequest req) {
//        return build(HttpStatus.METHOD_NOT_ALLOWED, ex.getMessage(), req, null);
//    }
//
//    @ExceptionHandler(NoResourceFoundException.class)
//    public ResponseEntity<ErrorResponse> noResource(NoResourceFoundException ex, HttpServletRequest req) {
//        return build(HttpStatus.NOT_FOUND, "No such endpoint", req, null);
//    }
//
//    /** Last resort: log the detail server-side, return nothing sensitive to the client. */
//    @ExceptionHandler(Exception.class)
//    public ResponseEntity<ErrorResponse> unexpected(Exception ex, HttpServletRequest req) {
//        log.error("Unhandled exception on {} {}", req.getMethod(), req.getRequestURI(), ex);
//        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Something went wrong on our side. Please try again.", req, null);
//    }
//
//    private ResponseEntity<ErrorResponse> build(HttpStatus status, String message, HttpServletRequest req, Map<String, String> fields) {
//        return ResponseEntity.status(status)
//                .body(ErrorResponse.of(status.value(), status.getReasonPhrase(), message, req.getRequestURI(), fields));
//    }
//}



package com.bookleaf.support.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> notFound(ResourceNotFoundException ex, HttpServletRequest req) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), req, null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> validation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        Map<String, String> fields = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(fe -> fields.putIfAbsent(fe.getField(), fe.getDefaultMessage() == null ? "invalid value" : fe.getDefaultMessage()));
        return build(HttpStatus.BAD_REQUEST, "Validation failed", req, fields);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> constraint(ConstraintViolationException ex, HttpServletRequest req) {
        Map<String, String> fields = new LinkedHashMap<>();
        ex.getConstraintViolations().forEach(v -> fields.putIfAbsent(v.getPropertyPath().toString(), v.getMessage()));
        return build(HttpStatus.BAD_REQUEST, "Validation failed", req, fields);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class})
    public ResponseEntity<ErrorResponse> badInput(Exception ex, HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, "Malformed request: missing, unparsable or invalid value (check enums and dates)", req, null);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> uploadTooLarge(MaxUploadSizeExceededException ex, HttpServletRequest req) {
        return build(HttpStatus.PAYLOAD_TOO_LARGE, "Attachments exceed the 50 MB total upload limit", req, null);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> illegalArgument(IllegalArgumentException ex, HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), req, null);
    }

    @ExceptionHandler({IllegalStateException.class, DataIntegrityViolationException.class})
    public ResponseEntity<ErrorResponse> conflict(Exception ex, HttpServletRequest req) {
        String msg = ex instanceof IllegalStateException ? ex.getMessage() : "The request conflicts with existing data";
        return build(HttpStatus.CONFLICT, msg, req, null);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> badCredentials(BadCredentialsException ex, HttpServletRequest req) {
        return build(HttpStatus.UNAUTHORIZED, "Invalid email or password", req, null);
    }

    @ExceptionHandler(TooManyRequestsException.class)
    public ResponseEntity<ErrorResponse> tooMany(TooManyRequestsException ex, HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header("Retry-After", String.valueOf(ex.getRetryAfterSeconds()))
                .body(ErrorResponse.of(429, HttpStatus.TOO_MANY_REQUESTS.getReasonPhrase(), ex.getMessage(), req.getRequestURI(), null));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> denied(AccessDeniedException ex, HttpServletRequest req) {
        return build(HttpStatus.FORBIDDEN, "You do not have permission to perform this action", req, null);
    }

    @ExceptionHandler(AiUnavailableException.class)
    public ResponseEntity<ErrorResponse> aiDown(AiUnavailableException ex, HttpServletRequest req) {
        return build(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage(), req, null);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> methodNotAllowed(HttpRequestMethodNotSupportedException ex, HttpServletRequest req) {
        return build(HttpStatus.METHOD_NOT_ALLOWED, ex.getMessage(), req, null);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> noResource(NoResourceFoundException ex, HttpServletRequest req) {
        return build(HttpStatus.NOT_FOUND, "No such endpoint", req, null);
    }

    /** Last resort: log the detail server-side, return nothing sensitive to the client. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> unexpected(Exception ex, HttpServletRequest req) {
        log.error("Unhandled exception on {} {}", req.getMethod(), req.getRequestURI(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Something went wrong on our side. Please try again.", req, null);
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String message, HttpServletRequest req, Map<String, String> fields) {
        return ResponseEntity.status(status)
                .body(ErrorResponse.of(status.value(), status.getReasonPhrase(), message, req.getRequestURI(), fields));
    }
}
