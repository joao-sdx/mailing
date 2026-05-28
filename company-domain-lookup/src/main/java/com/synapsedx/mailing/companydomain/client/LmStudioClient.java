package com.synapsedx.mailing.companydomain.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.synapsedx.mailing.companydomain.config.LmStudioProperties;
import com.synapsedx.mailing.companydomain.model.SerpResult;
import jakarta.annotation.PostConstruct;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
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
  private String promptTemplate;

  @PostConstruct
  void init() throws Exception {
    httpClient =
        HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(properties.connectTimeoutSeconds()))
            .build();
    promptTemplate =
        new ClassPathResource("domain-pick-prompt.md").getContentAsString(StandardCharsets.UTF_8);
  }

  public Optional<String> pickOfficialDomain(String company, List<SerpResult> results) {
    try {
      var rendered =
          promptTemplate
              .replace("{{company}}", company)
              .replace("{{results}}", renderResults(results));

      var messages = mapper.createArrayNode();
      messages.add(mapper.createObjectNode().put("role", "user").put("content", rendered));

      var requestNode = mapper.createObjectNode();
      requestNode.put("model", properties.model());
      requestNode.set("messages", messages);
      requestNode.put("temperature", 0);
      requestNode.put("max_tokens", 200);

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
        log.warn("llm_empty_response company={}", company);
        return Optional.empty();
      }
      var domainNode = mapper.readTree(content).path("domain");
      if (domainNode.isMissingNode() || domainNode.isNull()) {
        return Optional.empty();
      }
      var domain = domainNode.asText("");
      return domain.isBlank() ? Optional.empty() : Optional.of(domain);
    } catch (Exception e) {
      log.warn("llm_pick_failed company={} reason={}", company, e.getMessage());
      return Optional.empty();
    }
  }

  private String renderResults(List<SerpResult> results) {
    var sb = new StringBuilder();
    for (var r : results) {
      sb.append("- titre: ").append(r.title()).append("\n");
      sb.append("  url: ").append(r.url()).append("\n");
      sb.append("  description: ").append(r.snippet()).append("\n");
    }
    return sb.toString();
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
