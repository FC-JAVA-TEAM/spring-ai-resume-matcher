package com.telus.spring.ai.resume.controller;

import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.telus.spring.ai.resume.model.ApiResponse;
import com.telus.spring.ai.resume.model.CandidateStatus;
import com.telus.spring.ai.resume.model.dto.LockCandidateRequest;
import com.telus.spring.ai.resume.model.dto.UpdateStatusRequest;
import com.telus.spring.ai.resume.service.CandidateStatusService;
import com.telus.spring.ai.resume.service.ResumeMatchingService;

/**
 * REST controller for managing candidate status operations.
 */
@RestController
@RequestMapping("/api/candidate-status")
public class CandidateStatusController {
    
    private static final Logger logger = LoggerFactory.getLogger(CandidateStatusController.class);
    
    private final CandidateStatusService candidateStatusService;
    private final ResumeMatchingService resumeMatchingService;
    
    @Autowired
    public CandidateStatusController(
            CandidateStatusService candidateStatusService,
            ResumeMatchingService resumeMatchingService) {
        this.candidateStatusService = candidateStatusService;
        this.resumeMatchingService = resumeMatchingService;
    }
    
    /**
     * Lock a candidate for a specific position.
     * 
     * @param request The lock candidate request containing resumeId, managerId, position, and notes
     * @return The created CandidateStatus wrapped in an ApiResponse
     */
    @PostMapping("/lock")
    public ResponseEntity<ApiResponse<CandidateStatus>> lockCandidate(@RequestBody LockCandidateRequest request) {
        logger.info("Locking candidate with resumeId: {} for position: {} by manager: {}", 
                request.getResumeId(), request.getPosition(), request.getManagerId());
        
        UUID resumeUuid = UUID.fromString(request.getResumeId());
        CandidateStatus status = candidateStatusService.lockCandidate(
                resumeUuid, 
                request.getManagerId(), 
                request.getPosition(), 
                request.getNotes()
        );
        
        logger.info("Successfully locked candidate with resumeId: {}", request.getResumeId());
        return new ResponseEntity<>(ApiResponse.success(status), HttpStatus.CREATED);
    }
    
    /**
     * Release a previously locked candidate.
     * 
     * @param statusId The ID of the status record
     * @param request The update request containing managerId
     * @return The updated CandidateStatus wrapped in an ApiResponse
     */
//    @PutMapping("/{statusId}/release")
//    public ResponseEntity<ApiResponse<CandidateStatus>> releaseCandidate(
//            @PathVariable String statusId,
//            @RequestBody UpdateStatusRequest request) {
//        
//        logger.info("Releasing candidate with statusId: {} by manager: {}", statusId, request.getManagerId());
//        
//        UUID statusUuid = UUID.fromString(statusId);
//        CandidateStatus status = candidateStatusService.releaseCandidate(statusUuid, request.getManagerId());
//        
//        logger.info("Successfully released candidate with statusId: {}", statusId);
//        return ResponseEntity.ok(ApiResponse.success(status));
//    }
//    
    /**
     * Update the status of a candidate.
     * 
     * @param statusId The ID of the status record
     * @param request The update request containing managerId, newStatus, and notes
     * @return The updated CandidateStatus wrapped in an ApiResponse
     */
//    @PutMapping("/{statusId}/update")
//    public ResponseEntity<ApiResponse<CandidateStatus>> updateCandidateStatus(
//            @PathVariable String statusId,
//            @RequestBody UpdateStatusRequest request) {
//        
//        logger.info("Updating candidate status for statusId: {} to status: {} by manager: {}", 
//                statusId, request.getNewStatus(), request.getManagerId());
//        
//        UUID statusUuid = UUID.fromString(statusId);
//        CandidateStatus status = candidateStatusService.updateCandidateStatus(
//                statusUuid, 
//                request.getManagerId(), 
//                request.getNewStatus(), 
//                request.getNotes()
//        );
//        
//        logger.info("Successfully updated candidate status for statusId: {} to status: {}", 
//                statusId, request.getNewStatus());
//        return ResponseEntity.ok(ApiResponse.success(status));
//    }
    
