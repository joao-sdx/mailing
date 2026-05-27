package com.synapsedx.mailing.seonews.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.synapsedx.mailing.seonews.config.DataForSeoProperties;
import com.synapsedx.mailing.seonews.model.RawNewsItem;
import com.synapsedx.mailing.seonews.model.SearchQuery;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataForSeoClient {

  private static final String NEWS_ENDPOINT =
      "https://api.dataforseo.com/v3/serp/google/news/live/advanced";
  private static final String CONTENT_ENDPOINT =
      "https://api.dataforseo.com/v3/on_page/content_parsing/live";

  private final DataForSeoProperties properties;
  private final ObjectMapper objectMapper;
  private final HttpClient httpClient =
      HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build();

  public List<RawNewsItem> searchNews(SearchQuery query) throws Exception {
    var body =
        objectMapper.writeValueAsString(
            List.of(
                Map.of(
                    "keyword", query.keyword(),
                    "language_code", query.languageCode(),
                    "depth", query.depth(),
                    "location_code", query.locationCode(),
                    "location_name", query.locationName())));
    var raw = post(NEWS_ENDPOINT, body);
    var items =
        objectMapper.readTree(raw).path("tasks").path(0).path("result").path(0).path("items");
    var result = new ArrayList<RawNewsItem>();
    for (var item : items) {
      var url = item.path("url").asText(null);
      if (url == null) {
        continue;
      }
      result.add(
          new RawNewsItem(
              item.path("title").asText(null),
              url,
              item.path("domain").asText(null),
              item.path("time_published").asText(null)));
    }
    log.info("dataforseo_news_found keyword={} count={}", query.keyword(), result.size());
    return result;
  }

  public String fetchContent(String url) {
    try {
      var body =
          objectMapper.writeValueAsString(List.of(objectMapper.createObjectNode().put("url", url)));
      var raw = post(CONTENT_ENDPOINT, body);
      var topics =
          objectMapper
              .readTree(raw)
              .path("tasks")
              .path(0)
              .path("result")
              .path(0)
              .path("items")
              .path(0)
              .path("page_content")
              .path("main_topic");
      var md = new StringBuilder();
      for (var topic : topics) {
        var title = topic.path("h_title").asText("").strip();
        if (!title.isBlank()) {
          md.append("## ").append(title).append("\n\n");
        }
        for (var content : topic.path("primary_content")) {
          var text = content.path("text").asText("").strip();
          if (!text.isBlank()) {
            md.append(text).append("\n\n");
          }
        }
      }
      var result = md.toString().strip();
      log.info("content_fetched url={} chars={}", url, result.length());
      return result;
    } catch (Exception e) {
      log.warn("content_fetch_failed url={} reason={}", url, e.getMessage());
      return "";
    }
  }

  private String post(String endpoint, String body) throws Exception {
    var credentials = properties.api().user() + ":" + properties.api().key();
    var auth = Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
    var request =
        HttpRequest.newBuilder()
            .uri(URI.create(endpoint))
            .header("Authorization", "Basic " + auth)
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build();
    var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    log.debug("dataforseo_post endpoint={} status={}", endpoint, response.statusCode());
    if (response.statusCode() != 200) {
      throw new IllegalStateException("DataForSEO error status=" + response.statusCode());
    }
    return response.body();
  }
}
