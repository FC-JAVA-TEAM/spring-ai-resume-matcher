package com.telus.spring.ai.resume.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.telus.spring.ai.resume.model.CandidateEvaluationModel;
import com.telus.spring.ai.resume.model.dto.LockResumeRequest;

/**
 * Service interface for managing candidate evaluations.
 */
public interface CandidateEvaluationService {
    
    /**
     * Save a candidate evaluation.
     * 
     * @param evaluation The evaluation to save
     * @return The saved evaluation
     */
    CandidateEvaluationModel saveEvaluation(CandidateEvaluationModel evaluation);
    
    /**
     * Update a candidate evaluation only if it has changed.
     * 
     * @param evaluation The evaluation to update
     * @return The updated evaluation, or the existing evaluation if no changes were detected
     */
    CandidateEvaluationModel updateEvaluationIfChanged(CandidateEvaluationModel evaluation);
    
    /**
     * Find a candidate evaluation by resume ID.
     * 
     * @param resumeId The resume ID
     * @return The evaluation, if found
     */
    Optional<CandidateEvaluationModel> findByResumeId(UUID resumeId);
    
    /**
     * Find all candidate evaluations.
     * 
     * @return A list of all evaluations
     */
    List<CandidateEvaluationModel> findAllEvaluations();
    
    /**
     * Find candidate evaluations by lock status.
     * 
     * @param locked The lock status
     * @return A list of evaluations with the specified lock status
     */
    List<CandidateEvaluationModel> findByLocked(boolean locked);
    
    /**
     * Lock a resume for evaluation.
     * 
     * @param resumeId The resume ID
     * @param managerId The manager ID
     * @return The locked evaluation
     */
    CandidateEvaluationModel lockEvaluation(UUID resumeId, String managerId);
    
    /**
     * Unlock a resume that was previously locked.
     * 
     * @param resumeId The resume ID
     * @param managerId The manager ID
     * @return The unlocked evaluation
     */
    CandidateEvaluationModel unlockEvaluation(UUID resumeId, String managerId);
    
    /**
     * Process a lock/unlock request with optional evaluation data.
     * This method handles all the business logic for locking/unlocking resumes,
     * including dirty checking and validation.
     * 
     * @param request The lock/unlock request
     * @return The processed evaluation
     */
    CandidateEvaluationModel processLockRequest(LockResumeRequest request);
    
    /**
     * Check if a resume is already locked by a specific manager.
     * 
     * @param resumeId The resume ID
     * @param managerId The manager ID
     * @return true if the resume is already locked by the specified manager, false otherwise
     */
    boolean isLockedByManager(UUID resumeId, String managerId);
    
    /**
     * Check if evaluation data has changed.
     * 
     * @param existing The existing evaluation
     * @param updated The updated evaluation
     * @return true if evaluation data has changed, false otherwise
     */
    boolean hasEvaluationDataChanged(CandidateEvaluationModel existing, CandidateEvaluationModel updated);
}
