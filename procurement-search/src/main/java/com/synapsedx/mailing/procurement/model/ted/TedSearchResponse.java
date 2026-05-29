package com.synapsedx.mailing.procurement.model.ted;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record TedSearchResponse(
    @JsonProperty("totalNoticeCount") int totalNoticeCount,
    @JsonProperty("notices") List<TedNotice> notices,
    @JsonProperty("iterationNextToken") String iterationNextToken) {}
