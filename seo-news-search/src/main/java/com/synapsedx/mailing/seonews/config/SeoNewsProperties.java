package com.synapsedx.mailing.seonews.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("seo-news")
public record SeoNewsProperties(String outputDir) {}
