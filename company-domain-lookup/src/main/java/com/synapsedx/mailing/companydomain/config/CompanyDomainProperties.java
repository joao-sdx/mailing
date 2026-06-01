package com.synapsedx.mailing.companydomain.config;

import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("company-domain")
public record CompanyDomainProperties(
    String inputCsv,
    String outputCsv,
    String articlesDir,
    int serpDepth,
    int serpTopN,
    Map<String, String> columnMapping) {}
