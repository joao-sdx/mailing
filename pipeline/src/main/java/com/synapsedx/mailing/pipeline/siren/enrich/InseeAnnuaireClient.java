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
import java.util.concurrent.atomic.AtomicInteger;
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
  private static final long CALL_DELAY_MS = 500;
  private static final int MAX_RETRIES = 3;
  private static final long RETRY_BASE_DELAY_MS = 1_000;
  private static final int MAX_CONSECUTIVE_FAILURES = 10;

  private final ObjectMapper objectMapper;
  private final HttpClient httpClient =
      HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build();
  private final AtomicInteger consecutiveFailures = new AtomicInteger(0);

  @Override
  public Optional<AnnuaireEntreprise> findBySiren(String siren) {
    if (consecutiveFailures.get() >= MAX_CONSECUTIVE_FAILURES) {
      log.warn(
          "annuaire_api_circuit_open siren={} consecutive_failures={}",
          siren,
          consecutiveFailures.get());
      return Optional.empty();
    }
    try {
      Thread.sleep(CALL_DELAY_MS);
      var result = doFind(siren, 0);
      consecutiveFailures.set(0);
      return result;
    } catch (Exception e) {
      log.warn(
          "annuaire_api_failed siren={} consecutive={} reason={}",
          siren,
          consecutiveFailures.incrementAndGet(),
          e.toString());
      return Optional.empty();
    }
  }

  private Optional<AnnuaireEntreprise> doFind(String siren, int attempt) throws Exception {
    var encoded = URLEncoder.encode(siren, StandardCharsets.UTF_8);
    var request =
        HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + encoded))
            .header("Accept", "application/json")
            .GET()
            .build();
    var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

    if (response.statusCode() == 429) {
      if (attempt < MAX_RETRIES) {
        var delay = RETRY_BASE_DELAY_MS * (1L << attempt);
        log.warn(
            "annuaire_api_rate_limited siren={} retry={} wait={}ms", siren, attempt + 1, delay);
        Thread.sleep(delay);
        return doFind(siren, attempt + 1);
      }
      throw new IllegalStateException("rate_limited_exhausted status=429 siren=" + siren);
    }

    if (response.statusCode() != 200) {
      throw new IllegalStateException(
          "unexpected_status=" + response.statusCode() + " siren=" + siren);
    }

    var parsed = objectMapper.readValue(response.body(), AnnuaireResponse.class);
    if (parsed.results() == null || parsed.results().isEmpty()) {
      return Optional.empty();
    }
    return Optional.of(parsed.results().getFirst());
  }
}