    /**
     * get all status records for a specific resume.
     *       
     * 
     * @param resumeId The ID of the resume
     * @return List of status records
     */
//    @GetMapping("/resume/{resumeId}")
//    public ResponseEntity<List<CandidateStatus>> getStatusByResumeId(@PathVariable String resumeId) {
//        logger.info("Getting status records for resumeId: {}", resumeId);
//        
//        UUID resumeUuid = UUID.fromString(resumeId);
//        List<CandidateStatus> statuses = candidateStatusService.getStatusByResumeId(resumeUuid);
//        
//        logger.info("Found {} status records for resumeId: {}", statuses.size(), resumeId);
//        return ResponseEntity.ok(statuses);
//    }
    
    /**
     * Check if a candidate is locked.
     * 
     * @param resumeId The ID of the resume
     * @return true if the candidate is locked, false otherwise
     */
    @GetMapping("/resume/{resumeId}/locked")
    public ResponseEntity<Boolean> isCandidateLocked(@PathVariable String resumeId) {
        logger.info("Checking if candidate with resumeId: {} is locked", resumeId);
        
        UUID resumeUuid = UUID.fromString(resumeId);
        boolean isLocked = candidateStatusService.isCandidateLocked(resumeUuid);
        
        logger.info("Candidate with resumeId: {} is locked: {}", resumeId, isLocked);
        return ResponseEntity.ok(isLocked);
    }
    
    /**
     * Get all status records for a specific manager.
     * 
     * @param managerId The ID of the manager
     * @return List of status records
     */
    @GetMapping("/manager/{managerId}")
    public ResponseEntity<List<CandidateStatus>> getStatusByManager(@PathVariable String managerId) {
        logger.info("Getting status records for manager: {}", managerId);
        
        List<CandidateStatus> statuses = candidateStatusService.getStatusByManager(managerId);
        
        logger.info("Found {} status records for manager: {}", statuses.size(), managerId);
        return ResponseEntity.ok(statuses);
    }
    
    /**
     * Get all status records for a specific position.
     * 
     * @param position The position
     * @return List of status records
     */
    @GetMapping("/position")
    public ResponseEntity<List<CandidateStatus>> getStatusByPosition(@RequestParam String position) {
        logger.info("Getting status records for position: {}", position);
        
        List<CandidateStatus> statuses = candidateStatusService.getStatusByPosition(position);
        
        logger.info("Found {} status records for position: {}", statuses.size(), position);
        return ResponseEntity.ok(statuses);
    }
    
    /**
     * Get a specific status record by ID.
     * 
     * @param statusId The ID of the status record
     * @return The status record
     */
    @GetMapping("/{statusId}")
    public ResponseEntity<CandidateStatus> getStatusById(@PathVariable String statusId) {
        logger.info("Getting status record for statusId: {}", statusId);
        
        UUID statusUuid = UUID.fromString(statusId);
        CandidateStatus status = candidateStatusService.getStatusById(statusUuid);
        
        logger.info("Successfully retrieved status record for statusId: {}", statusId);
        return ResponseEntity.ok(status);
    }
    
