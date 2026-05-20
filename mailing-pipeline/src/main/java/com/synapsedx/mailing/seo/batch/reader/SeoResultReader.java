package com.synapsedx.mailing.seo.batch.reader;

import com.synapsedx.mailing.seo.SupabaseClient;
import com.synapsedx.mailing.seo.model.SeoResultItem;
import java.util.ArrayDeque;
import java.util.Map;
import java.util.Queue;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemReader;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SeoResultReader implements ItemReader<SeoResultItem> {

  private static final int PAGE_SIZE = 10;
  private static final Map<String, Object> FILTER = Map.of("scan_status", "No");

  private final SupabaseClient nocobase;
  private final Queue<SeoResultItem> buffer = new ArrayDeque<>();
  private int page = 1;
  private boolean exhausted = false;

  @Override
  public SeoResultItem read() throws Exception {
    if (buffer.isEmpty() && !exhausted) {
      var nodes = nocobase.list("seo_result", FILTER, page, PAGE_SIZE);
      log.info("seo_result_page page={} count={}", page, nodes.size());
      nodes.forEach(
          n ->
              buffer.add(
                  new SeoResultItem(
                      n.path("id").asInt(),
                      n.path("title").asText(""),
                      n.path("url").asText(""),
                      n.path("article").asText(""))));
      page++;
      if (nodes.size() < PAGE_SIZE) {
        exhausted = true;
      }
    }
    return buffer.poll();
  }
}
