package com.research.summarizer.controller;

import com.research.summarizer.dto.ResearchRequest;
import com.research.summarizer.dto.ResearchResponse;
import com.research.summarizer.service.ResearchSummarizerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * REST API Controller for research summarization
 * 
 * Exposes the POST /api/research/summarize endpoint
 */
@Slf4j
@RestController
@RequestMapping("/api/research")
@RequiredArgsConstructor
public class ResearchController {

  private final ResearchSummarizerService researchService;

  /**
   * POST endpoint to trigger the research summarization pipeline
   * 
   * Request: { "topic": "string", "maxSources": 5 }
   * Response: { "topic", "searchSource", "executiveSummary", "keyFindings",
   * "details", "sources" }
   */
  @PostMapping("/summarize")
  public ResponseEntity<?> summarizeResearch(@RequestBody ResearchRequest request) {
    log.info("Received research request for topic: {}", request.topic());

    // Validate request
    if (request.topic() == null || request.topic().trim().isEmpty()) {
      log.warn("Invalid research request: topic is empty");
      return ResponseEntity.badRequest()
          .body(createErrorResponse("Topic cannot be empty", 400));
    }

    try {
      // Process the research request through the agent pipeline
      var response = researchService.processResearch(request);

      log.info("Research request completed successfully for topic: {}", request.topic());
      return ResponseEntity.ok(response);

    } catch (Exception e) {
      log.error("Error processing research request for topic: {}", request.topic(), e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(createErrorResponse(
              "Failed to process research request: " + e.getMessage(),
              500));
    }
  }

  /**
   * Health check endpoint
   */
  @PostMapping("/health")
  public ResponseEntity<Map<String, String>> health() {
    var response = new HashMap<String, String>();
    response.put("status", "UP");
    response.put("service", "Research Summarizer Agent");
    return ResponseEntity.ok(response);
  }

  /**
   * Create error response body
   */
  private Map<String, Object> createErrorResponse(String message, int statusCode) {
    var error = new HashMap<String, Object>();
    error.put("error", message);
    error.put("statusCode", statusCode);
    return error;
  }
}
