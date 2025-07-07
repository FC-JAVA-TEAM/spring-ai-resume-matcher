package com.telus.spring.ai.resume.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for chat memory functionality.
 * This class sets up the necessary beans for chat memory support.
 */
@Configuration
public class ChatMemoryConfig {
    
    /**
     * Creates an in-memory chat memory repository.
     * This repository stores chat messages in memory rather than in a database.
     * 
     * @return A configured InMemoryChatMemoryRepository instance
     */
    @Bean
    public ChatMemoryRepository chatMemoryRepository() {
        return new InMemoryChatMemoryRepository();
    }
    
    /**
     * Creates a ChatMemory bean that uses MessageWindowChatMemory implementation.
     * This memory keeps a window of the most recent messages.
     * 
     * @param chatMemoryRepository The repository to store chat messages
     * @return A configured ChatMemory instance
     */
    @Bean
    public ChatMemory chatMemory(ChatMemoryRepository chatMemoryRepository) {
        return MessageWindowChatMemory.builder()
            .chatMemoryRepository(chatMemoryRepository)
            .maxMessages(20) // Keep the last 20 messages in memory
            .build();
    }
    
    /**
     * Creates a ChatClient bean that uses the ChatModel and ChatMemory.
     * The ChatClient provides a higher-level API for interacting with the chat model.
     * 
     * @param chatModel The underlying chat model
     * @param chatMemory The chat memory to use
     * @return A configured ChatClient instance
     */
    @Bean
    public ChatClient chatClient(ChatModel chatModel, ChatMemory chatMemory) {
        return ChatClient.builder(chatModel)
            .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
            .build();
    }
}
