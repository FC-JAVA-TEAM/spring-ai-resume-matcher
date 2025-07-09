package com.telus.spring.ai.resume.service.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.telus.spring.ai.resume.exception.CandidateAlreadyLockedException;
import com.telus.spring.ai.resume.exception.ResourceNotFoundException;
import com.telus.spring.ai.resume.model.CandidateEvaluationModel;
import com.telus.spring.ai.resume.model.CandidateStatus;
import com.telus.spring.ai.resume.model.CandidateStatusHistory;
import com.telus.spring.ai.resume.model.Resume;
import com.telus.spring.ai.resume.model.dto.LockResumeRequest;
import com.telus.spring.ai.resume.model.dto.StatusUpdateRequest;
import com.telus.spring.ai.resume.repository.CandidateEvaluationRepository;
import com.telus.spring.ai.resume.repository.CandidateStatusHistoryRepository;
import com.telus.spring.ai.resume.repository.ResumeRepository;
import com.telus.spring.ai.resume.service.CandidateEvaluationService;
import com.telus.spring.ai.resume.util.EntityComparisonUtils;
import com.telus.spring.ai.resume.util.StatusHistoryUtil;

/**
 * Implementation of the CandidateEvaluationService interface.
 */
@Service
public class CandidateEvaluationServiceImpl implements CandidateEvaluationService {
    
    private static final Logger logger = LoggerFactory.getLogger(CandidateEvaluationServiceImpl.class);
    
private final CandidateEvaluationRepository evaluationRepository;
private final ResumeRepository resumeRepository;
private final CandidateStatusHistoryRepository statusHistoryRepository;
private final StatusHistoryUtil statusHistoryUtil;

@Autowired
public CandidateEvaluationServiceImpl(CandidateEvaluationRepository evaluationRepository,
                                     ResumeRepository resumeRepository,
                                     CandidateStatusHistoryRepository statusHistoryRepository,
                                     StatusHistoryUtil statusHistoryUtil) {
    this.evaluationRepository = evaluationRepository;
    this.resumeRepository = resumeRepository;
    this.statusHistoryRepository = statusHistoryRepository;
    this.statusHistoryUtil = statusHistoryUtil;
}
    
    @Override
    @Transactional
    public CandidateEvaluationModel saveEvaluation(CandidateEvaluationModel evaluation) {
        // Verify resume exists
        if (!resumeRepository.existsById(evaluation.getResumeId())) {
            throw new ResourceNotFoundException("Resume", "id", evaluation.getResumeId());
        }
        
        // If this is a new evaluation (no ID), just save it
        if (evaluation.getId() == null) {
            logger.debug("Saving new evaluation for resume: {}", evaluation.getResumeId());
            return evaluationRepository.save(evaluation);
        }
        
        // For existing evaluations, use updateEvaluationIfChanged
        return updateEvaluationIfChanged(evaluation);
    }
    
