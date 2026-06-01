package com.synapsedx.mailing.apollo.config;

import java.util.List;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("apollo")
public record ApolloProperties(
    Api api,
    String peopleSearchEndpoint,
    String inputCsv,
    String outputCsv,
    int perPage,
    int topN,
    long throttleMillis,
    List<String> seniorities,
    List<String> titles,
    Map<String, String> columnMapping) {

  public ApolloProperties {
    if (peopleSearchEndpoint == null || peopleSearchEndpoint.isBlank()) {
      peopleSearchEndpoint = "https://api.apollo.io/api/v1/mixed_people/api_search";
    }
    if (titles == null) {
      titles = java.util.List.of();
    }
  }

  public record Api(String key) {}
}
