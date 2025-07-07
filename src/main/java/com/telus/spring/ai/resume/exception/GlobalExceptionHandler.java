package com.telus.spring.ai.resume.exception;

import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;

import com.telus.spring.ai.resume.model.ApiResponse;
import com.telus.spring.ai.resume.model.ErrorResponse;

/**
 * Global exception handler for the application.
 * This class centralizes exception handling across all controllers.
 */
@ControllerAdvice
public class GlobalExceptionHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    
    /**
     * Handle AiServiceUnavailableException.
     * 
     * @param ex The exception
     * @param request The web request
     * @return ResponseEntity with error details
     */
    @ExceptionHandler(AiServiceUnavailableException.class)
    public ResponseEntity<ApiResponse<Void>> handleAiServiceUnavailableException(
            AiServiceUnavailableException ex, WebRequest request) {
        
        String path = getPath(request);
        logger.error("AI Service Unavailable: {} (Path: {})", ex.getMessage(), path, ex);
        
        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.SERVICE_UNAVAILABLE.value(),
                "Service Unavailable",
                "The AI service is currently unavailable. Please try again later.",
                path
        );
        
        return new ResponseEntity<>(new ApiResponse<>(errorResponse), HttpStatus.SERVICE_UNAVAILABLE);
    }
    
    /**
     * Handle AiServiceTimeoutException.
     * 
     * @param ex The exception
     * @param request The web request
     * @return ResponseEntity with error details
     */
    @ExceptionHandler(AiServiceTimeoutException.class)
    public ResponseEntity<ApiResponse<Void>> handleAiServiceTimeoutException(
            AiServiceTimeoutException ex, WebRequest request) {
        
        String path = getPath(request);
        logger.error("AI Service Timeout: {} (Path: {})", ex.getMessage(), path, ex);
        
        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.GATEWAY_TIMEOUT.value(),
                "Gateway Timeout",
                "The AI service took too long to respond. Please try again later.",
                path
        );
        
        return new ResponseEntity<>(new ApiResponse<>(errorResponse), HttpStatus.GATEWAY_TIMEOUT);
    }
    
    /**
     * Handle ResourceNotFoundException.
     * 
     * @param ex The exception
     * @param request The web request
     * @return ResponseEntity with error details
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleResourceNotFoundException(
            ResourceNotFoundException ex, WebRequest request) {
        
        String path = getPath(request);
        logger.error("Resource Not Found: {} (Path: {})", ex.getMessage(), path, ex);
        
        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.NOT_FOUND.value(),
                "Not Found",
                ex.getMessage(),
                path
        );
        
        return new ResponseEntity<>(new ApiResponse<>(errorResponse), HttpStatus.NOT_FOUND);
    }
    
    /**
     * Handle SyncInProgressException.
     * 
     * @param ex The exception
     * @param request The web request
     * @return ResponseEntity with error details
     */
    @ExceptionHandler(SyncInProgressException.class)
    public ResponseEntity<ApiResponse<Void>> handleSyncInProgressException(
            SyncInProgressException ex, WebRequest request) {
        
        String path = getPath(request);
        logger.warn("Sync In Progress: {} (Path: {})", ex.getMessage(), path);
        
        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.CONFLICT.value(),
                "Conflict",
                ex.getMessage(),
                path
        );
        
        return new ResponseEntity<>(new ApiResponse<>(errorResponse), HttpStatus.CONFLICT);
    }
    
    /**
     * Handle CandidateAlreadyLockedException.
     * 
     * @param ex The exception
     * @param request The web request
     * @return ResponseEntity with error details
     */
    @ExceptionHandler(CandidateAlreadyLockedException.class)
    public ResponseEntity<ApiResponse<Void>> handleCandidateAlreadyLockedException(
            CandidateAlreadyLockedException ex, WebRequest request) {
        
        String path = getPath(request);
        logger.warn("Candidate Already Locked: {} (Path: {})", ex.getMessage(), path);
        
        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.CONFLICT.value(),
                "Candidate Locked",
                "This candidate is already locked by another manager. Please select a different candidate or try again later.",
                path
        );
        
        return new ResponseEntity<>(new ApiResponse<>(errorResponse), HttpStatus.CONFLICT);
    }
    
    /**
     * Handle UnauthorizedException.
     * 
     * @param ex The exception
     * @param request The web request
     * @return ResponseEntity with error details
     */
    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnauthorizedException(
            UnauthorizedException ex, WebRequest request) {
        
        String path = getPath(request);
        logger.warn("Unauthorized Action: {} (Path: {})", ex.getMessage(), path);
        
        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.FORBIDDEN.value(),
                "Unauthorized Action",
                ex.getMessage(), // Use the exception's message instead of hardcoded text
                path
        );
        
        return new ResponseEntity<>(new ApiResponse<>(errorResponse), HttpStatus.FORBIDDEN);
    }

    /**
     * Handle ResourceAccessException.
     * This is particularly important for handling network issues with null causes.
     * 
     * @param ex The exception
     * @param request The web request
     * @return ResponseEntity with error details
     */
    @ExceptionHandler(ResourceAccessException.class)
    public ResponseEntity<ApiResponse<Void>> handleResourceAccessException(
            ResourceAccessException ex, WebRequest request) {
        
        String path = getPath(request);
        logger.error("Resource Access Exception: {} (Path: {})", ex.getMessage(), path, ex);
        
        // Check if the cause is null, which is common with network issues
        String message = (ex.getCause() == null) 
                ? "Network error occurred while accessing external service. Please try again later."
                : ex.getMessage();
        
        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.SERVICE_UNAVAILABLE.value(),
                "Service Unavailable",
                message,
                path
        );
        
        return new ResponseEntity<>(new ApiResponse<>(errorResponse), HttpStatus.SERVICE_UNAVAILABLE);
    }
    
    /**
     * Handle TimeoutException.
     * 
     * @param ex The exception
     * @param request The web request
     * @return ResponseEntity with error details
     */
    @ExceptionHandler(TimeoutException.class)
    public ResponseEntity<ApiResponse<Void>> handleTimeoutException(
            TimeoutException ex, WebRequest request) {
        
        String path = getPath(request);
        logger.error("Timeout Exception: {} (Path: {})", ex.getMessage(), path, ex);
        
        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.GATEWAY_TIMEOUT.value(),
                "Gateway Timeout",
                "The operation timed out. Please try again later.",
                path
        );
        
        return new ResponseEntity<>(new ApiResponse<>(errorResponse), HttpStatus.GATEWAY_TIMEOUT);
    }
    
    /**
     * Handle all other exceptions.
     * 
     * @param ex The exception
     * @param request The web request
     * @return ResponseEntity with error details
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGlobalException(
            Exception ex, WebRequest request) {
        
        String path = getPath(request);
        logger.error("Unhandled Exception: {} (Path: {})", ex.getMessage(), path, ex);
        
        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Internal Server Error",
                "An unexpected error occurred. Please try again later.",
                path
        );
        
        return new ResponseEntity<>(new ApiResponse<>(errorResponse), HttpStatus.INTERNAL_SERVER_ERROR);
    }
    
    /**
     * Extract the request path from the WebRequest.
     * 
     * @param request The web request
     * @return The request path
     */
    private String getPath(WebRequest request) {
        if (request instanceof ServletWebRequest) {
            return ((ServletWebRequest) request).getRequest().getRequestURI();
        }
        return "";
    }
}
