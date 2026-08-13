package com.research.summarizer.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO representing a single source in the research
 * 
 * Java 21 Record: Immutable data carrier for source information
 */
public record SourceInfo(
    @JsonProperty("title") String title,

    @JsonProperty("url") String url,

    @JsonProperty("snippet") String snippet) {
}
