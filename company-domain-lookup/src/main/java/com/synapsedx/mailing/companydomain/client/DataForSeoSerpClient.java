package com.synapsedx.mailing.companydomain.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.synapsedx.mailing.companydomain.config.DataForSeoProperties;
import com.synapsedx.mailing.companydomain.model.SerpResult;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataForSeoSerpClient {

  @Value(
      "${dataforseo.serp-organic-endpoint:https://api.dataforseo.com/v3/serp/google/organic/live/advanced}")
  private String organicEndpoint;

  private final DataForSeoProperties properties;
  private final ObjectMapper objectMapper = new ObjectMapper();
  private final HttpClient httpClient =
      HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build();

  public List<SerpResult> searchOrganic(String company, int depth) throws Exception {
    var body =
        objectMapper.writeValueAsString(
            List.of(
                Map.of(
                    "keyword",
                    company,
                    "language_code",
                    "fr",
                    "depth",
                    depth,
                    "location_code",
                    2250,
                    "location_name",
                    "France")));
    var raw = post(organicEndpoint, body);
    var items =
        objectMapper.readTree(raw).path("tasks").path(0).path("result").path(0).path("items");
    var results = new ArrayList<SerpResult>();
    for (var item : items) {
      if (!"organic".equals(item.path("type").asText(""))) {
        continue;
      }
      var url = item.path("url").asText(null);
      if (url == null) {
        continue;
      }
      results.add(
          new SerpResult(item.path("title").asText(""), url, item.path("description").asText("")));
    }
    log.info("dataforseo_serp_organic company={} count={}", company, results.size());
    return results;
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
