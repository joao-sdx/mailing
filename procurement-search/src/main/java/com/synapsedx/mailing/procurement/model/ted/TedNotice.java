package com.synapsedx.mailing.procurement.model.ted;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;

/**
 * Raw TED notice as returned by the API. Multilingual fields are JsonNode (language-keyed maps).
 */
public record TedNotice(
    @JsonProperty("publication-number") String publicationNumber,
    @JsonProperty("notice-title") JsonNode noticeTitle, // {"fra": "...", "eng": "..."}
    @JsonProperty("buyer-name") JsonNode buyerName, // {"fra": "..."}
    @JsonProperty("buyer-country") String buyerCountry,
    @JsonProperty("classification-cpv") List<String> classificationCpv,
    @JsonProperty("total-value") JsonNode totalValue,
    @JsonProperty("publication-date") String publicationDate, // "YYYY-MM-DD" or "YYYYMMDD"
    @JsonProperty("deadline-receipt-tender") String deadlineReceiptTender,
    @JsonProperty("links") JsonNode links) {}
