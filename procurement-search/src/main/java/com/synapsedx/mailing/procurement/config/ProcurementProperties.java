package com.synapsedx.mailing.procurement.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("procurement")
public record ProcurementProperties(String inputYml, String outputCsv, long throttleMillis) {

  public ProcurementProperties {
    if (inputYml == null || inputYml.isBlank()) {
      inputYml = "classpath:procurement-queries.yml";
    }
    if (outputCsv == null || outputCsv.isBlank()) {
      outputCsv = "output/tenders.csv";
    }
    if (throttleMillis <= 0) {
      throttleMillis = 500L;
    }
  }
}
