package com.telus.spring.ai.resume.util;

import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.ResourceAccessException;

import com.telus.spring.ai.resume.exception.AiServiceTimeoutException;
import com.telus.spring.ai.resume.exception.AiServiceUnavailableException;

/**
 * Utility class for exception handling.
 * This provides helper methods for common exception handling scenarios.
 */
public class ExceptionUtils {
    
    private static final Logger logger = LoggerFactory.getLogger(ExceptionUtils.class);
    
    private ExceptionUtils() {
        // Private constructor to prevent instantiation
    }
    
    /**
     * Handle exceptions from AI service calls.
     * This method converts various exceptions to appropriate application exceptions.
     * 
     * @param ex The exception to handle
     * @param context Additional context information for logging
     * @return An appropriate application exception
     */
    public static RuntimeException handleAiServiceException(Exception ex, String context) {
        logger.error("AI service error in {}: {}", context, ex.getMessage(), ex);
        
        if (ex instanceof TimeoutException) {
            return new AiServiceTimeoutException(
                    "AI service operation timed out: " + context, ex);
        }
        
        if (ex instanceof ResourceAccessException) {
            ResourceAccessException rae = (ResourceAccessException) ex;
            String message = (rae.getCause() == null) 
                    ? "Network error while accessing AI service: " + context
                    : "Error accessing AI service: " + rae.getMessage();
            
            return new AiServiceUnavailableException(message, rae);
        }
        
        // For any other exception, wrap it in AiServiceUnavailableException
        return new AiServiceUnavailableException(
                "Error in AI service operation: " + context, ex);
    }
    
    /**
     * Log an exception with context information.
     * 
     * @param logger The logger to use
     * @param ex The exception to log
     * @param context Additional context information
     */
    public static void logException(Logger logger, Exception ex, String context) {
        if (ex instanceof ResourceAccessException && ((ResourceAccessException) ex).getCause() == null) {
            logger.error("Network error in {}: {}", context, ex.getMessage());
        } else {
            logger.error("Error in {}: {}", context, ex.getMessage(), ex);
        }
    }
}
