package com.synapsedx.mailing.pipeline.dataforseo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataForSeoClient implements DataForSeoPort {

  private static final String NEWS_ENDPOINT =
      "https://api.dataforseo.com/v3/serp/google/news/live/advanced";
  private static final int LOCATION_CODE_FRANCE = 2250;
  private static final int DEFAULT_DEPTH = 10;

  private final DataForSeoProperties properties;
  private final ObjectMapper objectMapper;
  private final HttpClient httpClient =
      HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build();

  @Override
  public JsonNode searchNews(String keyword) {
    try {
      var body =
          objectMapper.writeValueAsString(
              List.of(
                  Map.of(
                      "keyword", keyword,
                      "language_code", "fr",
                      "depth", DEFAULT_DEPTH,
                      "location_code", LOCATION_CODE_FRANCE,
                      "location_name", "France")));
      var raw = post(body);
      var result = objectMapper.readTree(raw).path("tasks").path(0).path("result").path(0);
      log.info("dataforseo_news_searched keyword={}", keyword);
      return result;
    } catch (Exception e) {
      log.warn("dataforseo_news_failed keyword={} reason={}", keyword, e.getMessage());
      return objectMapper.nullNode();
    }
  }

  private String post(String body) throws Exception {
    var credentials = properties.getApi().getUser() + ":" + properties.getApi().getKey();
    var auth = Base64.getEncoder().encodeToString(credentials.getBytes());
    var request =
        HttpRequest.newBuilder()
            .uri(URI.create(NEWS_ENDPOINT))
            .header("Authorization", "Basic " + auth)
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build();
    var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    log.debug("dataforseo_call status={}", response.statusCode());
    if (response.statusCode() != 200) {
      throw new IllegalStateException("DataForSEO API error: " + response.statusCode());
    }
    return response.body();
  }
}
