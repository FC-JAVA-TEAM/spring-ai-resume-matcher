package com.telus.spring.ai.resume.service;

import java.util.List;
import java.util.UUID;

import com.telus.spring.ai.resume.exception.CandidateAlreadyLockedException;
import com.telus.spring.ai.resume.exception.ResourceNotFoundException;
import com.telus.spring.ai.resume.exception.UnauthorizedException;
import com.telus.spring.ai.resume.model.CandidateStatus;
import com.telus.spring.ai.resume.model.Status;

/**
 * Service for managing candidate status operations.
 */
public interface CandidateStatusService {
    
    /**
     * Get all locked resume IDs from a list of resume IDs.
     * This batch operation is more efficient than checking one by one.
     * 
     * @param resumeIds List of resume IDs to check
     * @return List of resume IDs that are currently locked
     */
    List<UUID> getLockedResumeIds(List<UUID> resumeIds);
    
    /**
     * Lock a candidate for a specific position by a manager.
     * 
     * @param resumeId The ID of the resume to lock
     * @param managerId The ID of the manager locking the resume
     * @param position The position the candidate is being considered for
     * @param notes Optional notes about the lock
     * @return The created CandidateStatus
     * @throws CandidateAlreadyLockedException if the candidate is already locked
     */
    CandidateStatus lockCandidate(UUID resumeId, String managerId, String position, String notes);
    
    /**
     * Release a previously locked candidate.
     * 
     * @param statusId The ID of the status record
     * @param managerId The ID of the manager releasing the lock
     * @return The updated CandidateStatus
     * @throws ResourceNotFoundException if the status record is not found
     * @throws UnauthorizedException if the manager is not the one who locked the candidate
     */
    CandidateStatus releaseCandidate(UUID statusId, String managerId);
    
    /**
     * Update the status of a candidate.
     * 
     * @param statusId The ID of the status record
     * @param managerId The ID of the manager updating the status
     * @param newStatus The new status
     * @param notes Optional notes about the status update
     * @return The updated CandidateStatus
     * @throws ResourceNotFoundException if the status record is not found
     * @throws UnauthorizedException if the manager is not the one who locked the candidate
     */
    CandidateStatus updateCandidateStatus(UUID statusId, String managerId, Status newStatus, String notes);
    
    /**
     * Get all status records for a specific resume.
     * 
     * @param resumeId The ID of the resume
     * @return List of status records
     */
    List<CandidateStatus> getStatusByResumeId(UUID resumeId);
    
    /**
     * Check if a candidate is locked.
     * 
     * @param resumeId The ID of the resume
     * @return true if the candidate is locked, false otherwise
     */
    boolean isCandidateLocked(UUID resumeId);
    
    /**
     * Get all status records for a specific manager.
     * 
     * @param managerId The ID of the manager
     * @return List of status records
     */
    List<CandidateStatus> getStatusByManager(String managerId);
    
    /**
     * Get all status records for a specific position.
     * 
     * @param position The position
     * @return List of status records
     */
    List<CandidateStatus> getStatusByPosition(String position);
    
    /**
     * Get a specific status record by ID.
     * 
     * @param statusId The ID of the status record
     * @return The status record
     * @throws ResourceNotFoundException if the status record is not found
     */
    CandidateStatus getStatusById(UUID statusId);
    
    /**
     * Update the status of a candidate by resume ID.
     * If multiple status records exist for the resume, updates the most recent one.
     * 
     * @param resumeId The ID of the resume
     * @param managerId The ID of the manager updating the status
     * @param newStatus The new status
     * @param notes Optional notes about the status update
     * @return The updated CandidateStatus
     * @throws ResourceNotFoundException if no status record is found for the resume
     * @throws UnauthorizedException if the manager is not the one who locked the candidate
     */
    CandidateStatus updateCandidateStatusByResumeId(UUID resumeId, String managerId, Status newStatus, String notes);
}
