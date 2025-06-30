package com.telus.spring.ai.controller;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ICSController {

    private final ChatClient chatClient;
    private final VectorStore vectorStore;

    public ICSController(ChatClient.Builder builder, VectorStore vectorStore) {
        /*this.chatClient = builder
                .defaultAdvisors(new QuestionAnswerAdvisor(
                        vectorStore,
                        SearchRequest.defaults()
                ))
                .build();*/
    	
        this.chatClient = builder.build();
        this.vectorStore = vectorStore;
    }

    private String prompt = """
            Your task is to answer the questions about Indian Constitution. Use the information from the DOCUMENTS
            section to provide accurate answers. If unsure or if the answer isn't found in the DOCUMENTS section, 
            simply state that you don't know the answer.
                        
            QUESTION:
            {input}
                        
            DOCUMENTS:
            {documents}
                        
            """;

    @GetMapping("/ics")
    public String icsQuestion(@RequestParam String q) {
        return chatClient
                .prompt()
                .user(q)
                .call()
                .content();
    }


    @GetMapping("/ic")
    public String simplifyIC(@RequestParam String q) {
        try {
            // Try to use vector store for RAG
            List<Document> documents =
                    vectorStore.similaritySearch(SearchRequest.query(q).withTopK(5));
            
            String context = documents
                    .stream()
                    .map(document -> document.getContent().toString())
                    .collect(Collectors.joining("\n\n"));
            
            return chatClient
                    .prompt()
                    .system("You are an expert on the Indian Constitution. Use the provided context to answer questions accurately.")
                    .user("Context: " + context + "\n\nQuestion: " + q)
                    .call()
                    .content();
        } catch (Exception e) {
        	
        	System.err.println("Vector store query failed: " + e.getMessage());
            // Fallback to regular chat if vector store fails
            return chatClient
                    .prompt()
                    .system("You are an expert on the Indian Constitution.")
                    .user("Question about Indian Constitution: " + q)
                    .call()
                    .content();
        }
    }
}
