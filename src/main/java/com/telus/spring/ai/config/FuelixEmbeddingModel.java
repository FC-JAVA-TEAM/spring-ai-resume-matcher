package com.telus.spring.ai.config;

import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.api.OpenAiApi;

/**
 * Simple implementation of OpenAiEmbeddingModel that uses Fuelix.ai API.
 */
public class FuelixEmbeddingModel extends OpenAiEmbeddingModel {

    /**
     * Create a new Fuelix embedding model with default options.
     * 
     * @param openAiApi The OpenAiApi instance to use for making API requests
     */
    public FuelixEmbeddingModel(OpenAiApi openAiApi) {
        super(openAiApi);
    }
    
    /**
     * Create a new Fuelix embedding model with custom options.
     * 
     * @param openAiApi The OpenAiApi instance to use for making API requests
     * @param embeddingModel The embedding model to use (ignored in this implementation)
     */
    public FuelixEmbeddingModel(OpenAiApi openAiApi, String embeddingModel) {
        // In Spring AI 1.0.0, we can't directly set the model in the constructor
        // The model will be set via application properties
        super(openAiApi);
    }
}
