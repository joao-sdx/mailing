package com.synapsedx.mailing.seo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("seoit")
public record SeoidProperties(OpenAi openai) {

  public record OpenAi(
      String server,
      String model,
      String key,
      int connectTimeoutSeconds,
      int requestTimeoutSeconds) {}
}
