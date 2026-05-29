package com.synapsedx.mailing.procurement;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.moreThanOrExactly;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest
@SpringBatchTest
class ProcurementSearchJobIT {

  private static final Path OUTPUT_CSV = Path.of("target/it-tenders.csv");

  private static final WireMockServer TED_MOCK =
      new WireMockServer(WireMockConfiguration.options().dynamicPort());
  private static final WireMockServer BOAMP_MOCK =
      new WireMockServer(WireMockConfiguration.options().dynamicPort());

  static {
    TED_MOCK.start();
    BOAMP_MOCK.start();
  }

  @Autowired JobLauncherTestUtils jobLauncherTestUtils;

  @DynamicPropertySource
  static void props(DynamicPropertyRegistry r) {
    r.add(
        "ted.search-endpoint", () -> "http://127.0.0.1:" + TED_MOCK.port() + "/v3/notices/search");
    r.add(
        "boamp.records-endpoint",
        () ->
            "http://127.0.0.1:"
                + BOAMP_MOCK.port()
                + "/api/explore/v2.1/catalog/datasets/boamp/records");
    r.add("procurement.input-yml", () -> "src/test/resources/it-queries.yml");
    r.add("procurement.output-csv", () -> OUTPUT_CSV.toString());
    r.add("procurement.throttle-millis", () -> "1");
    r.add("spring.batch.job.enabled", () -> "false");
  }

  @AfterAll
  static void stopMocks() {
    TED_MOCK.stop();
    BOAMP_MOCK.stop();
  }

  @BeforeEach
  void resetMocksAndOutput() throws Exception {
    TED_MOCK.resetAll();
    BOAMP_MOCK.resetAll();
    Files.deleteIfExists(OUTPUT_CSV);
  }

  @Test
  void fetchesTendersFromBothSourcesAndWritesCombinedCsv() throws Exception {
    var tedBody =
        new String(
            new ClassPathResource("fixtures/ted-notices.json").getInputStream().readAllBytes(),
            StandardCharsets.UTF_8);
    var boampBody =
        new String(
            new ClassPathResource("fixtures/boamp-records.json").getInputStream().readAllBytes(),
            StandardCharsets.UTF_8);

    TED_MOCK.stubFor(
        post(urlEqualTo("/v3/notices/search"))
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody(tedBody)));

    BOAMP_MOCK.stubFor(
        get(urlPathEqualTo("/api/explore/v2.1/catalog/datasets/boamp/records"))
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody(boampBody)));

    var execution = jobLauncherTestUtils.launchJob();
    assertThat(execution.getStatus().toString()).isEqualTo("COMPLETED");

    var lines = Files.readAllLines(OUTPUT_CSV);
    assertThat(lines).hasSize(5); // header + 2 TED + 2 BOAMP
    assertThat(lines.get(0))
        .isEqualTo(
            "source,id,title,buyer,country,classification,value,publication_date,deadline,url");

    // TED rows
    assertThat(lines.get(1)).startsWith("TED,TED-001-2026,");
    assertThat(lines.get(1)).contains("Ville de Paris");
    assertThat(lines.get(1)).contains("2026-01-15");
    assertThat(lines.get(2)).startsWith("TED,TED-002-2026,");

    // BOAMP rows
    assertThat(lines.get(3)).startsWith("BOAMP,26-0001,");
    assertThat(lines.get(3)).contains("Mairie de Marseille");
    assertThat(lines.get(4)).startsWith("BOAMP,26-0002,");

    // verify API calls were made
    TED_MOCK.verify(1, postRequestedFor(urlEqualTo("/v3/notices/search")));
    BOAMP_MOCK.verify(
        moreThanOrExactly(1),
        getRequestedFor(urlPathEqualTo("/api/explore/v2.1/catalog/datasets/boamp/records")));
  }
}
