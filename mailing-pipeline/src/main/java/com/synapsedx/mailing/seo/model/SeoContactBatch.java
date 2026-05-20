package com.synapsedx.mailing.seo.model;

import java.util.List;

public record SeoContactBatch(int seoResultId, List<ExtractedContact> contacts) {}
