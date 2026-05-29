package com.synapsedx.mailing.companydomain.batch.support;

import com.synapsedx.mailing.companydomain.model.ArticleSummary;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class ArticleSummaryMap {

  private final ConcurrentHashMap<String, ArticleSummary> entries = new ConcurrentHashMap<>();

  public void put(ArticleSummary insight) {
    entries.put(insight.articleId(), insight);
  }

  public String summary(String key) {
    var entry = entries.get(key);
    return entry != null ? entry.summary() : "";
  }

  public String relevant(String key) {
    var entry = entries.get(key);
    return entry != null ? entry.relevant() : "";
  }
}
