package com.synapsedx.mailing.sedia.model;

public record FundingCall(
    String identifier,
    String callIdentifier,
    String title,
    String programme,
    String status,
    String deadline,
    String startDate,
    String budget,
    String url,
    String summary) {}
