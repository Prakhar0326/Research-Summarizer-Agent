
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
