package com.telus.spring.ai.resume.model;

/**
 * Represents the status of a candidate in the recruitment process.
 */
public enum Status {
    /**
     * Candidate is locked by a manager for a specific position.
     */
    LOCKED,
    
    /**
     * Candidate was previously locked but has been released.
     */
    OPEN
    
   
}
