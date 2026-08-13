package com.research.summarizer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * Main entry point for the Research Summarizer Agent application.
 * This Spring Boot application implements a multi-agent system that:
 * 1. Accepts research topics via REST API
 * 2. Routes queries through three specialized agents
 * 3. Returns structured summary reports
 */
@SpringBootApplication
//@ComponentScan(basePackages = { "com.research.summarizer" })
public class ResearchSummarizerApplication {

  public static void main(String[] args) {
    SpringApplication.run(ResearchSummarizerApplication.class, args);
  }
}
