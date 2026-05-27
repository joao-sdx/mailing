package com.synapsedx.mailing.seonews.model;

public record NewsArticle(
    String title,
    String url,
    String domain,
    String published,
    String keyword,
    String filePrefix,
    String content) {}
