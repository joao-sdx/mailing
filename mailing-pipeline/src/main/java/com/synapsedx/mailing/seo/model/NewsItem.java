package com.synapsedx.mailing.seo.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record NewsItem(
    String domain,
    String title,
    String url,
    @JsonProperty("time_published") String timePublished,
    String article) {}
