package com.synapsedx.mailing.procurement.model;

import java.time.LocalDate;

/** Normalized tender record written as one CSV row. */
public record Tender(
    String source, // "TED" or "BOAMP"
    String id, // publication-number (TED) or idweb (BOAMP)
    String title,
    String buyer,
    String country, // buyer country; always "FRA" for BOAMP
    String classification, // CPV codes joined (TED) or descripteur_libelle joined (BOAMP)
    String value, // total-value (TED); typically empty for BOAMP
    LocalDate publicationDate,
    LocalDate deadline, // deadline-receipt-tender (TED); datelimitereponse (BOAMP)
    String url,
    String relevant) {} // "true", "false", or "" when LLM gave no usable verdict
