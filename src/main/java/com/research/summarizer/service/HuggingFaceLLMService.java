//package com.research.summarizer.service;
//
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.fasterxml.jackson.databind.node.ObjectNode;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import okhttp3.OkHttpClient;
//import okhttp3.Request;
//import okhttp3.Response;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.stereotype.Service;
//
//import java.util.concurrent.TimeUnit;
//
///**
// * HuggingFace Inference API Service
// *
// * Provides access to open-source LLMs hosted on HuggingFace
// * API Documentation: https://huggingface.co/docs/api-inference
// */
//@Slf4j
//@Service
//@RequiredArgsConstructor
//public class HuggingFaceLLMService {
//
//  private final ObjectMapper objectMapper;
//
//  @Value("${huggingface.api.key:}")
//  private String hfApiKey;
//
//  @Value("${huggingface.model:mistralai/Mistral-7B-Instruct-v0.1}")
//  private String modelId;
//
//  @Value("${huggingface.timeout:30000}")
//  private long timeout;
//
//  private static final String HF_API_URL = "https://api-inference.huggingface.co/models/";
//  private static final String USER_AGENT = "Research-Summarizer-Agent/1.0";
//
//  /**
//   * Call HuggingFace model with a prompt
//   *
//   * @param prompt The input prompt for the model
//   * @return Generated text response
//   */
//  public String generateText(String prompt) {
//    if (hfApiKey == null || hfApiKey.isEmpty() || hfApiKey.equals("demo-key")) {
//      log.warn("HuggingFace API key not configured, returning empty response");
//      return "";
//    }
//
//    try {
//      log.debug("Calling HuggingFace model: {} with prompt length: {}", modelId, prompt.length());
//
//      String url = HF_API_URL + modelId;
//
//      // Build request body
//      ObjectNode requestBody = objectMapper.createObjectNode();
//      requestBody.put("inputs", prompt);
//      requestBody.put("max_new_tokens", 1024);
//      requestBody.put("temperature", 0.7);
//      requestBody.put("top_p", 0.95);
//
//      String jsonBody = objectMapper.writeValueAsString(requestBody);
//      log.debug("HF Request (first 200 chars): {}", jsonBody.substring(0, Math.min(200, jsonBody.length())));
//
//      // Execute request
//      var client = new OkHttpClient.Builder()
//          .connectTimeout(timeout, TimeUnit.MILLISECONDS)
//          .readTimeout(timeout, TimeUnit.MILLISECONDS)
//          .build();
//
//      Request.Builder requestBuilder = new Request.Builder()
//          .url(url)
//          .header("Authorization", "Bearer " + hfApiKey)
//          .header("Content-Type", "application/json")
//          .header("User-Agent", USER_AGENT)
//          .post(okhttp3.RequestBody.create(jsonBody, okhttp3.MediaType.parse("application/json")));
//
//      try (Response response = client.newCall(requestBuilder.build()).execute()) {
//        if (!response.isSuccessful()) {
//          log.error("HuggingFace API error. Status: {}. Message: {}", response.code(), response.message());
//          return "";
//        }
//
//        String responseBody = response.body() != null ? response.body().string() : "{}";
//        log.debug("HF Response (first 500 chars): {}", responseBody.substring(0, Math.min(500, responseBody.length())));
//
//        // Parse response and extract generated text
//        var responseNode = objectMapper.readTree(responseBody);
//
//        String generatedText = "";
//        if (responseNode.isArray() && responseNode.size() > 0) {
//          // Response is array: [{"generated_text": "..."}]
//          generatedText = responseNode.get(0).path("generated_text").asText("");
//        } else if (responseNode.has("generated_text")) {
//          // Response is object: {"generated_text": "..."}
//          generatedText = responseNode.get("generated_text").asText("");
//        }
//
//        log.debug("Generated text length: {}", generatedText.length());
//        return generatedText;
//      }
//
//    } catch (Exception e) {
//      log.error("Error calling HuggingFace API: {}", e.getMessage(), e);
//      return "";
//    }
//  }
//
//  /**
//   * Check if HuggingFace API is properly configured
//   */
//  public boolean isConfigured() {
//    return hfApiKey != null && !hfApiKey.isEmpty() && !hfApiKey.equals("demo-key");
//  }
//}

