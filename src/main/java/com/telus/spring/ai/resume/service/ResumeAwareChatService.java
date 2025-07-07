package com.telus.spring.ai.resume.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Service;

import com.telus.spring.ai.resume.exception.ResourceNotFoundException;
import com.telus.spring.ai.resume.model.Resume;
import com.telus.spring.ai.resume.model.ResumeMatch;
import com.telus.spring.ai.resume.model.chat.ChatMessage;
import com.telus.spring.ai.resume.model.chat.ChatResponse;
import com.telus.spring.ai.resume.repository.ResumeRepository;

/**
 * Service for handling chat interactions with resume context awareness.
 * Integrates with the resume matching functionality.
 */
@Service
public class ResumeAwareChatService {
    
    private static final Logger logger = LoggerFactory.getLogger(ResumeAwareChatService.class);
    
    private final ChatClient chatClient;
    private final ResumeMatchingService resumeMatchingService;
    private final ResumeRepository resumeRepository;
    private final ChatMemory chatMemory;
    
    public ResumeAwareChatService(
            ChatClient chatClient,
            ResumeMatchingService resumeMatchingService,
            ResumeRepository resumeRepository,
            ChatMemory chatMemory) {
        this.chatClient = chatClient;
        this.resumeMatchingService = resumeMatchingService;
        this.resumeRepository = resumeRepository;
        this.chatMemory = chatMemory;
    }
    
