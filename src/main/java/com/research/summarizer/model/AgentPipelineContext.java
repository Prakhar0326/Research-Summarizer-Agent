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

  private int maxSources = 5;

  /**
   * "MCP" or "WEB"
   */
  private String searchSource;

  /**
   * Raw results returned by Search Agent.
   */
  private List<SearchResult> rawSearchResults;

  /**
   * Structured insights extracted by Insight Extractor Agent.
   */
  private List<Insight> extractedInsights;

  /**
   * Top-level executive summary.
   */
  private String executiveSummary;

  /**
   * Key findings extracted from the insights.
   */
  private List<String> keyFindings;

  /**
   * Detailed markdown report.
   */
  private String detailedReport;

  /**
   * Sources used for the research.
   */
  private List<SourceInfo> sources;

  /**
   * Nested record for source information.
   */
  public record SourceInfo(
          String title,
          String url,
          String snippet) {
  }
}