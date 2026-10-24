package com.bookleaf.support.exception;

/** Raised when the LLM is disabled, rate-limited, down, or returned something unusable. */
public class AiUnavailableException extends RuntimeException {
    public AiUnavailableException(String message) { super(message); }

    public AiUnavailableException(String message, Throwable cause) { super(message, cause); }
}
