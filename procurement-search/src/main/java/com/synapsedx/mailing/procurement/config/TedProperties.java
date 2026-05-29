package com.synapsedx.mailing.procurement.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("ted")
public record TedProperties(
    String searchEndpoint, int connectTimeoutSeconds, int requestTimeoutSeconds) {

  public TedProperties {
    if (searchEndpoint == null || searchEndpoint.isBlank()) {
      searchEndpoint = "https://api.ted.europa.eu/v3/notices/search";
    }
    if (connectTimeoutSeconds <= 0) {
      connectTimeoutSeconds = 10;
    }
    if (requestTimeoutSeconds <= 0) {
      requestTimeoutSeconds = 30;
    }
  }
}