    /**
     * Process a chat message with resume context.
     * 
     * @param userId The ID of the user
     * @param message The message from the user
     * @param currentResumeId The ID of the current resume being discussed (optional)
     * @param currentJobDescription The current job description being discussed (optional)
     * @return A response to the message
     */
    public ChatResponse chat(String userId, String message, UUID currentResumeId, String currentJobDescription) {
        logger.info("Processing chat message from user {}: {}", userId, message);
        
        // Check for special commands
        if (message.startsWith("/explain")) {
            return handleExplainCommand(userId, message, currentResumeId, currentJobDescription);
        } else if (message.startsWith("/refine")) {
            return handleRefineCommand(userId, message, currentJobDescription);
        } else if (message.startsWith("/compare")) {
            return handleCompareCommand(userId, message, currentJobDescription);
        }
        
        // Create system prompt with context
        String systemPrompt = createSystemPrompt(currentResumeId, currentJobDescription);
        
        // Regular chat - use ChatClient with memory
        String response = chatClient.prompt()
            .system(systemPrompt)
            .user(message)
            .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, userId))
            .call()
            .content();
            
        logger.info("Generated response for user {}: {}", userId, response);
        return new ChatResponse(response);
    }
    
    /**
     * Get the chat history for a user.
     * 
     * @param userId The ID of the user
     * @return A list of chat messages
     */
    public List<ChatMessage> getChatHistory(String userId) {
        List<ChatMessage> history = new ArrayList<>();
        
        // Get messages from chat memory
        List<Message> messages = chatMemory.get(userId);
        
        // Convert to ChatMessage objects
        for (Message message : messages) {
            String role = message.getMessageType().toString().toLowerCase();
            String content = message.toString();
            history.add(new ChatMessage(role, content));
        }
        
        return history;
    }
    
    /**
     * Create a system prompt with context about the current resume and job description.
     * 
     * @param resumeId The ID of the current resume
     * @param jobDescription The current job description
     * @return A system prompt with context
     */
    private String createSystemPrompt(UUID resumeId, String jobDescription) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("You are an AI assistant specializing in resume analysis and job matching. ");
        
        if (resumeId != null) {
            // Add resume context
            Resume resume = resumeRepository.findById(resumeId).orElse(null);
            if (resume != null) {
                prompt.append("You are currently discussing a resume for " + resume.getName() + ". ");
                prompt.append("Key information from this resume: ");
                prompt.append("Email: " + resume.getEmail() + ", ");
                prompt.append("Phone: " + resume.getPhoneNumber() + ". ");
                
                // Extract a brief summary from the full text
                String fullText = resume.getFullText();
                if (fullText != null && !fullText.isEmpty()) {
               //     int summaryLength = Math.min(200, fullText.length());
                    prompt.append("Resume excerpt: \"" + fullText + "...\". ");
                }
            }
        }
        
        if (jobDescription != null && !jobDescription.isEmpty()) {
            prompt.append("The current job description being matched is: \"" + 
                          jobDescription.substring(0, Math.min(200, jobDescription.length())) + 
                          "...\". ");
        }
        
        prompt.append("Provide helpful, accurate information about resume matching. ");
        prompt.append("When asked about match quality, explain the factors that contribute to a good match. ");
        prompt.append("You can use special commands like /explain, /refine, and /compare for specific functionality. ");
        
        return prompt.toString();
    }
    
    /**
     * Handle the /explain command.
     * 
     * @param userId The ID of the user
     * @param message The message containing the command
     * @param currentResumeId The ID of the current resume
     * @param jobDescription The current job description
     * @return A response with the explanation
     */
    private ChatResponse handleExplainCommand(String userId, String message, UUID currentResumeId, String jobDescription) {
        logger.info("Handling /explain command from user {}: {}", userId, message);
        
        // Extract resume ID if provided in command
        UUID targetResumeId = extractResumeIdFromCommand(message).orElse(currentResumeId);
        
        if (targetResumeId == null) {
            return new ChatResponse("Please specify a resume ID or select a resume first.");
        }
        
        if (jobDescription == null || jobDescription.isEmpty()) {
            return new ChatResponse("Please provide a job description to match against.");
        }
        
        try {
            // Get detailed match explanation
            Resume resume = resumeRepository.findById(targetResumeId)
                .orElseThrow(() -> new ResourceNotFoundException("Resume", "id", targetResumeId));
                
            String explanation = resumeMatchingService.explainMatch(resume, jobDescription);
            
            // Add this interaction to chat memory
            chatClient.prompt()
                .user("/explain " + targetResumeId)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, userId))
                .call();
                
            return new ChatResponse(explanation);
        } catch (Exception e) {
            logger.error("Error handling /explain command", e);
            return new ChatResponse("Error generating explanation: " + e.getMessage());
        }
    }
    
    /**
     * Handle the /refine command.
     * 
     * @param userId The ID of the user
     * @param message The message containing the command
     * @param currentJobDescription The current job description
     * @return A response with the refined job description
     */
    private ChatResponse handleRefineCommand(String userId, String message, String currentJobDescription) {
        logger.info("Handling /refine command from user {}: {}", userId, message);
        
        if (currentJobDescription == null || currentJobDescription.isEmpty()) {
            return new ChatResponse("Please provide a job description to refine.");
        }
        
        // Extract refinement criteria
        String criteria = message.replaceFirst("/refine\\s*", "").trim();
        
        if (criteria.isEmpty()) {
            return new ChatResponse("Please specify refinement criteria. For example: /refine add more emphasis on Java skills");
        }
        
        // Use AI to refine the job description
        String systemPrompt = "You are an expert at writing job descriptions. " +
                             "You will be given a job description and refinement criteria. " +
                             "Your task is to refine the job description according to the criteria, " +
                             "while maintaining its professional tone and structure.";
        
        String userPrompt = "Job Description:\n" + currentJobDescription + "\n\n" +
                           "Refinement Criteria:\n" + criteria + "\n\n" +
                           "Please provide the refined job description.";
        
        String refinedJobDescription = chatClient.prompt()
            .system(systemPrompt)
            .user(userPrompt)
            .call()
            .content();
        
        // Add this interaction to chat memory
        chatClient.prompt()
            .user("/refine " + criteria)
            .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, userId))
            .call();
        
        ChatResponse response = new ChatResponse("I've refined the job description based on your criteria. Here's the updated version:\n\n" + refinedJobDescription);
        response.addData("refinedJobDescription", refinedJobDescription);
        
        return response;
    }
    
    /**
     * Handle the /compare command.
     * 
     * @param userId The ID of the user
     * @param message The message containing the command
     * @param jobDescription The current job description
     * @return A response with the comparison
     */
    private ChatResponse handleCompareCommand(String userId, String message, String jobDescription) {
        logger.info("Handling /compare command from user {}: {}", userId, message);
        
        // Extract resume IDs
        List<UUID> resumeIds = extractMultipleResumeIdsFromCommand(message);
        
        if (resumeIds.size() < 2) {
            return new ChatResponse("Please specify at least two resume IDs to compare. For example: /compare 123e4567-e89b-12d3-a456-426614174000 123e4567-e89b-12d3-a456-426614174001");
        }
        
        if (jobDescription == null || jobDescription.isEmpty()) {
            return new ChatResponse("Please provide a job description to compare against.");
        }
        
        try {
            StringBuilder comparison = new StringBuilder();
            comparison.append("# Resume Comparison\n\n");
            
            // Get resumes
            List<Resume> resumes = new ArrayList<>();
            for (UUID resumeId : resumeIds) {
                Resume resume = resumeRepository.findById(resumeId)
                    .orElseThrow(() -> new ResourceNotFoundException("Resume", "id", resumeId));
                resumes.add(resume);
            }
            
            // Get match explanations
            List<String> explanations = new ArrayList<>();
            for (Resume resume : resumes) {
                String explanation = resumeMatchingService.explainMatch(resume, jobDescription);
                explanations.add(explanation);
            }
            
            // Use AI to compare the resumes
            String systemPrompt = "You are an expert at comparing job candidates. " +
                                 "You will be given multiple resumes and their match explanations against a job description. " +
                                 "Your task is to compare them and highlight the strengths and weaknesses of each candidate.";
            
            StringBuilder userPrompt = new StringBuilder();
            userPrompt.append("Job Description:\n").append(jobDescription).append("\n\n");
            
            for (int i = 0; i < resumes.size(); i++) {
                Resume resume = resumes.get(i);
                String explanation = explanations.get(i);
                
                userPrompt.append("Resume ").append(i + 1).append(" (").append(resume.getName()).append("):\n");
                userPrompt.append(resume.getFullText()).append("\n\n");
                userPrompt.append("Match Explanation ").append(i + 1).append(":\n");
                userPrompt.append(explanation).append("\n\n");
            }
            
            userPrompt.append("Please provide a detailed comparison of these candidates, highlighting their relative strengths and weaknesses for this position.");
            
            String comparisonResult = chatClient.prompt()
                .system(systemPrompt)
                .user(userPrompt.toString())
                .call()
                .content();
            
            // Add this interaction to chat memory
            chatClient.prompt()
                .user("/compare " + String.join(" ", resumeIds.stream().map(UUID::toString).toList()))
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, userId))
                .call();
            
            return new ChatResponse(comparisonResult);
        } catch (Exception e) {
            logger.error("Error handling /compare command", e);
            return new ChatResponse("Error generating comparison: " + e.getMessage());
        }
    }
    
    /**
     * Extract a resume ID from a command.
     * 
     * @param command The command containing the resume ID
     * @return An optional containing the resume ID, if found
     */
    private Optional<UUID> extractResumeIdFromCommand(String command) {
        Pattern pattern = Pattern.compile("/\\w+\\s+([0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12})");
        Matcher matcher = pattern.matcher(command);
        
        if (matcher.find()) {
            try {
                return Optional.of(UUID.fromString(matcher.group(1)));
            } catch (IllegalArgumentException e) {
                return Optional.empty();
            }
        }
        
        return Optional.empty();
    }
    
    /**
     * Extract multiple resume IDs from a command.
     * 
     * @param command The command containing the resume IDs
     * @return A list of resume IDs
     */
    private List<UUID> extractMultipleResumeIdsFromCommand(String command) {
        List<UUID> resumeIds = new ArrayList<>();
        Pattern pattern = Pattern.compile("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
        Matcher matcher = pattern.matcher(command);
        
        while (matcher.find()) {
            try {
                resumeIds.add(UUID.fromString(matcher.group()));
            } catch (IllegalArgumentException e) {
                // Ignore invalid UUIDs
            }
        }
        
        return resumeIds;
    }
}
