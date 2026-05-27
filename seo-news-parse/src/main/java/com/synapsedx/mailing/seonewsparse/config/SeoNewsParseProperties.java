package com.synapsedx.mailing.seonewsparse.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("seo-news-parse")
public record SeoNewsParseProperties(String inputDir, String outputCsv) {}
