package com.synapsedx.mailing.procurement.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.synapsedx.mailing.procurement.config.TedProperties;
import com.synapsedx.mailing.procurement.model.ProcurementQuery;
import com.synapsedx.mailing.procurement.model.Source;
import com.synapsedx.mailing.procurement.model.Tender;
import com.synapsedx.mailing.procurement.query.TedQueryBuilder;
import jakarta.annotation.PostConstruct;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TedClient implements TenderSource {

  private static final List<String> FIELDS =
      List.of(
          "publication-number",
          "notice-title",
          "buyer-name",
          "buyer-country",
          "classification-cpv",
          "total-value",
          "publication-date",
          "deadline-receipt-tender-date-lot",
          "links");

  private final TedProperties properties;
  private final TedQueryBuilder queryBuilder;
  private final ObjectMapper objectMapper = new ObjectMapper();
  private HttpClient httpClient;

  @PostConstruct
  void init() {
    httpClient =
        HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(properties.connectTimeoutSeconds()))
            .build();
  }

  @Override
  public Source source() {
    return Source.TED;
  }

  @Override
  public List<Tender> search(ProcurementQuery query) throws Exception {
    var queryString = queryBuilder.build(query);
    var tenders = new ArrayList<Tender>();
    String nextToken = null;

    do {
      var body = buildRequestBody(queryString, nextToken);
      var raw = post(body);
      if (raw == null) {
        log.warn("ted_rate_limited_stopping total_so_far={}", tenders.size());
        break;
      }
      var root = objectMapper.readTree(raw);
      var notices = root.path("notices");
      if (!notices.isArray() || notices.isEmpty()) {
        break;
      }
      for (var notice : notices) {
        tenders.add(mapToTender(notice));
      }
      nextToken = root.path("iterationNextToken").asText(null);
      if (nextToken != null && nextToken.isBlank()) {
        nextToken = null;
      }
      log.debug(
          "ted_page_fetched query={} page_count={} total_so_far={}",
          queryString,
          notices.size(),
          tenders.size());
    } while (nextToken != null);

    log.info("ted_search_done query={} total={}", queryString, tenders.size());
    return tenders;
  }

  private String buildRequestBody(String queryString, String iterationNextToken) throws Exception {
    var node = objectMapper.createObjectNode();
    node.put("query", queryString);
    node.putPOJO("fields", FIELDS);
    node.put("scope", "ACTIVE");
    node.put("limit", 250);
    node.put("paginationMode", "ITERATION");
    node.put("checkQuerySyntax", false);
    if (iterationNextToken != null) {
      node.put("iterationNextToken", iterationNextToken);
    }
    return objectMapper.writeValueAsString(node);
  }

  private Tender mapToTender(JsonNode notice) {
    var id = notice.path("publication-number").asText(null);
    var title = firstLangValue(notice.path("notice-title"));
    var buyer = firstLangValue(notice.path("buyer-name"));
    var country = notice.path("buyer-country").asText(null);
    var cpvs = new ArrayList<String>();
    for (var cpv : notice.path("classification-cpv")) {
      cpvs.add(cpv.asText());
    }
    var classification = String.join(", ", cpvs);
    var value = notice.path("total-value").asText(null);
    var pubDate = parseDate(notice.path("publication-date").asText(null));
    var deadline = parseDate(notice.path("deadline-receipt-tender-date-lot").asText(null));
    var linksNode = notice.path("links").path("html");
    var urlIt = linksNode.fields();
    var url = urlIt.hasNext() ? urlIt.next().getValue().asText(null) : null;
    return new Tender(
        Source.TED.name(),
        id,
        title,
        buyer,
        country,
        classification,
        value,
        pubDate,
        deadline,
        url,
        "");
  }

  /** Returns the first non-null language value from a multilingual JsonNode map. */
  private String firstLangValue(JsonNode node) {
    if (node == null || node.isNull() || node.isMissingNode()) {
      return null;
    }
    for (var lang : List.of("fra", "eng", "MUL")) {
      var v = node.path(lang).asText(null);
      if (v != null && !v.isBlank()) {
        return v;
      }
    }
    var it = node.fields();
    if (it.hasNext()) {
      return it.next().getValue().asText(null);
    }
    return null;
  }

  private LocalDate parseDate(String raw) {
    if (raw == null || raw.isBlank()) {
      return null;
    }
    try {
      // YYYYMMDD (no separators)
      if (raw.length() == 8 && !raw.contains("-")) {
        return LocalDate.of(
            Integer.parseInt(raw.substring(0, 4)),
            Integer.parseInt(raw.substring(4, 6)),
            Integer.parseInt(raw.substring(6, 8)));
      }
      // YYYY-MM-DD+HH:MM or YYYY-MM-DD-HH:MM (date with timezone offset, no time component)
      if (raw.length() > 10 && (raw.charAt(10) == '+' || raw.charAt(10) == '-')) {
        return LocalDate.parse(raw.substring(0, 10));
      }
      return LocalDate.parse(raw);
    } catch (DateTimeParseException | NumberFormatException e) {
      log.warn("ted_date_parse_failed raw={}", raw);
      return null;
    }
  }

  private String post(String body) throws Exception {
    var request =
        HttpRequest.newBuilder()
            .uri(URI.create(properties.searchEndpoint()))
            .header("Content-Type", "application/json")
            .timeout(Duration.ofSeconds(properties.requestTimeoutSeconds()))
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build();
    var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    log.debug("ted_post status={}", response.statusCode());
    if (response.statusCode() == 429) {
      log.warn("ted_rate_limited");
      return null;
    }
    if (response.statusCode() != 200) {
      throw new IllegalStateException(
          "TED API error status=" + response.statusCode() + " body=" + response.body());
    }
    return response.body();
  }
}