    @Override
    @Transactional
    public CandidateEvaluationModel updateEvaluationIfChanged(CandidateEvaluationModel evaluation) {
        // For existing evaluations, check if there are actual changes
    	
    	if(null==evaluation.getId()) {
    		
    		CandidateEvaluationModel save = evaluationRepository.save(evaluation);
    		
    		statusHistoryUtil.recordStatusChange(
    				evaluation.getResumeId(),
                    evaluation.getId(),
                    CandidateStatus.INITIATE,
                    "New candidate Initilize",
                    evaluation.getStatus(),
                    evaluation.getCustomStatus(),
                    evaluation.getManagerId(),
                    "New candidate Initilize"
                );
    		 return save ;
    	}
        Optional<CandidateEvaluationModel> existingOpt = evaluationRepository.findById(evaluation.getId());
        if (existingOpt.isPresent()) {
            CandidateEvaluationModel existing = existingOpt.get();
            
            // If nothing has changed, return the existing evaluation without saving
            if (!hasEvaluationDataChanged(existing, evaluation)) {
                logger.debug("No changes detected for evaluation ID: {}, skipping save operation", evaluation.getId());
                return existing;
            }
            
            // If collections have changed, handle them separately to avoid delete-all-insert-all
            if (evaluation.getKeyStrengths() != null && existing.getKeyStrengths() != null) {
                EntityComparisonUtils.handleCollectionChanges(existing.getKeyStrengths(), evaluation.getKeyStrengths());
                // Set to null to prevent Hibernate from replacing the entire collection
                evaluation.setKeyStrengths(null);
            }
            
            if (evaluation.getImprovementAreas() != null && existing.getImprovementAreas() != null) {
                EntityComparisonUtils.handleCollectionChanges(existing.getImprovementAreas(), evaluation.getImprovementAreas());
                // Set to null to prevent Hibernate from replacing the entire collection
                evaluation.setImprovementAreas(null);
            }
            
            // Copy non-null properties from evaluation to existing
            copyNonNullProperties(evaluation, existing);
            
            logger.debug("Saving updated evaluation for ID: {}", existing.getId());
            return evaluationRepository.save(existing);
        }
        
        // If we couldn't find the existing evaluation, just save the new one
        logger.debug("Existing evaluation not found for ID: {}, creating new", evaluation.getId());
        return evaluationRepository.save(evaluation);
    }
    
    @Override
    public boolean hasEvaluationDataChanged(CandidateEvaluationModel existing, CandidateEvaluationModel updated) {
        // Check if basic properties are equal
        if (!Objects.equals(existing.getName(), updated.getName()) ||
            !Objects.equals(existing.getEmail(), updated.getEmail()) ||
            !Objects.equals(existing.getPhoneNumber(), updated.getPhoneNumber()) ||
            !Objects.equals(existing.getScore(), updated.getScore()) ||
            !Objects.equals(existing.getExecutiveSummary(), updated.getExecutiveSummary()) ||
            !Objects.equals(existing.getTechnicalSkills(), updated.getTechnicalSkills()) ||
            !Objects.equals(existing.getExperience(), updated.getExperience()) ||
            !Objects.equals(existing.getEducation(), updated.getEducation()) ||
            !Objects.equals(existing.getSoftSkills(), updated.getSoftSkills()) ||
            !Objects.equals(existing.getAchievements(), updated.getAchievements()) ||
            !Objects.equals(existing.getRecommendationType(), updated.getRecommendationType()) ||
            !Objects.equals(existing.getRecommendationReason(), updated.getRecommendationReason()) ||
            existing.isLocked() != updated.isLocked() ||
            !Objects.equals(existing.getManagerId(), updated.getManagerId()) ||
            !Objects.equals(existing.getLockedAt(), updated.getLockedAt())) {
            return true; // Changes detected, save required
        }
        
        // Check if collections are equal
        if (!EntityComparisonUtils.areCollectionsEqual(existing.getKeyStrengths(), updated.getKeyStrengths()) ||
            !EntityComparisonUtils.areCollectionsEqual(existing.getImprovementAreas(), updated.getImprovementAreas())) {
            return true; // Changes detected in collections, save required
        }
        
        // If we get here, the evaluations are equal
        return false; // No changes detected, no save required
    }
    
    /**
     * Copies non-null properties from source to target.
     * 
     * @param source The source object
     * @param target The target object
     */
    private void copyNonNullProperties(CandidateEvaluationModel source, CandidateEvaluationModel target) {
        if (source.getName() != null) target.setName(source.getName());
        if (source.getEmail() != null) target.setEmail(source.getEmail());
        if (source.getPhoneNumber() != null) target.setPhoneNumber(source.getPhoneNumber());
        if (source.getScore() != 0) target.setScore(source.getScore());
        if (source.getExecutiveSummary() != null) target.setExecutiveSummary(source.getExecutiveSummary());
        if (source.getTechnicalSkills() != null) target.setTechnicalSkills(source.getTechnicalSkills());
        if (source.getExperience() != null) target.setExperience(source.getExperience());
        if (source.getEducation() != null) target.setEducation(source.getEducation());
        if (source.getSoftSkills() != null) target.setSoftSkills(source.getSoftSkills());
        if (source.getAchievements() != null) target.setAchievements(source.getAchievements());
        if (source.getRecommendationType() != null) target.setRecommendationType(source.getRecommendationType());
        if (source.getRecommendationReason() != null) target.setRecommendationReason(source.getRecommendationReason());
        
        // Always copy these properties
        target.setLocked(source.isLocked());
        target.setManagerId(source.getManagerId());
        target.setLockedAt(source.getLockedAt());
        
        // Handle collections separately in handleCollectionChanges method
    }
    
