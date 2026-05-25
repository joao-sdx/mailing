package com.synapsedx.mailing.pipeline.siren.news.model;

import com.fasterxml.jackson.databind.JsonNode;

public record CompanyNewsResult(String rcs, String keyword, int seq, JsonNode data) {}
