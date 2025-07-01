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
import io.micrometer.observation.ObservationRegistry;

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
		return new OpenAiApi(baseUrl, token);
	}

	@Bean
	@Primary
	public ChatModel chatModel(OpenAiApi openAiApi) {
		// Create a custom OpenAiChatModel with the model name from configuration
		OpenAiChatModel chatModel = new OpenAiChatModel(openAiApi);
		
		// Set the model name using reflection to override the default
		try {
			java.lang.reflect.Field modelField = OpenAiChatModel.class.getDeclaredField("model");
			modelField.setAccessible(true);
			modelField.set(chatModel, model);
		} catch (Exception e) {
			// If reflection fails, log the error but continue
			System.err.println("Failed to set model name: " + e.getMessage());
		}
		
		return chatModel;
	}
	
	@Bean
	@Primary
	public ChatClient chatClient(ChatModel chatModel) {
		return ChatClient.builder(chatModel).build();
	}

	@Bean
	@Primary
	public EmbeddingModel embeddingModel(OpenAiApi openAiApi) {
		return new FuelixEmbeddingModel(openAiApi, embeddingModel);
	}

}