    @Override
    @Transactional(readOnly = true)
    public Optional<CandidateEvaluationModel> findByResumeId(UUID resumeId) {
        return evaluationRepository.findByResumeId(resumeId);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<CandidateEvaluationModel> findAllEvaluations() {
        return evaluationRepository.findAll();
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<CandidateEvaluationModel> findByLocked(boolean locked) {
        return evaluationRepository.findByLocked(locked);
    }
    
    @Override
    @Transactional
    public CandidateEvaluationModel lockEvaluation(UUID resumeId, String managerId) {
        // Verify resume exists
        Resume resume = resumeRepository.findById(resumeId)
            .orElseThrow(() -> new ResourceNotFoundException("Resume", "id", resumeId));
        
        // Check if already locked
        Optional<CandidateEvaluationModel> existingLock = evaluationRepository.findByResumeIdAndLocked(resumeId, true);
        if (existingLock.isPresent()) {
            throw new CandidateAlreadyLockedException(resumeId, existingLock.get().getManagerId());
        }
        
        // Check if there's an existing evaluation for this resume
        Optional<CandidateEvaluationModel> existingEvaluation = evaluationRepository.findByResumeId(resumeId);
        
        // Get existing evaluation or create a new one
        CandidateEvaluationModel evaluation;
        if (existingEvaluation.isPresent()) {
            evaluation = existingEvaluation.get();
            
            // Store previous status before updating
            CandidateStatus previousStatus = evaluation.getStatus();
            String previousCustomStatus = evaluation.getCustomStatus();
            
            // Set lock information
            evaluation.setLocked(true);
            evaluation.setManagerId(managerId);
            evaluation.setLockedAt(LocalDateTime.now());
            
            // Record the status change
            statusHistoryUtil.recordStatusChange(
                resumeId,
                evaluation.getId(),
                previousStatus,
                previousCustomStatus,
                evaluation.getStatus(),
                evaluation.getCustomStatus(),
                managerId,
                null
            );
        } else {
            evaluation = new CandidateEvaluationModel();
            evaluation.setResumeId(resumeId);
            evaluation.setName(resume.getName());
            evaluation.setEmail(resume.getEmail());
            evaluation.setPhoneNumber(resume.getPhoneNumber());
            
            // Set lock information
            evaluation.setLocked(true);
            evaluation.setManagerId(managerId);
            evaluation.setLockedAt(LocalDateTime.now());
            
            // For new evaluations, the previous status is null
            statusHistoryUtil.recordStatusChange(
                resumeId,
                null, // ID will be null until saved
                null, // No previous status for new evaluations
                null, // No previous custom status
                evaluation.getStatus(),
                evaluation.getCustomStatus(),
                managerId,
                null
            );
        }
        
        // Save to database
        CandidateEvaluationModel savedEvaluation = evaluationRepository.save(evaluation);
        
        // If this was a new evaluation, update the history record with the evaluation ID
        if (existingEvaluation.isEmpty()) {
            statusHistoryUtil.recordStatusChange(
                resumeId,
                savedEvaluation.getId(),
                null,
                null,
                savedEvaluation.getStatus(),
                savedEvaluation.getCustomStatus(),
                managerId,
                null
            );
        }
        
        return savedEvaluation;
    }
    
    @Override
    @Transactional
    public CandidateEvaluationModel unlockEvaluation(UUID resumeId, String managerId) {
        // Verify resume exists
        if (!resumeRepository.existsById(resumeId)) {
            throw new ResourceNotFoundException("Resume", "id", resumeId);
        }
        
        // Check if locked
        CandidateEvaluationModel existingLock = evaluationRepository.findByResumeIdAndLocked(resumeId, true)
            .orElseThrow(() -> new ResourceNotFoundException("Locked Resume", "resumeId", resumeId));
        
        // Check if the manager is the one who locked it
        if (!existingLock.getManagerId().equals(managerId)) {
            throw new CandidateAlreadyLockedException(resumeId, existingLock.getManagerId());
        }
        
        // Store previous status before updating
        CandidateStatus previousStatus = existingLock.getStatus();
        String previousCustomStatus = existingLock.getCustomStatus();
        
        // Update lock status
        existingLock.setLocked(false);
       // existingLock.setManagerId(null);
       // existingLock.setLockedAt(null);
        
      CandidateEvaluationModel save = evaluationRepository.save(existingLock);
        // Record the status change
        statusHistoryUtil.recordStatusChange(
            resumeId,
            existingLock.getId(),
            previousStatus,
            previousCustomStatus,
            existingLock.getStatus(),
            existingLock.getCustomStatus(),
            managerId,
            null
        );
        
        // Save to database
        return save;
    }
    
    @Override
    @Transactional
    public CandidateEvaluationModel processLockRequest(LockResumeRequest request) {
        logger.debug("Processing lock request for resume ID: {}, manager ID: {}", request.getResumeId(), request.getManagerId());
        
        // Check if resume exists
        Resume resume = resumeRepository.findById(request.getResumeId())
            .orElseThrow(() -> new ResourceNotFoundException("Resume", "id", request.getResumeId()));
        
        // Check if already locked
        Optional<CandidateEvaluationModel> existingLock = evaluationRepository.findByResumeIdAndLocked(request.getResumeId(), true);
        
        // If there's an existing lock by a different manager, throw an exception
        if (existingLock.isPresent() && !existingLock.get().getManagerId().equals(request.getManagerId())) {
            throw new CandidateAlreadyLockedException(request.getResumeId(), existingLock.get().getManagerId());
        }
        
        // If we're unlocking
        if (!request.isLocked() && existingLock.isPresent()) {
            return unlockEvaluation(request.getResumeId(), request.getManagerId());
        }
        
        // If already locked by the same manager and we're not changing anything, return the existing evaluation
        if (existingLock.isPresent() && !request.hasEvaluationData()) {
            logger.debug("Resume ID: {} is already locked by the same manager with no changes, returning existing evaluation", 
                       request.getResumeId());
            return existingLock.get();
        }
        
        // Check if there's an existing evaluation for this resume
        Optional<CandidateEvaluationModel> existingEvaluation = evaluationRepository.findByResumeId(request.getResumeId());
        
        // If the request is identical to the existing evaluation, return the existing evaluation without saving
        if (existingEvaluation.isPresent() && isRequestIdenticalToExistingEvaluation(request, existingEvaluation.get())) {
            logger.debug("Request is identical to existing evaluation, skipping update for resume ID: {}", request.getResumeId());
            return existingEvaluation.get();
        }
        
        // If the request contains evaluation data, update the evaluation
        if (request.hasEvaluationData()) {
            CandidateEvaluationModel evaluation;
            
            if (existingEvaluation.isPresent()) {
                evaluation = existingEvaluation.get();
                
                // Check if collections have changed before updating them
                boolean keyStrengthsChanged = !EntityComparisonUtils.areCollectionsEqual(
                    evaluation.getKeyStrengths(), request.getKeyStrengths());
                boolean improvementAreasChanged = !EntityComparisonUtils.areCollectionsEqual(
                    evaluation.getImprovementAreas(), request.getImprovementAreas());
                
                // Only update collections if they've changed
                if (keyStrengthsChanged) {
                    if (evaluation.getKeyStrengths() != null && request.getKeyStrengths() != null) {
                        EntityComparisonUtils.handleCollectionChanges(evaluation.getKeyStrengths(), request.getKeyStrengths());
                    } else {
                        evaluation.setKeyStrengths(request.getKeyStrengths());
                    }
                }
                
                if (improvementAreasChanged) {
                    if (evaluation.getImprovementAreas() != null && request.getImprovementAreas() != null) {
                        EntityComparisonUtils.handleCollectionChanges(evaluation.getImprovementAreas(), request.getImprovementAreas());
                    } else {
                        evaluation.setImprovementAreas(request.getImprovementAreas());
                    }
                }
                
                // Update evaluation data only if it's different
                if (!Objects.equals(evaluation.getExecutiveSummary(), request.getExecutiveSummary())) {
                    evaluation.setExecutiveSummary(request.getExecutiveSummary());
                }
                
                // Convert String to Integer for numeric fields
                if (request.getTechnicalSkills() != null) {
                    try {
                        evaluation.setTechnicalSkills(Integer.parseInt(request.getTechnicalSkills()));
                    } catch (NumberFormatException e) {
                        logger.warn("Invalid technical skills value: {}", request.getTechnicalSkills());
                    }
                }
                
                if (request.getExperience() != null) {
                    try {
                        evaluation.setExperience(Integer.parseInt(request.getExperience()));
                    } catch (NumberFormatException e) {
                        logger.warn("Invalid experience value: {}", request.getExperience());
                    }
                }
                
                if (request.getEducation() != null) {
                    try {
                        evaluation.setEducation(Integer.parseInt(request.getEducation()));
                    } catch (NumberFormatException e) {
                        logger.warn("Invalid education value: {}", request.getEducation());
                    }
                }
                
                if (request.getSoftSkills() != null) {
                    try {
                        evaluation.setSoftSkills(Integer.parseInt(request.getSoftSkills()));
                    } catch (NumberFormatException e) {
                        logger.warn("Invalid soft skills value: {}", request.getSoftSkills());
                    }
                }
                
                if (request.getAchievements() != null) {
                    try {
                        evaluation.setAchievements(Integer.parseInt(request.getAchievements()));
                    } catch (NumberFormatException e) {
                        logger.warn("Invalid achievements value: {}", request.getAchievements());
                    }
                }
                
                evaluation.setRecommendationType(request.getRecommendationType());
                evaluation.setRecommendationReason(request.getRecommendationReason());
                evaluation.setScore(request.getScore());
            } else {
                // Create a new evaluation
                evaluation = new CandidateEvaluationModel();
                evaluation.setResumeId(request.getResumeId());
                evaluation.setName(resume.getName());
                evaluation.setEmail(resume.getEmail());
                evaluation.setPhoneNumber(resume.getPhoneNumber());
                
                // Set evaluation data
                evaluation.setExecutiveSummary(request.getExecutiveSummary());
                evaluation.setKeyStrengths(request.getKeyStrengths());
                evaluation.setImprovementAreas(request.getImprovementAreas());
                
                // Convert String to Integer for numeric fields
                if (request.getTechnicalSkills() != null) {
                    try {
                        evaluation.setTechnicalSkills(Integer.parseInt(request.getTechnicalSkills()));
                    } catch (NumberFormatException e) {
                        logger.warn("Invalid technical skills value: {}", request.getTechnicalSkills());
                    }
                }
                
                if (request.getExperience() != null) {
                    try {
                        evaluation.setExperience(Integer.parseInt(request.getExperience()));
                    } catch (NumberFormatException e) {
                        logger.warn("Invalid experience value: {}", request.getExperience());
                    }
                }
                
                if (request.getEducation() != null) {
                    try {
                        evaluation.setEducation(Integer.parseInt(request.getEducation()));
                    } catch (NumberFormatException e) {
                        logger.warn("Invalid education value: {}", request.getEducation());
                    }
                }
                
                if (request.getSoftSkills() != null) {
                    try {
                        evaluation.setSoftSkills(Integer.parseInt(request.getSoftSkills()));
                    } catch (NumberFormatException e) {
                        logger.warn("Invalid soft skills value: {}", request.getSoftSkills());
                    }
                }
                
                if (request.getAchievements() != null) {
                    try {
                        evaluation.setAchievements(Integer.parseInt(request.getAchievements()));
                    } catch (NumberFormatException e) {
                        logger.warn("Invalid achievements value: {}", request.getAchievements());
                    }
                }
                
                evaluation.setRecommendationType(request.getRecommendationType());
                evaluation.setRecommendationReason(request.getRecommendationReason());
                evaluation.setScore(request.getScore());
            }
            
            // Store previous status before updating
            CandidateStatus previousStatus = evaluation.getStatus();
            String previousCustomStatus = evaluation.getCustomStatus();
            
            // Set lock information
            evaluation.setLocked(request.isLocked());
            evaluation.setManagerId(request.getManagerId());
            evaluation.setLockedAt(LocalDateTime.now());
            
            
           CandidateEvaluationModel update = updateEvaluationIfChanged(evaluation);
            // Record the status change
            statusHistoryUtil.recordStatusChange(
                request.getResumeId(),
                evaluation.getId(),
                previousStatus,
                previousCustomStatus,
                evaluation.getStatus(),
                evaluation.getCustomStatus(),
                request.getManagerId(),
                null
            );
            
            // Save to database
            return update;
        } else {
            // Just lock the resume without saving evaluation data
            return lockEvaluation(request.getResumeId(), request.getManagerId());
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public boolean isLockedByManager(UUID resumeId, String managerId) {
        Optional<CandidateEvaluationModel> existingLock = evaluationRepository.findByResumeIdAndLocked(resumeId, true);
        return existingLock.isPresent() && existingLock.get().getManagerId().equals(managerId);
    }
    
    @Override
    @Transactional
    public CandidateEvaluationModel updateStatus(UUID resumeId, CandidateStatus status, 
                                               String customStatus, String managerId, String comments) {
        logger.debug("Updating status for resume ID: {} to {}", resumeId, status);
        
        // Find the evaluation
        CandidateEvaluationModel evaluation = findByResumeId(resumeId)
            .orElseThrow(() -> new ResourceNotFoundException("Resume", "id", resumeId));
        
        // Store previous status before updating
        CandidateStatus previousStatus = evaluation.getStatus();
        String previousCustomStatus = evaluation.getCustomStatus();
        
        // Update the evaluation status
        evaluation.setStatus(status);
        evaluation.setCustomStatus(customStatus);
        
        // For backward compatibility
        evaluation.setLocked(status == CandidateStatus.LOCKED);
        if (status == CandidateStatus.LOCKED) {
            evaluation.setManagerId(managerId);
            evaluation.setLockedAt(LocalDateTime.now());
        }
        
        // Record the status change using the utility
      CandidateEvaluationModel update = evaluationRepository.save(evaluation);
        statusHistoryUtil.recordStatusChange(
            resumeId,
            evaluation.getId(),
            previousStatus,
            previousCustomStatus,
            status,
            customStatus,
            managerId,
            comments
        );
        
        // Save the updated evaluation
        return update;
    }
    
    @Override
    @Transactional
    public CandidateEvaluationModel processStatusUpdate(StatusUpdateRequest request) {
        logger.debug("Processing status update request for resume ID: {}", request.getResumeId());
        
        // Validate the request
        if (request.getResumeId() == null) {
            throw new IllegalArgumentException("Resume ID is required");
        }
        
        if (request.getStatus() == null) {
            throw new IllegalArgumentException("Status is required");
        }
        
        if (request.getManagerId() == null) {
            throw new IllegalArgumentException("Manager ID is required");
        }
        
        // If status is CUSTOM, custom status is required
        if (request.getStatus() == CandidateStatus.CUSTOM && 
            (request.getCustomStatus() == null || request.getCustomStatus().isEmpty())) {
            throw new IllegalArgumentException("Custom status is required when status is CUSTOM");
        }
        
        // Update the status
        return updateStatus(
            request.getResumeId(),
            request.getStatus(),
            request.getCustomStatus(),
            request.getManagerId(),
            request.getComments()
        );
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<CandidateStatusHistory> getStatusHistory(UUID resumeId) {
        logger.debug("Getting status history for resume ID: {}", resumeId);
        
        // Verify resume exists
        if (!resumeRepository.existsById(resumeId)) {
            throw new ResourceNotFoundException("Resume", "id", resumeId);
        }
        
        // Get the status history
        return statusHistoryRepository.findByResumeIdOrderByChangedAtDesc(resumeId);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<CandidateStatusHistory> getStatusHistoryByEvaluationId(UUID evaluationId) {
        logger.debug("Getting status history for evaluation ID: {}", evaluationId);
        
        // Verify evaluation exists
        if (!evaluationRepository.existsById(evaluationId)) {
            throw new ResourceNotFoundException("Evaluation", "id", evaluationId);
        }
        
        // Get the status history
        return statusHistoryRepository.findByEvaluationIdOrderByChangedAtDesc(evaluationId);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<CandidateStatusHistory> getStatusHistoryByUser(String userId) {
        logger.debug("Getting status history for user ID: {}", userId);
        
        // Get the status history
        return statusHistoryRepository.findByChangedByOrderByChangedAtDesc(userId);
    }
    
    /**
     * Checks if a request is identical to an existing evaluation.
     * 
     * @param request The lock request
     * @param existing The existing evaluation
     * @return true if the request is identical to the existing evaluation, false otherwise
     */
    private boolean isRequestIdenticalToExistingEvaluation(LockResumeRequest request, CandidateEvaluationModel existing) {
        // Check if lock status and manager ID are the same
        if (existing.isLocked() != request.isLocked() || 
            !Objects.equals(existing.getManagerId(), request.getManagerId())) {
            return false;
        }
        
        // If the request doesn't have evaluation data, we've already checked lock status and manager ID
        if (!request.hasEvaluationData()) {
            return true;
        }
        
        // Check if evaluation data is the same
        if (!Objects.equals(existing.getExecutiveSummary(), request.getExecutiveSummary()) ||
            !EntityComparisonUtils.areCollectionsEqual(existing.getKeyStrengths(), request.getKeyStrengths()) ||
            !EntityComparisonUtils.areCollectionsEqual(existing.getImprovementAreas(), request.getImprovementAreas()) ||
            !Objects.equals(existing.getScore(), request.getScore()) ||
            !Objects.equals(existing.getRecommendationType(), request.getRecommendationType()) ||
            !Objects.equals(existing.getRecommendationReason(), request.getRecommendationReason())) {
            return false;
        }
        
        // Check numeric fields
        Integer requestTechnicalSkills = null;
        Integer requestExperience = null;
        Integer requestEducation = null;
        Integer requestSoftSkills = null;
        Integer requestAchievements = null;
        
        try {
            if (request.getTechnicalSkills() != null) {
                requestTechnicalSkills = Integer.parseInt(request.getTechnicalSkills());
            }
            if (request.getExperience() != null) {
                requestExperience = Integer.parseInt(request.getExperience());
            }
            if (request.getEducation() != null) {
                requestEducation = Integer.parseInt(request.getEducation());
            }
            if (request.getSoftSkills() != null) {
                requestSoftSkills = Integer.parseInt(request.getSoftSkills());
            }
            if (request.getAchievements() != null) {
                requestAchievements = Integer.parseInt(request.getAchievements());
            }
        } catch (NumberFormatException e) {
            logger.warn("Error parsing numeric fields from request", e);
            return false;
        }
        
        if (!Objects.equals(existing.getTechnicalSkills(), requestTechnicalSkills) ||
            !Objects.equals(existing.getExperience(), requestExperience) ||
            !Objects.equals(existing.getEducation(), requestEducation) ||
            !Objects.equals(existing.getSoftSkills(), requestSoftSkills) ||
            !Objects.equals(existing.getAchievements(), requestAchievements)) {
            return false;
        }
        
        // If we get here, the request is identical to the existing evaluation
        return true;
    }
}
