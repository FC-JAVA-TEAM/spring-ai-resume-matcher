# Spring AI Resume Matching Application

This application uses Spring AI to match resumes against job descriptions using vector similarity search and AI-powered analysis.

## Features

- Upload and parse resumes in PDF and DOCX formats
- Store resumes in a PostgreSQL database
- Index resumes in a vector store for similarity search
- Match resumes against job descriptions using AI
- Provide detailed analysis of resume matches

## Comprehensive Error Handling and Retry Mechanism

The application includes a robust error handling and retry mechanism to handle various transient errors from the AI service, such as 503 Service Unavailable errors, network timeouts, and connection issues. This ensures that the application can continue to function even when the AI service is temporarily unavailable.

### How It Works

1. **RetryTemplate Configuration**: The application uses Spring Retry's `RetryTemplate` to automatically retry failed AI API calls.

2. **Exponential Backoff**: Each retry attempt waits longer than the previous one, using an exponential backoff strategy to avoid overwhelming the AI service.

3. **Selective Retries**: Specific errors (503, 502, 504, and network issues including ResourceAccessException with null causes) trigger retries, while other errors are handled immediately.

4. **Fallback Mechanism**: If all retry attempts fail, the application provides a graceful fallback response instead of failing completely.

5. **Exception Handling**: Comprehensive exception handling throughout the application ensures that errors are properly logged and don't cause the application to crash.

6. **Configurable Parameters**: The retry behavior can be configured through application properties:
   - `resume.matching.ai-retry-count`: Number of retry attempts (default: 3)
   - `resume.matching.ai-retry-delay-ms`: Initial delay between retries in milliseconds (default: 1000)
   - `resume.matching.ai-retry-max-delay-ms`: Maximum delay between retries in milliseconds (default: 10000)
   - `resume.matching.ai-retry-multiplier`: Multiplier for exponential backoff (default: 2.0)

### Implementation Details

The error handling and retry mechanism is implemented in the following components:

1. **RetryConfig**: Configures the RetryTemplate with the appropriate retry policy and backoff strategy, specifically handling ResourceAccessException for network issues.

2. **ResumeMatchingServiceImpl**: Uses the RetryTemplate to execute AI API calls with retry logic and provides fallback mechanisms.

3. **ResumeVectorStoreConfig**: Implements retry logic for embedding operations with proper exception handling and fallback mechanisms.

4. **application.properties**: Contains the configuration parameters for the retry mechanism.

## Getting Started

### Prerequisites

- Java 17 or higher
- PostgreSQL 14 or higher with pgvector extension
- Maven 3.8 or higher

### Configuration

Configure the application in `application.properties`:

```properties
# Database configuration
spring.datasource.url=jdbc:postgresql://localhost:5432/postgres
spring.datasource.username=your_username
spring.datasource.password=your_password

# Fuelix.ai configuration
fuelix.api.base-url=https://api-beta.fuelix.ai
fuelix.api.token=your_api_token
fuelix.api.model=claude-3-7-sonnet
fuelix.api.embedding-model=text-embedding-ada-002

# Retry configuration
resume.matching.ai-retry-count=3
resume.matching.ai-retry-delay-ms=1000
resume.matching.ai-retry-max-delay-ms=10000
resume.matching.ai-retry-multiplier=2.0
```

### Running the Application

```bash
mvn spring-boot:run
```

The application will be available at http://localhost:8080.
