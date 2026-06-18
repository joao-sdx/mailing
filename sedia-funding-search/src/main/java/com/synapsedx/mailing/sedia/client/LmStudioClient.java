package com.synapsedx.mailing.sedia.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.synapsedx.mailing.sedia.config.LmStudioProperties;
import jakarta.annotation.PostConstruct;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Locale;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class LmStudioClient {

  private final LmStudioProperties properties;
  private final ObjectMapper mapper = new ObjectMapper();

  private HttpClient httpClient;
  private String relevancePromptTemplate;
  private String contentPromptTemplate;

  @PostConstruct
  void init() throws Exception {
    httpClient =
        HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(properties.connectTimeoutSeconds()))
            .build();
    relevancePromptTemplate =
        new ClassPathResource("sedia-relevance-prompt.md")
            .getContentAsString(StandardCharsets.UTF_8);
    contentPromptTemplate =
        new ClassPathResource("sedia-content-prompt.md").getContentAsString(StandardCharsets.UTF_8);
  }

  public Optional<Boolean> assessRelevance(String callText) {
    try {
      var rendered = relevancePromptTemplate.replace("{{call}}", callText);

      var messages = mapper.createArrayNode();
      messages.add(mapper.createObjectNode().put("role", "user").put("content", rendered));

      var requestNode = mapper.createObjectNode();
      requestNode.put("model", properties.model());
      requestNode.set("messages", messages);
      requestNode.put("temperature", 0);
      requestNode.put("max_tokens", properties.relevanceMaxTokens());

      var rawResponse = post(mapper.writeValueAsString(requestNode));
      var content =
          mapper
              .readTree(rawResponse)
              .path("choices")
              .path(0)
              .path("message")
              .path("content")
              .asText("");
      content = content.replaceAll("(?s)```json\\s*", "").replaceAll("(?s)```\\s*", "").strip();
      if (content.isBlank()) {
        log.warn("call_relevance_empty");
        return Optional.empty();
      }
      var lower = content.toLowerCase(Locale.ROOT);
      if (lower.equals("true") || lower.equals("false")) {
        return Optional.of(Boolean.parseBoolean(lower));
      }
      if (lower.contains("true")) {
        return Optional.of(true);
      }
      if (lower.contains("false")) {
        return Optional.of(false);
      }
      log.warn("call_relevance_unparseable response={}", content);
      return Optional.empty();
    } catch (Exception e) {
      log.warn("call_relevance_failed reason={}", e.getMessage());
      return Optional.empty();
    }
  }

  public Optional<String> summarize(String callText) {
    try {
      var rendered = contentPromptTemplate.replace("{{call}}", callText);

      var messages = mapper.createArrayNode();
      messages.add(mapper.createObjectNode().put("role", "user").put("content", rendered));

      var requestNode = mapper.createObjectNode();
      requestNode.put("model", properties.model());
      requestNode.set("messages", messages);
      requestNode.put("temperature", 0);
      requestNode.put("max_tokens", properties.contentMaxTokens());

      var rawResponse = post(mapper.writeValueAsString(requestNode));
      var content =
          mapper
              .readTree(rawResponse)
              .path("choices")
              .path(0)
              .path("message")
              .path("content")
              .asText("");
      content = content.replaceAll("(?s)```json\\s*", "").replaceAll("(?s)```\\s*", "").strip();
      if (content.isBlank()) {
        log.warn("call_summary_empty");
        return Optional.empty();
      }
      log.debug("call_summary_done length={}", content.length());
      return Optional.of(content);
    } catch (Exception e) {
      log.warn("call_summary_failed reason={}", e.getMessage());
      return Optional.empty();
    }
  }

  private String post(String body) throws Exception {
    var request =
        HttpRequest.newBuilder()
            .uri(URI.create(properties.server().replaceAll("/+$", "") + "/v1/chat/completions"))
            .header("Authorization", "Bearer " + properties.key())
            .header("Content-Type", "application/json")
            .timeout(Duration.ofSeconds(properties.requestTimeoutSeconds()))
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build();
    var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    log.debug("llm_call status={}", response.statusCode());
    if (response.statusCode() != 200) {
      throw new IllegalStateException(
          "LM Studio error status=" + response.statusCode() + " body=" + response.body());
    }
    return response.body();
  }
}
