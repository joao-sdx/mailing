package com.synapsedx.mailing.pipeline.siren.news;

/** One denomination search target derived from a company record. */
record CompanySearchQuery(String rcs, String denomination, int seq) {}
