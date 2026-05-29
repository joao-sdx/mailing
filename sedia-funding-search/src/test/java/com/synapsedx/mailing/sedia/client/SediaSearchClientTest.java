package com.synapsedx.mailing.sedia.client;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.synapsedx.mailing.sedia.config.SediaProperties;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class SediaSearchClientTest {

  private static final WireMockServer SERVER =
      new WireMockServer(WireMockConfiguration.options().dynamicPort());

  static {
    SERVER.start();
  }

  @AfterAll
  static void stopServer() {
    SERVER.stop();
  }

  @BeforeEach
  void reset() {
    SERVER.resetAll();
  }

  private SediaSearchClient buildClient() throws Exception {
    var props =
        new SediaProperties(
            "http://127.0.0.1:" + SERVER.port() + "/search-api/prod/rest/search",
            "SEDIA",
            "***",
            List.of("43108390", "43152860"),
            List.of("31094502"),
            List.of("1"),
            2,
            "output/test.csv");
    var client = new SediaSearchClient(props);
    client.init();
    return client;
  }

  @Test
  void parsesSearchResponseIntoFundingCalls() throws Exception {
    var body =
        new String(
            new ClassPathResource("fixtures/sedia-search-response.json")
                .getInputStream()
                .readAllBytes());
    SERVER.stubFor(
        post(urlPathEqualTo("/search-api/prod/rest/search"))
            .willReturn(aResponse().withStatus(200).withBody(body)));

    var client = buildClient();
    var page = client.search(1);

    assertThat(page.totalResults()).isEqualTo(2);
    assertThat(page.pageNumber()).isEqualTo(1);
    assertThat(page.calls()).hasSize(2);

    var first = page.calls().getFirst();
    assertThat(first.identifier()).isEqualTo("HORIZON-EIC-2026-ACCELERATOR-01-01");
    assertThat(first.callIdentifier()).isEqualTo("HORIZON-EIC-2026-ACCELERATOR-01");
    assertThat(first.title()).isEqualTo("EIC Accelerator Open 2026");
    assertThat(first.programme()).isEqualTo("Horizon Europe");
    assertThat(first.deadline()).isEqualTo("2026-10-01T00:00:00.000+0000");
    assertThat(first.url()).contains("HORIZON-EIC-2026-ACCELERATOR-01-01");

    var second = page.calls().getLast();
    assertThat(second.identifier()).isEqualTo("DIGITAL-2024-CLOUD-AI-01-01");
    assertThat(second.programme()).isEqualTo("Digital Europe");
  }

  @Test
  void sendsQueryWithFrameworkProgrammesAndStatus() throws Exception {
    var body =
        new String(
            new ClassPathResource("fixtures/sedia-search-response.json")
                .getInputStream()
                .readAllBytes());
    SERVER.stubFor(
        post(urlPathEqualTo("/search-api/prod/rest/search"))
            .willReturn(aResponse().withStatus(200).withBody(body)));

    buildClient().search(1);

    SERVER.verify(
        postRequestedFor(urlPathEqualTo("/search-api/prod/rest/search"))
            .withRequestBody(containing("frameworkProgramme"))
            .withRequestBody(containing("43108390"))
            .withRequestBody(containing("43152860"))
            .withRequestBody(containing("31094502")));
  }

  @Test
  void throwsOnNon200Response() throws Exception {
    SERVER.stubFor(
        post(urlPathEqualTo("/search-api/prod/rest/search"))
            .willReturn(aResponse().withStatus(429).withBody("Too Many Requests")));

    var client = buildClient();
    assertThatThrownBy(() -> client.search(1))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("status=429");
  }

  @Test
  void handlesEmptyResultsGracefully() throws Exception {
    var emptyBody = "{\"totalResults\":0,\"pageNumber\":1,\"pageSize\":2,\"results\":[]}";
    SERVER.stubFor(
        post(urlPathEqualTo("/search-api/prod/rest/search"))
            .willReturn(aResponse().withStatus(200).withBody(emptyBody)));

    var page = buildClient().search(1);

    assertThat(page.totalResults()).isEqualTo(0);
    assertThat(page.calls()).isEmpty();
  }
}
