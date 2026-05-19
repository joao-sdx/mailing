package com.synapsedx.mailing.seo.batch.writer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.synapsedx.mailing.seo.NocobaseClient;
import com.synapsedx.mailing.seo.batch.SeoJobContext;
import com.synapsedx.mailing.seo.config.DataForSeoProperties;
import com.synapsedx.mailing.seo.model.DataForSeoRequest;
import com.synapsedx.mailing.seo.model.NewsItem;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataForSeoResponseWriter implements ItemWriter<DataForSeoRequest> {

  private static final String NEWS_ENDPOINT =
      "https://api.dataforseo.com/v3/serp/google/news/live/advanced";
  private static final String CONTENT_ENDPOINT =
      "https://api.dataforseo.com/v3/on_page/content_parsing/live";

  private final DataForSeoProperties properties;
  private final SeoJobContext jobContext;
  private final NocobaseClient nocobase;
  private final ObjectMapper mapper = new ObjectMapper();
  private final HttpClient httpClient = HttpClient.newHttpClient();

  @Override
  public void write(Chunk<? extends DataForSeoRequest> chunk) throws Exception {
    for (var request : chunk.getItems()) {
      var rawResponse = post(NEWS_ENDPOINT, request.body());
      var newsItems = parseItems(rawResponse);

      var queryId = nocobase.create("seo_query", queryFields(request));
      log.info("seo_query_created id={} keyword={}", queryId, request.query().keyword());

      for (var item : newsItems) {
        var resultId =
            nocobase.findByUrl("seo_result", item.url()).orElseGet(() -> createResult(item));
        nocobase.addRelation("seo_query", queryId, "results", resultId);
        log.info("seo_result_linked queryId={} resultId={} url={}", queryId, resultId, item.url());
      }
    }
  }

  private int createResult(NewsItem item) {
    try {
      return nocobase.create("seo_result", resultFields(item));
    } catch (Exception e) {
      throw new RuntimeException("Failed to create seo_result for url=" + item.url(), e);
    }
  }

  private List<NewsItem> parseItems(String rawResponse) throws Exception {
    var root = mapper.readTree(rawResponse);
    var items = root.path("tasks").path(0).path("result").path(0).path("items");
    var newsItems = new ArrayList<NewsItem>();

    for (var item : items) {
      var url = item.path("url").asText(null);
      if (url == null) {
        continue;
      }
      var timePublished = item.path("time_published").asText(null);
      var article = fetchArticle(url);
      newsItems.add(
          new NewsItem(
              item.path("domain").asText(null),
              item.path("title").asText(null),
              url,
              timePublished,
              article));
      log.info(
          "article_fetched url={} time_published=[{}] chars={}",
          url,
          timePublished,
          article.length());
    }

    return newsItems;
  }

  private String fetchArticle(String url) {
    try {
      var body = mapper.writeValueAsString(List.of(mapper.createObjectNode().put("url", url)));
      var response = post(CONTENT_ENDPOINT, body);
      var root = mapper.readTree(response);
      var topics =
          root.path("tasks")
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
      return md.toString().strip();
    } catch (Exception e) {
      log.warn("content_parsing_failed url={} reason={}", url, e.getMessage());
      return "";
    }
  }

  private Map<String, Object> queryFields(DataForSeoRequest request) {
    var q = request.query();
    var fields = new LinkedHashMap<String, Object>();
    fields.put("keyword", q.keyword());
    fields.put("language_code", q.languageCode());
    fields.put("depth", q.depth());
    fields.put("location_code", q.locationCode());
    fields.put("location_name", q.locationName());
    fields.put("file_prefix", q.filePrefix());
    fields.put("execution_date", Instant.ofEpochSecond(jobContext.jobStartTime()).toString());
    return fields;
  }

  private Map<String, Object> resultFields(NewsItem item) {
    var fields = new LinkedHashMap<String, Object>();
    fields.put("type", "news");
    fields.put("title", item.title());
    fields.put("domain", item.domain());
    fields.put("url", item.url());
    fields.put("article", item.article());
    return fields;
  }

  private String post(String endpoint, String body) throws Exception {
    var credentials = properties.api().user() + ":" + properties.api().key();
    var auth = Base64.getEncoder().encodeToString(credentials.getBytes());
    var request =
        HttpRequest.newBuilder()
            .uri(URI.create(endpoint))
            .header("Authorization", "Basic " + auth)
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build();
    var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    log.info("dataforseo_call endpoint={} status={}", endpoint, response.statusCode());
    return response.body();
  }
}
