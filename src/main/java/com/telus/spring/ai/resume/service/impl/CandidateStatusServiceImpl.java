package com.telus.spring.ai.resume.service.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.telus.spring.ai.resume.exception.CandidateAlreadyLockedException;
import com.telus.spring.ai.resume.exception.ResourceNotFoundException;
import com.telus.spring.ai.resume.exception.UnauthorizedException;
import com.telus.spring.ai.resume.model.CandidateStatus;
import com.telus.spring.ai.resume.model.Status;
import com.telus.spring.ai.resume.repository.CandidateStatusRepository;
import com.telus.spring.ai.resume.service.CandidateStatusService;

/**
 * Implementation of the CandidateStatusService interface.
 */
@Service
public class CandidateStatusServiceImpl implements CandidateStatusService {
    
    private static final Logger logger = LoggerFactory.getLogger(CandidateStatusServiceImpl.class);
    
    private final CandidateStatusRepository candidateStatusRepository;
    
    @Autowired
    public CandidateStatusServiceImpl(CandidateStatusRepository candidateStatusRepository) {
        this.candidateStatusRepository = candidateStatusRepository;
    }
    
    @Override
    @Transactional
    public CandidateStatus lockCandidate(UUID resumeId, String managerId, String position, String notes) {
        logger.info("Locking candidate with resumeId: {} for position: {} by manager: {}", resumeId, position, managerId);
        
        // Check if already locked
        if (isCandidateLocked(resumeId)) {
            logger.warn("Candidate with resumeId: {} is already locked", resumeId);
            throw new CandidateAlreadyLockedException("This candidate is already locked by another manager");
        }
        
        // Provide a default value if position is null
        String actualPosition = (position != null) ? position : "Unspecified Position";
        
        CandidateStatus status = new CandidateStatus(resumeId, managerId, actualPosition, Status.LOCKED);
        status.setNotes(notes);
        status.setCreatedAt(LocalDateTime.now());
        
        return candidateStatusRepository.save(status);
    }
    
    @Override
    @Transactional
    public CandidateStatus releaseCandidate(UUID statusId, String managerId) {
        logger.info("Releasing candidate with statusId: {} by manager: {}", statusId, managerId);
        
        CandidateStatus status = getStatusById(statusId);
        
        // Verify the manager is the one who locked it
        if (!status.getManagerId().equals(managerId)) {
            throw new UnauthorizedException(
                String.format("Only the manager %s who locked the candidate can update their status. Your manager ID: %s", 
                    status.getManagerId(), managerId));
        }
        
        status.setStatus(Status.OPEN);
        status.setUpdatedAt(LocalDateTime.now());
        
        return candidateStatusRepository.save(status);
    }
    
    @Override
    @Transactional
    public CandidateStatus updateCandidateStatus(UUID statusId, String managerId, Status newStatus, String notes) {
        logger.info("Updating candidate status with statusId: {} to status: {} by manager: {}", 
                statusId, newStatus, managerId);
        
        CandidateStatus status = getStatusById(statusId);
        
        // Verify the manager is the one who locked it
        if (!status.getManagerId().equals(managerId)) {
            throw new UnauthorizedException(
                String.format("Only the manager %s who locked the candidate can update their status. Your manager ID: %s", 
                    status.getManagerId(), managerId));
        }
        
        status.setStatus(newStatus);
        if (notes != null && !notes.isEmpty()) {
            status.setNotes(notes);
        }
        status.setUpdatedAt(LocalDateTime.now());
        
        return candidateStatusRepository.save(status);
    }
    
    @Override
    public List<CandidateStatus> getStatusByResumeId(UUID resumeId) {
        logger.info("Getting status records for resumeId: {}", resumeId);
        return candidateStatusRepository.findByResumeId(resumeId);
    }
    
    @Override
    public boolean isCandidateLocked(UUID resumeId) {
        logger.info("Checking if candidate with resumeId: {} is locked", resumeId);
        return candidateStatusRepository.existsByResumeIdAndStatus(resumeId, Status.LOCKED);
    }
    
    @Override
    public List<CandidateStatus> getStatusByManager(String managerId) {
        logger.info("Getting status records for managerId: {}", managerId);
        return candidateStatusRepository.findByManagerId(managerId);
    }
    
    @Override
    public List<CandidateStatus> getStatusByPosition(String position) {
        logger.info("Getting status records for position: {}", position);
        return candidateStatusRepository.findByPosition(position);
    }
    
    @Override
    public CandidateStatus getStatusById(UUID statusId) {
        logger.info("Getting status record for statusId: {}", statusId);
        return candidateStatusRepository.findById(statusId)
                .orElseThrow(() -> new ResourceNotFoundException("CandidateStatus", "id", statusId));
    }
    
    @Override
    @Transactional
    public CandidateStatus updateCandidateStatusByResumeId(UUID resumeId, String managerId, Status newStatus, String notes) {
        logger.info("Updating candidate status with resumeId: {} to status: {} by manager: {}", 
                resumeId, newStatus, managerId);
        
        // Get all status records for this resume
        List<CandidateStatus> statuses = getStatusByResumeId(resumeId);
        
        if (statuses.isEmpty()) {
            logger.warn("No status records found for resumeId: {}", resumeId);
            throw new ResourceNotFoundException("CandidateStatus", "resumeId", resumeId);
        }
        
        // Find the most recent status (the one with the latest createdAt timestamp)
        CandidateStatus mostRecentStatus = statuses.stream()
                .sorted((s1, s2) -> s2.getCreatedAt().compareTo(s1.getCreatedAt()))
                .findFirst()
                .get(); // Safe to call get() as we've already checked that the list is not empty
        
        
        
        // Verify the manager is the one who locked it
        if (!mostRecentStatus.getManagerId().equals(managerId)) {
            throw new UnauthorizedException(
                String.format("Only the manager %s who locked the candidate can update their status. Your manager ID: %s", 
                    mostRecentStatus.getManagerId(), managerId));
        }
        
        // Update the status
        mostRecentStatus.setStatus(newStatus);
        if (notes != null && !notes.isEmpty()) {
            mostRecentStatus.setNotes(notes);
        }
        mostRecentStatus.setUpdatedAt(LocalDateTime.now());
        
        return candidateStatusRepository.save(mostRecentStatus);
    }
    
    @Override
    public List<UUID> getLockedResumeIds(List<UUID> resumeIds) {
        logger.info("Getting locked resume IDs from a list of {} resume IDs", resumeIds.size());
        
        if (resumeIds == null || resumeIds.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        
        return candidateStatusRepository.findByResumeIdInAndStatus(resumeIds, Status.LOCKED)
                .stream()
                .map(CandidateStatus::getResumeId)
                .collect(java.util.stream.Collectors.toList());
    }
}
