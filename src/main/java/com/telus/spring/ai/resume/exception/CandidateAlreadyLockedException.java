package com.telus.spring.ai.resume.exception;

/**
 * Exception thrown when attempting to lock a candidate that is already locked.
 */
public class CandidateAlreadyLockedException extends RuntimeException {
    
    public CandidateAlreadyLockedException(String message) {
        super(message);
    }
    
    public CandidateAlreadyLockedException(String message, Throwable cause) {
        super(message, cause);
    }
}
