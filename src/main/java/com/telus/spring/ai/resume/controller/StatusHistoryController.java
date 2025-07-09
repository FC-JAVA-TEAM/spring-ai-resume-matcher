package com.telus.spring.ai.resume.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.telus.spring.ai.resume.model.CandidateEvaluationModel;
import com.telus.spring.ai.resume.model.CandidateStatusHistory;
import com.telus.spring.ai.resume.model.dto.StatusUpdateRequest;
import com.telus.spring.ai.resume.service.CandidateEvaluationService;

/**
 * REST controller for managing candidate status history.
 */
@RestController
@RequestMapping("/api/status")
public class StatusHistoryController {
    
    private final CandidateEvaluationService evaluationService;
    
    @Autowired
    public StatusHistoryController(CandidateEvaluationService evaluationService) {
        this.evaluationService = evaluationService;
    }
    
    /**
     * Update the status of a candidate.
     * 
     * @param request The status update request
     * @return The updated candidate evaluation
     */
    @PostMapping("/update")
    public ResponseEntity<CandidateEvaluationModel> updateStatus(@RequestBody StatusUpdateRequest request) {
        CandidateEvaluationModel evaluation = evaluationService.processStatusUpdate(request);
        return ResponseEntity.ok(evaluation);
    }
    
    /**
     * Get the status history for a resume.
     * 
     * @param resumeId The ID of the resume
     * @return List of status history records
     */
    @GetMapping("/history/resume/{resumeId}")
    public ResponseEntity<List<CandidateStatusHistory>> getHistoryByResumeId(@PathVariable UUID resumeId) {
        List<CandidateStatusHistory> history = evaluationService.getStatusHistory(resumeId);
        return ResponseEntity.ok(history);
    }
    
    /**
     * Get the status history for an evaluation.
     * 
     * @param evaluationId The ID of the evaluation
     * @return List of status history records
     */
    @GetMapping("/history/evaluation/{evaluationId}")
    public ResponseEntity<List<CandidateStatusHistory>> getHistoryByEvaluationId(@PathVariable UUID evaluationId) {
        // Use the repository directly for now
        List<CandidateStatusHistory> history = evaluationService.getStatusHistoryByEvaluationId(evaluationId);
        return ResponseEntity.ok(history);
    }
    
    /**
     * Get the status history for a user.
     * 
     * @param userId The ID of the user
     * @return List of status history records
     */
    @GetMapping("/history/user/{userId}")
    public ResponseEntity<List<CandidateStatusHistory>> getHistoryByUser(@PathVariable String userId) {
        // Use the repository directly for now
        List<CandidateStatusHistory> history = evaluationService.getStatusHistoryByUser(userId);
        return ResponseEntity.ok(history);
    }
}
