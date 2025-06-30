package com.telus.spring.ai.resume.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.telus.spring.ai.resume.model.Resume;
import com.telus.spring.ai.resume.model.ResumeMatch;
import com.telus.spring.ai.resume.model.ResumeResponse;
import com.telus.spring.ai.resume.service.ResumeMatchingService;
import com.telus.spring.ai.resume.service.ResumeStorageService;
import com.telus.spring.ai.resume.service.impl.ResumeMatchingServiceImpl;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Controller for resume matching endpoints.
 * Simplified to focus only on matching functionality without file uploads.
 * Uses synchronous endpoints with optimized internal processing.
 */
@RestController
@RequestMapping("/api/resumes")
public class ResumeController {
    
    private static final Logger logger = LoggerFactory.getLogger(ResumeController.class);
    
    private final ResumeStorageService storageService;
    private final ResumeMatchingService matchingService;
    
    public ResumeController(
            ResumeStorageService storageService,
            ResumeMatchingService matchingService) {
        this.storageService = storageService;
        this.matchingService = matchingService;
    }
    
    /**
     * Match resumes to a job description.
     * This endpoint is synchronous but uses optimized internal parallel processing.
     * 
     * @param jobDescription The job description to match against
     * @param limit The maximum number of matches to return
     * @return A list of resume matches
     */
    @PostMapping("/match")
    public ResponseEntity<List<ResumeResponse>> matchResumes(
            @RequestParam("jd") String jobDescription,
            @RequestParam(value = "limit", defaultValue = "5") int limit) {
        try {
            logger.info("Matching resumes to job description, limit: {}", limit);
            
            // Find matching resumes - this method is internally optimized for parallel processing
            List<ResumeMatch> matches = matchingService.findMatchingResumes(jobDescription, limit);
            
            // Convert to response objects with match information
            List<ResumeResponse> responses = matches.stream()
                    .map(match -> {
                        ResumeResponse response = new ResumeResponse(match.getResume());
                        response.setMatchScore(match.getScore());
                        response.setMatchExplanation(match.getExplanation());
                        return response;
                    })
                    .collect(Collectors.toList());
            
            logger.info("Found {} matching resumes", responses.size());
            
            return ResponseEntity.ok(responses);
        } catch (Exception e) {
            logger.error("Error matching resumes", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Get a resume by ID.
     * 
     * @param id The ID of the resume to get
     * @return The resume
     */
    @GetMapping("/{id}")
    public ResponseEntity<ResumeResponse> getResume(@PathVariable UUID id) {
        logger.info("Getting resume: {}", id);
        
        return storageService.getResumeById(id)
                .map(resume -> {
                    ResumeResponse response = new ResumeResponse(resume);
                    return ResponseEntity.ok(response);
                })
                .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * Get detailed match information for a resume and job description.
     * This endpoint is synchronous but uses the optimized async method internally.
     * 
     * @param id The ID of the resume to match
     * @param jobDescription The job description to match against
     * @return The match information
     */
    @GetMapping("/{id}/match")
    public ResponseEntity<ResumeMatch> getResumeMatch(
            @PathVariable UUID id,
            @RequestParam("jd") String jobDescription) {
        logger.info("Getting match for resume: {} and job description", id);
        
        Optional<Resume> resumeOpt = storageService.getResumeById(id);
        if (resumeOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        Resume resume = resumeOpt.get();
        
        try {
            // Call the async method and wait for the result
            String explanation = matchingService.explainMatchAsync(resume, jobDescription).join();
            
            // Extract score from the explanation
            int score = 0;
            if (matchingService instanceof ResumeMatchingServiceImpl) {
                score = ((ResumeMatchingServiceImpl) matchingService).extractScoreFromExplanation(explanation);
            }
            
            ResumeMatch match = new ResumeMatch(resume, score, explanation);
            return ResponseEntity.ok(match);
        } catch (Exception e) {
            logger.error("Error generating match for resume: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
