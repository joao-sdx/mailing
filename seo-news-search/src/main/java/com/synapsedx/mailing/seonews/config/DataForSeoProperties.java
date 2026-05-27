package com.synapsedx.mailing.seonews.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("dataforseo")
public record DataForSeoProperties(Api api) {
  public record Api(String user, String key) {}
}
