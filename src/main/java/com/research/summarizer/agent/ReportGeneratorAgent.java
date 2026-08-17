
package com.research.summarizer.agent;

import com.research.summarizer.model.AgentPipelineContext;
import com.research.summarizer.model.Insight;
import com.research.summarizer.service.HuggingFaceLLMService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReportGeneratorAgent {

  private final HuggingFaceLLMService llmService;

  public AgentPipelineContext execute(
          AgentPipelineContext context) {

    List<Insight> insights =
            context.getExtractedInsights();

    log.info(
            "Report Generator Agent processing {} insights",
            insights != null
                    ? insights.size()
                    : 0
    );

    /*
     * --------------------------------------------------
     * KEY FINDINGS
     * --------------------------------------------------
     *
     * Don't depend on another LLM call for this.
     *
     * Agent 2 has already extracted the facts.
     */
    List<String> keyFindings =
            createKeyFindings(insights);

    context.setKeyFindings(keyFindings);

    String executiveSummary =
            generateExecutiveSummary(
                    context.getTopic(),
                    insights
            );

    context.setExecutiveSummary(
            executiveSummary
    );

    /*
     * --------------------------------------------------
     * DETAILED REPORT
     * --------------------------------------------------
     */
    String detailedReport =
            generateDetailedReport(
                    context.getTopic(),
                    insights
            );

    context.setDetailedReport(
            detailedReport
    );

    /*
     * --------------------------------------------------
     * SOURCES
     * --------------------------------------------------
     */
    if (context.getRawSearchResults() != null) {

      context.setSources(
              context.getRawSearchResults()
                      .stream()
                      .map(
                              sr ->
                                      new AgentPipelineContext.SourceInfo(
                                              sr.title(),
                                              sr.url(),
                                              sr.snippet()
                                      )
                      )
                      .toList()
      );

    } else {

      context.setSources(
              new ArrayList<>()
      );
    }

    log.info(
            "Report Generator Agent completed"
    );

    return context;
  }

  /**
   * Create key findings directly from Agent 2 insights.
   */
  private List<String> createKeyFindings(
          List<Insight> insights) {

    if (insights == null
            || insights.isEmpty()) {

      return new ArrayList<>();
    }

    return insights
            .stream()
            .filter(
                    insight ->
                            insight.content() != null
                                    && !insight.content().isBlank()
            )
            .limit(5)
            .map(Insight::content)
            .toList();
  }

  /**
   * Generate executive summary.
   */
  private String generateExecutiveSummary(
          String topic,
          List<Insight> insights) {

    if (insights == null
            || insights.isEmpty()) {

      return "No summary available for topic: "
              + topic;
    }

    try {

      String prompt =
              buildSummaryPrompt(
                      topic,
                      insights
              );

      log.info(
              "Generating executive summary for topic: {}",
              topic
      );

      String summary =
              llmService.generateText(prompt);

      if (summary == null
              || summary.isBlank()) {

        log.warn(
                "LLM returned empty executive summary"
        );

        return generateFallbackSummary(
                topic,
                insights
        );
      }

      return cleanLLMResponse(summary);

    } catch (Exception e) {

      log.error(
              "Error generating executive summary: {}",
              e.getMessage(),
              e
      );

      return generateFallbackSummary(
              topic,
              insights
      );
    }
  }

  private String buildSummaryPrompt(
          String topic,
          List<Insight> insights) {

    StringBuilder research =
            new StringBuilder();

    for (Insight insight : insights) {

      research
              .append("- ")
              .append(insight.content())
              .append("\n");
    }

    return """
                You are a professional research analyst.

                Write a concise executive summary about:

                %s

                Use ONLY the research insights below.

                Rules:
                - 2 to 4 sentences.
                - Be factual.
                - Do not invent information.
                - Do not mention the research process.
                - Do not use markdown.
                - Return only the summary.

                Research Insights:
                %s

                Executive Summary:
                """.formatted(
            topic,
            research
    );
  }

  /**
   * Generate detailed report.
   */
  private String generateDetailedReport(
          String topic,
          List<Insight> insights) {

    if (insights == null
            || insights.isEmpty()) {

      return "No detailed findings available.";
    }

    try {

      String prompt =
              buildDetailedReportPrompt(
                      topic,
                      insights
              );

      log.info(
              "Generating detailed report for topic: {}",
              topic
      );

      String report =
              llmService.generateText(prompt);

      if (report == null
              || report.isBlank()) {

        log.warn(
                "LLM returned empty detailed report"
        );

        return generateFallbackDetailedReport(
                insights
        );
      }

      return cleanLLMResponse(report);

    } catch (Exception e) {

      log.error(
              "Error generating detailed report: {}",
              e.getMessage(),
              e
      );

      return generateFallbackDetailedReport(
              insights
      );
    }
  }

  private String buildDetailedReportPrompt(
          String topic,
          List<Insight> insights) {

    StringBuilder research =
            new StringBuilder();

    for (Insight insight : insights) {

      research
              .append("- [")
              .append(insight.type())
              .append("] ")
              .append(insight.content())
              .append("\n");
    }

    return """
                You are a professional research report writer.

                Create a concise research report about:

                %s

                Use ONLY the information contained
                in the research insights.

                Do NOT invent facts.

                Format the response using Markdown.

                Use exactly these sections:

                ## Key Findings

                ## Statistics

                ## Definitions

                Rules:
                - Keep the report concise.
                - Use bullet points.
                - Do not add unsupported information.
                - Do not include sources.
                - Do not include an executive summary.
                - Return only the Markdown report.

                Research Insights:

                %s

                Detailed Report:
                """.formatted(
            topic,
            research
    );
  }

  /**
   * Fallback detailed report.
   *
   * This works even when HuggingFace is unavailable
   * or credits are exhausted.
   */
  private String generateFallbackDetailedReport(
          List<Insight> insights) {

    StringBuilder report =
            new StringBuilder();

    /*
     * Key Findings
     */
    report.append("## Key Findings\n\n");

    insights.stream()
            .filter(
                    i ->
                            "fact".equals(i.type())
            )
            .limit(5)
            .forEach(
                    i ->
                            report
                                    .append("- ")
                                    .append(i.content())
                                    .append("\n")
            );

    /*
     * Statistics
     */
    report.append("\n## Statistics\n\n");

    List<Insight> statistics =
            insights.stream()
                    .filter(
                            i ->
                                    "statistic"
                                            .equals(i.type())
                    )
                    .limit(5)
                    .toList();

    if (statistics.isEmpty()) {

      report.append(
              "No statistics found.\n"
      );

    } else {

      statistics.forEach(
              i ->
                      report
                              .append("- ")
                              .append(i.content())
                              .append("\n")
      );
    }

    /*
     * Definitions
     */
    report.append("\n## Definitions\n\n");

    List<Insight> definitions =
            insights.stream()
                    .filter(
                            i ->
                                    "definition"
                                            .equals(i.type())
                    )
                    .limit(5)
                    .toList();

    if (definitions.isEmpty()) {

      report.append(
              "No definitions found.\n"
      );

    } else {

      definitions.forEach(
              i ->
                      report
                              .append("- ")
                              .append(i.content())
                              .append("\n")
      );
    }

    return report.toString();
  }

  /**
   * Fallback executive summary.
   */
  private String generateFallbackSummary(
          String topic,
          List<Insight> insights) {

    if (insights == null
            || insights.isEmpty()) {

      return "No summary available for topic: "
              + topic;
    }

    StringBuilder summary =
            new StringBuilder();

    summary
            .append(topic)
            .append(" can be understood through ")
            .append(insights.size())
            .append(" key findings. ");

    insights.stream()
            .limit(2)
            .forEach(
                    insight ->
                            summary
                                    .append(
                                            insight.content()
                                    )
                                    .append(" ")
            );

    return summary.toString().trim();
  }

  /**
   * Remove accidental markdown code fences.
   */
  private String cleanLLMResponse(
          String response) {

    if (response == null) {
      return "";
    }

    return response
            .replace("```markdown", "")
            .replace("```text", "")
            .replace("```", "")
            .trim();
  }
}
