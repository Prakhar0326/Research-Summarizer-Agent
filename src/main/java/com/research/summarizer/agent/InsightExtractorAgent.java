//package com.research.summarizer.agent;
//
//import com.fasterxml.jackson.databind.JsonNode;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.research.summarizer.model.AgentPipelineContext;
//import com.research.summarizer.model.Insight;
//import com.research.summarizer.model.SearchResult;
//import com.research.summarizer.service.HuggingFaceLLMService;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.stereotype.Component;
//
//import java.util.ArrayList;
//import java.util.List;
//
///**
// * Agent 2: Insight Extractor
// *
// * Responsibility: Receive raw search results from Agent 1. Extract key facts,
// * statistics, definitions, and notable quotes using LLM. Return a structured
// * list of insights to Agent 3.
// *
// * Uses HuggingFace Inference API for LLM-based extraction
// */
//@Slf4j
//@Component
//@RequiredArgsConstructor
//public class InsightExtractorAgent {
//
//  private final HuggingFaceLLMService llmService;
//  private final ObjectMapper objectMapper;
//
//  /**
//   * Execute the insight extractor agent
//   */
//  public AgentPipelineContext execute(AgentPipelineContext context) {
//    log.info("Insight Extractor Agent processing {} search results",
//        context.getRawSearchResults() != null ? context.getRawSearchResults().size() : 0);
//
//    var insights = new ArrayList<Insight>();
//
//    if (context.getRawSearchResults() != null && !context.getRawSearchResults().isEmpty()) {
//      insights = extractInsights(context.getRawSearchResults());
//    }
//
//    context.setExtractedInsights(insights);
//
//    log.info("Insight Extractor Agent completed. Extracted {} insights", insights.size());
//
//    return context;
//  }
//
//  /**
//   * Extract insights from raw search results using LLM
//   */
//  private ArrayList<Insight> extractInsights(List<SearchResult> searchResults) {
//    var insights = new ArrayList<Insight>();
//
//    for (var result : searchResults) {
//      log.debug("Extracting insights from result: {}", result.title());
//
//      // Use LLM to extract insights from each result
//      var extractedInsights = extractInsightsWithLLM(result);
//      insights.addAll(extractedInsights);
//    }
//
//    // Sort insights by confidence score (highest first)
//    insights.sort((a, b) -> Double.compare(b.confidence(), a.confidence()));
//
//    log.debug("Total insights extracted: {}", insights.size());
//    return insights;
//  }
//
//  /**
//   * Use LLM to extract structured insights from search result
//   */
//  private List<Insight> extractInsightsWithLLM(SearchResult result) {
//    var insights = new ArrayList<Insight>();
//
//    try {
//      // Build the prompt for LLM
//      String prompt = buildExtractionPrompt(result);
//
//      log.debug("Calling LLM for insight extraction. Result: {}", result.title());
//
//      // Call HuggingFace LLM
//      String llmResponse = llmService.generateText(prompt);
//
//      if (llmResponse == null || llmResponse.isEmpty()) {
//        log.warn("LLM returned empty response for result: {}", result.title());
//        return insights;
//      }
//
//      log.debug("LLM Response length: {} chars", llmResponse.length());
//
//      // Parse LLM response to extract insights
//      insights = parseLLMResponse(llmResponse, result.url());
//
//    } catch (Exception e) {
//      log.error("Error extracting insights with LLM: {}", e.getMessage(), e);
//      // Fallback: return empty insights
//      return new ArrayList<>();
//    }
//
//    return insights;
//  }
//
//  /**
//   * Build extraction prompt for the LLM
//   */
//  private String buildExtractionPrompt(SearchResult result) {
//    return String.format(
//        """
//        Extract insights from the following text. Return a JSON array with objects containing:
//        - "type": one of "fact", "statistic", or "definition"
//        - "content": the extracted insight text
//
//        Text to analyze:
//        Title: %s
//        Content: %s
//
//        Return ONLY valid JSON array, no other text.
//        Example format: [{"type":"fact","content":"..."},{"type":"statistic","content":"..."}]
//        """,
//        result.title(),
//        result.content());
//  }
//
//  /**
//   * Parse LLM response and convert to Insight objects
//   */
//  private ArrayList<Insight> parseLLMResponse(String llmResponse, String url) {
//    var insights = new ArrayList<Insight>();
//
//    try {
//      // Extract JSON from LLM response (it might have extra text)
//      String jsonString = extractJsonFromResponse(llmResponse);
//
//      if (jsonString == null || jsonString.isEmpty()) {
//        log.debug("No JSON found in LLM response");
//        return insights;
//      }
//
//      // Parse JSON array
//      JsonNode responseArray = objectMapper.readTree(jsonString);
//
//      if (!responseArray.isArray()) {
//        log.warn("LLM response is not a JSON array");
//        return insights;
//      }
//
//      // Convert each item to Insight
//      for (JsonNode item : responseArray) {
//        String type = item.path("type").asText("");
//        String content = item.path("content").asText("");
//
//        if (!type.isEmpty() && !content.isEmpty()) {
//          double confidence = getConfidenceForType(type);
//          insights.add(new Insight(type, content, url, confidence));
//          log.debug("Extracted {} insight: {}", type, content.substring(0, Math.min(50, content.length())));
//        }
//      }
//
//    } catch (Exception e) {
//      log.warn("Error parsing LLM response: {}", e.getMessage());
//    }
//
//    return insights;
//  }
//
//  /**
//   * Extract JSON from LLM response (handles cases where LLM adds extra text)
//   */
//  private String extractJsonFromResponse(String response) {
//    int startIdx = response.indexOf('[');
//    int endIdx = response.lastIndexOf(']');
//
//    if (startIdx >= 0 && endIdx > startIdx) {
//      return response.substring(startIdx, endIdx + 1);
//    }
//
//    return null;
//  }
//
//  /**
//   * Get confidence score based on insight type
//   */
//  private double getConfidenceForType(String type) {
//    return switch (type) {
//      case "fact" -> 0.85;
//      case "statistic" -> 0.90;
//      case "definition" -> 0.80;
//      default -> 0.75;
//    };
//  }
//}

