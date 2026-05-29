package com.synapsedx.mailing.procurement.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.synapsedx.mailing.procurement.config.BoampProperties;
import com.synapsedx.mailing.procurement.model.ProcurementQuery;
import com.synapsedx.mailing.procurement.model.Source;
import com.synapsedx.mailing.procurement.model.Tender;
import com.synapsedx.mailing.procurement.query.BoampQueryBuilder;
import jakarta.annotation.PostConstruct;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class BoampClient implements TenderSource {

  private static final int PAGE_SIZE = 100;
  private static final int MAX_OFFSET = 9900;

  private static final String SELECT =
      "idweb,id,objet,nomacheteur,dateparution,datelimitereponse,descripteur_libelle,url_avis";

  private final BoampProperties properties;
  private final BoampQueryBuilder queryBuilder;
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
    return Source.BOAMP;
  }

  @Override
  public List<Tender> search(ProcurementQuery query) throws Exception {
    var boampQuery = queryBuilder.build(query);
    var tenders = new ArrayList<Tender>();
    var offset = 0;
    var totalCount = -1;

    do {
      var url = buildUrl(boampQuery, offset);
      var response = get(url);

      if (response.statusCode() == 429) {
        log.warn("boamp_rate_limited offset={}", offset);
        break;
      }
      if (response.statusCode() != 200) {
        throw new IllegalStateException("BOAMP API error status=" + response.statusCode());
      }

      var root = objectMapper.readTree(response.body());
      if (totalCount < 0) {
        totalCount = root.path("total_count").asInt(0);
      }

      var results = root.path("results");
      for (var record : results) {
        tenders.add(mapToTender(record));
      }

      log.debug(
          "boamp_page_fetched offset={} page_count={} total_so_far={}",
          offset,
          results.size(),
          tenders.size());

      if (results.size() < PAGE_SIZE) {
        break;
      }
      offset += PAGE_SIZE;
    } while (offset <= MAX_OFFSET);

    if (totalCount > tenders.size()) {
      log.warn("boamp_results_truncated total={} fetched={}", totalCount, tenders.size());
    }
    log.info("boamp_search_done where={} total={}", boampQuery.where(), tenders.size());
    return tenders;
  }

  private String buildUrl(BoampQueryBuilder.BoampQuery boampQuery, int offset) {
    var sb = new StringBuilder(properties.recordsEndpoint());
    sb.append("?select=").append(encode(SELECT));
    sb.append("&order_by=").append(encode("dateparution DESC"));
    sb.append("&limit=").append(PAGE_SIZE);
    sb.append("&offset=").append(offset);
    if (boampQuery.where() != null && !boampQuery.where().isBlank()) {
      sb.append("&where=").append(encode(boampQuery.where()));
    }
    for (var refine : boampQuery.refineParams()) {
      sb.append("&refine=").append(encode(refine));
    }
    if (properties.apiKey() != null && !properties.apiKey().isBlank()) {
      sb.append("&apikey=").append(encode(properties.apiKey()));
    }
    return sb.toString();
  }

  private Tender mapToTender(JsonNode record) {
    var id = firstNonNull(record.path("idweb").asText(null), record.path("id").asText(null));
    var title = record.path("objet").asText(null);
    var buyer = record.path("nomacheteur").asText(null);
    var country = "FRA";
    var descriptors = new ArrayList<String>();
    for (var d : record.path("descripteur_libelle")) {
      descriptors.add(d.asText());
    }
    var classification = String.join(", ", descriptors);
    var pubDate = parseDate(record.path("dateparution").asText(null));
    var deadline = parseDeadline(record.path("datelimitereponse").asText(null));
    var url = record.path("url_avis").asText(null);
    return new Tender(
        Source.BOAMP.name(),
        id,
        title,
        buyer,
        country,
        classification,
        null,
        pubDate,
        deadline,
        url);
  }

  private LocalDate parseDate(String raw) {
    if (raw == null || raw.isBlank()) {
      return null;
    }
    try {
      return LocalDate.parse(raw);
    } catch (DateTimeParseException e) {
      log.warn("boamp_date_parse_failed raw={}", raw);
      return null;
    }
  }

  private LocalDate parseDeadline(String raw) {
    if (raw == null || raw.isBlank()) {
      return null;
    }
    try {
      return OffsetDateTime.parse(raw).toLocalDate();
    } catch (DateTimeParseException e) {
      try {
        return LocalDate.parse(raw);
      } catch (DateTimeParseException e2) {
        log.warn("boamp_deadline_parse_failed raw={}", raw);
        return null;
      }
    }
  }

  private String firstNonNull(String a, String b) {
    return a != null ? a : b;
  }

  private String encode(String value) {
    return URLEncoder.encode(value, StandardCharsets.UTF_8);
  }

  private HttpResponse<String> get(String url) throws Exception {
    var request =
        HttpRequest.newBuilder()
            .uri(URI.create(url))
            .timeout(Duration.ofSeconds(properties.requestTimeoutSeconds()))
            .GET()
            .build();
    var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    log.debug("boamp_get url={} status={}", url, response.statusCode());
    return response;
  }
}
