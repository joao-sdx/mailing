package com.synapsedx.mailing.seonewsparse.batch.processor;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.synapsedx.mailing.seonewsparse.config.LmStudioProperties;
import com.synapsedx.mailing.seonewsparse.model.PersonRow;
import jakarta.annotation.PostConstruct;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class LmStudioExtractProcessor implements ItemProcessor<Path, List<PersonRow>> {

  private static final Pattern FRONTMATTER_SPLIT = Pattern.compile("(?m)^---\\s*$");

  private final LmStudioProperties properties;
  private final ObjectMapper mapper = new ObjectMapper();

  private HttpClient httpClient;
  String systemPrompt;
  String userPromptTemplate;

  @PostConstruct
  void init() throws Exception {
    httpClient =
        HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(properties.connectTimeoutSeconds()))
            .build();
    systemPrompt =
        new ClassPathResource("seoit-system-prompt.md").getContentAsString(StandardCharsets.UTF_8);
    userPromptTemplate =
        new ClassPathResource("seoit-prompt.md").getContentAsString(StandardCharsets.UTF_8);
  }

  @Override
  public List<PersonRow> process(Path articlePath) {
    try {
      var rawContent = Files.readString(articlePath);

      var parts = FRONTMATTER_SPLIT.split(rawContent, -1);
      String frontmatter;
      String body;
      if (parts.length >= 3) {
        frontmatter = parts[1];
        body = parts[2];
      } else {
        frontmatter = "";
        body = rawContent;
      }

      var userPrompt = userPromptTemplate.replace("{article_content}", body);
      var messages = mapper.createArrayNode();
      messages.add(mapper.createObjectNode().put("role", "system").put("content", systemPrompt));
      messages.add(mapper.createObjectNode().put("role", "user").put("content", userPrompt));
      var requestNode = mapper.createObjectNode();
      requestNode.put("model", properties.model());
      requestNode.set("messages", messages);
      requestNode.put("temperature", 0.1);

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

      if (content.isEmpty() || content.matches("\\[\\s*\\]")) {
        log.warn("llm_empty_response article={}", articlePath.getFileName());
        return null;
      }

      var articleId = articlePath.getFileName().toString();
      var raw = mapper.readValue(content, new TypeReference<List<Map<String, String>>>() {});
      var contacts =
          raw.stream()
              .map(
                  m ->
                      new PersonRow(
                          m.getOrDefault("prenom", ""),
                          m.getOrDefault("nom", ""),
                          m.getOrDefault("societe", ""),
                          articleId))
              .toList();

      log.info(
          "llm_contacts_found article={} count={}", articlePath.getFileName(), contacts.size());
      return contacts;

    } catch (Exception e) {
      log.warn(
          "llm_extract_failed article={} reason={}", articlePath.getFileName(), e.getMessage());
      return null;
    }
  }

  String post(String body) throws Exception {
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
      log.warn("llm_http_error status={}", response.statusCode());
      throw new IllegalStateException("LM Studio error status=" + response.statusCode());
    }
    return response.body();
  }
}