//=====================================================================================================

//package com.research.summarizer.service;
//
//import com.fasterxml.jackson.databind.JsonNode;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.fasterxml.jackson.databind.node.ArrayNode;
//import com.fasterxml.jackson.databind.node.ObjectNode;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import okhttp3.MediaType;
//import okhttp3.OkHttpClient;
//import okhttp3.Request;
//import okhttp3.RequestBody;
//import okhttp3.Response;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.stereotype.Service;
//
//import java.io.IOException;
//import java.util.concurrent.TimeUnit;
//
//@Slf4j
//@Service
//@RequiredArgsConstructor
//public class HuggingFaceLLMService {
//
//  private final ObjectMapper objectMapper;
//
//  @Value("${huggingface.api.key:}")
//  private String hfApiKey;
//
//  @Value("${huggingface.model:openai/gpt-oss-120b}")
//  private String modelId;
//
//  @Value("${huggingface.timeout:60000}")
//  private long timeout;
//
//  /**
//   * Current Hugging Face OpenAI-compatible endpoint.
//   */
//  private static final String HF_API_URL =
//          "https://router.huggingface.co/v1/chat/completions";
//
//  private static final String USER_AGENT =
//          "Research-Summarizer-Agent/1.0";
//
//  private static final MediaType JSON =
//          MediaType.parse("application/json");
//
//  /**
//   * OkHttp client.
//   *
//   * It is better to create this once instead of creating
//   * a new client for every request.
//   */
//  private final OkHttpClient client = new OkHttpClient.Builder()
//          .connectTimeout(30, TimeUnit.SECONDS)
//          .readTimeout(timeout, TimeUnit.MILLISECONDS)
//          .writeTimeout(30, TimeUnit.SECONDS)
//          .build();
//
//  /**
//   * Sends a prompt to Hugging Face and returns the generated text.
//   *
//   * @param prompt input prompt
//   * @return generated response
//   */
//  public String generateText(String prompt) {
//
//    if (!isConfigured()) {
//      log.warn("HuggingFace API key is not configured");
//      return "";
//    }
//
//    if (prompt == null || prompt.isBlank()) {
//      log.warn("Prompt is empty");
//      return "";
//    }
//
//    try {
//      log.debug(
//              "Calling HuggingFace model: {}, prompt length: {}",
//              modelId,
//              prompt.length()
//      );
//
//      // ---------------------------------------------------------
//      // 1. Build request JSON
//      // ---------------------------------------------------------
//
//      ObjectNode requestBody =
//              objectMapper.createObjectNode();
//
//      /*
//       * Model to use.
//       *
//       * Example:
//       * openai/gpt-oss-120b
//       *
//       * You can also specify a provider:
//       * deepseek-ai/DeepSeek-R1:novita
//       *
//       * Or routing policy:
//       * deepseek-ai/DeepSeek-R1:fastest
//       */
//      requestBody.put("model", modelId);
//
//      /*
//       * messages array
//       */
//      ArrayNode messages =
//              requestBody.putArray("messages");
//
//      /*
//       * User message
//       */
//      ObjectNode userMessage =
//              messages.addObject();
//
//      userMessage.put("role", "user");
//      userMessage.put("content", prompt);
//
//      /*
//       * Generation parameters
//       */
//      requestBody.put("max_tokens", 1024);
//      requestBody.put("temperature", 0.7);
//      requestBody.put("top_p", 0.95);
//
//      /*
//       * We don't want streaming response.
//       */
//      requestBody.put("stream", false);
//
//      String jsonBody =
//              objectMapper.writeValueAsString(requestBody);
//
//      log.debug(
//              "HF request: {}",
//              jsonBody.substring(
//                      0,
//                      Math.min(500, jsonBody.length())
//              )
//      );
//
//      // ---------------------------------------------------------
//      // 2. Build HTTP request
//      // ---------------------------------------------------------
//
//      RequestBody body =
//              RequestBody.create(jsonBody, JSON);
//
//      Request request =
//              new Request.Builder()
//                      .url(HF_API_URL)
//                      .header(
//                              "Authorization",
//                              "Bearer " + hfApiKey
//                      )
//                      .header(
//                              "Content-Type",
//                              "application/json"
//                      )
//                      .header(
//                              "User-Agent",
//                              USER_AGENT
//                      )
//                      .post(body)
//                      .build();
//
//      // ---------------------------------------------------------
//      // 3. Execute request
//      // ---------------------------------------------------------
//
//      try (Response response =
//                   client.newCall(request).execute()) {
//
//        String responseBody =
//                response.body() != null
//                        ? response.body().string()
//                        : "";
//
//        log.debug(
//                "HuggingFace response status: {}",
//                response.code()
//        );
//
//        log.debug(
//                "HF response: {}",
//                responseBody.substring(
//                        0,
//                        Math.min(1000, responseBody.length())
//                )
//        );
//
//        // -----------------------------------------------------
//        // 4. Handle HTTP errors
//        // -----------------------------------------------------
//
//        if (!response.isSuccessful()) {
//
//          log.error(
//                  "HuggingFace API error. Status: {}, Message: {}, Body: {}",
//                  response.code(),
//                  response.message(),
//                  responseBody
//          );
//
//          return "";
//        }
//
//        if (responseBody.isBlank()) {
//          log.warn("HuggingFace returned an empty response");
//          return "";
//        }
//
//        // -----------------------------------------------------
//        // 5. Parse response
//        // -----------------------------------------------------
//
//        JsonNode responseNode =
//                objectMapper.readTree(responseBody);
//
//        /*
//         * Expected response:
//         *
//         * {
//         *   "choices": [
//         *     {
//         *       "message": {
//         *         "role": "assistant",
//         *         "content": "..."
//         *       }
//         *     }
//         *   ]
//         * }
//         */
//
//        String generatedText =
//                responseNode
//                        .path("choices")
//                        .path(0)
//                        .path("message")
//                        .path("content")
//                        .asText("");
//
//        if (generatedText.isBlank()) {
//
//          log.warn(
//                  "Could not extract generated text from HF response: {}",
//                  responseBody
//          );
//
//          return "";
//        }
//
//        log.debug(
//                "Generated text length: {}",
//                generatedText.length()
//        );
//
//        return generatedText.trim();
//      }
//
//    } catch (IOException e) {
//
//      log.error(
//              "IO error while calling HuggingFace API: {}",
//              e.getMessage(),
//              e
//      );
//
//      return "";
//
//    } catch (Exception e) {
//
//      log.error(
//              "Error calling HuggingFace API: {}",
//              e.getMessage(),
//              e
//      );
//
//      return "";
//    }
//  }
//
//  /**
//   * Checks whether Hugging Face is configured.
//   */
//  public boolean isConfigured() {
//
//    return hfApiKey != null
//            && !hfApiKey.isBlank()
//            && !"demo-key".equalsIgnoreCase(hfApiKey.trim());
//  }
//}

