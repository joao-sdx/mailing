package com.synapsedx.mailing.sedia.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.synapsedx.mailing.sedia.config.SediaProperties;
import com.synapsedx.mailing.sedia.model.FundingCall;
import com.synapsedx.mailing.sedia.model.SearchPage;
import jakarta.annotation.PostConstruct;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SediaSearchClient {

  private static final String BOUNDARY = "SediaBoundary42";
  private static final String CRLF = "\r\n";
  private static final String SORT_JSON = "{\"field\":\"deadlineDate\",\"order\":\"ASC\"}";
  private static final String LANGUAGES_JSON = "[\"en\"]";

  private final SediaProperties properties;
  private final ObjectMapper mapper = new ObjectMapper();

  private HttpClient httpClient;

  @PostConstruct
  void init() {
    httpClient = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build();
  }

  public SearchPage search(int pageNumber) throws Exception {
    var queryJson = buildQueryJson();
    var body = buildMultipartBody(queryJson);
    var url =
        properties.searchEndpoint().replaceAll("/+$", "")
            + "?apiKey="
            + properties.apiKey()
            + "&text="
            + URLEncoder.encode(properties.text(), StandardCharsets.UTF_8)
            + "&pageSize="
            + properties.pageSize()
            + "&pageNumber="
            + pageNumber
            + "&language=en";

    var request =
        HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Content-Type", "multipart/form-data; boundary=" + BOUNDARY)
            .timeout(Duration.ofSeconds(30))
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build();

    var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    log.debug("sedia_search_response page={} status={}", pageNumber, response.statusCode());
    if (response.statusCode() != 200) {
      throw new IllegalStateException(
          "SEDIA error status=" + response.statusCode() + " body=" + response.body());
    }
    return parseResponse(response.body(), pageNumber);
  }

  private String buildQueryJson() throws Exception {
    var root = mapper.createObjectNode();
    var bool = root.putObject("bool");
    var must = bool.putArray("must");
    must.addObject()
        .set(
            "terms", mapper.createObjectNode().set("type", mapper.valueToTree(properties.types())));
    must.addObject()
        .set(
            "terms",
            mapper.createObjectNode().set("status", mapper.valueToTree(properties.statuses())));
    must.addObject()
        .set(
            "terms",
            mapper
                .createObjectNode()
                .set("frameworkProgramme", mapper.valueToTree(properties.frameworkProgrammes())));
    return mapper.writeValueAsString(root);
  }

  private String buildMultipartBody(String queryJson) {
    var sb = new StringBuilder();
    appendPart(sb, "query", queryJson);
    appendPart(sb, "sort", SORT_JSON);
    appendPart(sb, "languages", LANGUAGES_JSON);
    sb.append("--").append(BOUNDARY).append("--").append(CRLF);
    return sb.toString();
  }

  private void appendPart(StringBuilder sb, String name, String json) {
    sb.append("--").append(BOUNDARY).append(CRLF);
    sb.append("Content-Disposition: form-data; name=\"").append(name).append("\"").append(CRLF);
    sb.append("Content-Type: application/json").append(CRLF);
    sb.append(CRLF);
    sb.append(json).append(CRLF);
  }

  private SearchPage parseResponse(String body, int pageNumber) throws Exception {
    var root = mapper.readTree(body);
    var totalResults = root.path("totalResults").asInt(0);
    var calls = new ArrayList<FundingCall>();

    for (var result : root.path("results")) {
      var meta = result.path("metadata");
      calls.add(
          new FundingCall(
              firstOf(meta, "identifier"),
              firstOf(meta, "callIdentifier"),
              result.path("title").asText(""),
              firstOf(meta, "frameworkProgramme"),
              firstOf(meta, "status"),
              firstOf(meta, "deadlineDate"),
              firstOf(meta, "startDate"),
              firstOf(meta, "budget"),
              result.path("url").asText(""),
              result.path("summary").asText("")));
    }

    log.info("sedia_page_parsed page={} calls={} total={}", pageNumber, calls.size(), totalResults);
    return new SearchPage(totalResults, pageNumber, calls);
  }

  private String firstOf(JsonNode metadata, String field) {
    var node = metadata.path(field);
    if (node.isMissingNode() || !node.isArray() || node.isEmpty()) {
      return "";
    }
    return node.get(0).asText("");
  }
}