//package com.research.summarizer.agent;
//
//import com.fasterxml.jackson.databind.JsonNode;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.research.summarizer.model.AgentPipelineContext;
//import com.research.summarizer.model.Insight;
//import com.research.summarizer.model.SearchResult;
//import com.research.summarizer.service.HuggingFaceLLMService;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.stereotype.Component;
//
//import java.util.ArrayList;
//import java.util.List;
//
///**
// * Agent 2: Insight Extractor
// *
// * Responsibility:
// * - Receive raw search results from Agent 1
// * - Send relevant search content to the LLM
// * - Extract facts, statistics and definitions
// * - Convert LLM response into structured Insight objects
// *
// * Uses HuggingFace Inference API for LLM-based extraction.
// */
//@Slf4j
//@Component
//@RequiredArgsConstructor
//public class InsightExtractorAgent {
//
//  private final HuggingFaceLLMService llmService;
//  private final ObjectMapper objectMapper;
//
//  /**
//   * Maximum number of search results sent to the LLM.
//   */
//  private static final int MAX_RESULTS_FOR_LLM = 5;
//
//  /**
//   * Maximum amount of content taken from one search result.
//   *
//   * Tavily can return a large amount of raw page content.
//   * Sending the entire webpage to the LLM is unnecessary.
//   */
//  private static final int MAX_CONTENT_LENGTH = 6000;
//
//  /**
//   * Execute the Insight Extractor Agent.
//   */
//  public AgentPipelineContext execute(
//          AgentPipelineContext context) {
//
//    List<SearchResult> searchResults =
//            context.getRawSearchResults();
//
//    log.info(
//            "Insight Extractor Agent processing {} search results",
//            searchResults != null
//                    ? searchResults.size()
//                    : 0
//    );
//
//    List<Insight> insights =
//            new ArrayList<>();
//
//    /*
//     * No search results.
//     */
//    if (searchResults == null ||
//            searchResults.isEmpty()) {
//
//      log.warn(
//              "No search results available for insight extraction"
//      );
//
//      context.setExtractedInsights(insights);
//
//      return context;
//    }
//
//    try {
//
//      insights =
//              extractInsights(searchResults);
//
//    } catch (Exception e) {
//
//      log.error(
//              "Error extracting insights: {}",
//              e.getMessage(),
//              e
//      );
//    }
//
//    context.setExtractedInsights(insights);
//
//    log.info(
//            "Insight Extractor Agent completed. Extracted {} insights",
//            insights.size()
//    );
//
//    return context;
//  }
//
//  /**
//   * Extract insights from multiple search results
//   * using one LLM request.
//   */
//  private List<Insight> extractInsights(
//          List<SearchResult> searchResults) {
//
//    List<Insight> insights =
//            new ArrayList<>();
//
//    /*
//     * Only use the top 5 results.
//     */
//    List<SearchResult> resultsForLLM =
//            searchResults.stream()
//                    .limit(MAX_RESULTS_FOR_LLM)
//                    .toList();
//
//    String prompt =
//            buildExtractionPrompt(resultsForLLM);
//
//    log.info(
//            "Calling LLM for insight extraction using {} search results",
//            resultsForLLM.size()
//    );
//
//    log.debug(
//            "Insight extraction prompt length: {} characters",
//            prompt.length()
//    );
//
//    /*
//     * Call HuggingFace.
//     */
//    String llmResponse =
//            llmService.generateText(prompt);
//
//    if (llmResponse == null ||
//            llmResponse.isBlank()) {
//
//      log.warn(
//              "LLM returned empty response during insight extraction"
//      );
//
//      /*
//       * Use actual search results as fallback.
//       */
//      return createFallbackInsights(
//              resultsForLLM
//      );
//    }
//
//    log.info(
//            "LLM returned insight response of {} characters",
//            llmResponse.length()
//    );
//
//    log.debug(
//            "LLM insight response: {}",
//            llmResponse.substring(
//                    0,
//                    Math.min(2000, llmResponse.length())
//            )
//    );
//
//    /*
//     * Parse the LLM JSON.
//     */
//    insights =
//            parseLLMResponse(
//                    llmResponse,
//                    resultsForLLM
//            );
//
//    /*
//     * If parsing failed, don't leave the
//     * pipeline empty.
//     */
//    if (insights.isEmpty()) {
//
//      log.warn(
//              "Could not extract structured insights from LLM response. " +
//                      "Using search-result fallback."
//      );
//
//      insights =
//              createFallbackInsights(
//                      resultsForLLM
//              );
//    }
//
//    /*
//     * Highest confidence first.
//     */
//    insights.sort(
//            (a, b) ->
//                    Double.compare(
//                            b.confidence(),
//                            a.confidence()
//                    )
//    );
//
//    return insights;
//  }
//
//  /**
//   * Build the LLM prompt.
//   */
//  private String buildExtractionPrompt(
//          List<SearchResult> searchResults) {
//
//    StringBuilder sources =
//            new StringBuilder();
//
//    int sourceNumber = 1;
//
//    for (SearchResult result :
//            searchResults) {
//
//      String content =
//              result.content();
//
//      /*
//       * Some SearchResult implementations may
//       * have empty content but a populated snippet.
//       */
//      if (content == null ||
//              content.isBlank()) {
//
//        content = result.snippet();
//      }
//
//      if (content == null) {
//        content = "";
//      }
//
//      /*
//       * Prevent very large webpage content
//       * from being sent to the LLM.
//       */
//      if (content.length() >
//              MAX_CONTENT_LENGTH) {
//
//        content =
//                content.substring(
//                        0,
//                        MAX_CONTENT_LENGTH
//                ) + "...";
//      }
//
//      sources.append(
//                      "SOURCE "
//              ).append(sourceNumber)
//              .append("\n");
//
//      sources.append(
//              "TITLE: "
//      ).append(
//              safeString(result.title())
//      ).append("\n");
//
//      sources.append(
//              "URL: "
//      ).append(
//              safeString(result.url())
//      ).append("\n");
//
//      sources.append(
//                      "CONTENT:\n"
//              ).append(content)
//              .append("\n\n");
//
//      sourceNumber++;
//    }
//
//    return """
//                You are a research analyst.
//
//                Analyze the following web search results.
//
//                Extract important information that is explicitly
//                supported by the provided sources.
//
//                Do NOT invent facts.
//
//                Return ONLY a valid JSON array.
//
//                Each object must contain exactly these fields:
//
//                {
//                  "type": "fact",
//                  "content": "..."
//                }
//
//                The "type" must be one of:
//
//                - fact
//                - statistic
//                - definition
//
//                Rules:
//
//                1. Extract important facts about the topic.
//                2. Extract statistics only when an actual number,
//                   date, record, or measurable value is present.
//                3. Extract definitions when something is explicitly defined.
//                4. Do not make up information.
//                5. Do not include markdown.
//                6. Do not include ```json.
//                7. Return ONLY the JSON array.
//                8. Extract approximately 5-10 useful insights.
//                9. Keep each insight concise.
//                10. Do not repeat the same information.
//
//                Example response:
//
//                [
//                  {
//                    "type": "fact",
//                    "content": "MS Dhoni made his international debut in 2004."
//                  },
//                  {
//                    "type": "statistic",
//                    "content": "Dhoni led India in 331 international matches."
//                  }
//                ]
//
//                SEARCH RESULTS:
//
//                %s
//                """.formatted(sources);
//  }
//
//  /**
//   * Parse the JSON returned by the LLM.
//   */
//  private List<Insight> parseLLMResponse(
//          String llmResponse,
//          List<SearchResult> searchResults) {
//
//    List<Insight> insights =
//            new ArrayList<>();
//
//    try {
//
//      String jsonString =
//              extractJsonFromResponse(
//                      llmResponse
//              );
//
//      if (jsonString == null ||
//              jsonString.isBlank()) {
//
//        log.warn(
//                "No JSON array found in LLM response"
//        );
//
//        return insights;
//      }
//
//      JsonNode responseArray =
//              objectMapper.readTree(
//                      jsonString
//              );
//
//      if (!responseArray.isArray()) {
//
//        log.warn(
//                "LLM response is not a JSON array"
//        );
//
//        return insights;
//      }
//
//      /*
//       * Current Insight model contains one source URL.
//       *
//       * Since the LLM prompt doesn't return source numbers,
//       * use the first source as the default URL.
//       */
//      String sourceUrl =
//              searchResults.isEmpty()
//                      ? ""
//                      : safeString(
//                      searchResults
//                              .get(0)
//                              .url()
//              );
//
//      for (JsonNode item :
//              responseArray) {
//
//        String type =
//                item.path("type")
//                        .asText("")
//                        .trim()
//                        .toLowerCase();
//
//        String content =
//                item.path("content")
//                        .asText("")
//                        .trim();
//
//        if (content.isBlank()) {
//          continue;
//        }
//
//        if (!isValidType(type)) {
//
//          log.debug(
//                  "Ignoring invalid insight type: {}",
//                  type
//          );
//
//          continue;
//        }
//
//        double confidence =
//                getConfidenceForType(type);
//
//        insights.add(
//                new Insight(
//                        type,
//                        content,
//                        sourceUrl,
//                        confidence
//                )
//        );
//
//        log.debug(
//                "Extracted {} insight: {}",
//                type,
//                content.substring(
//                        0,
//                        Math.min(
//                                100,
//                                content.length()
//                        )
//                )
//        );
//      }
//
//    } catch (Exception e) {
//
//      log.warn(
//              "Error parsing LLM response: {}",
//              e.getMessage()
//      );
//    }
//
//    return insights;
//  }
//
//  /**
//   * Extract JSON array from the model response.
//   *
//   * Handles:
//   *
//   * [
//   *   {...}
//   * ]
//   *
//   * and:
//   *
//   * ```json
//   * [...]
//   * ```
//   */
//  private String extractJsonFromResponse(
//          String response) {
//
//    if (response == null) {
//      return null;
//    }
//
//    String cleaned =
//            response
//                    .replace(
//                            "```json",
//                            ""
//                    )
//                    .replace(
//                            "```JSON",
//                            ""
//                    )
//                    .replace(
//                            "```",
//                            ""
//                    )
//                    .trim();
//
//    int startIndex =
//            cleaned.indexOf('[');
//
//    int endIndex =
//            cleaned.lastIndexOf(']');
//
//    if (startIndex >= 0 &&
//            endIndex > startIndex) {
//
//      return cleaned.substring(
//              startIndex,
//              endIndex + 1
//      );
//    }
//
//    return null;
//  }
//
//  /**
//   * Validate insight type.
//   */
//  private boolean isValidType(
//          String type) {
//
//    return "fact".equals(type)
//            || "statistic".equals(type)
//            || "definition".equals(type);
//  }
//
//  /**
//   * Get confidence based on insight type.
//   */
//  private double getConfidenceForType(
//          String type) {
//
//    return switch (type) {
//
//      case "statistic" -> 0.90;
//
//      case "fact" -> 0.85;
//
//      case "definition" -> 0.80;
//
//      default -> 0.75;
//    };
//  }
//
//  /**
//   * Fallback when the LLM is unavailable or
//   * returns invalid JSON.
//   *
//   * This uses actual Tavily search content.
//   * No fake information is generated.
//   */
//  private List<Insight> createFallbackInsights(
//          List<SearchResult> searchResults) {
//
//    List<Insight> insights =
//            new ArrayList<>();
//
//    for (SearchResult result :
//            searchResults) {
//
//      String content =
//              result.content();
//
//      if (content == null ||
//              content.isBlank()) {
//
//        content =
//                result.snippet();
//      }
//
//      if (content == null ||
//              content.isBlank()) {
//
//        continue;
//      }
//
//      /*
//       * Keep fallback insight reasonably small.
//       */
//      if (content.length() > 1000) {
//
//        content =
//                content.substring(
//                        0,
//                        1000
//                ) + "...";
//      }
//
//      insights.add(
//              new Insight(
//                      "fact",
//                      content,
//                      safeString(result.url()),
//                      0.70
//              )
//      );
//    }
//
//    log.info(
//            "Created {} fallback insights from search results",
//            insights.size()
//    );
//
//    return insights;
//  }
//
//  /**
//   * Prevent null values from appearing in prompts.
//   */
//  private String safeString(
//          String value) {
//
//    return value == null
//            ? ""
//            : value;
//  }
//}

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
