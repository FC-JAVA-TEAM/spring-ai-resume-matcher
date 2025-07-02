package com.telus.spring.ai.resume.model;

/**
 * Represents a match between a resume and a job description.
 * Includes both the text explanation and structured analysis.
 */
public class ResumeMatch {
    
    private Resume resume;
    private Integer score;
    private String explanation;
    private ResumeAnalysis analysis;
    
    // Default constructor
    public ResumeMatch() {
    }
    
    // Constructor with fields
    public ResumeMatch(Resume resume, Integer score, String explanation) {
        this.resume = resume;
        this.score = score;
        this.explanation = explanation;
    }
    
    // Constructor with analysis
    public ResumeMatch(Resume resume, Integer score, String explanation, ResumeAnalysis analysis) {
        this.resume = resume;
        this.score = score;
        this.explanation = explanation;
        this.analysis = analysis;
    }
    
    // Getters and setters
    public Resume getResume() {
        return resume;
    }
    
    public void setResume(Resume resume) {
        this.resume = resume;
    }
    
    public Integer getScore() {
        return score;
    }

    public void setScore(Integer score) {
        this.score = score;
    }
    
    public String getExplanation() {
        return explanation;
    }
    
    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }
    
    public ResumeAnalysis getAnalysis() {
        return analysis;
    }
    
    public void setAnalysis(ResumeAnalysis analysis) {
        this.analysis = analysis;
    }
    
    @Override
    public String toString() {
        return "ResumeMatch{" +
                "resume=" + resume +
                ", score=" + score +
                ", explanation='" + explanation + '\'' +
                ", analysis=" + analysis +
                '}';
    }
}
