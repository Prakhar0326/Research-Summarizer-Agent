package com.research.summarizer.agent;

import com.research.summarizer.model.AgentPipelineContext;
import com.research.summarizer.model.Insight;
import com.research.summarizer.service.HuggingFaceLLMService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Agent 3: Report Generator
 * 
 * Responsibility: Receive the structured insights from Agent 2. Generate a
 * final, formatted summary report with clear sections: Executive Summary, Key
 * Findings, Details, and Sources using LLM text generation.
 * 
 * Uses HuggingFace Inference API for LLM-based report generation
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReportGeneratorAgent {

  private final HuggingFaceLLMService llmService;

  /**
   * Execute the report generator agent
   */
  public AgentPipelineContext execute(AgentPipelineContext context) {
    log.info("Report Generator Agent processing {} insights",
        context.getExtractedInsights() != null ? context.getExtractedInsights().size() : 0);

    // Generate executive summary using LLM
    var executiveSummary = generateExecutiveSummaryWithLLM(
        context.getTopic(),
        context.getExtractedInsights());
    context.setExecutiveSummary(executiveSummary);

    // Generate detailed report using LLM
    var detailedReport = generateDetailedReportWithLLM(context.getExtractedInsights());
    context.setDetailedReport(detailedReport);

    // Convert search results to source info
    if (context.getRawSearchResults() != null) {
      context.setSources(
          context.getRawSearchResults().stream()
              .map(sr -> new AgentPipelineContext.SourceInfo(
                  sr.title(),
                  sr.url(),
                  sr.snippet()))
              .toList());
    } else {
      context.setSources(new ArrayList<>());
    }

    log.info("Report Generator Agent completed");

    return context;
  }

  /**
   * Generate executive summary using LLM
   */
  private String generateExecutiveSummaryWithLLM(String topic, List<Insight> insights) {
    if (insights == null || insights.isEmpty()) {
      return String.format("No summary available for topic: %s", topic);
    }

    try {
      // Build prompt for LLM
      String prompt = buildSummaryPrompt(topic, insights);
      
      log.debug("Generating executive summary with LLM for topic: {}", topic);
      
      // Call LLM to generate summary
      String summary = llmService.generateText(prompt);
      
      if (summary == null || summary.isEmpty()) {
        log.warn("LLM returned empty summary");
        return generateFallbackSummary(topic, insights);
      }

      return summary.trim();

    } catch (Exception e) {
      log.error("Error generating summary with LLM: {}", e.getMessage(), e);
      return generateFallbackSummary(topic, insights);
    }
  }

  /**
   * Build summary generation prompt for the LLM
   */
  private String buildSummaryPrompt(String topic, List<Insight> insights) {
    StringBuilder insightsText = new StringBuilder();
    for (Insight insight : insights) {
      insightsText.append("- [").append(insight.type()).append("] ").append(insight.content()).append("\n");
    }

    return String.format(
        """
        Write a concise executive summary for the research topic below.
        The summary should be 2-3 sentences and highlight the most important findings.
        
        Topic: %s
        
        Available Insights:
        %s
        
        Executive Summary:
        """,
        topic,
        insightsText.toString());
  }

  /**
   * Fallback summary generation (template-based)
   */
  private String generateFallbackSummary(String topic, List<Insight> insights) {
    var summary = new StringBuilder();
    summary.append("Research Summary: ").append(topic).append("\n\n");

    // Count insight types
    var factCount = insights.stream()
        .filter(i -> "fact".equals(i.type()))
        .count();

    var statCount = insights.stream()
        .filter(i -> "statistic".equals(i.type()))
        .count();

    var defCount = insights.stream()
        .filter(i -> "definition".equals(i.type()))
        .count();

    summary.append("This research summary contains ")
        .append(insights.size()).append(" key insights including ")
        .append(factCount).append(" facts, ")
        .append(statCount).append(" statistics, and ")
        .append(defCount).append(" definitions.\n\n");

    // Top 3 insights
    summary.append("Key Highlights:\n");
    insights.stream()
        .limit(3)
        .forEach(insight -> {
          summary.append("• ").append(insight.content()).append("\n");
        });

    return summary.toString();
  }

  /**
   * Generate detailed report using LLM
   */
  private String generateDetailedReportWithLLM(List<Insight> insights) {
    if (insights == null || insights.isEmpty()) {
      return "No detailed findings available.";
    }

    try {
      // Build prompt for detailed report
      String prompt = buildDetailedReportPrompt(insights);
      
      log.debug("Generating detailed report with LLM");
      
      // Call LLM to generate report
      String report = llmService.generateText(prompt);
      
      if (report == null || report.isEmpty()) {
        log.warn("LLM returned empty report");
        return generateFallbackDetailedReport(insights);
      }

      return report.trim();

    } catch (Exception e) {
      log.error("Error generating detailed report with LLM: {}", e.getMessage(), e);
      return generateFallbackDetailedReport(insights);
    }
  }

  /**
   * Build detailed report generation prompt for the LLM
   */
  private String buildDetailedReportPrompt(List<Insight> insights) {
    StringBuilder insightsText = new StringBuilder();
    
    // Organize by type
    List<Insight> facts = insights.stream()
        .filter(i -> "fact".equals(i.type()))
        .limit(10)
        .toList();
    
    List<Insight> stats = insights.stream()
        .filter(i -> "statistic".equals(i.type()))
        .limit(10)
        .toList();
    
    List<Insight> defs = insights.stream()
        .filter(i -> "definition".equals(i.type()))
        .limit(10)
        .toList();

    if (!facts.isEmpty()) {
      insightsText.append("Facts:\n");
      for (Insight f : facts) {
        insightsText.append("- ").append(f.content()).append("\n");
      }
      insightsText.append("\n");
    }

    if (!stats.isEmpty()) {
      insightsText.append("Statistics:\n");
      for (Insight s : stats) {
        insightsText.append("- ").append(s.content()).append("\n");
      }
      insightsText.append("\n");
    }

    if (!defs.isEmpty()) {
      insightsText.append("Definitions:\n");
      for (Insight d : defs) {
        insightsText.append("- ").append(d.content()).append("\n");
      }
    }

    return String.format(
        """
        Format the following insights into a well-structured detailed report.
        Use markdown format with clear sections: "## Key Findings", "## Statistics", "## Definitions".
        Make the report informative and easy to read.
        
        Insights:
        %s
        
        Detailed Report:
        """,
        insightsText.toString());
  }

  /**
   * Fallback detailed report generation (template-based)
   */
  private String generateFallbackDetailedReport(List<Insight> insights) {
    var report = new StringBuilder();

    // Section: Key Findings (Facts)
    report.append("## Key Findings\n\n");
    var facts = insights.stream()
        .filter(i -> "fact".equals(i.type()))
        .limit(5)
        .toList();

    if (!facts.isEmpty()) {
      for (var fact : facts) {
        report.append("- ").append(fact.content()).append("\n");
      }
    } else {
      report.append("No specific facts found.\n");
    }

    report.append("\n");

    // Section: Statistics
    report.append("## Statistics\n\n");
    var stats = insights.stream()
        .filter(i -> "statistic".equals(i.type()))
        .limit(5)
        .toList();

    if (!stats.isEmpty()) {
      for (var stat : stats) {
        report.append("- ").append(stat.content()).append("\n");
      }
    } else {
      report.append("No statistics found.\n");
    }

    report.append("\n");

    // Section: Definitions
    report.append("## Definitions\n\n");
    var defs = insights.stream()
        .filter(i -> "definition".equals(i.type()))
        .limit(5)
        .toList();

    if (!defs.isEmpty()) {
      for (var def : defs) {
        report.append("- ").append(def.content()).append("\n");
      }
    } else {
      report.append("No definitions found.\n");
    }

    return report.toString();
  }
}
