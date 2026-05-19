package com.synapsedx.mailing.seo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("nocobase")
public record NocobaseProperties(String url, String apiKey) {}
