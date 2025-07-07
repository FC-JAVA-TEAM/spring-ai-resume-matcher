package com.telus.spring.ai.resume.model.dto;

import com.telus.spring.ai.resume.model.Status;

/**
 * Request DTO for updating a candidate's status.
 */
public class UpdateStatusRequest {
    
    private String statusId;
    
    private String resumeId;
    
    private String managerId;
    
    private Status newStatus;
    
    private String notes;
    
    // Getters and setters
    
    public String getStatusId() {
        return statusId;
    }
    
    public void setStatusId(String statusId) {
        this.statusId = statusId;
    }
    
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
    
    public Status getNewStatus() {
        return newStatus;
    }
    
    public void setNewStatus(Status newStatus) {
        this.newStatus = newStatus;
    }
    
    public String getNotes() {
        return notes;
    }
    
    public void setNotes(String notes) {
        this.notes = notes;
    }
}
