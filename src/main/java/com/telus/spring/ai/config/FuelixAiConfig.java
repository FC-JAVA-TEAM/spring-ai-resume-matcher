package com.telus.spring.ai.config;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class FuelixAiConfig {

    private final FuelixProperties fuelixProperties;
    
    public FuelixAiConfig(FuelixProperties fuelixProperties) {
        this.fuelixProperties = fuelixProperties;
    }


	@Bean
	@Primary
	public OpenAiApi openAiApi() {
		// In Spring AI 1.0.0, we need to use the builder pattern
		return OpenAiApi.builder()
				.baseUrl(fuelixProperties.getBaseUrl())
				.apiKey(fuelixProperties.getToken())
				.build();
	}

	@Bean
	@Primary
	public ChatModel chatModel(OpenAiApi openAiApi) {
		// Create OpenAiChatOptions with the model name from configuration
		OpenAiChatOptions openAiChatOptions = OpenAiChatOptions.builder()
				.model(fuelixProperties.getModel())
				.temperature(0.7)
				.build();
		
		// Use the builder pattern for OpenAiChatModel
		return OpenAiChatModel.builder()
				.openAiApi(openAiApi)
				.defaultOptions(openAiChatOptions)
				.build();
	}



	@Bean
	@Primary
	public EmbeddingModel embeddingModel(OpenAiApi openAiApi) {
		return new FuelixEmbeddingModel(openAiApi, fuelixProperties.getEmbeddingModel());
	}

}
