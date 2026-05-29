package com.synapsedx.mailing.procurement.model.boamp;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record BoampResponse(
    @JsonProperty("total_count") int totalCount,
    @JsonProperty("results") List<BoampRecord> results) {}
