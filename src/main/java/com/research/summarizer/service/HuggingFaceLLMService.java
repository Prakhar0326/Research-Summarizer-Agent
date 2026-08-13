package com.research.summarizer.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * HuggingFace Inference API Service
 * 
 * Provides access to open-source LLMs hosted on HuggingFace
 * API Documentation: https://huggingface.co/docs/api-inference
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HuggingFaceLLMService {

  private final ObjectMapper objectMapper;

  @Value("${huggingface.api.key:}")
  private String hfApiKey;

  @Value("${huggingface.model:mistralai/Mistral-7B-Instruct-v0.1}")
  private String modelId;

  @Value("${huggingface.timeout:30000}")
  private long timeout;

  private static final String HF_API_URL = "https://api-inference.huggingface.co/models/";
  private static final String USER_AGENT = "Research-Summarizer-Agent/1.0";

  /**
   * Call HuggingFace model with a prompt
   * 
   * @param prompt The input prompt for the model
   * @return Generated text response
   */
  public String generateText(String prompt) {
    if (hfApiKey == null || hfApiKey.isEmpty() || hfApiKey.equals("demo-key")) {
      log.warn("HuggingFace API key not configured, returning empty response");
      return "";
    }

    try {
      log.debug("Calling HuggingFace model: {} with prompt length: {}", modelId, prompt.length());

      String url = HF_API_URL + modelId;
      
      // Build request body
      ObjectNode requestBody = objectMapper.createObjectNode();
      requestBody.put("inputs", prompt);
      requestBody.put("max_new_tokens", 1024);
      requestBody.put("temperature", 0.7);
      requestBody.put("top_p", 0.95);

      String jsonBody = objectMapper.writeValueAsString(requestBody);
      log.debug("HF Request (first 200 chars): {}", jsonBody.substring(0, Math.min(200, jsonBody.length())));

      // Execute request
      var client = new OkHttpClient.Builder()
          .connectTimeout(timeout, TimeUnit.MILLISECONDS)
          .readTimeout(timeout, TimeUnit.MILLISECONDS)
          .build();

      Request.Builder requestBuilder = new Request.Builder()
          .url(url)
          .header("Authorization", "Bearer " + hfApiKey)
          .header("Content-Type", "application/json")
          .header("User-Agent", USER_AGENT)
          .post(okhttp3.RequestBody.create(jsonBody, okhttp3.MediaType.parse("application/json")));

      try (Response response = client.newCall(requestBuilder.build()).execute()) {
        if (!response.isSuccessful()) {
          log.error("HuggingFace API error. Status: {}. Message: {}", response.code(), response.message());
          return "";
        }

        String responseBody = response.body() != null ? response.body().string() : "{}";
        log.debug("HF Response (first 500 chars): {}", responseBody.substring(0, Math.min(500, responseBody.length())));

        // Parse response and extract generated text
        var responseNode = objectMapper.readTree(responseBody);
        
        String generatedText = "";
        if (responseNode.isArray() && responseNode.size() > 0) {
          // Response is array: [{"generated_text": "..."}]
          generatedText = responseNode.get(0).path("generated_text").asText("");
        } else if (responseNode.has("generated_text")) {
          // Response is object: {"generated_text": "..."}
          generatedText = responseNode.get("generated_text").asText("");
        }

        log.debug("Generated text length: {}", generatedText.length());
        return generatedText;
      }

    } catch (Exception e) {
      log.error("Error calling HuggingFace API: {}", e.getMessage(), e);
      return "";
    }
  }

  /**
   * Check if HuggingFace API is properly configured
   */
  public boolean isConfigured() {
    return hfApiKey != null && !hfApiKey.isEmpty() && !hfApiKey.equals("demo-key");
  }
}
