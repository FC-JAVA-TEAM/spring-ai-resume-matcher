package com.telus.spring.ai.resume.model;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Standardized API response model for all REST API responses.
 * This provides a consistent structure for both success and error responses.
 * 
 * @param <T> The type of data contained in the response
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
    
    private boolean success;
    private T data;
    private ErrorResponse error;
    
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timestamp;
    
    /**
     * Default constructor.
     */
    public ApiResponse() {
        this.timestamp = LocalDateTime.now();
    }
    
    /**
     * Constructor for successful responses.
     * 
     * @param data The data to include in the response
     */
    public ApiResponse(T data) {
        this();
        this.success = true;
        this.data = data;
    }
    
    /**
     * Constructor for error responses.
     * 
     * @param error The error details
     */
    public ApiResponse(ErrorResponse error) {
        this();
        this.success = false;
        this.error = error;
    }
    
    /**
     * Static factory method for creating success responses.
     * 
     * @param <T> The type of data
     * @param data The data to include in the response
     * @return A new ApiResponse instance with the provided data
     */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(data);
    }
    
    /**
     * Static factory method for creating error responses.
     * 
     * @param <T> The type of data (not used in error responses)
     * @param error The error details
     * @return A new ApiResponse instance with the provided error
     */
    public static <T> ApiResponse<T> error(ErrorResponse error) {
        return new ApiResponse<>(error);
    }
    
    // Getters and setters
    
    public boolean isSuccess() {
        return success;
    }
    
    public void setSuccess(boolean success) {
        this.success = success;
    }
    
    public T getData() {
        return data;
    }
    
    public void setData(T data) {
        this.data = data;
    }
    
    public ErrorResponse getError() {
        return error;
    }
    
    public void setError(ErrorResponse error) {
        this.error = error;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
