package com.synapsedx.mailing.companydomain.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("lmstudio")
public record LmStudioProperties(
    String server,
    String model,
    String key,
    int connectTimeoutSeconds,
    int requestTimeoutSeconds) {}
