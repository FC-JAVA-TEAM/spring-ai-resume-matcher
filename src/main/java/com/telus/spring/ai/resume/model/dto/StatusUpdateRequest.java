package com.telus.spring.ai.resume.model.dto;

import java.util.UUID;

import com.telus.spring.ai.resume.model.CandidateStatus;

/**
 * DTO for updating the status of a candidate.
 */
public class StatusUpdateRequest {
    
    private UUID resumeId;
    private CandidateStatus status;
    private String customStatus;
    private String managerId;
    private String comments;
    
    // Default constructor
    public StatusUpdateRequest() {
    }
    
    // Constructor with all fields
    public StatusUpdateRequest(UUID resumeId, CandidateStatus status, String customStatus, String managerId, String comments) {
        this.resumeId = resumeId;
        this.status = status;
        this.customStatus = customStatus;
        this.managerId = managerId;
        this.comments = comments;
    }
    
    // Getters and setters
    public UUID getResumeId() {
        return resumeId;
    }
    
    public void setResumeId(UUID resumeId) {
        this.resumeId = resumeId;
    }
    
    public CandidateStatus getStatus() {
        return status;
    }
    
    public void setStatus(CandidateStatus status) {
        this.status = status;
    }
    
    public String getCustomStatus() {
        return customStatus;
    }
    
    public void setCustomStatus(String customStatus) {
        this.customStatus = customStatus;
    }
    
    public String getManagerId() {
        return managerId;
    }
    
    public void setManagerId(String managerId) {
        this.managerId = managerId;
    }
    
    public String getComments() {
        return comments;
    }
    
    public void setComments(String comments) {
        this.comments = comments;
    }
    
    @Override
    public String toString() {
        return "StatusUpdateRequest{" +
               "resumeId=" + resumeId +
               ", status=" + status +
               ", customStatus='" + customStatus + '\'' +
               ", managerId='" + managerId + '\'' +
               ", comments='" + comments + '\'' +
               '}';
    }
}
