package com.telus.spring.ai.resume.model.dto;

/**
 * Request DTO for locking a candidate.
 */
public class LockCandidateRequest {
    
    private String resumeId;
    
    private String managerId;
    
    private String position;
    
    private String notes;
    
    // Getters and setters
    
    public String getResumeId() {
        return resumeId;
    }
    
    public void setResumeId(String resumeId) {
        this.resumeId = resumeId;
    }
    
    public String getManagerId() {
        return managerId;
    }
    
    public void setManagerId(String managerId) {
        this.managerId = managerId;
    }
    
    public String getPosition() {
        return position;
    }
    
    public void setPosition(String position) {
        this.position = position;
    }
    
    public String getNotes() {
        return notes;
    }
    
    public void setNotes(String notes) {
        this.notes = notes;
    }
}
