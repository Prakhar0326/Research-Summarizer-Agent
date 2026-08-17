package com.research.summarizer.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.research.summarizer.model.AgentPipelineContext;
import com.research.summarizer.model.Insight;
import com.research.summarizer.model.SearchResult;
import com.research.summarizer.service.HuggingFaceLLMService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class InsightExtractorAgent {

  private final HuggingFaceLLMService llmService;
  private final ObjectMapper objectMapper;

  private static final int MAX_INSIGHTS = 5;

  /*
   * Don't send huge Tavily raw content to the LLM.
   */
  private static final int MAX_CONTENT_LENGTH = 3000;

  public AgentPipelineContext execute(
          AgentPipelineContext context) {

    List<SearchResult> searchResults =
            context.getRawSearchResults();

    log.info(
            "Insight Extractor Agent processing {} search results",
            searchResults != null
                    ? searchResults.size()
                    : 0
    );

    List<Insight> insights =
            extractInsights(searchResults);

    context.setExtractedInsights(insights);

    log.info(
            "Insight Extractor Agent completed. Extracted {} insights",
            insights.size()
    );

    return context;
  }

  /**
   * Extract insights from all search results.
   */
  private List<Insight> extractInsights(
          List<SearchResult> searchResults) {

    if (searchResults == null
            || searchResults.isEmpty()) {

      return new ArrayList<>();
    }

    /*
     * One LLM call for all search results.
     * This is much cheaper than one call per result.
     */
    List<Insight> llmInsights =
            extractInsightsWithLLM(searchResults);

    if (!llmInsights.isEmpty()) {

      llmInsights.sort(
              (a, b) ->
                      Double.compare(
                              b.confidence(),
                              a.confidence()
                      )
      );

      return llmInsights;
    }

    /*
     * If LLM fails, use snippets rather than entire
     * webpage content.
     */
    log.warn(
            "Could not extract structured insights from LLM response. " +
                    "Using search-result fallback."
    );

    return createFallbackInsights(searchResults);
  }

  /**
   * Call LLM once using all search results.
   */
  private List<Insight> extractInsightsWithLLM(
          List<SearchResult> searchResults) {

    List<Insight> insights =
            new ArrayList<>();

    try {

      String prompt =
              buildExtractionPrompt(searchResults);

      log.debug(
              "Insight extraction prompt length: {} characters",
              prompt.length()
      );

      String llmResponse =
              llmService.generateText(prompt);

      if (llmResponse == null
              || llmResponse.isBlank()) {

        log.warn(
                "LLM returned empty response for insight extraction"
        );

        return insights;
      }

      log.info(
              "LLM returned insight response of {} characters",
              llmResponse.length()
      );

      log.debug(
              "LLM insight response: {}",
              llmResponse
      );

      String json =
              extractJsonArray(llmResponse);

      if (json == null) {

        log.warn(
                "No valid JSON array found in LLM response"
        );

        return insights;
      }

      JsonNode responseArray =
              objectMapper.readTree(json);

      if (!responseArray.isArray()) {

        log.warn(
                "LLM response is not a JSON array"
        );

        return insights;
      }

      for (JsonNode item : responseArray) {

        if (insights.size() >= MAX_INSIGHTS) {
          break;
        }

        String type =
                item.path("type")
                        .asText("")
                        .trim();

        String content =
                item.path("content")
                        .asText("")
                        .trim();

        if (type.isEmpty()
                || content.isEmpty()) {

          continue;
        }

        if (!isValidType(type)) {
          continue;
        }

        insights.add(
                new Insight(
                        type,
                        content,
                        "",
                        getConfidenceForType(type)
                )
        );
      }

    } catch (Exception e) {

      log.warn(
              "Error parsing LLM insight response: {}",
              e.getMessage()
      );
    }

    return insights;
  }

  /**
   * Build a small prompt.
   */
  private String buildExtractionPrompt(
          List<SearchResult> searchResults) {

    StringBuilder sources =
            new StringBuilder();

    int sourceNumber = 1;

    for (SearchResult result : searchResults) {

      String content =
              result.content();

      if (content == null
              || content.isBlank()) {

        content = result.snippet();
      }

      if (content == null) {
        content = "";
      }

      /*
       * Prevent enormous Tavily content from
       * entering the LLM prompt.
       */
      if (content.length() > MAX_CONTENT_LENGTH) {

        content =
                content.substring(
                        0,
                        MAX_CONTENT_LENGTH
                );
      }

      sources
              .append("\nSOURCE ")
              .append(sourceNumber++)
              .append("\n");

      sources
              .append("Title: ")
              .append(result.title())
              .append("\n");

      sources
              .append("URL: ")
              .append(result.url())
              .append("\n");

      sources
              .append("Content: ")
              .append(content)
              .append("\n");
    }

    return """
                You are a research analyst.

                Analyze the web search results below.

                Extract EXACTLY 5 important facts.

                Rules:
                1. Use ONLY information explicitly present in the sources.
                2. Do NOT invent information.
                3. Each insight must be concise.
                4. Each content value must be less than 200 characters.
                5. type must be one of:
                   - fact
                   - statistic
                   - definition
                6. Return ONLY valid JSON.
                7. Do NOT use markdown.
                8. Do NOT add explanations.

                Required format:

                [
                  {
                    "type": "fact",
                    "content": "..."
                  }
                ]

                Web Search Results:
                """ + sources;
  }

  /**
   * Extract JSON array.
   *
   * Handles normal responses and some markdown-wrapped
   * responses.
   */
  private String extractJsonArray(
          String response) {

    if (response == null
            || response.isBlank()) {

      return null;
    }

    String cleaned =
            response
                    .replace("```json", "")
                    .replace("```", "")
                    .trim();

    int start =
            cleaned.indexOf('[');

    int end =
            cleaned.lastIndexOf(']');

    /*
     * Normal valid JSON.
     */
    if (start >= 0
            && end > start) {

      String json =
              cleaned.substring(
                      start,
                      end + 1
              );

      try {

        JsonNode node =
                objectMapper.readTree(json);

        if (node.isArray()) {
          return json;
        }

      } catch (Exception ignored) {
        log.debug(
                "Extracted JSON array is malformed"
        );
      }
    }

    /*
     * LLM may have stopped because of token limit.
     * Try to recover complete objects before the
     * truncation point.
     */
    if (start >= 0) {

      String partial =
              cleaned.substring(start);

      int lastCompleteObject =
              partial.lastIndexOf("}");

      if (lastCompleteObject > 0) {

        String recovered =
                partial.substring(
                        0,
                        lastCompleteObject + 1
                ) + "]";

        try {

          JsonNode node =
                  objectMapper.readTree(
                          recovered
                  );

          if (node.isArray()) {

            log.warn(
                    "Recovered truncated JSON response"
            );

            return recovered;
          }

        } catch (Exception ignored) {
          log.debug(
                  "Unable to recover truncated JSON"
          );
        }
      }
    }

    return null;
  }

  /**
   * Fallback when LLM fails.
   *
   * IMPORTANT:
   * Use snippet, NOT entire webpage content.
   */
  private List<Insight> createFallbackInsights(
          List<SearchResult> searchResults) {

    List<Insight> insights =
            new ArrayList<>();

    for (SearchResult result : searchResults) {

      if (insights.size() >= MAX_INSIGHTS) {
        break;
      }

      String content =
              result.snippet();

      if (content == null
              || content.isBlank()) {

        content = result.content();
      }

      if (content == null
              || content.isBlank()) {

        continue;
      }

      if (content.length() > 500) {

        content =
                content.substring(0, 500)
                        + "...";
      }

      insights.add(
              new Insight(
                      "fact",
                      content.trim(),
                      result.url(),
                      0.70
              )
      );
    }

    log.info(
            "Created {} fallback insights from search results",
            insights.size()
    );

    return insights;
  }

  private boolean isValidType(String type) {

    return "fact".equals(type)
            || "statistic".equals(type)
            || "definition".equals(type);
  }

  private double getConfidenceForType(
          String type) {

    return switch (type) {

      case "statistic" -> 0.90;

      case "fact" -> 0.85;

      case "definition" -> 0.80;

      default -> 0.75;
    };
  }
}