    /**
     * Match resumes to a job description with option to exclude locked candidates.
     * This endpoint uses the ResumeMatchingService to find matching resumes and
     * then checks if each resume is locked using the CandidateStatusService.
     * 
     * @param request The match request containing job description, limit, and excludeLocked flag
     * @return A list of resume matches with lock status
     */
//    @PostMapping("/match")
//    public ResponseEntity<List<ResumeMatch>> matchResumes(@RequestBody MatchRequest request) {
//        logger.info("Matching resumes to job description, limit: {}, excludeLocked: {}", 
//                request.getLimit(), request.isExcludeLocked());
//        
//        // Get matching resumes from the ResumeMatchingService
//        List<ResumeMatch> matches = resumeMatchingService.findMatchingResumes(request.getJobDescription(), request.getLimit());
//        
//        // Process matches to include lock status and filter out locked candidates if requested
//        List<ResumeMatch> processedMatches = matches.stream()
//                .map(match -> {
//                    // Get the resume ID
//                    Resume resume = match.getResume();
//                    UUID resumeId = resume.getId();
//                    
//                    // Check if the resume is locked
//                    boolean isLocked = candidateStatusService.isCandidateLocked(resumeId);
//                    
//                    // Create a new ResumeMatch with the lock status
//                    return new ResumeMatch(
//                            resume,
//                            match.getScore(),
//                            match.getExplanation(),
//                            match.getAnalysis(),
//                            isLocked
//                    );
//                })
//                .filter(match -> !request.isExcludeLocked() || !match.isLocked())
//                .collect(Collectors.toList());
//        
//        logger.info("Found {} matching resumes after filtering", processedMatches.size());
//        
//        return ResponseEntity.ok(processedMatches);
//    }
//    
    /**
     * Match resumes to a job description with option to exclude locked candidates.
     * This endpoint is accessible at /api/resumes/match-new and returns results in ApiResponse format.
     * 
     * @param request The match request containing job description, limit, and excludeLocked flag
     * @return A list of resume matches with lock status wrapped in an ApiResponse
     */
//    @PostMapping(path = "/resumes/match-new")
//    public ResponseEntity<ApiResponse<List<ResumeMatch>>> matchResumesNew(@RequestBody MatchRequest request) {
//        logger.info("New endpoint: Matching resumes to job description, limit: {}, excludeLocked: {}", 
//                request.getLimit(), request.isExcludeLocked());
//        
//        // Get matching resumes from the ResumeMatchingService
//        List<ResumeMatch> matches = resumeMatchingService.findMatchingResumes(request.getJobDescription(), request.getLimit());
//        
//        // Process matches to include lock status and filter out locked candidates if requested
//        List<ResumeMatch> processedMatches = matches.stream()
//                .map(match -> {
//                    // Get the resume ID
//                    Resume resume = match.getResume();
//                    UUID resumeId = resume.getId();
//                    
//                    // Check if the resume is locked
//                    boolean isLocked = candidateStatusService.isCandidateLocked(resumeId);
//                    
//                    // Create a new ResumeMatch with the lock status
//                    return new ResumeMatch(
//                            resume,
//                            match.getScore(),
//                            match.getExplanation(),
//                            match.getAnalysis(),
//                            isLocked
//                    );
//                })
//                .filter(match -> !request.isExcludeLocked() || !match.isLocked())
//                .collect(Collectors.toList());
//        
//        logger.info("Found {} matching resumes after filtering", processedMatches.size());
//        
//        return ResponseEntity.ok(ApiResponse.success(processedMatches));
//    }
    
    /**
     * Get all locked candidates.
     * 
     * @return List of locked candidate statuses
     */
    @GetMapping("/locked")
    public ResponseEntity<List<CandidateStatus>> getAllLockedCandidates() {
        logger.info("Getting all locked candidates");
        
       // candidateStatusService.
        List<CandidateStatus> lockedCandidates = candidateStatusService.getLockedResumeIds();
        
        logger.info("Found {} locked candidates", lockedCandidates.size());
        return ResponseEntity.ok(lockedCandidates);
    }
    
    /**
     * Update the status of a candidate by resume ID.
     * If multiple status records exist for the resume, updates the most recent one.
     * 
     * @param resumeId The ID of the resume
     * @param request The update request containing managerId, newStatus, and notes
     * @return The updated CandidateStatus
     */
    @PutMapping("/resume/update")
    public ResponseEntity<CandidateStatus> updateCandidateStatusByResumeId(
          //  @PathVariable String resumeId,
            @RequestBody UpdateStatusRequest request) {
        
//        logger.info("Updating candidate status for resumeId: {} to status: {} by manager: {}", 
//                resumeId, request.getNewStatus(), request.getManagerId());
//        
        UUID resumeUuid = UUID.fromString(request.getResumeId());
        CandidateStatus status = candidateStatusService.updateCandidateStatusByResumeId(
                resumeUuid, 
                request.getManagerId(), 
                request.getNewStatus(), 
                request.getNotes()
        );
        
//        logger.info("Successfully updated candidate status for resumeId: {} to status: {}", 
//                resumeId, request.getNewStatus());
        return ResponseEntity.ok(status);
    }
}
