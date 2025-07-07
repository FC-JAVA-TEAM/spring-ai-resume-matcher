package com.telus.spring.ai.resume.model;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Entity representing the status of a candidate in the recruitment process.
 * This tracks which managers have locked which candidates for specific positions.
 */
@Entity
@Table(name = "candidate_status")
public class CandidateStatus {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(nullable = false)
    private UUID resumeId;
    
    
    
    @Column(nullable = false)
    private String managerId;
    
    @Column(nullable = true)
    private String position;
    
    @Column(nullable = true)
    @Enumerated(EnumType.STRING)
    private Status status;
    
    @Column
    private String notes;
    
    @Column(nullable = false)
    private LocalDateTime createdAt;
    
    @Column
    private LocalDateTime updatedAt;
    
    // Default constructor
    public CandidateStatus() {
    }
    
    // Constructor with fields
    public CandidateStatus(UUID resumeId, String managerId, String position, Status status) {
        this.resumeId = resumeId;
        this.managerId = managerId;
        this.position = position;
        this.status = status;
        this.createdAt = LocalDateTime.now();
    }
    
    // Getters and setters
    public UUID getId() {
        return id;
    }
    
    public void setId(UUID id) {
        this.id = id;
    }
    
    public UUID getResumeId() {
        return resumeId;
    }
    
    public void setResumeId(UUID resumeId) {
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
    
    public Status getStatus() {
        return status;
    }
    
    public void setStatus(Status status) {
        this.status = status;
    }
    
    public String getNotes() {
        return notes;
    }
    
    public void setNotes(String notes) {
        this.notes = notes;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    @Override
    public String toString() {
        return "CandidateStatus{" +
                "id='" + id + '\'' +
                ", resumeId='" + resumeId + '\'' +
                ", managerId='" + managerId + '\'' +
                ", position='" + position + '\'' +
                ", status=" + status +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
