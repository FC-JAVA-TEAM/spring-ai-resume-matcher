package com.telus.spring.ai.resume.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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
        } else if (message.startsWith("/info")) {
            return handleInfoCommand(userId, message, currentResumeId);
        } else if (message.startsWith("/list")) {
            return handleListCommand(userId, message);
        } else if (message.startsWith("/extract")) {
            return handleExtractCommand(userId, message, currentResumeId);
        } else if (message.startsWith("/help")) {
            return handleHelpCommand(userId);
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
    
    /**
     * Handle the /info command.
     * 
     * @param userId The ID of the user
     * @param message The message containing the command
     * @param currentResumeId The ID of the current resume
     * @return A response with the resume information
     */
    private ChatResponse handleInfoCommand(String userId, String message, UUID currentResumeId) {
        logger.info("Handling /info command from user {}: {}", userId, message);
        
        // Extract resume ID if provided in command
        UUID targetResumeId = extractResumeIdFromCommand(message).orElse(currentResumeId);
        
        if (targetResumeId == null) {
            return new ChatResponse("Please specify a resume ID or select a resume first.");
        }
        
        try {
            // Get resume details
            Resume resume = resumeRepository.findById(targetResumeId)
                .orElseThrow(() -> new ResourceNotFoundException("Resume", "id", targetResumeId));
            
            // Use AI to generate a detailed summary of the resume
            String systemPrompt = "You are an expert at analyzing resumes. " +
                                 "You will be given a resume and your task is to provide a detailed summary " +
                                 "of the candidate's profile, including their skills, experience, education, " +
                                 "and other relevant information. Focus on extracting key facts and presenting " +
                                 "them in a structured format.";
            
            String userPrompt = "Resume for " + resume.getName() + ":\n\n" + resume.getFullText() + 
                               "\n\nPlease provide a detailed summary of this candidate's profile, including:" +
                               "\n1. Professional Summary" +
                               "\n2. Skills and Technologies" +
                               "\n3. Work Experience (including current/last organization)" +
                               "\n4. Education" +
                               "\n5. Certifications (if any)" +
                               "\n6. Key Achievements";
            
            String resumeInfo = chatClient.prompt()
                .system(systemPrompt)
                .user(userPrompt)
                .call()
                .content();
            
            // Add this interaction to chat memory
            chatClient.prompt()
                .user("/info " + targetResumeId)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, userId))
                .call();
            
            return new ChatResponse(resumeInfo);
        } catch (Exception e) {
            logger.error("Error handling /info command", e);
            return new ChatResponse("Error generating resume information: " + e.getMessage());
        }
    }
    
    /**
     * Handle the /list command.
     * 
     * @param userId The ID of the user
     * @param message The message containing the command
     * @return A response with the list of resumes
     */
    private ChatResponse handleListCommand(String userId, String message) {
        logger.info("Handling /list command from user {}: {}", userId, message);
        
        // Extract filter criteria if provided
        String criteria = message.replaceFirst("/list\\s*", "").trim();
        
        try {
            // Get all resumes
            List<Resume> resumes = resumeRepository.findAll();
            
            if (resumes.isEmpty()) {
                return new ChatResponse("No resumes found in the system.");
            }
            
            // Use AI to filter and summarize resumes if criteria provided
            if (!criteria.isEmpty()) {
                String systemPrompt = "You are an expert at analyzing resumes. " +
                                     "You will be given a list of resumes and filter criteria. " +
                                     "Your task is to filter the resumes based on the criteria and " +
                                     "provide a brief summary of each matching resume.";
                
                StringBuilder userPrompt = new StringBuilder();
                userPrompt.append("Filter Criteria: ").append(criteria).append("\n\n");
                userPrompt.append("Resumes:\n\n");
                
                for (int i = 0; i < resumes.size(); i++) {
                    Resume resume = resumes.get(i);
                    userPrompt.append("Resume ").append(i + 1).append(" (").append(resume.getName()).append("):\n");
                    userPrompt.append(resume.getFullText()).append("\n\n");
                }
                
                userPrompt.append("Please filter these resumes based on the criteria and provide a brief summary of each matching resume. " +
                                 "Include the candidate's name, current/last role, years of experience, and key skills.");
                
                String resumeList = chatClient.prompt()
                    .system(systemPrompt)
                    .user(userPrompt.toString())
                    .call()
                    .content();
                
                // Add this interaction to chat memory
                chatClient.prompt()
                    .user("/list " + criteria)
                    .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, userId))
                    .call();
                
                return new ChatResponse(resumeList);
            } else {
                // If no criteria provided, just list all resumes with basic info
                StringBuilder resumeList = new StringBuilder();
                resumeList.append("# Available Resumes\n\n");
                
                for (Resume resume : resumes) {
                    resumeList.append("## ").append(resume.getName()).append("\n");
                    resumeList.append("- Email: ").append(resume.getEmail()).append("\n");
                    resumeList.append("- Phone: ").append(resume.getPhoneNumber()).append("\n");
                    resumeList.append("- File: ").append(resume.getOriginalFileName()).append("\n");
                    resumeList.append("- ID: ").append(resume.getId()).append("\n\n");
                }
                
                // Add this interaction to chat memory
                chatClient.prompt()
                    .user("/list")
                    .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, userId))
                    .call();
                
                return new ChatResponse(resumeList.toString());
            }
        } catch (Exception e) {
            logger.error("Error handling /list command", e);
            return new ChatResponse("Error generating resume list: " + e.getMessage());
        }
    }
    
    /**
     * Handle the /extract command.
     * 
     * @param userId The ID of the user
     * @param message The message containing the command
     * @param currentResumeId The ID of the current resume
     * @return A response with the extracted information
     */
    private ChatResponse handleExtractCommand(String userId, String message, UUID currentResumeId) {
        logger.info("Handling /extract command from user {}: {}", userId, message);
        
        // Parse the command: /extract [field] [resumeId]
        String[] parts = message.split("\\s+", 3);
        
        if (parts.length < 2) {
            return new ChatResponse("Please specify what to extract. For example: `/extract last_company` or `/extract skills`");
        }
        
        String field = parts[1].toLowerCase();
        
        // Extract resume ID if provided in command
        UUID targetResumeId = (parts.length > 2) ? 
            extractResumeIdFromCommand(parts[2]).orElse(currentResumeId) : 
            currentResumeId;
        
        if (targetResumeId == null) {
            return new ChatResponse("Please specify a resume ID or select a resume first.");
        }
        
        try {
            // Get resume details
            Resume resume = resumeRepository.findById(targetResumeId)
                .orElseThrow(() -> new ResourceNotFoundException("Resume", "id", targetResumeId));
            
            // Define extraction prompts for different fields
            Map<String, String> extractionPrompts = new HashMap<>();
            extractionPrompts.put("last_company", "What is the candidate's most recent or current employer? Provide only the company name without any additional text.");
            extractionPrompts.put("experience", "How many years of professional experience does the candidate have? Provide only the number of years without any additional text.");
            extractionPrompts.put("education", "What is the candidate's educational background? List all degrees, institutions, and graduation years in a concise format.");
            extractionPrompts.put("skills", "What are the candidate's technical and professional skills? Provide a comma-separated list of skills without any additional text.");
            extractionPrompts.put("contact", "What are the candidate's contact details? Provide name, email, phone number, and any other contact information in a structured format.");
            
            if (!extractionPrompts.containsKey(field)) {
                return new ChatResponse("Unknown field to extract. Available fields: last_company, experience, education, skills, contact");
            }
            
            // Use AI to extract the specific information
            String systemPrompt = "You are an expert at analyzing resumes. " +
                                 "You will be given a resume and asked to extract specific information. " +
                                 "Your task is to provide a precise, concise answer without unnecessary explanation.";
            
            String userPrompt = "Resume for " + resume.getName() + ":\n\n" + resume.getFullText() + 
                               "\n\n" + extractionPrompts.get(field);
            
            String extractedInfo = chatClient.prompt()
                .system(systemPrompt)
                .user(userPrompt)
                .call()
                .content();
            
            // Add this interaction to chat memory
            chatClient.prompt()
                .user("/extract " + field + " " + targetResumeId)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, userId))
                .call();
            
            return new ChatResponse(extractedInfo);
        } catch (Exception e) {
            logger.error("Error handling /extract command", e);
            return new ChatResponse("Error extracting information: " + e.getMessage());
        }
    }
    
    /**
     * Handle the /help command.
     * 
     * @param userId The ID of the user
     * @return A response with help information
     */
    private ChatResponse handleHelpCommand(String userId) {
        logger.info("Handling /help command from user {}", userId);
        
        StringBuilder helpText = new StringBuilder();
        helpText.append("# Available Commands\n\n");
        
        helpText.append("## `/explain`\n");
        helpText.append("Get a detailed explanation of how well a resume matches a job description.\n");
        helpText.append("Usage: `/explain [resumeId]` (if no resumeId is provided, uses the currently selected resume)\n\n");
        
        helpText.append("## `/refine`\n");
        helpText.append("Refine a job description based on specific criteria.\n");
        helpText.append("Usage: `/refine [criteria]` (e.g., `/refine add more emphasis on Java skills`)\n\n");
        
        helpText.append("## `/compare`\n");
        helpText.append("Compare multiple resumes against a job description.\n");
        helpText.append("Usage: `/compare [resumeId1] [resumeId2] ...` (requires at least two resume IDs)\n\n");
        
        helpText.append("## `/info`\n");
        helpText.append("Get detailed information about a specific resume.\n");
        helpText.append("Usage: `/info [resumeId]` (if no resumeId is provided, uses the currently selected resume)\n\n");
        
        helpText.append("## `/list`\n");
        helpText.append("Get a list of resumes, optionally filtered by criteria.\n");
        helpText.append("Usage: `/list [criteria]` (e.g., `/list java developers` or just `/list` for all resumes)\n\n");
        
        helpText.append("## `/extract`\n");
        helpText.append("Extract specific information from a resume.\n");
        helpText.append("Usage: `/extract [field] [resumeId]` (if no resumeId is provided, uses the currently selected resume)\n");
        helpText.append("Available fields: last_company, experience, education, skills, contact\n\n");
        
        helpText.append("## `/help`\n");
        helpText.append("Display this help information.\n");
        helpText.append("Usage: `/help`\n\n");
        
        // Add this interaction to chat memory
        chatClient.prompt()
            .user("/help")
            .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, userId))
            .call();
        
        return new ChatResponse(helpText.toString());
    }
}
