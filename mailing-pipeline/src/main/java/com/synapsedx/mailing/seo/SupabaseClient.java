package com.synapsedx.mailing.seo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.synapsedx.mailing.seo.config.SupabaseProperties;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SupabaseClient {

  private final SupabaseProperties properties;
  private final ObjectMapper mapper = new ObjectMapper();
  private final HttpClient httpClient =
      HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build();

  public int create(String table, Map<String, Object> fields) throws Exception {
    var body = mapper.writeValueAsString(fields);
    var request =
        base(table)
            .header("Prefer", "return=representation")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build();
    var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    log.info("supabase_create table={} status={}", table, response.statusCode());
    assertOk(response, "create " + table);
    return mapper.readTree(response.body()).get(0).path("id").asInt();
  }

  public void update(String table, int id, Map<String, Object> fields) throws Exception {
    var body = mapper.writeValueAsString(fields);
    var request =
        base(table + "?id=eq." + id)
            .method("PATCH", HttpRequest.BodyPublishers.ofString(body))
            .build();
    var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    log.info("supabase_update table={} id={} status={}", table, id, response.statusCode());
    assertOk(response, "update " + table + "/" + id);
  }

  public void addRelation(String collection, int recordId, String relation, int targetId)
      throws Exception {
    switch (collection + "." + relation) {
      case "seo_query.results" ->
          // Link seo_result to its parent seo_query via FK column
          update("seo_result", targetId, Map.of("seo_query_id", recordId));
      case "crm_contacts.seo_articles" -> {
        // Insert into join table; ignore if already linked
        var body =
            mapper.writeValueAsString(Map.of("seo_result_id", targetId, "contact_id", recordId));
        var request =
            base("seo_result_contacts")
                .header("Prefer", "resolution=ignore-duplicates")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        log.info(
            "supabase_relation collection={} recordId={} relation={} targetId={} status={}",
            collection,
            recordId,
            relation,
            targetId,
            response.statusCode());
        assertOk(response, "addRelation " + collection + "." + relation);
      }
      default ->
          throw new IllegalArgumentException("Unknown relation: " + collection + "." + relation);
    }
  }

  public Optional<Integer> findByUrl(String table, String url) throws Exception {
    return findFirst(table, Map.of("url", url));
  }

  public Optional<Integer> findFirst(String table, Map<String, Object> filter) throws Exception {
    var nodes = list(table, filter, 1, 1);
    return nodes.isEmpty() ? Optional.empty() : Optional.of(nodes.getFirst().path("id").asInt());
  }

  public List<JsonNode> list(String table, Map<String, Object> filter, int page, int pageSize)
      throws Exception {
    var params = new StringBuilder();
    filter.forEach(
        (k, v) ->
            params
                .append(k)
                .append("=eq.")
                .append(URLEncoder.encode(String.valueOf(v), StandardCharsets.UTF_8))
                .append("&"));
    params
        .append("limit=")
        .append(pageSize)
        .append("&offset=")
        .append((long) (page - 1) * pageSize);
    var request = base(table + "?" + params).GET().build();
    var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    assertOk(response, "list " + table);
    var result = new ArrayList<JsonNode>();
    mapper.readTree(response.body()).forEach(result::add);
    return result;
  }

  private HttpRequest.Builder base(String path) {
    return HttpRequest.newBuilder()
        .uri(URI.create(properties.url() + "/rest/v1/" + path))
        .header("apikey", properties.anonKey())
        .header("Authorization", "Bearer " + properties.serviceRoleKey())
        .header("Content-Type", "application/json");
  }

  private void assertOk(HttpResponse<String> response, String context) {
    if (response.statusCode() < 200 || response.statusCode() >= 300) {
      throw new RuntimeException(
          "supabase_%s_failed status=%d body=%s"
              .formatted(
                  context,
                  response.statusCode(),
                  response.body().substring(0, Math.min(200, response.body().length()))));
    }
  }
}
