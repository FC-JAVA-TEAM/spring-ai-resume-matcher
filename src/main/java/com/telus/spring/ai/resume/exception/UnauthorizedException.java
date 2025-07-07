package com.telus.spring.ai.resume.exception;

/**
 * Exception thrown when a user attempts to perform an action they are not authorized to perform.
 * For example, when a manager tries to release a candidate that was locked by another manager.
 */
public class UnauthorizedException extends RuntimeException {
    
    public UnauthorizedException(String message) {
        super(message);
    }
    
    public UnauthorizedException(String message, Throwable cause) {
        super(message, cause);
    }
}
