package com.synapsedx.mailing.seo.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record SearchQuery(
    String keyword,
    @JsonProperty("language_code") String languageCode,
    int depth,
    @JsonProperty("location_code") int locationCode,
    @JsonProperty("location_name") String locationName,
    @JsonProperty("file_prefix") String filePrefix,
    int offset) {}
