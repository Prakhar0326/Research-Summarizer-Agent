package com.research.summarizer.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Response DTO for the research summarization endpoint
 * 
 * Java 21 Record: Immutable data carrier for API response
 */
public record ResearchResponse(
    @JsonProperty("topic") String topic,

    @JsonProperty("searchSource") String searchSource, // "MCP" or "WEB"

    @JsonProperty("executiveSummary") String executiveSummary,

    @JsonProperty("keyFindings") List<String> keyFindings,

    @JsonProperty("details") String details,

    @JsonProperty("sources") List<SourceInfo> sources) {
}
