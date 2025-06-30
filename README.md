# Spring AI Resume Matcher

A Spring Boot application that uses AI to parse, analyze, and match resumes with job descriptions. This application leverages Spring AI, Vaadin UI framework, and vector databases for efficient resume processing and matching.

## Features

- **Resume Parsing**: Extract structured information from PDF resumes
- **AI-Powered Matching**: Match resumes against job descriptions using AI
- **Vector Database Storage**: Store and search resumes efficiently using vector embeddings
- **User-Friendly Interface**: Clean UI built with Vaadin framework
- **Scheduled Synchronization**: Automatic synchronization of vector database

## Technologies Used

- Spring Boot
- Spring AI
- Vaadin Framework
- H2 Database
- Vector Database for embeddings
- Thymeleaf Templates
- RESTful APIs

## Getting Started

### Prerequisites

- Java 17 or higher
- Maven
- Git

### Installation

1. Clone the repository
   ```
   git clone https://github.com/FC-JAVA-TEAM/spring-ai-resume-matcher.git
   ```

2. Navigate to the project directory
   ```
   cd spring-ai-resume-matcher
   ```

3. Build the project
   ```
   mvn clean install
   ```

4. Run the application
   ```
   mvn spring-boot:run
   ```

5. Access the application at `http://localhost:8080`

## Project Structure

- `src/main/java/com/telus/spring/ai/resume`: Core resume processing functionality
- `src/main/java/com/telus/spring/ai/resume/ui`: Vaadin UI components
- `src/main/resources/templates`: Thymeleaf templates
- `src/main/resources/static`: Static resources (CSS, JS)
- `src/main/resources/prompts`: AI prompt templates

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.
