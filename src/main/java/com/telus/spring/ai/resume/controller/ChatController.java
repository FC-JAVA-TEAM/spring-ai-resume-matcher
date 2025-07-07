package com.telus.spring.ai.resume.controller;

import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.telus.spring.ai.resume.model.chat.ChatMessage;
import com.telus.spring.ai.resume.model.chat.ChatRequest;
import com.telus.spring.ai.resume.model.chat.ChatResponse;
import com.telus.spring.ai.resume.service.ResumeAwareChatService;

/**
 * Controller for chat-related endpoints.
 */
@RestController
@RequestMapping("/api/chat")
public class ChatController {
    
    private static final Logger logger = LoggerFactory.getLogger(ChatController.class);
    
    private final ResumeAwareChatService chatService;
    
    public ChatController(ResumeAwareChatService chatService) {
        this.chatService = chatService;
    }
    
    /**
     * Send a message to the chat service.
     * 
     * @param userId The ID of the user
     * @param request The chat request
     * @return A response from the chat service
     */
    @PostMapping("/{userId}/message")
    public ResponseEntity<ChatResponse> sendMessage(
            @PathVariable String userId,
            @RequestBody ChatRequest request) {
        
        logger.info("Received chat message from user {}: {}", userId, request);
        
        ChatResponse response = chatService.chat(
            userId,
            request.getMessage(),
            request.getCurrentResumeId(),
            request.getCurrentJobDescription()
        );
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get the chat history for a user.
     * 
     * @param userId The ID of the user
     * @return A list of chat messages
     */
    @GetMapping("/{userId}/history")
    public ResponseEntity<List<ChatMessage>> getChatHistory(@PathVariable String userId) {
        logger.info("Retrieving chat history for user {}", userId);
        
        List<ChatMessage> history = chatService.getChatHistory(userId);
        
        return ResponseEntity.ok(history);
    }
    
    /**
     * Clear the chat history for a user.
     * 
     * @param userId The ID of the user
     * @return A success message
     */
    @PostMapping("/{userId}/clear")
    public ResponseEntity<ChatResponse> clearChatHistory(@PathVariable String userId) {
        logger.info("Clearing chat history for user {}", userId);
        
        // This will be implemented in the chat service
        // For now, just return a success message
        
        return ResponseEntity.ok(new ChatResponse("Chat history cleared successfully."));
    }
    
    /**
     * Match a new resume against a job description.
     * 
     * @param userId The ID of the user
     * @param request The chat request containing the job description
     * @return A response with the match results
     */
    @PostMapping("/{userId}/match-new")
    public ResponseEntity<ChatResponse> matchNewResume(
            @PathVariable String userId,
            @RequestBody ChatRequest request) {
        
        logger.info("Matching new resume for user {}", userId);
        
        if (request.getCurrentJobDescription() == null || request.getCurrentJobDescription().isEmpty()) {
            return ResponseEntity.badRequest().body(new ChatResponse("Please provide a job description."));
        }
        
        // Create a system prompt for matching
        String systemPrompt = "You are an AI assistant specializing in resume analysis and job matching. " +
                             "You will be given a job description. " +
                             "Your task is to analyze the job description and provide insights on what makes a good match.";
        
        // Use the chat service to generate a response
        ChatResponse response = chatService.chat(
            userId,
            "Please analyze this job description and tell me what skills and qualifications would make a good match: " + 
            request.getCurrentJobDescription(),
            null,
            request.getCurrentJobDescription()
        );
        
        return ResponseEntity.ok(response);
    }
    
}

/**
 * Controller for resume matching endpoints.
 */
@RestController
@RequestMapping("/api/resumes")
class ResumeMatchController {
    
    private static final Logger logger = LoggerFactory.getLogger(ResumeMatchController.class);
    
    private final ResumeAwareChatService chatService;
    
    public ResumeMatchController(ResumeAwareChatService chatService) {
        this.chatService = chatService;
    }
    
    /**
     * Match a new resume against a job description.
     * This endpoint is specifically for the URL pattern /api/resumes/match-new-chat
     * 
     * @param request The chat request containing the job description
     * @return A response with the match results
     */
    @PostMapping("/match-new-chat")
    public ResponseEntity<ChatResponse> matchNewResume(
            @RequestBody ChatRequest request) {
        
        // Generate a random user ID for this request
        String userId = UUID.randomUUID().toString();
        logger.info("Matching new resume with dedicated endpoint for user {}", userId);
        
        if (request.getCurrentJobDescription() == null || request.getCurrentJobDescription().isEmpty()) {
            return ResponseEntity.badRequest().body(new ChatResponse("Please provide a job description."));
        }
        
        // Use the chat service to generate a response
        ChatResponse response = chatService.chat(
            userId,
            "Please analyze this job description and tell me what skills and qualifications would make a good match: " + 
            request.getCurrentJobDescription(),
            null,
            request.getCurrentJobDescription()
        );
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Match a new resume against a job description using a GET request.
     * This endpoint is specifically for the URL pattern /api/resumes/match-new?jd=...
     * 
     * @param jd The job description as a query parameter
     * @return A response with the match results
     */
    @GetMapping("/match-new")
    public ResponseEntity<ChatResponse> matchNewResumeGet(
            @org.springframework.web.bind.annotation.RequestParam(name = "jd", required = false) String jd) {
        
        // Generate a random user ID for this request
        String userId = UUID.randomUUID().toString();
        logger.info("Matching new resume with GET endpoint for user {}", userId);
        
        if (jd == null || jd.isEmpty()) {
            return ResponseEntity.badRequest().body(new ChatResponse("Please provide a job description using the 'jd' query parameter."));
        }
        
        // Use the chat service to generate a response
        ChatResponse response = chatService.chat(
            userId,
            "Please analyze this job description and tell me what skills and qualifications would make a good match: " + jd,
            null,
            jd
        );
        
        return ResponseEntity.ok(response);
    }
}
