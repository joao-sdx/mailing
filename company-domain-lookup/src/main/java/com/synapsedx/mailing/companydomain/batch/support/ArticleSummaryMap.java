package com.synapsedx.mailing.companydomain.batch.support;

import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class ArticleSummaryMap {

  private final ConcurrentHashMap<String, String> entries = new ConcurrentHashMap<>();

  public void put(String key, String summary) {
    entries.put(key, summary == null ? "" : summary);
  }

  public String get(String key) {
    return entries.getOrDefault(key, "");
  }
}
