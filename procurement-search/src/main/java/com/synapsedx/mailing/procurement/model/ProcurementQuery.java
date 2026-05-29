package com.synapsedx.mailing.procurement.model;

public record ProcurementQuery(
    Source source,
    SearchFilter filter,
    String rawQuery) {} // nullable; when present, sent verbatim to the source (filter ignored)
