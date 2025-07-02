package com.telus.spring.ai.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class FuelixAiConfig {

	@Value("${fuelix.api.base-url}")
	private String baseUrl;

	@Value("${fuelix.api.token}")
	private String token;

	@Value("${fuelix.api.model}")
	private String model;

	@Value("${fuelix.api.embedding-model}")
	private String embeddingModel;


	@Bean
	@Primary
	public OpenAiApi openAiApi() {
		// In Spring AI 1.0.0, we need to use the builder pattern
		return OpenAiApi.builder()
				.baseUrl(baseUrl)
				.apiKey(token)
				.build();
	}

	@Bean
	@Primary
	public ChatModel chatModel(OpenAiApi openAiApi) {
		// Create OpenAiChatOptions with the model name from configuration
		OpenAiChatOptions openAiChatOptions = OpenAiChatOptions.builder()
				.model(model)
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
		return new FuelixEmbeddingModel(openAiApi, embeddingModel);
	}

}
