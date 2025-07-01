package com.telus.spring.ai.resume.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.telus.spring.ai.resume.model.Resume;
import com.telus.spring.ai.resume.model.ResumeMatch;
import com.telus.spring.ai.resume.repository.ResumeRepository;
import com.telus.spring.ai.resume.service.ResumeMatchingService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

/**
 * Implementation of ResumeMatchingService that uses vector similarity search
 * to find resumes that match a job description.
 * Optimized for maximum parallelism.
 */
@Service
public class ResumeMatchingServiceImpl implements ResumeMatchingService {
    
    private static final Logger logger = LoggerFactory.getLogger(ResumeMatchingServiceImpl.class);
    
    private  VectorStore vectorStore;
    private final ChatModel chatModel;
    private final ResumeRepository resumeRepository;
    private final String resumeMatchPrompt;
    private final RetryTemplate aiRetryTemplate;
    
    @Value("${resume.matching.ai-timeout-seconds:30}")
    private int aiOperationTimeoutSeconds;
    
    @Autowired
    @Qualifier("resumeProcessingExecutor")
    private Executor resumeProcessingExecutor;
    
    @Autowired
    @Qualifier("aiOperationsExecutor")
    private Executor aiOperationsExecutor;
    
    public ResumeMatchingServiceImpl(
            @Qualifier("resumeVectorStore") VectorStore vectorStore,
    	//	VectorStore vectorStore,
            ChatModel chatModel,
            ResumeRepository resumeRepository,
            @Qualifier("resumeMatchPrompt") String resumeMatchPrompt,
            RetryTemplate aiRetryTemplate) {
        this.vectorStore = vectorStore;
        this.chatModel = chatModel;
        this.resumeRepository = resumeRepository;
        this.resumeMatchPrompt = resumeMatchPrompt;
        this.aiRetryTemplate = aiRetryTemplate;
    }
    
    @Override
    public List<ResumeMatch> findMatchingResumes(String jobDescription, int limit) {
        logger.info("Finding resumes matching job description: {}", jobDescription);
        
        // Search for similar documents in the vector store
        List<Document> documents = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(jobDescription)
                        .topK(limit)
                        // Filter is applied at the database level in the custom ResumeVectorStore implementation
                        .build()
        );
        
        logger.info("Found {} matching documents", documents.size());
        
        // Process ALL documents in parallel instead of in batches
        List<CompletableFuture<ResumeMatch>> futures = documents.stream()
                .map(document -> processDocumentAsync(document, jobDescription))
                .collect(Collectors.toList());
        
        // Wait for ALL futures to complete
        List<ResumeMatch> matches = futures.stream()
                .map(CompletableFuture::join)  // This will wait for completion
                .filter(Objects::nonNull)      // Filter out any nulls (failed processing)
                .collect(Collectors.toList());
        
        logger.info("Processed total of {} matches", matches.size());
        
