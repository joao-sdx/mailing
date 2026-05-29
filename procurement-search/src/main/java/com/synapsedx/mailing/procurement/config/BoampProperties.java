package com.synapsedx.mailing.procurement.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("boamp")
public record BoampProperties(
    String recordsEndpoint,
    String apiKey, // optional; null means anonymous (lower rate-limit quota)
    int connectTimeoutSeconds,
    int requestTimeoutSeconds) {

  public BoampProperties {
    if (recordsEndpoint == null || recordsEndpoint.isBlank()) {
      recordsEndpoint =
          "https://boamp-datadila.opendatasoft.com/api/explore/v2.1/catalog/datasets/boamp/records";
    }
    if (connectTimeoutSeconds <= 0) {
      connectTimeoutSeconds = 10;
    }
    if (requestTimeoutSeconds <= 0) {
      requestTimeoutSeconds = 30;
    }
  }
}
