package com.research.summarizer.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.research.summarizer.model.SearchResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Service for querying the OpenAI Docs MCP (Model Context Protocol) server
 * 
 * Implements JSON-RPC 2.0 protocol over HTTP with three steps:
 * 1. initialize - Handshake with protocol version
 * 2. notifications/initialized - Required follow-up
 * 3. tools/call with "search" tool - Execute the actual query
 * 
 * URL: https://developers.openai.com/mcp
 * Transport: JSON-RPC 2.0 over HTTP (supports both JSON and text/event-stream responses)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MCPService {

  private final ObjectMapper objectMapper;

  @Value("${mcp.server.url:https://developers.openai.com/mcp}")
  private String mcpServerUrl;

  @Value("${mcp.timeout:30000}")
  private long mcpTimeout;

  private String sessionId; // Captured from initialize response header
  private final AtomicInteger nextId = new AtomicInteger(1);
  private static final String MCP_USER_AGENT = "Research-Summarizer-Agent/1.0";
  private static final String PROTOCOL_VERSION = "2025-03-26";

  /**
   * Search the OpenAI Docs MCP server for documentation related to the query
   * Follows the JSON-RPC 2.0 protocol: initialize -> initialized notification -> tools/call
   */
  public List<SearchResult> searchOpenAIDocs(String query) {
    log.info("Querying OpenAI Docs MCP server with query: {}", query);

    var results = new ArrayList<SearchResult>();

    try {
      // Step 1: Initialize handshake
      initialize();

      // Step 2: Execute search tool
      String searchResponse = search(query);
      
      if (searchResponse != null && !searchResponse.isEmpty()) {
        results = (ArrayList<SearchResult>) parseSearchResponse(searchResponse, query);
        log.info("MCP server returned {} results", results.size());
      } else {
        log.warn("MCP server returned empty search response for query: {}", query);
      }

    } catch (Exception e) {
      log.error("Error querying MCP server: {}", e.getMessage(), e);
      return new ArrayList<>();
    }

    return results;
  }

  /**
   * Step 1: Initialize MCP connection
   */
  private void initialize() throws Exception {
    log.debug("Initializing MCP connection...");
    
    ObjectNode params = objectMapper.createObjectNode();
    params.put("protocolVersion", PROTOCOL_VERSION);
    params.putObject("capabilities").putObject("tools");
    
    ObjectNode clientInfo = params.putObject("clientInfo");
    clientInfo.put("name", "Research-Summarizer-Agent");
    clientInfo.put("version", "1.0.0");

    String requestBody = buildJsonRpcRequest("initialize", params);
    log.debug("MCP Initialize Request: {}", requestBody.substring(0, Math.min(200, requestBody.length())));

    HttpResponse response = postRequest(requestBody);
    
    log.info("MCP Initialize Response Status: {}", response.statusCode);
    log.debug("MCP Initialize Response (first 500 chars): {}", 
        response.body.substring(0, Math.min(500, response.body.length())));

    // Capture session ID from header if provided
    if (response.headers.containsKey("mcp-session-id")) {
      sessionId = response.headers.get("mcp-session-id");
      log.debug("Captured MCP Session ID: {}", sessionId);
    }

    // Step 2: Send initialized notification
    sendInitializedNotification();
  }

  /**
   * Step 2: Send required initialized notification (fire-and-forget)
   */
  private void sendInitializedNotification() throws Exception {
    log.debug("Sending initialized notification...");
    
    ObjectNode notification = objectMapper.createObjectNode();
    notification.put("jsonrpc", "2.0");
    notification.put("method", "notifications/initialized");
    notification.set("params", objectMapper.createObjectNode());

    String requestBody = objectMapper.writeValueAsString(notification);
    log.debug("MCP Initialized Notification: {}", requestBody);

    postRequest(requestBody);
    log.debug("Initialized notification sent");
  }

  /**
   * Step 3: Call the search tool
   */
  private String search(String question) throws Exception {
    log.debug("Calling MCP search tool with query: {}", question);
    
    ObjectNode params = objectMapper.createObjectNode();
    params.put("name", "search_openai_docs");
    
    ObjectNode arguments = params.putObject("arguments");
    arguments.put("query", question);

    String requestBody = buildJsonRpcRequest("tools/call", params);
    log.debug("MCP Search Request: {}", requestBody.substring(0, Math.min(200, requestBody.length())));

    HttpResponse response = postRequest(requestBody);
    
    log.info("MCP Search Response Status: {}", response.statusCode);
    log.debug("MCP Search Response Length: {} bytes", response.body.length());
    log.debug("MCP Search Response (first 800 chars): {}", 
        response.body.substring(0, Math.min(800, response.body.length())));

    // Parse JSON-RPC response
    JsonNode result = parseJsonRpcResult(response);
    
    // Extract text content from result
    StringBuilder text = new StringBuilder();
    if (result.path("content").isArray()) {
      for (JsonNode block : result.path("content")) {
        if ("text".equals(block.path("type").asText())) {
          text.append(block.path("text").asText()).append("\n");
        }
      }
    }
    
    return text.toString();
  }

  /**
   * Build a JSON-RPC 2.0 request
   */
  private String buildJsonRpcRequest(String method, ObjectNode params) throws Exception {
    ObjectNode request = objectMapper.createObjectNode();
    request.put("jsonrpc", "2.0");
    request.put("id", nextId.getAndIncrement());
    request.put("method", method);
    request.set("params", params);
    
    return objectMapper.writeValueAsString(request);
  }

  /**
   * POST request to MCP server
   */
  private HttpResponse postRequest(String requestBody) throws Exception {
    var client = new OkHttpClient.Builder()
        .connectTimeout(mcpTimeout, TimeUnit.MILLISECONDS)
        .readTimeout(mcpTimeout, TimeUnit.MILLISECONDS)
        .sslSocketFactory(createTrustAllSSLContext().getSocketFactory(), getTrustAllTrustManager())
        .hostnameVerifier((hostname, session) -> true)
        .build();

    Request.Builder requestBuilder = new Request.Builder()
        .url(mcpServerUrl)
        .header("Content-Type", "application/json")
        .header("Accept", "application/json, text/event-stream")
        .header("User-Agent", MCP_USER_AGENT)
        .post(okhttp3.RequestBody.create(requestBody, okhttp3.MediaType.parse("application/json")));

    if (sessionId != null) {
      requestBuilder.header("Mcp-Session-Id", sessionId);
    }

    try (Response response = client.newCall(requestBuilder.build()).execute()) {
      String body = response.body() != null ? response.body().string() : "";
      
      HttpResponse httpResponse = new HttpResponse();
      httpResponse.statusCode = response.code();
      httpResponse.body = body;
      
      if (response.headers() != null) {
        response.headers().forEach(header -> 
          httpResponse.headers.put(header.getFirst().toLowerCase(), header.getSecond())
        );
      }
      
      return httpResponse;
    }
  }

  /**
   * Parse JSON-RPC 2.0 response (handles both JSON and text/event-stream)
   */
  private JsonNode parseJsonRpcResult(HttpResponse response) throws Exception {
    String body = response.body;
    
    // Check if response is text/event-stream format
    String contentType = response.headers.getOrDefault("content-type", "");
    
    JsonNode message = null;
    
    if (contentType.contains("text/event-stream")) {
      log.debug("Parsing text/event-stream response");
      JsonNode last = null;
      for (String line : body.split("\\R")) {
        if (line.startsWith("data:")) {
          String payload = line.substring("data:".length()).trim();
          if (!payload.isEmpty()) {
            last = objectMapper.readTree(payload);
          }
        }
      }
      message = last;
    } else {
      log.debug("Parsing JSON response");
      message = objectMapper.readTree(body);
    }
    
    if (message == null) {
      return objectMapper.createObjectNode();
    }
    
    return message.path("result");
  }

  /**
   * Calculate relevance score for a search result based on query match
   * Improved algorithm:
   * - Title match containing query keywords: 0.95
   * - URL match containing query keywords: 0.85
   * - Snippet match containing query keywords: 0.75
   * - Bonus for having multiple relevant keywords
   * - Penalty for results without relevant keywords
   */
  private double calculateRelevanceScore(String query, String title, String url, String snippet) {
    String lowerQuery = query.toLowerCase();
    String lowerTitle = title.toLowerCase();
    String lowerUrl = url.toLowerCase();
    String lowerSnippet = snippet.toLowerCase();
    
    // Extract meaningful keywords (length >= 4)
    String[] allKeywords = lowerQuery.split("\\s+");
    java.util.List<String> keywords = new java.util.ArrayList<>();
    for (String keyword : allKeywords) {
      if (keyword.length() >= 4) {
        keywords.add(keyword);
      }
    }
    
    // If no meaningful keywords, return default score
    if (keywords.isEmpty()) {
      return 0.5;
    }
    
    double score = 0.0;
    int titleMatches = 0;
    int urlMatches = 0;
    int snippetMatches = 0;
    
    // Check each keyword
    for (String keyword : keywords) {
      if (lowerTitle.contains(keyword)) {
        score += 0.95;
        titleMatches++;
      } else if (lowerUrl.contains(keyword)) {
        score += 0.85;
        urlMatches++;
      } else if (lowerSnippet.contains(keyword)) {
        score += 0.75;
        snippetMatches++;
      }
    }
    
    // If no keywords matched at all, return very low score
    if (titleMatches + urlMatches + snippetMatches == 0) {
      return 0.2;
    }
    
    // Calculate average score based on meaningful keywords only
    double finalScore = score / keywords.size();
    
    // Bonus: if multiple keywords in title, boost score
    if (titleMatches >= 2) {
      finalScore = Math.min(finalScore * 1.2, 0.99);
    }
    
    log.debug("Relevance scoring - title matches: {}, url matches: {}, snippet matches: {}, score: {}", 
        titleMatches, urlMatches, snippetMatches, finalScore);
    
    return finalScore;
  }

  /**
   * Parse search response and extract individual hits from JSON
   * MCP server returns format: {"hits":[{"url":"...", "snippet":"...", "title":"..."}, ...]}
   * Results are ranked by relevance to the query before returning.
   */
  private List<SearchResult> parseSearchResponse(String responseText, String query) {
    var results = new ArrayList<SearchResult>();

    try {
      log.debug("Parsing MCP response text (length: {} chars)", responseText.length());
      
      // Try to parse as JSON first (the response likely contains JSON hits)
      try {
        JsonNode root = objectMapper.readTree(responseText);
        
        // Check if it's a hits array or has a hits field
        JsonNode hitsNode = null;
        
        if (root.isArray()) {
          hitsNode = root;
          log.debug("Response is direct array with {} elements", root.size());
        } else if (root.has("hits")) {
          hitsNode = root.get("hits");
          log.debug("Response has 'hits' field with {} elements", hitsNode.size());
        } else if (root.has("results")) {
          hitsNode = root.get("results");
          log.debug("Response has 'results' field with {} elements", hitsNode.size());
        }
        
        // Process each hit
        if (hitsNode != null && hitsNode.isArray()) {
          for (JsonNode hit : hitsNode) {
            // Log raw hit to discover exact field names
            log.info("MCP raw hit JSON: {}", hit.toString().substring(0, Math.min(500, hit.toString().length())));
            
            // Try multiple field names for URL
            String url = hit.path("url").asText("");
            if (url.isEmpty()) url = hit.path("link").asText("");
            if (url.isEmpty()) url = hit.path("href").asText("");
            
            // Try multiple field names for snippet/content
            String snippet = hit.path("snippet").asText("");
            if (snippet.isEmpty()) snippet = hit.path("content").asText("");
            if (snippet.isEmpty()) snippet = hit.path("text").asText("");
            if (snippet.isEmpty()) snippet = hit.path("body").asText("");
            if (snippet.isEmpty()) snippet = hit.path("description").asText("");
            if (snippet.isEmpty()) snippet = hit.path("summary").asText("");
            
            // Try multiple field names for title
            String title = hit.path("title").asText("");
            if (title.isEmpty()) title = hit.path("page_title").asText("");
            if (title.isEmpty()) title = hit.path("name").asText("");
            if (title.isEmpty()) title = hit.path("heading").asText("");
            
            // If no title, derive from URL or snippet
            if (title.isEmpty() && !url.isEmpty()) {
              // Extract readable title from URL path
              String[] urlParts = url.split("/");
              title = urlParts[urlParts.length - 1].replace("-", " ").replace("_", " ");
            }
            if (title.isEmpty() && !snippet.isEmpty()) {
              title = snippet.substring(0, Math.min(80, snippet.length()));
            }
            
            // Only add if we have at least a URL or snippet
            if (!url.isEmpty() || !snippet.isEmpty()) {
              double relevanceScore = calculateRelevanceScore(query, title, url, snippet);
              SearchResult result = new SearchResult(
                  title.isEmpty() ? "OpenAI Docs Result" : title,
                  url,
                  snippet,
                  snippet.isEmpty() ? "" : snippet.substring(0, Math.min(200, snippet.length())),
                  relevanceScore
              );
              results.add(result);
              log.debug("Added MCP hit: title={}, url={}, snippet_len={}, relevance={}", title, url, snippet.length(), relevanceScore);
            }
          }
          
          log.info("Successfully parsed {} structured hits from MCP response", results.size());
          if (!results.isEmpty()) {
            // Sort results by relevance score (descending) before returning
            results.sort((a, b) -> Double.compare(b.relevanceScore(), a.relevanceScore()));
            log.info("Results sorted by relevance score. Top result relevance: {}", results.get(0).relevanceScore());
            return results;
          }
        }
      } catch (Exception e) {
        log.debug("Response is not JSON or doesn't have expected hits structure: {}", e.getMessage());
      }
      
      // Fallback: if JSON parsing fails or no hits found, treat as plain text
      // Split by double newline or process line by line
      if (results.isEmpty() && !responseText.isEmpty()) {
        log.debug("Falling back to plain text parsing");
        String[] lines = responseText.split("\n");
        
        StringBuilder currentText = new StringBuilder();
        for (String line : lines) {
          if (line.trim().isEmpty()) {
            if (currentText.length() > 0) {
              String resultText = currentText.toString().trim();
              if (!resultText.isEmpty()) {
                SearchResult result = new SearchResult(
                    "OpenAI Docs",
                    mcpServerUrl,
                    resultText,
                    resultText.substring(0, Math.min(200, resultText.length())),
                    0.90
                );
                results.add(result);
                log.debug("Added fallback text result");
              }
              currentText = new StringBuilder();
            }
          } else {
            currentText.append(line).append(" ");
          }
        }
        
        // Add any remaining text
        if (currentText.length() > 0) {
          String resultText = currentText.toString().trim();
          if (!resultText.isEmpty()) {
            SearchResult result = new SearchResult(
                "OpenAI Docs",
                mcpServerUrl,
                resultText,
                resultText.substring(0, Math.min(200, resultText.length())),
                0.90
            );
            results.add(result);
            log.debug("Added final fallback text result");
          }
        }
      }
      
      log.info("Parsed {} total results from MCP response", results.size());

    } catch (Exception e) {
      log.error("Error parsing MCP search response: {}", e.getMessage(), e);
    }

    return results;
  }

  /**
   * Helper class to hold HTTP response data
   */
  private static class HttpResponse {
    int statusCode;
    String body;
    java.util.Map<String, String> headers = new java.util.HashMap<>();
  }

  private SSLContext createTrustAllSSLContext() throws Exception {
    SSLContext sslContext = SSLContext.getInstance("TLS");
    sslContext.init(null, new TrustManager[] { getTrustAllTrustManager() }, new SecureRandom());
    return sslContext;
  }

  private X509TrustManager getTrustAllTrustManager() {
    return new X509TrustManager() {
      @Override
      public X509Certificate[] getAcceptedIssuers() {
        return new X509Certificate[0];
      }

      @Override
      public void checkClientTrusted(X509Certificate[] certs, String authType) {
      }

      @Override
      public void checkServerTrusted(X509Certificate[] certs, String authType) {
      }
    };
  }
}
