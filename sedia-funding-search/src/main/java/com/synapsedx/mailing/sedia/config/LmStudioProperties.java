package com.synapsedx.mailing.sedia.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("lmstudio")
public record LmStudioProperties(
    String server,
    String model,
    String key,
    int connectTimeoutSeconds,
    int requestTimeoutSeconds,
    int relevanceMaxTokens) {}
