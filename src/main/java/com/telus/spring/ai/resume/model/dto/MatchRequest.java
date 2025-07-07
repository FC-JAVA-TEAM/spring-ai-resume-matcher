package com.telus.spring.ai.resume.model.dto;

/**
 * Request DTO for matching resumes to a job description.
 */
public class MatchRequest {
    
    private String jobDescription;
    
    private int limit = 5;
    
    private boolean excludeLocked = true;
    
    // Getters and setters
    
    public String getJobDescription() {
        return jobDescription;
    }
    
    public void setJobDescription(String jobDescription) {
        this.jobDescription = jobDescription;
    }
    
    public int getLimit() {
        return limit;
    }
    
    public void setLimit(int limit) {
        this.limit = limit;
    }
    
    public boolean isExcludeLocked() {
        return excludeLocked;
    }
    
    public void setExcludeLocked(boolean excludeLocked) {
        this.excludeLocked = excludeLocked;
    }
}
