package com.telus.spring.ai.resume.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.telus.spring.ai.resume.model.CandidateStatus;
import com.telus.spring.ai.resume.model.Status;

/**
 * Repository for managing CandidateStatus entities.
 */
@Repository
public interface CandidateStatusRepository extends JpaRepository<CandidateStatus, UUID> {
    
    /**
     * Find all status records for a specific resume.
     * 
     * @param resumeId The ID of the resume
     * @return List of status records
     */
    List<CandidateStatus> findByResumeId(UUID resumeId);
    
    /**
     * Find a status record for a specific resume with a specific status.
     * 
     * @param resumeId The ID of the resume
     * @param status The status to find
     * @return Optional containing the status record if found
     */
    Optional<CandidateStatus> findByResumeIdAndStatus(UUID resumeId, Status status);
    
    /**
     * Find all status records for a specific manager.
     * 
     * @param managerId The ID of the manager
     * @return List of status records
     */
    List<CandidateStatus> findByManagerId(String managerId);
    
    /**
     * Find all status records for a specific position.
     * 
     * @param position The position
     * @return List of status records
     */
    List<CandidateStatus> findByPosition(String position);
    
    /**
     * Check if a resume has a specific status.
     * 
     * @param resumeId The ID of the resume
     * @param status The status to check
     * @return true if the resume has the specified status, false otherwise
     */
    boolean existsByResumeIdAndStatus(UUID resumeId, Status status);
    
    /**
     * Find all status records for a list of resume IDs with a specific status.
     * 
     * @param resumeIds The list of resume IDs
     * @param status The status to find
     * @return List of status records
     */
    List<CandidateStatus> findByResumeIdInAndStatus(List<UUID> resumeIds, Status status);
}
