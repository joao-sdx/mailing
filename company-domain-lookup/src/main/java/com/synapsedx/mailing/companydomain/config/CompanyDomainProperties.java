package com.synapsedx.mailing.companydomain.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("company-domain")
public record CompanyDomainProperties(
    String inputCsv, String outputCsv, int serpDepth, int serpTopN) {}
