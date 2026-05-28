package com.synapsedx.mailing.companydomain.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("company-domain")
public record CompanyDomainProperties(
    String inputCsv, String outputCsv, String articlesDir, int serpDepth, int serpTopN) {}
