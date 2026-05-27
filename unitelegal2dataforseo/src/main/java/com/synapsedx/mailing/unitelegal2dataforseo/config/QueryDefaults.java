package com.synapsedx.mailing.unitelegal2dataforseo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("query-defaults")
public record QueryDefaults(
    String languageCode, int depth, int locationCode, String locationName, String filePrefix) {}
