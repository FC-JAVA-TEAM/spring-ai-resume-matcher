# Resume Matcher Application Architecture

## Overview

The Resume Matcher application is a Spring Boot application that uses AI to match resumes with job descriptions. It provides a web interface for users to upload resumes, match them with job descriptions, and view the results.

## Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────────────┐
│                                                                         │
│                           Web Layer (UI)                                │
│                                                                         │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐    │
│  │   HomeView  │  │  UploadView │  │  MatchView  │  │ ResumesView │    │
│  └─────────────┘  └─────────────┘  └─────────────┘  └─────────────┘    │
│                                                                         │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐    │
│  │  SyncView   │  │ MatchNewView│  │ ResumeDetail│  │  ChatView   │    │
│  └─────────────┘  └─────────────┘  └─────────────┘  └─────────────┘    │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
                                   │
                                   ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                                                                         │
│                         Controller Layer                                │
│                                                                         │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐    │
│  │ResumeControl│  │AdminControl │  │ChatController│  │ResumeLockCtrl    │
│  └─────────────┘  └─────────────┘  └─────────────┘  └─────────────┘    │
│                                                                         │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐                     │
│  │CandidateCtrl│  │MatchNewCtrl │  │LockableMatch│                     │
│  └─────────────┘  └─────────────┘  └─────────────┘                     │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
                                   │
                                   ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                                                                         │
│                          Service Layer                                  │
│                                                                         │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐    │
│  │ResumeStorage│  │ResumeParser │  │ResumeMatching│  │CandidateEval│    │
│  └─────────────┘  └─────────────┘  └─────────────┘  └─────────────┘    │
│                                                                         │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐    │
│  │CandidateStat│  │LockableMatch│  │ResumeAwareChat│ │VectorSyncSch│    │
│  └─────────────┘  └─────────────┘  └─────────────┘  └─────────────┘    │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
                                   │
                                   ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                                                                         │
│                         Repository Layer                                │
│                                                                         │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐    │
│  │ResumeRepo   │  │CandidateEval│  │CandidateStat│  │LockableMatch│    │
│  └─────────────┘  └─────────────┘  └─────────────┘  └─────────────┘    │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
                                   │
                                   ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                                                                         │
│                          Database Layer                                 │
│                                                                         │
│  ┌─────────────────────────────────────────────────────────────────┐    │
│  │                      PostgreSQL + pgvector                       │    │
│  └─────────────────────────────────────────────────────────────────┘    │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
                                   │
                                   ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                                                                         │
│                           AI Services                                   │
│                                                                         │
│  ┌─────────────────────┐  ┌─────────────────────────────────────────┐  │
│  │   FuelixChatModel   │  │        FuelixEmbeddingModel             │  │
│  └─────────────────────┘  └─────────────────────────────────────────┘  │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

## Key Components

### Web Layer (UI)
- **HomeView**: Landing page
- **UploadView**: Resume upload interface
- **MatchView**: Resume matching interface
- **ResumesView**: View all resumes
- **SyncView**: Sync vector store
- **MatchNewView**: New matching interface
- **ResumeDetailView**: View resume details
- **ChatView**: Chat with AI about resumes

### Controller Layer
- **ResumeController**: Handles resume operations
- **AdminController**: Admin operations
- **ChatController**: Chat operations
- **ResumeLockController**: Resume locking operations
- **CandidateStatusController**: Candidate status operations
- **MatchNewController**: New matching operations
- **LockableResumeMatchController**: Lockable resume match operations

### Service Layer
- **ResumeStorageService**: Resume storage operations
- **ResumeParserService**: Resume parsing operations
- **ResumeMatchingService**: Resume matching operations
- **CandidateEvaluationService**: Candidate evaluation operations
- **CandidateStatusService**: Candidate status operations
- **LockableResumeMatchService**: Lockable resume match operations
- **ResumeAwareChatService**: Chat operations with resume context
- **VectorStoreSyncScheduler**: Vector store sync operations

### Repository Layer
- **ResumeRepository**: Resume data access
- **CandidateEvaluationRepository**: Candidate evaluation data access
- **CandidateStatusRepository**: Candidate status data access
- **LockableResumeMatchRepository**: Lockable resume match data access

### Database Layer
- **PostgreSQL + pgvector**: Database with vector extensions for AI operations

### AI Services
- **FuelixChatModel**: Chat model for AI operations
- **FuelixEmbeddingModel**: Embedding model for AI operations

## Recent Improvements

### Idempotency Handling
We've improved the system to better handle duplicate requests by:

1. **Removing requestId field**: The requestId field was removed from LockResumeRequest and CandidateEvaluationModel as it was causing issues with duplicate requests.

2. **Enhanced duplicate detection**: Added logic to detect when a request is identical to an existing evaluation, avoiding unnecessary database operations.

3. **Optimized collection handling**: Improved how collections (like keyStrengths and improvementAreas) are updated to avoid delete-all-insert-all operations.

4. **Dirty checking**: Added more robust dirty checking to only update fields that have actually changed.

5. **Error handling**: Better error handling for numeric field parsing and other potential issues.

These changes make the system more robust and efficient, especially when handling concurrent or duplicate requests.

## Data Flow

1. User uploads a resume through the UploadView
2. ResumeController processes the upload and calls ResumeStorageService
3. ResumeStorageService stores the resume and calls ResumeParserService
4. ResumeParserService extracts information from the resume
5. User can then match the resume with job descriptions through MatchView
6. ResumeMatchingService uses AI to match the resume with job descriptions
7. Results are displayed to the user
8. User can lock/unlock resumes and add evaluations through ResumeLockController
9. CandidateEvaluationService handles the evaluation data

## Technologies Used

- **Spring Boot**: Application framework
- **Spring Data JPA**: Data access
- **PostgreSQL**: Database
- **pgvector**: Vector extensions for PostgreSQL
- **Vaadin**: Web UI framework
- **Spring AI**: AI integration
- **Fuelix AI**: AI service provider
