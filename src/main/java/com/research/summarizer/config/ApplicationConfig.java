package com.research.summarizer.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring configuration for the Research Summarizer application
 */
@Configuration
public class ApplicationConfig {

  /**
   * Configure ObjectMapper for JSON serialization/deserialization
   */
  @Bean
  public ObjectMapper objectMapper() {
    var mapper = new ObjectMapper();
    mapper.findAndRegisterModules();
    return mapper;
  }
}
