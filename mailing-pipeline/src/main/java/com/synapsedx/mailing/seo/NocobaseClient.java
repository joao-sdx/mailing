package com.synapsedx.mailing.seo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.synapsedx.mailing.seo.config.NocobaseProperties;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NocobaseClient {

  private final NocobaseProperties properties;
  private final ObjectMapper mapper = new ObjectMapper();
  private final HttpClient httpClient =
      HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build();

  public int create(String collection, Map<String, Object> fields) throws Exception {
    var body = mapper.writeValueAsString(fields);
    var request = post(collection + ":create", body);
    var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    log.info("nocobase_create collection={} status={}", collection, response.statusCode());
    assertOk(response, "create " + collection);
    return mapper.readTree(response.body()).path("data").path("id").asInt();
  }

  public Optional<Integer> findByUrl(String collection, String url) throws Exception {
    var filter = URLEncoder.encode("{\"url\":\"" + url + "\"}", StandardCharsets.UTF_8);
    var uri =
        URI.create(
            properties.url() + "/api/" + collection + ":list?filter=" + filter + "&pageSize=1");
    var request =
        HttpRequest.newBuilder()
            .uri(uri)
            .header("Authorization", "Bearer " + properties.apiKey())
            .header("Content-Type", "application/json")
            .GET()
            .build();
    var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    assertOk(response, "findByUrl " + collection);
    var list = mapper.readTree(response.body()).path("data").path("list");
    if (list.isArray() && !list.isEmpty()) {
      return Optional.of(list.get(0).path("id").asInt());
    }
    return Optional.empty();
  }

  public void addRelation(String collection, int recordId, String relation, int targetId)
      throws Exception {
    var body = mapper.writeValueAsString(List.of(targetId));
    var path = collection + "/" + recordId + "/" + relation + ":add";
    var request = post(path, body);
    var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    log.info(
        "nocobase_relation collection={} id={} relation={} target={} status={}",
        collection,
        recordId,
        relation,
        targetId,
        response.statusCode());
    assertOk(response, "addRelation " + collection + "/" + recordId + "/" + relation);
  }

  private void assertOk(HttpResponse<String> response, String context) {
    if (response.statusCode() < 200 || response.statusCode() >= 300) {
      throw new RuntimeException(
          "nocobase_%s_failed status=%d body=%s"
              .formatted(
                  context,
                  response.statusCode(),
                  response.body().substring(0, Math.min(200, response.body().length()))));
    }
    try {
      var errors = mapper.readTree(response.body()).path("errors");
      if (errors.isArray() && !errors.isEmpty()) {
        throw new RuntimeException(
            "nocobase_%s_error %s".formatted(context, errors.get(0).path("message").asText()));
      }
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception ignored) {
    }
  }

  private HttpRequest post(String path, String body) {
    return HttpRequest.newBuilder()
        .uri(URI.create(properties.url() + "/api/" + path))
        .header("Authorization", "Bearer " + properties.apiKey())
        .header("Content-Type", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(body))
        .build();
  }
}
