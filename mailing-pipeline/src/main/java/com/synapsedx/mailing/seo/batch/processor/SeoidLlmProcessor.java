package com.synapsedx.mailing.seo.batch.processor;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.synapsedx.mailing.seo.config.SeoidProperties;
import com.synapsedx.mailing.seo.model.TargetContact;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SeoidLlmProcessor implements ItemProcessor<Path, List<TargetContact>> {

  private final SeoidProperties properties;
  private final ObjectMapper mapper = new ObjectMapper();

  private HttpClient httpClient;
  private String systemPrompt;
  private String userPromptTemplate;

  @PostConstruct
  void init() throws Exception {
    var openai = properties.openai();
    httpClient =
        HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(openai.connectTimeoutSeconds()))
            .build();
    systemPrompt =
        new ClassPathResource("seoit-system-prompt.md").getContentAsString(StandardCharsets.UTF_8);
    userPromptTemplate =
        new ClassPathResource("seoit-prompt.md").getContentAsString(StandardCharsets.UTF_8);
  }

  @Override
  public List<TargetContact> process(Path articlePath) throws Exception {
    var articleContent = Files.readString(articlePath);
    var userPrompt = userPromptTemplate.replace("{article_content}", articleContent);

    var messages = mapper.createArrayNode();
    messages.add(mapper.createObjectNode().put("role", "system").put("content", systemPrompt));
    messages.add(mapper.createObjectNode().put("role", "user").put("content", userPrompt));

    var requestNode = mapper.createObjectNode();
    requestNode.put("model", properties.openai().model());
    requestNode.set("messages", messages);
    requestNode.put("temperature", 0.1);

    log.info("llm_call_start article={}", articlePath.getFileName());
    var rawResponse = post(requestNode.toString());
    var content =
        mapper
            .readTree(rawResponse)
            .path("choices")
            .path(0)
            .path("message")
            .path("content")
            .asText("");

    content = content.replaceAll("(?s)```json\\s*", "").replaceAll("(?s)```\\s*", "").strip();

    if (content.isEmpty() || content.equals("[]")) {
      return List.of();
    }

    var absolutePath = articlePath.toAbsolutePath().toString();
    var raw = mapper.readValue(content, new TypeReference<List<Map<String, String>>>() {});
    var contacts =
        raw.stream()
            .map(
                m ->
                    new TargetContact(
                        m.getOrDefault("nom", ""),
                        m.getOrDefault("role", ""),
                        m.getOrDefault("societe", ""),
                        m.getOrDefault("email", ""),
                        absolutePath))
            .toList();

    log.info("llm_contacts_found article={} count={}", articlePath.getFileName(), contacts.size());
    return contacts;
  }

  private String post(String body) throws Exception {
    var request =
        HttpRequest.newBuilder()
            .uri(
                URI.create(
                    properties.openai().server().replaceAll("/+$", "") + "/v1/chat/completions"))
            .header("Authorization", "Bearer " + properties.openai().key())
            .header("Content-Type", "application/json")
            .timeout(Duration.ofSeconds(properties.openai().requestTimeoutSeconds()))
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build();
    var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    log.info("llm_call status={}", response.statusCode());
    return response.body();
  }
}
