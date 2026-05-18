package com.synapsedx.mailing.seo.batch.writer;

import com.synapsedx.mailing.seo.config.DataForSeoProperties;
import com.synapsedx.mailing.seo.model.DataForSeoRequest;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Base64;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataForSeoResponseWriter implements ItemWriter<DataForSeoRequest> {

  private static final String ENDPOINT =
      "https://api.dataforseo.com/v3/serp/google/news/live/advanced";
  private static final Path OUTPUT_DIR = Path.of("output");

  private final DataForSeoProperties properties;
  private final HttpClient httpClient = HttpClient.newHttpClient();

  @Override
  public void write(Chunk<? extends DataForSeoRequest> chunk) throws Exception {
    Files.createDirectories(OUTPUT_DIR);
    for (var request : chunk.getItems()) {
      var responseBody = callApi(request);
      persist(request, responseBody);
    }
  }

  private String callApi(DataForSeoRequest request) throws Exception {
    var credentials = properties.api().user() + ":" + properties.api().key();
    var auth = Base64.getEncoder().encodeToString(credentials.getBytes());
    var httpRequest =
        HttpRequest.newBuilder()
            .uri(URI.create(ENDPOINT))
            .header("Authorization", "Basic " + auth)
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(request.body()))
            .build();
    var response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
    log.info(
        "dataforseo_response keyword={} status={}",
        request.query().keyword(),
        response.statusCode());
    return response.body();
  }

  private void persist(DataForSeoRequest request, String responseBody) throws Exception {
    var query = request.query();
    var filename = "seo-" + query.filePrefix() + "-" + Instant.now().getEpochSecond() + ".json";
    Files.writeString(OUTPUT_DIR.resolve(filename), responseBody);
    log.info("dataforseo_saved file={}", filename);
  }
}
