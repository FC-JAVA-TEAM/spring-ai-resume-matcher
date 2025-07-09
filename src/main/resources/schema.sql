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

CREATE TABLE IF NOT EXISTS candidate_evaluations (
    id UUID PRIMARY KEY,
    resume_Id UUID ,
    name VARCHAR(255),
    email VARCHAR(255),
    phone_number VARCHAR(50),
    score INTEGER,
    executive_summary TEXT,
    technical_skills INTEGER,
    experience INTEGER,
    education INTEGER,
    soft_skills INTEGER,
    achievements INTEGER,
    recommendation_type VARCHAR(100),
    recommendation_reason TEXT,
    locked BOOLEAN DEFAULT FALSE,
    manager_id VARCHAR(100),
    locked_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS candidate_evaluation_model_key_strengths (
    candidate_evaluation_model_id UUID REFERENCES candidate_evaluations(id),
    key_strengths VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS candidate_evaluation_model_improvement_areas (
    candidate_evaluation_model_id UUID REFERENCES candidate_evaluations(id),
    improvement_areas VARCHAR(255)
);
