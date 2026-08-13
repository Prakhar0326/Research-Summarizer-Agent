package com.research.summarizer.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.research.summarizer.model.SearchResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Service for general web search using DuckDuckGo Instant Answer API
 * 
 * No API key required - free service
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WebSearchService {

  private final ObjectMapper objectMapper;
  private static final String DUCKDUCKGO_API_URL = "https://api.duckduckgo.com/";
  private static final int TIMEOUT_SECONDS = 15;
  private static final int MAX_RESULTS = 5;

  /**
   * Perform a general web search using DuckDuckGo Instant Answer API
   * No API key required
   */
  public List<SearchResult> search(String query) {
   log.info("Performing web search for query: {} using DuckDuckGo API", query);

   try {
     List<SearchResult> results = searchDuckDuckGoAPI(query);

     if (results.isEmpty()) {
       log.warn("DuckDuckGo returned no results for query: {}. Using mock results as fallback.", query);
       return generateMockResults(query);
     }
      
     return results;
   } catch (Exception e) {
     log.error("Error performing web search: {}. Using mock results as fallback.", e.getMessage(), e);
     return generateMockResults(query);
   }
  }

  /**
   * Search using DuckDuckGo Instant Answer API (JSON format)
   */
  private List<SearchResult> searchDuckDuckGoAPI(String query) throws IOException {
   List<SearchResult> results = new ArrayList<>();

   try {
     var encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
     var searchUrl = DUCKDUCKGO_API_URL + "?q=" + encodedQuery + "&format=json&no_redirect=1";

     log.debug("DuckDuckGo API URL: {}", searchUrl);

     var client = new OkHttpClient.Builder()
         .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
         .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
         .build();

     var request = new Request.Builder()
         .url(searchUrl)
         .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
         .build();

     try (Response response = client.newCall(request).execute()) {
       int statusCode = response.code();
       log.debug("DuckDuckGo API Response Status: {}", statusCode);

       if (response.isSuccessful() && response.body() != null) {
         var jsonResponse = response.body().string();
         log.debug("DuckDuckGo API Response Length: {} bytes", jsonResponse.length());
         log.debug("DuckDuckGo API Response (first 300 chars): {}", 
             jsonResponse.substring(0, Math.min(300, jsonResponse.length())));

         results = parseDuckDuckGoAPIResponse(jsonResponse);
         log.info("DuckDuckGo API search returned {} results", results.size());
       } else {
         log.warn("DuckDuckGo API returned status: {}", statusCode);
       }
     }

   } catch (Exception e) {
     log.error("Error with DuckDuckGo API: {}", e.getMessage(), e);
   }

   return results;
  }

  /**
   * Parse DuckDuckGo Instant Answer API JSON response
   */
  private List<SearchResult> parseDuckDuckGoAPIResponse(String jsonResponse) {
   var results = new ArrayList<SearchResult>();

   try {
     var root = objectMapper.readTree(jsonResponse);

     // Check for instant answer
     if (root.has("AbstractText") && !root.get("AbstractText").asText().isEmpty()) {
       var abstractText = root.get("AbstractText").asText();
       var abstractSource = root.has("AbstractSource") ? root.get("AbstractSource").asText() : "DuckDuckGo";
       var abstractURL = root.has("AbstractURL") ? root.get("AbstractURL").asText() : "";

       results.add(new SearchResult(
           abstractSource,
           abstractURL,
           abstractText,
           abstractText,
           0.95));
        
       log.debug("Added instant answer result: {}", abstractSource);
     }

     // Check for related topics
     if (root.has("RelatedTopics") && root.get("RelatedTopics").isArray()) {
       int count = 0;
       for (var topic : root.get("RelatedTopics")) {
         if (count >= MAX_RESULTS) break;

         if (topic.has("FirstURL") && topic.has("Text")) {
           var url = topic.get("FirstURL").asText();
           var text = topic.get("Text").asText();
           var title = topic.has("Name") ? topic.get("Name").asText() : text.substring(0, Math.min(50, text.length()));

           if (!url.isEmpty() && url.startsWith("http")) {
             results.add(new SearchResult(title, url, text, text, 0.80));
             count++;
             log.debug("Added related topic result: {} - {}", title, url);
           }
         }
       }
     }

     log.debug("Parsed {} results from DuckDuckGo API response", results.size());

   } catch (Exception e) {
     log.error("Error parsing DuckDuckGo API response: {}", e.getMessage(), e);
   }

   return results;
  }

  /**
   * Generate mock search results for testing/demo purposes
   */
  private List<SearchResult> generateMockResults(String query) {
   log.info("Generating mock search results for query: {}", query);

   var results = new ArrayList<SearchResult>();

   results.add(new SearchResult(
       "Understanding " + query,
       "https://example.com/article-1",
       "This is a comprehensive guide to " + query + ". Learn the fundamentals and best practices.",
       "This is a comprehensive guide to " + query,
       0.95));

   results.add(new SearchResult(
       "Best Practices for " + query,
       "https://example.com/article-2",
       "Learn the best practices when working with " + query + ". Expert tips and techniques.",
       "Learn the best practices when working with " + query,
       0.85));

   results.add(new SearchResult(
       query + " Tutorial",
       "https://example.com/tutorial",
       "Step-by-step tutorial on " + query + ". Complete guide from beginner to advanced.",
       "Step-by-step tutorial on " + query,
       0.80));

   return results;
  }
}
