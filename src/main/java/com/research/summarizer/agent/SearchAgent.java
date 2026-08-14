package com.research.summarizer.agent;

import com.research.summarizer.model.AgentPipelineContext;
import com.research.summarizer.model.SearchResult;
import com.research.summarizer.service.MCPService;
import com.research.summarizer.service.WebSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Agent 1: Search Agent
 * 
 * Responsibility: Receive the user query. Determine if it is OpenAI
 * developer-related.
 * If yes, query the OpenAI Docs MCP server first. If the MCP response is
 * insufficient
 * or empty, fall back to a general web search tool. Return raw search results
 * to Agent 2.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SearchAgent {


  private final MCPService mcpService;
  private final WebSearchService webSearchService;

  /**
   * OpenAI-related keywords for query classification
   */
  private static final String[] OPENAI_KEYWORDS = {
      "openai", "gpt", "api", "chat completion", "embedding",
      "fine-tuning", "fine tuning", "agents sdk", "agents", "responses api", "responses",
      "models", "sdk", "function calling", "retrieval"
  };

  /**
   * Execute the search agent to find raw search results
   */
  public AgentPipelineContext execute(AgentPipelineContext context) {
    log.info("Search Agent processing topic: {}", context.getTopic());

    var topic = context.getTopic();
    var isOpenAIRelated = isOpenAIRelated(topic);

    log.debug("Query classified as OpenAI-related: {}", isOpenAIRelated);

    List<SearchResult> results;
    String searchSource;

    if (isOpenAIRelated) {
      log.info("Routing query to OpenAI Docs MCP server");
      results = mcpService.searchOpenAIDocs(topic);
      searchSource = "MCP";

      // Fall back to web search if MCP results are insufficient
      if (results == null || results.isEmpty()) {
        log.warn("MCP server returned no results, falling back to web search");
        results = webSearchService.search(topic);
        searchSource = "WEB";
      }
    } else {
      log.info("Routing query to general web search");
      results = webSearchService.search(topic);
      searchSource = "WEB";
    }

    context.setRawSearchResults(results);
    context.setSearchSource(searchSource);

    // Limit results to maxSources
    int maxSources = context.getMaxSources();
    if (results != null && results.size() > maxSources) {
      log.info("Limiting results from {} to {} (maxSources)", results.size(), maxSources);
      context.setRawSearchResults(results.subList(0, maxSources));
    }

    log.info("Search Agent completed. Found {} results from {}",
        context.getRawSearchResults() != null ? context.getRawSearchResults().size() : 0, searchSource);

    return context;
  }

  /**
   * Determine if a query is OpenAI developer-related
   * 
   * Decision logic:
   * - Check if query contains OpenAI-specific keywords
   * - Classification is deterministic and case-insensitive
   */
  private boolean isOpenAIRelated(String topic) {
    var lowerTopic = topic.toLowerCase();

    for (var keyword : OPENAI_KEYWORDS) {
      if (lowerTopic.contains(keyword)) {
        log.debug("Query contains OpenAI keyword: {}", keyword);
        return true;
      }
    }

    return false;
  }
}
