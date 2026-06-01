package com.synapsedx.mailing.apollo.client;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.synapsedx.mailing.apollo.config.ApolloProperties;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class ApolloClientTest {

  private static final WireMockServer MOCK =
      new WireMockServer(WireMockConfiguration.options().dynamicPort());

  static {
    MOCK.start();
  }

  @AfterAll
  static void stop() {
    MOCK.stop();
  }

  @BeforeEach
  void reset() {
    MOCK.resetAll();
  }

  private ApolloClient buildClient() {
    var props =
        new ApolloProperties(
            new ApolloProperties.Api("test-key"),
            "http://127.0.0.1:" + MOCK.port() + "/api/v1/mixed_people/api_search",
            "input/contacts.csv",
            "output/people.csv",
            25,
            10,
            0,
            List.of("owner", "founder", "c_suite", "vp", "head", "director"),
            List.of(),
            Map.of("company", "company", "domain", "domain"));
    var client = new ApolloClient(props);
    client.init();
    return client;
  }

  @Test
  void parsesPersonFieldsFromResponse() throws Exception {
    var body =
        new String(
            new ClassPathResource("fixtures/apollo-people-response.json")
                .getInputStream()
                .readAllBytes(),
            StandardCharsets.UTF_8);
    MOCK.stubFor(
        post(urlEqualTo("/api/v1/mixed_people/api_search"))
            .willReturn(aResponse().withStatus(200).withBody(body)));

    var people =
        buildClient()
            .searchDecisionMakers("factofrance.com", List.of("c_suite", "vp"), List.of(), 25);

    assertThat(people).hasSize(2);
    assertThat(people.getFirst().id()).isEqualTo("abc123");
    assertThat(people.getFirst().firstName()).isEqualTo("Sophie");
    assertThat(people.getFirst().lastNameObfuscated()).isEqualTo("B.");
    assertThat(people.getFirst().title()).isEqualTo("Chief Executive Officer");

    MOCK.verify(
        1,
        postRequestedFor(urlEqualTo("/api/v1/mixed_people/api_search"))
            .withHeader("X-Api-Key", containing("test-key"))
            .withRequestBody(containing("q_organization_domains_list"))
            .withRequestBody(containing("factofrance.com"))
            .withRequestBody(containing("person_seniorities"))
            .withRequestBody(containing("c_suite")));
  }

  @Test
  void returns429AsEmptyList() throws Exception {
    MOCK.stubFor(
        post(urlEqualTo("/api/v1/mixed_people/api_search"))
            .willReturn(aResponse().withStatus(429)));

    var people = buildClient().searchDecisionMakers("example.com", List.of("vp"), List.of(), 25);

    assertThat(people).isEmpty();
  }

  @Test
  void throwsOnNon200NonRateLimitError() {
    MOCK.stubFor(
        post(urlEqualTo("/api/v1/mixed_people/api_search"))
            .willReturn(aResponse().withStatus(401).withBody("Unauthorized")));

    assertThatThrownBy(
            () -> buildClient().searchDecisionMakers("example.com", List.of("vp"), List.of(), 25))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("status=401");
  }

  @Test
  void returnsEmptyListWhenNoPeopleFound() throws Exception {
    var body =
        new String(
            new ClassPathResource("fixtures/apollo-empty-response.json")
                .getInputStream()
                .readAllBytes(),
            StandardCharsets.UTF_8);
    MOCK.stubFor(
        post(urlEqualTo("/api/v1/mixed_people/api_search"))
            .willReturn(aResponse().withStatus(200).withBody(body)));

    var people =
        buildClient().searchDecisionMakers("unknown.com", List.of("c_suite"), List.of(), 25);

    assertThat(people).isEmpty();
  }
}
