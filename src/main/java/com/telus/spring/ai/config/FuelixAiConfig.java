package com.telus.spring.ai.config;

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
		return new OpenAiApi(baseUrl, token);
	}

	@Bean
	@Primary
	public ChatModel chatModel(OpenAiApi openAiApi) {
		OpenAiChatOptions options = OpenAiChatOptions.builder().model(model).temperature(0.2d)
				.maxTokens(50000).build();

		return new OpenAiChatModel(openAiApi, options);
	}

	@Bean
	@Primary
	public EmbeddingModel embeddingModel(OpenAiApi openAiApi) {
		return new FuelixEmbeddingModel(openAiApi, embeddingModel);
	}

}
