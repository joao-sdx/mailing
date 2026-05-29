package com.synapsedx.mailing.sedia.model;

import java.util.List;

public record SearchPage(int totalResults, int pageNumber, List<FundingCall> calls) {}
