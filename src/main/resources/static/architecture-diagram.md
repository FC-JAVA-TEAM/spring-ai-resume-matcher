# Resume Matcher Application Architecture

## Overview

The Resume Matcher application is a Spring Boot-based system that uses AI to match resumes with job descriptions and manage the candidate evaluation process. The application leverages Spring AI for natural language processing and vector embeddings to provide intelligent resume matching and analysis.

## System Components

### Core Components

1. **Resume Management**
   - Upload and storage of candidate resumes
   - Parsing and extraction of resume data
   - Vector embedding of resume content for semantic search

2. **Resume Matching**
   - Matching resumes against job descriptions
   - AI-powered analysis of candidate skills and experience
   - Scoring and ranking of candidates

3. **Candidate Evaluation**
   - Evaluation of candidates based on various criteria
   - Status tracking throughout the hiring process
   - Audit trail of status changes

4. **User Interface**
   - Web-based UI using Vaadin framework
   - REST API for programmatic access

## Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────────┐
│                           Client Layer                               │
├───────────────┬───────────────────────────────┬────────────────────┤
│  Vaadin UI    │        REST API               │    Chat Interface   │
│  Components   │        Controllers            │                     │
└───────┬───────┴──────────────┬────────────────┴──────────┬─────────┘
        │                      │                           │
        ▼                      ▼                           ▼
┌─────────────────────────────────────────────────────────────────────┐
│                           Service Layer                              │
├───────────────┬───────────────┬───────────────┬────────────────────┤
│ Resume        │ Resume        │ Candidate     │ Resume-Aware        │
│ Storage       │ Matching      │ Evaluation    │ Chat Service        │
│ Service       │ Service       │ Service       │                     │
└───────┬───────┴───────┬───────┴───────┬───────┴──────────┬─────────┘
        │               │               │                  │
        ▼               ▼               ▼                  ▼
┌─────────────────────────────────────────────────────────────────────┐
│                           Data Layer                                 │
├───────────────┬───────────────┬───────────────┬────────────────────┤
│ Resume        │ Vector        │ Candidate     │ Status History      │
│ Repository    │ Store         │ Evaluation    │ Repository          │
│               │               │ Repository    │                     │
└───────────────┴───────────────┴───────────────┴────────────────────┘
```

## Key Flows

### Resume Upload and Processing Flow

1. User uploads resume through UI or API
2. ResumeStorageService stores the file and metadata
3. ResumeParserService extracts structured data
4. Vector embeddings are created and stored in the vector database
5. Resume is available for matching and evaluation

### Resume Matching Flow

1. User submits job description or search query
2. ResumeMatchingService processes the query
3. Vector similarity search finds relevant resumes
4. AI model analyzes and scores matches
5. Ranked results are returned to the user

### Candidate Evaluation Flow

1. Hiring manager reviews matched resumes
2. Manager can lock a candidate for evaluation
3. Evaluation data is recorded (scores, comments, etc.)
4. Status updates are tracked with audit history
5. Notifications can be sent based on status changes

## Database Schema

The application uses a relational database with the following key tables:

1. **resumes** - Stores resume metadata and content
2. **resume_vector_store** - Stores vector embeddings for semantic search
3. **candidate_evaluations** - Stores evaluation data and scores
4. **candidate_status_history** - Tracks status changes for audit purposes

## Integration Points

1. **AI Services**
   - Integration with Spring AI for NLP and embeddings
   - Custom prompt templates for resume analysis

2. **Storage**
   - File storage for resume documents
   - Vector database for semantic search (pgvector)

3. **Authentication**
   - User authentication and authorization
   - Role-based access control

## Technology Stack

- **Backend**: Spring Boot, Spring AI
- **Frontend**: Vaadin, HTML/CSS/JavaScript
- **Database**: PostgreSQL with pgvector extension
- **AI**: OpenAI integration, vector embeddings
- **Build/Deploy**: Maven, Docker

## Future Enhancements

1. Enhanced matching algorithms with fine-tuned models
2. Integration with applicant tracking systems
3. Automated interview scheduling
4. Candidate communication workflows
5. Analytics dashboard for hiring metrics
