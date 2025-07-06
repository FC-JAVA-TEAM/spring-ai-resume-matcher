# Docker Build and Deployment Instructions

## Issue Encountered

When deploying the Spring AI application to Cloud Run, we encountered an error where the container failed to start and listen on the port defined by the PORT=8080 environment variable. The logs showed the following error:

```
Failed to bind properties under 'spring.ai.openai.api-key' to java.lang.String:
Reason: java.lang.IllegalArgumentException: Could not resolve placeholder 'FUELIX_API_TOKEN' in value "${FUELIX_API_TOKEN}"
```

## Root Cause

The application was failing to start because it couldn't resolve the `FUELIX_API_TOKEN` environment variable. This variable is required for the application to connect to the Fuelix AI API for embedding and chat functionality.

In our application, the following properties in `application.properties` depend on environment variables:

```properties
spring.ai.openai.api-key=${fuelix.api.token}
spring.ai.openai.base-url=${fuelix.api.base-url}
```

And in `application-prod.properties`, we have:

```properties
fuelix.api.base-url=${FUELIX_API_BASE_URL:https://api-beta.fuelix.ai}
fuelix.api.token=${FUELIX_API_TOKEN}
fuelix.api.model=${FUELIX_API_MODEL:claude-3-7-sonnet}
fuelix.api.embedding-model=${FUELIX_API_EMBEDDING_MODEL:text-embedding-ada-002}
```

When deploying to Cloud Run, we needed to provide these environment variables, but they were missing from our initial deployment command.

## Solution

We updated the Cloud Run deployment command to include all the necessary environment variables:

```bash
gcloud run deploy treko \
  --image=shaikh79/treko:latest \
  --platform=managed \
  --region=asia-south1 \
  --allow-unauthenticated \
  --service-account=cloud-run-deployer@telus-koodo.iam.gserviceaccount.com \
  --set-env-vars="SPRING_PROFILES_ACTIVE=prod,VAADIN_PRODUCTION_MODE=true,DB_USERNAME=postgres,DB_PASSWORD=8899,SPRING_JPA_PROPERTIES_HIBERNATE_DIALECT=org.hibernate.dialect.PostgreSQLDialect,SPRING_JPA_HIBERNATE_DDL_AUTO=update,SPRING_DATASOURCE_URL=jdbc:postgresql:///postgres?cloudSqlInstance=telus-koodo:asia-south1:postgres&socketFactory=com.google.cloud.sql.postgres.SocketFactory,FUELIX_API_BASE_URL=https://api-beta.fuelix.ai,FUELIX_API_TOKEN=nHeX0UQumAogwKoOX9k6RSDrPDAyLGgTKoCMqYlinqGrSKLw,FUELIX_API_MODEL=claude-3-7-sonnet,FUELIX_API_EMBEDDING_MODEL=text-embedding-ada-002" \
  --add-cloudsql-instances=telus-koodo:asia-south1:postgres \
  --timeout=10m
```

The key additions were:
- `FUELIX_API_BASE_URL=https://api-beta.fuelix.ai`
- `FUELIX_API_TOKEN=nHeX0UQumAogwKoOX9k6RSDrPDAyLGgTKoCMqYlinqGrSKLw`
- `FUELIX_API_MODEL=claude-3-7-sonnet`
- `FUELIX_API_EMBEDDING_MODEL=text-embedding-ada-002`

## Deployment Process

1. Build the Docker image:
   ```bash
   docker build -t shaikh79/treko:latest .
   ```

2. Push the Docker image to Docker Hub:
   ```bash
   docker push shaikh79/treko:latest
   ```

3. Deploy to Cloud Run with all required environment variables:
   ```bash
   gcloud run deploy treko \
     --image=shaikh79/treko:latest \
     --platform=managed \
     --region=asia-south1 \
     --allow-unauthenticated \
     --service-account=cloud-run-deployer@telus-koodo.iam.gserviceaccount.com \
     --set-env-vars="SPRING_PROFILES_ACTIVE=prod,VAADIN_PRODUCTION_MODE=true,DB_USERNAME=postgres,DB_PASSWORD=8899,SPRING_JPA_PROPERTIES_HIBERNATE_DIALECT=org.hibernate.dialect.PostgreSQLDialect,SPRING_JPA_HIBERNATE_DDL_AUTO=update,SPRING_DATASOURCE_URL=jdbc:postgresql:///postgres?cloudSqlInstance=telus-koodo:asia-south1:postgres&socketFactory=com.google.cloud.sql.postgres.SocketFactory,FUELIX_API_BASE_URL=https://api-beta.fuelix.ai,FUELIX_API_TOKEN=nHeX0UQumAogwKoOX9k6RSDrPDAyLGgTKoCMqYlinqGrSKLw,FUELIX_API_MODEL=claude-3-7-sonnet,FUELIX_API_EMBEDDING_MODEL=text-embedding-ada-002" \
     --add-cloudsql-instances=telus-koodo:asia-south1:postgres \
     --timeout=10m
   ```

## Verification

After deployment, the application is accessible at:
https://treko-480868035316.asia-south1.run.app

The application is now running successfully with the Resume AI interface showing the main features:
- Vector Database
- Match with Jobs
- Automatic Sync

## Best Practices for Future Deployments

1. Always ensure all required environment variables are set in the deployment command.
2. Use Cloud Run secrets for sensitive information like API tokens and database passwords.
3. Consider using a CI/CD pipeline to automate the build and deployment process.
4. Monitor the application logs after deployment to catch any issues early.
