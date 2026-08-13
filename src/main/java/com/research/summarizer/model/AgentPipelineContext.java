package com.research.summarizer.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Context object that flows through the agent pipeline
 * 
 * Mutable data carrier that accumulates results from each agent stage
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AgentPipelineContext {

  private String topic;
  private int maxSources = 5; // Default max sources
  private String searchSource; // "MCP" or "WEB"
  private List<SearchResult> rawSearchResults;
  private List<Insight> extractedInsights;
  private String executiveSummary;
  private String detailedReport;
  private List<SourceInfo> sources;

  /**
   * Nested record for source information
   */
  public record SourceInfo(
      String title,
      String url,
      String snippet) {
  }
}
