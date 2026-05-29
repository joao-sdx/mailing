package com.synapsedx.mailing.procurement.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.synapsedx.mailing.procurement.config.LmStudioProperties;
import com.synapsedx.mailing.procurement.model.Tender;
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
  private String promptTemplate;
  private String postmasterProfile;

  @PostConstruct
  void init() throws Exception {
    httpClient =
        HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(properties.connectTimeoutSeconds()))
            .build();
    promptTemplate =
        new ClassPathResource("postmaster-prompt.md").getContentAsString(StandardCharsets.UTF_8);
    postmasterProfile =
        new ClassPathResource("postmaster-profile.md").getContentAsString(StandardCharsets.UTF_8);
  }

  /**
   * Asks the local LM Studio model whether Postmaster is a plausible respondent for the given
   * tender. Returns {@code Optional.empty()} when the LLM gives no usable answer — the batch job
   * must continue regardless.
   */
  public Optional<Boolean> assessPostmasterFit(Tender tender) {
    try {
      var tenderText = buildTenderText(tender);
      var rendered =
          promptTemplate
              .replace("{{postmaster}}", postmasterProfile)
              .replace("{{tender}}", tenderText);

      var messages = mapper.createArrayNode();
      messages.add(mapper.createObjectNode().put("role", "user").put("content", rendered));

      var requestNode = mapper.createObjectNode();
      requestNode.put("model", properties.model());
      requestNode.set("messages", messages);
      requestNode.put("temperature", 0);
      requestNode.put("max_tokens", properties.maxTokens());

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
        log.warn("tender_relevance_empty id={}", tender.id());
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
      log.warn("tender_relevance_unparseable id={} response={}", tender.id(), content);
      return Optional.empty();
    } catch (Exception e) {
      log.warn("tender_relevance_failed id={} reason={}", tender.id(), e.getMessage());
      return Optional.empty();
    }
  }

  private String buildTenderText(Tender tender) {
    return "Titre: "
        + orEmpty(tender.title())
        + "\n"
        + "Classification: "
        + orEmpty(tender.classification())
        + "\n"
        + "Acheteur: "
        + orEmpty(tender.buyer())
        + "\n"
        + "Pays: "
        + orEmpty(tender.country());
  }

  private String orEmpty(String value) {
    return value != null ? value : "";
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
