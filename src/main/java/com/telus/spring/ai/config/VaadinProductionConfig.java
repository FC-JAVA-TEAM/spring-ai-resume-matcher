package com.telus.spring.ai.config;

import com.vaadin.flow.server.InitParameters;
import org.springframework.context.annotation.Configuration;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Configuration class to explicitly set Vaadin to production mode.
 * This ensures that Vaadin doesn't try to run in development mode
 * when deployed to production environments.
 */
@Configuration
public class VaadinProductionConfig {

    private static final Logger logger = LoggerFactory.getLogger(VaadinProductionConfig.class);

    /**
     * Sets the system property to force Vaadin into production mode.
     * This is done at application startup and takes precedence over
     * environment variables.
     */
    @PostConstruct
    public void forceProductionMode() {
        logger.info("Forcing Vaadin to run in production mode");
        System.setProperty(InitParameters.SERVLET_PARAMETER_PRODUCTION_MODE, "true");
        System.setProperty("vaadin.productionMode", "true");
    }
}
