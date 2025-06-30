package com.telus.spring.ai.resume.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.telus.spring.ai.resume.model.Resume;
import com.telus.spring.ai.resume.model.SyncResult;

import java.util.Optional;
import java.util.UUID;

/**
 * Service for storing and retrieving resumes.
 * Simplified to focus on database operations and vector store synchronization.
 */
public interface ResumeStorageService {
    
    /**
     * Get a resume by ID.
     * 
     * @param id The ID of the resume to get
     * @return The resume, if found
     */
    Optional<Resume> getResumeById(UUID id);
    
    /**
     * Get all resumes with pagination.
     * 
     * @param pageable The pagination information
     * @return A page of resumes
     */
    Page<Resume> getAllResumes(Pageable pageable);
    
    /**
     * Delete a resume by ID.
     * 
     * @param id The ID of the resume to delete
     */
    void deleteResume(UUID id);
    
    /**
     * Synchronize the vector store with the database.
     * This ensures that:
     * 1. Every resume in the database has exactly one entry in the vector store
     * 2. There are no orphaned entries in the vector store
     * 3. There are no duplicate entries in the vector store
     * 
     * @return The result of the synchronization
     */
    SyncResult synchronizeVectorStore();
}
