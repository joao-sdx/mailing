package com.synapsedx.mailing.companydomain;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import java.nio.charset.StandardCharsets;
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
class CompanyDomainLookupMissingArticleIT {

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
    r.add(
        "company-domain.input-csv",
        () -> "src/test/resources/fixtures/it-contacts-missing-article.csv");
    r.add("company-domain.output-csv", () -> "target/it-missing-article.csv");
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
  void resetMocks() throws Exception {
    DATAFORSEO_MOCK.resetAll();
    LMSTUDIO_MOCK.resetAll();
    var emptyBody =
        new String(
            new ClassPathResource("fixtures/dataforseo-organic-empty.json")
                .getInputStream()
                .readAllBytes(),
            StandardCharsets.UTF_8);
    DATAFORSEO_MOCK.stubFor(
        post(urlEqualTo("/v3/serp/google/organic/live/advanced"))
            .willReturn(aResponse().withStatus(200).withBody(emptyBody)));
  }

  @Test
  void jobFailsWhenArticleFileMissing() throws Exception {
    var execution = jobLauncherTestUtils.launchJob();
    assertThat(execution.getStatus().toString()).isEqualTo("FAILED");
  }
}
