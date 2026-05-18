package com.synapsedx.mailing.seo.batch.writer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.synapsedx.mailing.seo.batch.SeoJobContext;
import com.synapsedx.mailing.seo.config.DataForSeoProperties;
import com.synapsedx.mailing.seo.model.DataForSeoRequest;
import com.synapsedx.mailing.seo.model.NewsItem;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
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
  private static final Path OUTPUT_DIR = Path.of("output");

  private final DataForSeoProperties properties;
  private final SeoJobContext jobContext;
  private final ObjectMapper mapper = new ObjectMapper();
  private final HttpClient httpClient = HttpClient.newHttpClient();

  @Override
  public void write(Chunk<? extends DataForSeoRequest> chunk) throws Exception {
    var articlesDir = OUTPUT_DIR.resolve("seo/articles-" + jobContext.jobStartTime());
    Files.createDirectories(OUTPUT_DIR);
    Files.createDirectories(articlesDir);

    for (var request : chunk.getItems()) {
      var rawResponse = post(NEWS_ENDPOINT, request.body());
      saveRaw(request, rawResponse);

      var newsItems = parseAndFetchArticles(rawResponse, articlesDir);
      saveRes(request, newsItems);
    }
  }

  private List<NewsItem> parseAndFetchArticles(String rawResponse, Path articlesDir)
      throws Exception {
    var root = mapper.readTree(rawResponse);
    var items = root.path("tasks").path(0).path("result").path(0).path("items");
    var newsItems = new ArrayList<NewsItem>();

    for (var item : items) {
      var url = item.path("url").asText(null);
      var domain = item.path("domain").asText(null);
      var title = item.path("title").asText(null);
      var timePublished = item.path("time_published").asText(null);

      if (url == null) {
        continue;
      }

      var articleId = String.valueOf(jobContext.nextArticleId());
      var textFilePath = articlesDir.resolve(articleId + ".md");
      fetchAndSaveArticle(url, textFilePath);

      newsItems.add(new NewsItem(domain, title, url, timePublished, textFilePath.toString()));
      log.info("article_processed id={} url={}", articleId, url);
    }

    return newsItems;
  }

  private void fetchAndSaveArticle(String url, Path destination) {
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
      Files.writeString(destination, md.toString().strip());
    } catch (Exception e) {
      log.warn("content_parsing_failed url={} reason={}", url, e.getMessage());
    }
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

  private void saveRaw(DataForSeoRequest request, String body) throws Exception {
    var filename = rawFilename(request);
    Files.writeString(OUTPUT_DIR.resolve(filename), body);
    log.info("dataforseo_saved file={}", filename);
  }

  private void saveRes(DataForSeoRequest request, List<NewsItem> items) throws Exception {
    var filename = rawFilename(request).replace(".json", "-res.json");
    Files.writeString(OUTPUT_DIR.resolve(filename), mapper.writeValueAsString(items));
    log.info("dataforseo_res_saved file={} items={}", filename, items.size());
  }

  private String rawFilename(DataForSeoRequest request) {
    return "seo-" + request.query().filePrefix() + "-" + jobContext.jobStartTime() + ".json";
  }
}
