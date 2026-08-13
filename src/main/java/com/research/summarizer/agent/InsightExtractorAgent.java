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

/**
 * Agent 2: Insight Extractor
 * 
 * Responsibility: Receive raw search results from Agent 1. Extract key facts,
 * statistics, definitions, and notable quotes using LLM. Return a structured 
 * list of insights to Agent 3.
 * 
 * Uses HuggingFace Inference API for LLM-based extraction
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InsightExtractorAgent {

  private final HuggingFaceLLMService llmService;
  private final ObjectMapper objectMapper;

  /**
   * Execute the insight extractor agent
   */
  public AgentPipelineContext execute(AgentPipelineContext context) {
    log.info("Insight Extractor Agent processing {} search results",
        context.getRawSearchResults() != null ? context.getRawSearchResults().size() : 0);

    var insights = new ArrayList<Insight>();

    if (context.getRawSearchResults() != null && !context.getRawSearchResults().isEmpty()) {
      insights = extractInsights(context.getRawSearchResults());
    }

    context.setExtractedInsights(insights);

    log.info("Insight Extractor Agent completed. Extracted {} insights", insights.size());

    return context;
  }

  /**
   * Extract insights from raw search results using LLM
   */
  private ArrayList<Insight> extractInsights(List<SearchResult> searchResults) {
    var insights = new ArrayList<Insight>();

    for (var result : searchResults) {
      log.debug("Extracting insights from result: {}", result.title());
      
      // Use LLM to extract insights from each result
      var extractedInsights = extractInsightsWithLLM(result);
      insights.addAll(extractedInsights);
    }

    // Sort insights by confidence score (highest first)
    insights.sort((a, b) -> Double.compare(b.confidence(), a.confidence()));

    log.debug("Total insights extracted: {}", insights.size());
    return insights;
  }

  /**
   * Use LLM to extract structured insights from search result
   */
  private List<Insight> extractInsightsWithLLM(SearchResult result) {
    var insights = new ArrayList<Insight>();

    try {
      // Build the prompt for LLM
      String prompt = buildExtractionPrompt(result);
      
      log.debug("Calling LLM for insight extraction. Result: {}", result.title());
      
      // Call HuggingFace LLM
      String llmResponse = llmService.generateText(prompt);
      
      if (llmResponse == null || llmResponse.isEmpty()) {
        log.warn("LLM returned empty response for result: {}", result.title());
        return insights;
      }

      log.debug("LLM Response length: {} chars", llmResponse.length());

      // Parse LLM response to extract insights
      insights = parseLLMResponse(llmResponse, result.url());

    } catch (Exception e) {
      log.error("Error extracting insights with LLM: {}", e.getMessage(), e);
      // Fallback: return empty insights
      return new ArrayList<>();
    }

    return insights;
  }

  /**
   * Build extraction prompt for the LLM
   */
  private String buildExtractionPrompt(SearchResult result) {
    return String.format(
        """
        Extract insights from the following text. Return a JSON array with objects containing:
        - "type": one of "fact", "statistic", or "definition"
        - "content": the extracted insight text
        
        Text to analyze:
        Title: %s
        Content: %s
        
        Return ONLY valid JSON array, no other text.
        Example format: [{"type":"fact","content":"..."},{"type":"statistic","content":"..."}]
        """,
        result.title(),
        result.content());
  }

  /**
   * Parse LLM response and convert to Insight objects
   */
  private ArrayList<Insight> parseLLMResponse(String llmResponse, String url) {
    var insights = new ArrayList<Insight>();

    try {
      // Extract JSON from LLM response (it might have extra text)
      String jsonString = extractJsonFromResponse(llmResponse);
      
      if (jsonString == null || jsonString.isEmpty()) {
        log.debug("No JSON found in LLM response");
        return insights;
      }

      // Parse JSON array
      JsonNode responseArray = objectMapper.readTree(jsonString);
      
      if (!responseArray.isArray()) {
        log.warn("LLM response is not a JSON array");
        return insights;
      }

      // Convert each item to Insight
      for (JsonNode item : responseArray) {
        String type = item.path("type").asText("");
        String content = item.path("content").asText("");
        
        if (!type.isEmpty() && !content.isEmpty()) {
          double confidence = getConfidenceForType(type);
          insights.add(new Insight(type, content, url, confidence));
          log.debug("Extracted {} insight: {}", type, content.substring(0, Math.min(50, content.length())));
        }
      }

    } catch (Exception e) {
      log.warn("Error parsing LLM response: {}", e.getMessage());
    }

    return insights;
  }

  /**
   * Extract JSON from LLM response (handles cases where LLM adds extra text)
   */
  private String extractJsonFromResponse(String response) {
    int startIdx = response.indexOf('[');
    int endIdx = response.lastIndexOf(']');
    
    if (startIdx >= 0 && endIdx > startIdx) {
      return response.substring(startIdx, endIdx + 1);
    }
    
    return null;
  }

  /**
   * Get confidence score based on insight type
   */
  private double getConfidenceForType(String type) {
    return switch (type) {
      case "fact" -> 0.85;
      case "statistic" -> 0.90;
      case "definition" -> 0.80;
      default -> 0.75;
    };
  }
}
