# Spring AI Resume Matching Application

This application uses Spring AI to match resumes against job descriptions using vector similarity search and AI-powered analysis.

## Features

- Upload and parse resumes in PDF and DOCX formats
- Store resumes in a PostgreSQL database
- Index resumes in a vector store for similarity search
- Match resumes against job descriptions using AI
- Provide detailed analysis of resume matches
- Environment-specific configuration for local, development, and production deployments

## Environment-Based Configuration

The application uses Spring profiles to manage different environments:

### Available Profiles

- **local**: For local development
- **dev**: For development cloud environment
- **prod**: For production deployment

### Configuration Structure

- **application.properties**: Common configuration shared across all environments
- **application-local.properties**: Configuration specific to local development
- **application-dev.properties**: Configuration specific to development cloud environment
- **application-prod.properties**: Configuration specific to production deployment

### Environment Variables

Sensitive information like database credentials and API keys are externalized using environment variables:

```
# Database credentials
DB_USERNAME=your_username
DB_PASSWORD=your_password

# Fuelix AI credentials
FUELIX_API_BASE_URL=https://api-beta.fuelix.ai
FUELIX_API_TOKEN=your_api_token
FUELIX_API_MODEL=claude-3-7-sonnet
FUELIX_API_EMBEDDING_MODEL=text-embedding-ada-002
```

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

## Getting Started

### Prerequisites

- Java 17 or higher
- PostgreSQL 14 or higher with pgvector extension
- Maven 3.8 or higher

### Running Locally

1. Clone the repository
2. Create a `.env` file in the project root with your environment variables (see `.env.example`)
3. Run the application with the local profile:

```bash
# Using environment variables
export SPRING_PROFILES_ACTIVE=local
./mvnw spring-boot:run

# Or directly with Maven
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

The application will be available at http://localhost:8080.

### Running in Cloud Environments

For development and production environments, set the appropriate environment variables:

```bash
export SPRING_PROFILES_ACTIVE=dev  # or prod
export DB_USERNAME=your_username
export DB_PASSWORD=your_password
export FUELIX_API_BASE_URL=https://api-beta.fuelix.ai
export FUELIX_API_TOKEN=your_api_token
./mvnw spring-boot:run
```

## Deployment

### Docker

Build a Docker image:

```bash
# Build the application with Vaadin production mode
docker build -t spring-ai-resume-matcher .
```

Run the container with environment variables:

```bash
docker run -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e DB_USERNAME=your_username \
  -e DB_PASSWORD=your_password \
  -e FUELIX_API_BASE_URL=https://api-beta.fuelix.ai \
  -e FUELIX_API_TOKEN=your_api_token \
  spring-ai-resume-matcher
```

### Google Cloud Run Deployment

Complete steps to deploy to Google Cloud Run:

1. **Build and tag the Docker image**:
   ```bash
   # Build the Docker image
   docker build -t spring-ai-resume-matcher .
   
   # Tag the image for Google Container Registry
   docker tag spring-ai-resume-matcher gcr.io/YOUR_PROJECT_ID/spring-ai-resume-matcher:latest
   ```

2. **Configure Docker for Google Container Registry**:
   ```bash
   # Configure Docker to use gcloud as a credential helper
   gcloud auth configure-docker
   ```

3. **Push the Docker image to Google Container Registry**:
   ```bash
   # Push the image to Google Container Registry
   docker push gcr.io/YOUR_PROJECT_ID/spring-ai-resume-matcher:latest
   ```

4. **Deploy to Cloud Run with environment variables**:
   ```bash
   # Deploy to Cloud Run
   gcloud run deploy spring-ai-resume-matcher \
     --image gcr.io/YOUR_PROJECT_ID/spring-ai-resume-matcher:latest \
     --platform managed \
     --region asia-south1 \
     --memory 1Gi \
     --timeout 300 \
     --allow-unauthenticated \
     --set-env-vars="SPRING_PROFILES_ACTIVE=prod,DB_USERNAME=your_username,DB_PASSWORD=your_password,FUELIX_API_BASE_URL=https://api-beta.fuelix.ai,FUELIX_API_TOKEN=your_api_token"
   ```

5. **Update an existing deployment** (when needed):
   ```bash
   # Update the Cloud Run service (environment variables persist unless changed)
   gcloud run deploy spring-ai-resume-matcher \
     --image gcr.io/YOUR_PROJECT_ID/spring-ai-resume-matcher:latest \
     --platform managed \
     --region asia-south1
   ```
