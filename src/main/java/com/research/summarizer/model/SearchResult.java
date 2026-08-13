package com.research.summarizer.model;

import java.util.List;

/**
 * Represents raw search results from either MCP or web search
 * 
 * Java 21 Record: Immutable data carrier for search results
 */
public record SearchResult(
    String title,
    String url,
    String content,
    String snippet,
    Double relevanceScore) {
}