        return matches;
    }
    
    /**
     * Process a document asynchronously to create a ResumeMatch.
     * This implementation uses true parallel AI calls for better performance.
     * 
     * @param document The document to process
     * @param jobDescription The job description to match against
     * @return A CompletableFuture that will contain the ResumeMatch when complete
     */
    @Async("resumeProcessingExecutor")
    public CompletableFuture<ResumeMatch> processDocumentAsync(Document document, String jobDescription) {
        try {
            // Extract metadata
            Map<String, Object> metadata = document.getMetadata();
            
            // Get resume ID from metadata
            String resumeIdStr = String.valueOf(metadata.get("resumeId"));
            UUID resumeId = UUID.fromString(resumeIdStr);
            
            // Create Resume object directly from metadata if possible
            Resume resume;
            
            // Check if we have all the necessary metadata to create a Resume object
            if (metadata.containsKey("name") && metadata.containsKey("email") && 
                metadata.containsKey("phoneNumber")) {
                
                // Create Resume object directly from metadata
                resume = new Resume();
                resume.setId(resumeId);
                resume.setName(metadata.get("name").toString());
                resume.setEmail(metadata.get("email").toString());
                resume.setPhoneNumber(metadata.get("phoneNumber").toString());
                // Get content from document
                String content = document.getMetadata().containsKey("content") 
                    ? (String) document.getMetadata().get("content") 
                    : document.toString();
                resume.setFullText(content);
                
                if (metadata.containsKey("fileType")) {
                    resume.setFileType(metadata.get("fileType").toString());
                }
                
                if (metadata.containsKey("originalFileName")) {
                    resume.setOriginalFileName(metadata.get("originalFileName").toString());
                }
            } else {
                // Get resume from database if metadata is incomplete
                resume = resumeRepository.findById(resumeId)
                        .orElseThrow(() -> new IllegalStateException("Resume not found with ID: " + resumeId));
            }
            
            // Generate explanation asynchronously
            return explainMatchAsync(resume, jobDescription)
                .thenApply(explanation -> {
                    int score = extractScoreFromExplanation(explanation);
                    ResumeMatch match = new ResumeMatch(resume, score, explanation);
                    logger.info("Processed match for resume: {}, score: {}", resumeId, score);
                    return match;
                })
                .exceptionally(ex -> {
                    logger.error("Error generating explanation for resume: {}", resumeId, ex);
                    // Return a match with default values in case of error
                    return new ResumeMatch(resume, 0, "Unable to generate explanation due to an error: " + ex.getMessage());
                });
        } catch (Exception e) {
            logger.error("Error processing document: {}", document.getId(), e);
            return CompletableFuture.completedFuture(null);
        }
    }
    
    @Override
    public String explainMatch(Resume resume, String jobDescription) {
        logger.info("Generating explanation for resume: {}", resume.getId());
        
        try {
            // Create a prompt template from the injected template string
            PromptTemplate template = new PromptTemplate(resumeMatchPrompt);
            
            // Create the prompt with variables
            Prompt prompt = template.create(Map.of(
                    "jobDescription", jobDescription,
                    "resumeText", resume.getFullText()
            ));
            
            // Get the response from the AI with retry for 503 errors
            String explanation = aiRetryTemplate.execute(context -> {
                // Log retry attempts
                if (context.getRetryCount() > 0) {
                    logger.info("Retry attempt {} for resume {}", 
                               context.getRetryCount(), resume.getId());
                }
                
                // Make the AI call with Spring AI 1.0.0-M6
                var response = chatModel.call(prompt);
                if (response != null && response.getResult() != null && 
                    response.getResult().getOutput() != null) {
                    return response.getResult().getOutput().getText();
                }
                return "Unable to generate explanation. No valid response from AI service.";
            }, context -> {
                // This is the recovery callback - called when all retries fail
                logger.error("All retries failed for resume {}: {}", 
                           resume.getId(), context.getLastThrowable().getMessage());
                
                // Generate fallback explanation
                return "Unable to generate explanation after multiple attempts. The AI service is currently unavailable. Please try again later.";
            });
            
            logger.info("Generated explanation for resume: {}", resume.getId());
            
            return explanation;
        } catch (Exception e) {
            logger.error("Error generating explanation for resume: {}", resume.getId(), e);
            return "Unable to generate explanation due to an error.";
        }
    }
    
    /**
     * Generate an explanation asynchronously for a resume match.
     * This method makes the AI call directly in the async thread for true parallelism.
     * Includes timeout handling to prevent hanging on slow AI responses.
     * 
     * @param resume The resume to explain
     * @param jobDescription The job description to match against
     * @return A CompletableFuture that will contain the explanation when complete
     */
    @Override
    @Async("aiOperationsExecutor")
    public CompletableFuture<String> explainMatchAsync(Resume resume, String jobDescription) {
        logger.info("Generating async explanation for resume: {}", resume.getId());
        
        try {
            // Create a prompt template from the injected template string
            PromptTemplate template = new PromptTemplate(resumeMatchPrompt);
            
            // Create the prompt with variables
            Prompt prompt = template.create(Map.of(
                    "jobDescription", jobDescription,
                    "resumeText", resume.getFullText()
            ));
            
            // Create a CompletableFuture for the AI call
            CompletableFuture<String> aiCallFuture = new CompletableFuture<>();
            
            // Execute the AI call in a separate thread
            CompletableFuture.runAsync(() -> {
                try {
                    // Get the response from the AI with retry for 503 errors
                    String explanation = aiRetryTemplate.execute(context -> {
                        // Log retry attempts
                        if (context.getRetryCount() > 0) {
                            logger.info("Async retry attempt {} for resume {}", 
                                       context.getRetryCount(), resume.getId());
                        }
                        
                        // Make the AI call with Spring AI 1.0.0-M6
                var response = chatModel.call(prompt);
                if (response != null && response.getResult() != null && 
                    response.getResult().getOutput() != null) {
                    return response.getResult().getOutput().getText();
                }
                return "Unable to generate explanation. No valid response from AI service.";
                    }, context -> {
                        // This is the recovery callback - called when all retries fail
                        logger.error("All async retries failed for resume {}: {}", 
                                   resume.getId(), context.getLastThrowable().getMessage());
                        
                        // Generate fallback explanation
                        return "Unable to generate explanation after multiple attempts. The AI service is currently unavailable. Please try again later.";
                    });
                    
                    // Complete the future with the result
                    aiCallFuture.complete(explanation);
                    logger.info("Generated async explanation for resume: {}", resume.getId());
                } catch (Exception e) {
                    // Complete the future exceptionally if there's an error
                    aiCallFuture.completeExceptionally(e);
                    logger.error("Error in AI call for resume: {}", resume.getId(), e);
                }
            }, aiOperationsExecutor);
            
            // Add timeout handling - configurable timeout
            return aiCallFuture.orTimeout(aiOperationTimeoutSeconds, TimeUnit.SECONDS)
                .exceptionally(ex -> {
                    if (ex instanceof TimeoutException) {
                        logger.error("Timeout generating explanation for resume: {}", resume.getId());
                        return "Unable to generate explanation due to timeout. The AI service took too long to respond.";
                    } else {
                        logger.error("Error generating explanation for resume: {}", resume.getId(), ex);
                        return "Unable to generate explanation due to an error: " + ex.getMessage();
                    }
                });
        } catch (Exception e) {
            logger.error("Error setting up async explanation for resume: {}", resume.getId(), e);
            CompletableFuture<String> future = new CompletableFuture<>();
            future.completeExceptionally(e);
            return future;
        }
    }
    
    /**
     * Extract the match score from the AI's explanation.
     * 
     * @param explanation The explanation text from the AI
     * @return The match score as a value between 0 and 100
     */
    public int extractScoreFromExplanation(String explanation) {
        try {
            // Look for "MATCH SCORE: [X/100]" pattern
            String[] lines = explanation.split("\n");
            for (String line : lines) {
                line = line.trim();
                if (line.startsWith("MATCH SCORE:") || line.contains("MATCH SCORE:")) {
                    // Extract the number
                    String scoreText = line.replaceAll(".*?(\\d+)\\s*/\\s*100.*", "$1");
                    int score = Integer.parseInt(scoreText);
                    return score;
                }
            }
            
            // If we can't find the specific format, look for any number followed by /100
            for (String line : lines) {
                if (line.matches(".*\\b(\\d+)\\s*/\\s*100\\b.*")) {
                    String scoreText = line.replaceAll(".*?(\\d+)\\s*/\\s*100.*", "$1");
                    int score = Integer.parseInt(scoreText);
                    return score;
                }
            }
            
            // If we still can't find it, look for any number between 0 and 100
            for (String line : lines) {
                if (line.matches(".*\\b([0-9]{1,3})\\b.*")) {
                    String scoreText = line.replaceAll(".*?\\b([0-9]{1,3})\\b.*", "$1");
                    int score = Integer.parseInt(scoreText);
                    if (score >= 0 && score <= 100) {
                        return score;
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("Error extracting score from explanation: {}", e.getMessage());
        }
        
        // Default score if we can't extract it
        return 1;
    }
}
