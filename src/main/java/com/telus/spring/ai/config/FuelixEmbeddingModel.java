package com.telus.spring.ai.config;


import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
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
        super(openAiApi, MetadataMode.EMBED);
    }
    
    /**
     * Create a new Fuelix embedding model with custom options.
     * 
     * @param openAiApi The OpenAiApi instance to use for making API requests
     * @param embeddingModel The embedding model to use
     */
    public FuelixEmbeddingModel(OpenAiApi openAiApi, String embeddingModel) {
        super(openAiApi, MetadataMode.EMBED, 
              OpenAiEmbeddingOptions.builder()
                  .withModel(embeddingModel)
                  .build());
    }
}
