package com.synapsedx.mailing.seo.batch.processor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.synapsedx.mailing.seo.config.SeoidProperties;
import com.synapsedx.mailing.seo.model.SeoSummaryResult;
import com.synapsedx.mailing.seo.model.SeoSummaryTask;
import jakarta.annotation.PostConstruct;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SeoSummaryProcessor implements ItemProcessor<SeoSummaryTask, SeoSummaryResult> {

  private final SeoidProperties properties;
  private final ObjectMapper mapper = new ObjectMapper();

  private HttpClient httpClient;
  private String promptTemplate;

  @PostConstruct
  void init() throws Exception {
    var openai = properties.openai();
    httpClient =
        HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(openai.connectTimeoutSeconds()))
            .build();
    promptTemplate =
        new ClassPathResource("seoit-summary.md").getContentAsString(StandardCharsets.UTF_8);
  }

  @Override
  public SeoSummaryResult process(SeoSummaryTask task) throws Exception {
    var article = task.newsItem().article();
    if (article.isBlank()) {
      log.warn(
          "seo_summary_skip queryId={} url={} reason=empty_article",
          task.queryId(),
          task.newsItem().url());
      return new SeoSummaryResult(task.queryId(), task.newsItem(), "");
    }

    var prompt = promptTemplate.replace("{article_content}", article);
    var messages = mapper.createArrayNode();
    messages.add(mapper.createObjectNode().put("role", "user").put("content", prompt));

    var requestNode = mapper.createObjectNode();
    requestNode.put("model", properties.openai().model());
    requestNode.set("messages", messages);
    requestNode.put("temperature", 0.1);

    log.info("seo_summary_start queryId={} url={}", task.queryId(), task.newsItem().url());
    var rawResponse = post(requestNode.toString());
    var summary =
        mapper
            .readTree(rawResponse)
            .path("choices")
            .path(0)
            .path("message")
            .path("content")
            .asText("")
            .strip();

    log.info("seo_summary_done queryId={} chars={}", task.queryId(), summary.length());
    return new SeoSummaryResult(task.queryId(), task.newsItem(), summary);
  }

  private String post(String body) throws Exception {
    var openai = properties.openai();
    var request =
        HttpRequest.newBuilder()
            .uri(URI.create(openai.server().replaceAll("/+$", "") + "/v1/chat/completions"))
            .header("Authorization", "Bearer " + openai.key())
            .header("Content-Type", "application/json")
            .timeout(Duration.ofSeconds(openai.requestTimeoutSeconds()))
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build();
    var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    log.info("llm_call status={}", response.statusCode());
    return response.body();
  }
}
