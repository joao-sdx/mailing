package com.synapsedx.mailing.sedia.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("sedia")
public record SediaProperties(
    String searchEndpoint,
    String apiKey,
    String text,
    List<String> frameworkProgrammes,
    List<String> statuses,
    List<String> types,
    int pageSize,
    String outputCsv) {}
