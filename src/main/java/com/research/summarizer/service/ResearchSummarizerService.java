package com.research.summarizer.service;

import com.research.summarizer.agent.InsightExtractorAgent;
import com.research.summarizer.agent.ReportGeneratorAgent;
import com.research.summarizer.agent.SearchAgent;
import com.research.summarizer.dto.ResearchRequest;
import com.research.summarizer.dto.ResearchResponse;
import com.research.summarizer.dto.SourceInfo;
import com.research.summarizer.model.AgentPipelineContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Service that orchestrates the three-agent pipeline
 * 
 * Pipeline flow:
 * 1. SearchAgent: Find raw search results
 * 2. InsightExtractorAgent: Extract structured insights
 * 3. ReportGeneratorAgent: Generate final report
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ResearchSummarizerService {

  private final SearchAgent searchAgent;
  private final InsightExtractorAgent insightExtractorAgent;
  private final ReportGeneratorAgent reportGeneratorAgent;

  /**
   * Process a research request through the agent pipeline
   */
  public ResearchResponse processResearch(ResearchRequest request) {
    var traceId = UUID.randomUUID().toString();
    log.info("[{}] Starting research pipeline for topic: {}", traceId, request.topic());

    try {
      // Initialize pipeline context
      var context = new AgentPipelineContext();
      context.setTopic(request.topic());
      context.setMaxSources(request.maxSources() != null ? request.maxSources() : 5);

      log.debug("[{}] Agent 1: SearchAgent - Starting", traceId);
      // Agent 1: Search
      context = searchAgent.execute(context);
      log.debug("[{}] Agent 1: SearchAgent - Completed. Found {} results from {}",
          traceId,
          context.getRawSearchResults() != null ? context.getRawSearchResults().size() : 0,
          context.getSearchSource());

      log.debug("[{}] Agent 2: InsightExtractorAgent - Starting", traceId);
      // Agent 2: Extract Insights
      context = insightExtractorAgent.execute(context);
      log.debug("[{}] Agent 2: InsightExtractorAgent - Completed. Extracted {} insights",
          traceId,
          context.getExtractedInsights() != null ? context.getExtractedInsights().size() : 0);

      log.debug("[{}] Agent 3: ReportGeneratorAgent - Starting", traceId);
      // Agent 3: Generate Report
      context = reportGeneratorAgent.execute(context);
      log.debug("[{}] Agent 3: ReportGeneratorAgent - Completed", traceId);

      // Convert context to response
      var response = buildResponse(context);

      log.info("[{}] Research pipeline completed successfully", traceId);
      return response;

    } catch (Exception e) {
      log.error("[{}] Error processing research request", traceId, e);
      throw new RuntimeException("Failed to process research request", e);
    }
  }

  /**
   * Convert agent pipeline context to REST response
   */
  private ResearchResponse buildResponse(AgentPipelineContext context) {
    return new ResearchResponse(
        context.getTopic(),
        context.getSearchSource(),
        context.getExecutiveSummary(),
        extractKeyFindings(context),
        context.getDetailedReport(),
        context.getSources().stream()
            .map(src -> new SourceInfo(
                src.title(),
                src.url(),
                src.snippet()))
            .toList());
  }

  /**
   * Extract key findings from insights
   */
  private java.util.List<String> extractKeyFindings(AgentPipelineContext context) {
    if (context.getExtractedInsights() == null || context.getExtractedInsights().isEmpty()) {
      return java.util.Collections.emptyList();
    }

    return context.getExtractedInsights().stream()
        .limit(5)
        .map(insight -> insight.content())
        .toList();
  }
}
