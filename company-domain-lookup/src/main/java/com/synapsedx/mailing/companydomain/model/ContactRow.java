package com.synapsedx.mailing.companydomain.model;

import java.util.List;

public record ContactRow(
    List<String> headers, List<String> values, String company, String articleId) {}
