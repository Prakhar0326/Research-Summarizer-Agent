package com.research.summarizer.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Request DTO for the research summarization endpoint
 * 
 * Java 21 Record: Immutable data carrier for API request
 */
public record ResearchRequest(
    @JsonProperty("topic") String topic,

    @JsonProperty("maxSources") Integer maxSources) {
}
