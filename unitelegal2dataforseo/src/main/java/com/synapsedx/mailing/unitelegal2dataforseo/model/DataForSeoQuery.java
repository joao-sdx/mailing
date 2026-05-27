package com.synapsedx.mailing.unitelegal2dataforseo.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder({
  "keyword",
  "language_code",
  "depth",
  "location_code",
  "location_name",
  "file_prefix"
})
public record DataForSeoQuery(
    String keyword,
    @JsonProperty("language_code") String languageCode,
    int depth,
    @JsonProperty("location_code") int locationCode,
    @JsonProperty("location_name") String locationName,
    @JsonProperty("file_prefix") String filePrefix) {}
