package com.synapsedx.mailing.companydomain;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
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
class CompanyDomainLookupJobIT {

  private static final Path OUTPUT_CSV = Path.of("target/it-enriched.csv");
  private static final WireMockServer DATAFORSEO_MOCK =
      new WireMockServer(WireMockConfiguration.options().dynamicPort());
  private static final WireMockServer LMSTUDIO_MOCK =
      new WireMockServer(WireMockConfiguration.options().dynamicPort());

  static {
    DATAFORSEO_MOCK.start();
    LMSTUDIO_MOCK.start();
  }

  @Autowired JobLauncherTestUtils jobLauncherTestUtils;

  @DynamicPropertySource
  static void props(DynamicPropertyRegistry r) {
    r.add("dataforseo.api.user", () -> "u");
    r.add("dataforseo.api.key", () -> "k");
    r.add(
        "dataforseo.serp-organic-endpoint",
        () ->
            "http://127.0.0.1:" + DATAFORSEO_MOCK.port() + "/v3/serp/google/organic/live/advanced");
    r.add("lmstudio.server", () -> "http://127.0.0.1:" + LMSTUDIO_MOCK.port());
    r.add("company-domain.input-csv", () -> "src/test/resources/fixtures/it-contacts.csv");
    r.add("company-domain.output-csv", () -> OUTPUT_CSV.toString());
    r.add("company-domain.serp-depth", () -> "10");
    r.add("company-domain.serp-top-n", () -> "5");
    r.add("spring.batch.job.enabled", () -> "false");
  }

  @AfterAll
  static void stopMocks() {
    DATAFORSEO_MOCK.stop();
    LMSTUDIO_MOCK.stop();
  }

  @BeforeEach
  void resetMocksAndOutput() throws Exception {
    DATAFORSEO_MOCK.resetAll();
    LMSTUDIO_MOCK.resetAll();
    Files.deleteIfExists(OUTPUT_CSV);
  }

  @Test
  void enrichesContactsWithDomain() throws Exception {
    var factoBody =
        new String(
            new ClassPathResource("fixtures/dataforseo-organic-factofrance.json")
                .getInputStream()
                .readAllBytes(),
            StandardCharsets.UTF_8);
    var emptyBody =
        new String(
            new ClassPathResource("fixtures/dataforseo-organic-empty.json")
                .getInputStream()
                .readAllBytes(),
            StandardCharsets.UTF_8);

    DATAFORSEO_MOCK.stubFor(
        post(urlEqualTo("/v3/serp/google/organic/live/advanced"))
            .withRequestBody(containing("Factofrance"))
            .willReturn(aResponse().withStatus(200).withBody(factoBody)));
    DATAFORSEO_MOCK.stubFor(
        post(urlEqualTo("/v3/serp/google/organic/live/advanced"))
            .withRequestBody(containing("ARTZAINAK"))
            .willReturn(aResponse().withStatus(200).withBody(emptyBody)));
    DATAFORSEO_MOCK.stubFor(
        post(urlEqualTo("/v3/serp/google/organic/live/advanced"))
            .withRequestBody(containing("Mutuel"))
            .willReturn(aResponse().withStatus(200).withBody(emptyBody)));

    LMSTUDIO_MOCK.stubFor(
        post(urlEqualTo("/v1/chat/completions"))
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody(
                        "{\"choices\":[{\"message\":{\"content\":\"{\\\"domain\\\":\\\"https://www.factofrance.com/\\\"}\"}}]}")));

    var execution = jobLauncherTestUtils.launchJob();
    assertThat(execution.getStatus().toString()).isEqualTo("COMPLETED");

    var lines = Files.readAllLines(OUTPUT_CSV);
    assertThat(lines).hasSize(6);
    assertThat(lines.get(0)).isEqualTo("first_name,last_name,company,article_id,domain");
    assertThat(lines.get(1)).isEqualTo("Philippe,Mutin,Factofrance,r1.md,factofrance.com");
    assertThat(lines.get(2)).isEqualTo("Marc,Tyan,Factofrance,r2.md,factofrance.com");
    assertThat(lines.get(3)).isEqualTo("Beñat,Cazanave,ARTZAINAK,r3.md,");
    assertThat(lines.get(4)).isEqualTo("Isabelle,Gautier,Crédit Mutuel,r4.md,");
    assertThat(lines.get(5)).isEqualTo("Jean,Test,,r5.md,");
  }
}
