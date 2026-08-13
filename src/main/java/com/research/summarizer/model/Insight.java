package com.research.summarizer.model;

/**
 * Represents a single insight extracted by the Insight Extractor agent
 * 
 * Java 21 Record: Immutable data carrier for extracted insights
 */
public record Insight(
    String type, // "fact", "statistic", "definition", "quote"
    String content,
    String source,
    Double confidence) {
}
