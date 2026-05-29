package com.synapsedx.mailing.procurement.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDate;
import java.util.List;

public record SearchFilter(
    List<String> keywords,
    LocalDate publicationDateFrom,
    List<String> countries, // TED: ISO-3166 alpha-3 (FRA, DEU, LUX, ...)
    List<String> departments, // BOAMP: code_departement (e.g. "75", "92")
    @JsonProperty("activeOnly") boolean activeOnly) {}
