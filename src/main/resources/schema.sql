CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS hstore;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE IF NOT EXISTS vector_store (
    id uuid DEFAULT uuid_generate_v4() PRIMARY KEY,
    content text,
    metadata json,
    embedding vector(1536)  -- 1536 is the default embedding dimension
);

-- Index creation is now handled by the application code to avoid issues with existing indexes

-- Resume management tables
CREATE TABLE IF NOT EXISTS resumes (
    id uuid DEFAULT uuid_generate_v4() PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    phone_number VARCHAR(50) NOT NULL,
    full_text TEXT,
    uploaded_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    file_type VARCHAR(10) NOT NULL,
    original_file_name VARCHAR(255) NOT NULL,
    CONSTRAINT unique_resume UNIQUE (name, email, phone_number)
);

-- Resume vector store table (separate from the main vector_store)
CREATE TABLE IF NOT EXISTS resume_vector_store (
    id uuid DEFAULT uuid_generate_v4() PRIMARY KEY,
    resume_id uuid NOT NULL REFERENCES resumes(id) ON DELETE CASCADE,
    content text,
    metadata json,
    embedding vector(1536),
    CONSTRAINT unique_resume_id UNIQUE (resume_id)
);

-- Index creation is now handled by the application code to avoid issues with existing indexes

-- Candidate status table
CREATE TABLE IF NOT EXISTS candidate_status (
    id uuid DEFAULT uuid_generate_v4() PRIMARY KEY,
    resume_id uuid NOT NULL REFERENCES resumes(id) ON DELETE CASCADE,
    manager_id VARCHAR(255) NOT NULL,
    position VARCHAR(255) NULL,  -- Allow NULL values
    status VARCHAR(50),
    notes TEXT,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP
);
