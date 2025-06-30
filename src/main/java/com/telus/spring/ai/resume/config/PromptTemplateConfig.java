package com.telus.spring.ai.resume.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.util.FileCopyUtils;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

/**
 * Configuration for prompt templates.
 */
@Configuration
public class PromptTemplateConfig {

    private static final Logger logger = LoggerFactory.getLogger(PromptTemplateConfig.class);

    @Value("classpath:prompts/resume-match.prompt")
    private Resource resumeMatchPromptResource;
    
    /**
     * Load the resume match prompt template.
     */
    @Bean(name = "resumeMatchPrompt")
    public String resumeMatchPrompt() throws IOException {
        logger.info("Loading resume match prompt template");
        return loadTemplate(resumeMatchPromptResource);
    }
    
    /**
     * Load a template from a resource.
     */
    private String loadTemplate(Resource resource) throws IOException {
        try (Reader reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8)) {
            return FileCopyUtils.copyToString(reader);
        }
    }
}
