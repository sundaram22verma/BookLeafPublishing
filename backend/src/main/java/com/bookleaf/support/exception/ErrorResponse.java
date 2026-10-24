package com.bookleaf.support.exception;

import java.time.Instant;
import java.util.Map;

/** Uniform error body for every non-2xx response (including 401/403 from the security filter chain). */
public record ErrorResponse(Instant timestamp, int status, String error, String message, String path,
                            Map<String, String> fieldErrors) {

    public static ErrorResponse of(int status, String error, String message, String path, Map<String, String> fieldErrors) {
        return new ErrorResponse(Instant.now(), status, error, message, path, fieldErrors);
    }
}
