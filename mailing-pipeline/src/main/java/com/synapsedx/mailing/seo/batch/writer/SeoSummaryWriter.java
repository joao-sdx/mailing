package com.synapsedx.mailing.seo.batch.writer;

import com.synapsedx.mailing.seo.SupabaseClient;
import com.synapsedx.mailing.seo.model.NewsItem;
import com.synapsedx.mailing.seo.model.SeoSummaryResult;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SeoSummaryWriter implements ItemWriter<SeoSummaryResult> {

  private final SupabaseClient nocobase;

  @Override
  public void write(Chunk<? extends SeoSummaryResult> chunk) throws Exception {
    for (var result : chunk.getItems()) {
      var item = result.newsItem();
      var fields = resultFields(item, result.summary());
      var existing = nocobase.findByUrl("seo_result", item.url());
      int resultId;
      if (existing.isPresent()) {
        resultId = existing.get();
        nocobase.update("seo_result", resultId, fields);
      } else {
        resultId = nocobase.create("seo_result", fields);
      }
      nocobase.addRelation("seo_query", result.queryId(), "results", resultId);
      log.info(
          "seo_result_saved queryId={} resultId={} url={}", result.queryId(), resultId, item.url());
    }
  }

  private Map<String, Object> resultFields(NewsItem item, String summary) {
    var fields = new LinkedHashMap<String, Object>();
    fields.put("type", "news");
    putIfPresent(fields, "title", item.title());
    putIfPresent(fields, "domain", item.domain());
    putIfPresent(fields, "url", item.url());
    putIfPresent(fields, "article", item.article());
    putIfPresent(fields, "summary", summary);
    return fields;
  }

  private void putIfPresent(Map<String, Object> fields, String key, String value) {
    if (value != null && !value.isBlank()) {
      fields.put(key, value);
    }
  }
}
