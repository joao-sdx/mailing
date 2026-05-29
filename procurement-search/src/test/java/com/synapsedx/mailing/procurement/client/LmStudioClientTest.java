package com.synapsedx.mailing.procurement.client;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.synapsedx.mailing.procurement.config.LmStudioProperties;
import com.synapsedx.mailing.procurement.model.Tender;
import java.time.LocalDate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LmStudioClientTest {

  private WireMockServer server;
  private LmStudioClient client;

  @BeforeEach
  void setUp() throws Exception {
    server = new WireMockServer(WireMockConfiguration.options().dynamicPort());
    server.start();
    var props =
        new LmStudioProperties(
            "http://127.0.0.1:" + server.port(), "test-model", "test-key", 5, 10, 512);
    client = new LmStudioClient(props);
    client.init();
  }

  @AfterEach
  void tearDown() {
    server.stop();
  }

  @Test
  void returnsTrueWhenResponseIsTrue() throws Exception {
    stubLmStudio("true");

    var result = client.assessPostmasterFit(tender());

    assertThat(result).contains(true);
  }

  @Test
  void returnsFalseWhenResponseIsFalse() throws Exception {
    stubLmStudio("false");

    var result = client.assessPostmasterFit(tender());

    assertThat(result).contains(false);
  }

  @Test
  void returnsFalseWhenResponseIsFencedJson() throws Exception {
    stubLmStudio("```json\nfalse\n```");

    var result = client.assessPostmasterFit(tender());

    assertThat(result).contains(false);
  }

  @Test
  void returnsEmptyWhenResponseIsGarbage() throws Exception {
    stubLmStudio("I cannot determine the answer from this context.");

    var result = client.assessPostmasterFit(tender());

    assertThat(result).isEmpty();
  }

  @Test
  void returnsEmptyWhenResponseIsBlank() throws Exception {
    stubLmStudio("");

    var result = client.assessPostmasterFit(tender());

    assertThat(result).isEmpty();
  }

  @Test
  void returnsEmptyWhenServerReturnsError() throws Exception {
    server.stubFor(
        post(urlEqualTo("/v1/chat/completions"))
            .willReturn(aResponse().withStatus(500).withBody("Internal Server Error")));

    var result = client.assessPostmasterFit(tender());

    assertThat(result).isEmpty();
  }

  private void stubLmStudio(String content) {
    var body =
        """
        {"choices":[{"message":{"content":"%s"}}]}
        """
            .formatted(content.replace("\"", "\\\"").replace("\n", "\\n"));
    server.stubFor(
        post(urlEqualTo("/v1/chat/completions"))
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody(body)));
  }

  private Tender tender() {
    return new Tender(
        "TED",
        "TED-001",
        "Fourniture de logiciels",
        "Ministère de l'Économie",
        "FRA",
        "72000000",
        "",
        LocalDate.of(2026, 1, 15),
        LocalDate.of(2026, 3, 1),
        "https://ted.europa.eu/1",
        "");
  }
}