//=======================================================================================

package com.research.summarizer.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class HuggingFaceLLMService {

  private final ObjectMapper objectMapper;

  @Value("${huggingface.api.key:}")
  private String hfApiKey;

  @Value("${huggingface.model:openai/gpt-oss-120b}")
  private String modelId;

  @Value("${huggingface.timeout:60000}")
  private long timeout;

  private static final String HF_API_URL =
          "https://router.huggingface.co/v1/chat/completions";

  private static final String USER_AGENT =
          "Research-Summarizer-Agent/1.0";

  private static final MediaType JSON =
          MediaType.parse("application/json");

  /**
   * Reusable HTTP client.
   */
  private OkHttpClient createClient() {
    return new OkHttpClient.Builder()
            .connectTimeout(timeout, TimeUnit.MILLISECONDS)
            .readTimeout(timeout, TimeUnit.MILLISECONDS)
            .writeTimeout(timeout, TimeUnit.MILLISECONDS)
            .build();
  }

  /**
   * Generate text using HuggingFace chat completion API.
   */
  public String generateText(String prompt) {

    if (!isConfigured()) {
      log.warn("HuggingFace API key is not configured");
      return "";
    }

    try {

      log.debug(
              "Calling HuggingFace model: {}, prompt length: {}",
              modelId,
              prompt != null ? prompt.length() : 0
      );

      ObjectNode requestBody =
              objectMapper.createObjectNode();

      requestBody.put("model", modelId);

      var messages = requestBody.putArray("messages");

      ObjectNode userMessage = messages.addObject();
      userMessage.put("role", "user");
      userMessage.put("content", prompt);

      /*
       * Keep output reasonably small.
       * This prevents Agent 2 from producing huge/truncated JSON.
       */
      requestBody.put("max_tokens", 1200);

      requestBody.put("temperature", 0.2);

      requestBody.put("top_p", 0.9);

      requestBody.put("stream", false);

      String jsonBody =
              objectMapper.writeValueAsString(requestBody);

      log.debug(
              "HF request: {}",
              jsonBody.substring(
                      0,
                      Math.min(500, jsonBody.length())
              )
      );

      RequestBody body =
              RequestBody.create(jsonBody, JSON);

      Request request =
              new Request.Builder()
                      .url(HF_API_URL)
                      .header(
                              "Authorization",
                              "Bearer " + hfApiKey
                      )
                      .header(
                              "Content-Type",
                              "application/json"
                      )
                      .header(
                              "User-Agent",
                              USER_AGENT
                      )
                      .post(body)
                      .build();

      try (
              Response response =
                      createClient()
                              .newCall(request)
                              .execute()
      ) {

        String responseBody =
                response.body() != null
                        ? response.body().string()
                        : "";

        log.debug(
                "HuggingFace response status: {}",
                response.code()
        );

        if (!response.isSuccessful()) {

          log.error(
                  "HuggingFace API error. Status: {}, Body: {}",
                  response.code(),
                  responseBody.substring(
                          0,
                          Math.min(
                                  1000,
                                  responseBody.length()
                          )
                  )
          );

          if (response.code() == 402) {
            log.error(
                    "HuggingFace credits are exhausted. " +
                            "Please add credits or use another provider/model."
            );
          }

          return "";
        }

        if (responseBody.isBlank()) {
          log.warn("HuggingFace returned an empty response");
          return "";
        }

        log.debug(
                "HF response: {}",
                responseBody.substring(
                        0,
                        Math.min(
                                1500,
                                responseBody.length()
                        )
                )
        );

        JsonNode root =
                objectMapper.readTree(responseBody);

        /*
         * OpenAI-compatible response:
         *
         * {
         *   "choices": [
         *      {
         *        "message": {
         *           "content": "..."
         *        }
         *      }
         *   ]
         * }
         */

        JsonNode choices =
                root.path("choices");

        if (!choices.isArray()
                || choices.isEmpty()) {

          log.warn(
                  "HuggingFace response does not contain choices"
          );

          return "";
        }

        String generatedText =
                choices
                        .get(0)
                        .path("message")
                        .path("content")
                        .asText("");

        if (generatedText.isBlank()) {
          log.warn(
                  "HuggingFace generated empty content"
          );
          return "";
        }

        log.debug(
                "Generated text length: {}",
                generatedText.length()
        );

        return generatedText.trim();
      }

    } catch (Exception e) {

      log.error(
              "Error calling HuggingFace API: {}",
              e.getMessage(),
              e
      );

      return "";
    }
  }

  public boolean isConfigured() {

    return hfApiKey != null
            && !hfApiKey.isBlank()
            && !"demo-key".equals(hfApiKey);
  }
}
