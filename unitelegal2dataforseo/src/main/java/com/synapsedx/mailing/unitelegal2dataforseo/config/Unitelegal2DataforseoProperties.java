package com.synapsedx.mailing.unitelegal2dataforseo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("unitelegal2dataforseo")
public record Unitelegal2DataforseoProperties(String inputCsv, String outputYml) {}
