package com.synapsedx.mailing.pipeline.siren.enrich;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.synapsedx.mailing.pipeline.siren.enrich.model.annuaire.AnnuaireEntreprise;
import com.synapsedx.mailing.pipeline.siren.enrich.model.annuaire.AnnuaireResponse;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/** HTTP client for the French government Annuaire des Entreprises API, queried by SIREN. */
@Slf4j
@Component
@RequiredArgsConstructor
public class InseeAnnuaireClient implements InseeAnnuairePort {

  private static final String BASE_URL =
      "https://recherche-entreprises.api.gouv.fr/search?limite=1&q=";

  private final ObjectMapper objectMapper;
  private final HttpClient httpClient =
      HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build();

  public Optional<AnnuaireEntreprise> findBySiren(String siren) {
    try {
      var encoded = URLEncoder.encode(siren, StandardCharsets.UTF_8);
      var request =
          HttpRequest.newBuilder()
              .uri(URI.create(BASE_URL + encoded))
              .header("Accept", "application/json")
              .GET()
              .build();
      var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() != 200) {
        log.warn("annuaire_api_error siren={} status={}", siren, response.statusCode());
        return Optional.empty();
      }
      var parsed = objectMapper.readValue(response.body(), AnnuaireResponse.class);
      if (parsed.results() == null || parsed.results().isEmpty()) {
        return Optional.empty();
      }
      return Optional.of(parsed.results().getFirst());
    } catch (Exception e) {
      log.warn("annuaire_api_failed siren={} reason={}", siren, e.getMessage());
      return Optional.empty();
    }
  }
}
