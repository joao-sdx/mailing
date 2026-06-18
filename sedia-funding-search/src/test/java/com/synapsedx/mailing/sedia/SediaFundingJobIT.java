package com.synapsedx.mailing.sedia;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
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
class SediaFundingJobIT {

  private static final Path OUTPUT_CSV = Path.of("target/it-sedia-calls.csv");

  private static final WireMockServer SEDIA_MOCK =
      new WireMockServer(WireMockConfiguration.options().dynamicPort());
  private static final WireMockServer LMSTUDIO_MOCK =
      new WireMockServer(WireMockConfiguration.options().dynamicPort());

  static {
    SEDIA_MOCK.start();
    LMSTUDIO_MOCK.start();
  }

  @Autowired JobLauncherTestUtils jobLauncherTestUtils;

  @DynamicPropertySource
  static void props(DynamicPropertyRegistry r) {
    r.add(
        "sedia.search-endpoint",
        () -> "http://127.0.0.1:" + SEDIA_MOCK.port() + "/search-api/prod/rest/search");
    r.add("lmstudio.server", () -> "http://127.0.0.1:" + LMSTUDIO_MOCK.port());
    r.add("sedia.output-csv", () -> OUTPUT_CSV.toString());
    r.add("sedia.page-size", () -> "2");
    r.add("spring.batch.job.enabled", () -> "false");
  }

  @AfterAll
  static void stopMocks() {
    SEDIA_MOCK.stop();
    LMSTUDIO_MOCK.stop();
  }

  @BeforeEach
  void resetMocksAndOutput() throws Exception {
    SEDIA_MOCK.resetAll();
    LMSTUDIO_MOCK.resetAll();
    Files.deleteIfExists(OUTPUT_CSV);
  }

  @Test
  void fetchesCallsScoresRelevanceAndWritesCsv() throws Exception {
    var searchBody =
        new String(
            new ClassPathResource("fixtures/sedia-search-response.json")
                .getInputStream()
                .readAllBytes());

    SEDIA_MOCK.stubFor(
        post(urlPathEqualTo("/search-api/prod/rest/search"))
            .willReturn(aResponse().withStatus(200).withBody(searchBody)));

    LMSTUDIO_MOCK.stubFor(
        post(urlPathEqualTo("/v1/chat/completions"))
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"choices\":[{\"message\":{\"content\":\"true\"}}]}")));

    var execution = jobLauncherTestUtils.launchJob();
    assertThat(execution.getStatus().toString()).isEqualTo("COMPLETED");

    var lines = Files.readAllLines(OUTPUT_CSV);
    assertThat(lines).hasSize(3); // header + 2 data rows
    assertThat(lines.getFirst())
        .isEqualTo(
            "identifier,call_identifier,title,programme,status,deadline,start_date,budget,url,relevant,summary");
    assertThat(lines.get(1))
        .startsWith("HORIZON-EIC-2026-ACCELERATOR-01-01,HORIZON-EIC-2026-ACCELERATOR-01,")
        .endsWith(",true,true");
    assertThat(lines.get(2))
        .startsWith("DIGITAL-2024-CLOUD-AI-01-01,DIGITAL-2024-CLOUD-AI-01,")
        .endsWith(",true,true");

    // verify LM Studio was called for relevance + summary for each of the 2 calls = 4 total
    LMSTUDIO_MOCK.verify(
        4,
        postRequestedFor(urlPathEqualTo("/v1/chat/completions"))
            .withRequestBody(containing("Synapse Postmaster")));

    // verify 2 relevance calls used the relevance max_tokens setting
    LMSTUDIO_MOCK.verify(
        2,
        postRequestedFor(urlPathEqualTo("/v1/chat/completions"))
            .withRequestBody(containing("\"max_tokens\":1024")));

    // verify 2 summary calls fired with the content max_tokens setting
    LMSTUDIO_MOCK.verify(
        2,
        postRequestedFor(urlPathEqualTo("/v1/chat/completions"))
            .withRequestBody(containing("\"max_tokens\":2048")));
  }
}
