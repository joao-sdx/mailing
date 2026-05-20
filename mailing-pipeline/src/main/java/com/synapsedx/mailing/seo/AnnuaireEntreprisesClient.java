package com.synapsedx.mailing.seo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.synapsedx.mailing.seo.model.CompanyEnrichment;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class AnnuaireEntreprisesClient {

  private static final String BASE_URL =
      "https://recherche-entreprises.api.gouv.fr/search?limite=1&q=";

  private final ObjectMapper mapper = new ObjectMapper();
  private final HttpClient httpClient =
      HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build();

  public Optional<CompanyEnrichment> enrich(String name) throws Exception {
    var encoded = URLEncoder.encode(name, StandardCharsets.UTF_8);
    var request =
        HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + encoded))
            .header("Accept", "application/json")
            .GET()
            .build();
    var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    log.info("annuaire_search name={} status={}", name, response.statusCode());
    if (response.statusCode() != 200) {
      return Optional.empty();
    }
    var results = mapper.readTree(response.body()).path("results");
    if (!results.isArray() || results.isEmpty()) {
      return Optional.empty();
    }
    return Optional.of(parse(results.get(0)));
  }

  private CompanyEnrichment parse(JsonNode node) {
    return new CompanyEnrichment(
        nullableText(node, "siren"),
        nullableText(node.path("siege"), "activite_principale"),
        sectionLabel(nullableText(node, "section_activite_principale")),
        effectifLabel(nullableText(node, "tranche_effectif_salarie")),
        nullableText(node, "categorie_entreprise"),
        nullableText(node.path("siege"), "libelle_commune"));
  }

  private String nullableText(JsonNode node, String field) {
    var n = node.path(field);
    return n.isNull() || n.isMissingNode() ? null : n.asText(null);
  }

  private String sectionLabel(String code) {
    if (code == null) {
      return null;
    }
    return switch (code) {
      case "K" -> "Finance et assurance";
      case "J" -> "Information et communication";
      case "G" -> "Commerce";
      case "M" -> "Services professionnels";
      case "N" -> "Services administratifs";
      case "Q" -> "Santé";
      case "L" -> "Immobilier";
      case "C" -> "Industrie manufacturière";
      case "F" -> "Construction";
      case "H" -> "Transport";
      case "O" -> "Administration publique";
      case "P" -> "Enseignement";
      case "I" -> "Hébergement et restauration";
      default -> code;
    };
  }

  private String effectifLabel(String code) {
    if (code == null) {
      return null;
    }
    return switch (code) {
      case "NN", "00" -> "0";
      case "01" -> "1-2";
      case "02" -> "3-5";
      case "03" -> "6-9";
      case "11" -> "10-19";
      case "12" -> "20-49";
      case "21" -> "50-99";
      case "22" -> "100-199";
      case "31" -> "200-249";
      case "32" -> "250-499";
      case "41" -> "500-999";
      case "42" -> "1000-1999";
      case "51" -> "2000-4999";
      case "52" -> "5000-9999";
      case "53" -> "10000+";
      default -> code;
    };
  }
}
