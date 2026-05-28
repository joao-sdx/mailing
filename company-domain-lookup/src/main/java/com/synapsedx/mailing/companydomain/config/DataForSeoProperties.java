package com.synapsedx.mailing.companydomain.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("dataforseo")
public record DataForSeoProperties(Api api, String serpOrganicEndpoint) {

  public DataForSeoProperties {
    if (serpOrganicEndpoint == null || serpOrganicEndpoint.isBlank()) {
      serpOrganicEndpoint = "https://api.dataforseo.com/v3/serp/google/organic/live/advanced";
    }
  }

  public record Api(String user, String key) {}
}
