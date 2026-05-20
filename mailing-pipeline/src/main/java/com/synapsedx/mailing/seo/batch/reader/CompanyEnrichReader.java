package com.synapsedx.mailing.seo.batch.reader;

import com.synapsedx.mailing.seo.SupabaseClient;
import com.synapsedx.mailing.seo.model.CompanyToEnrich;
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
public class CompanyEnrichReader implements ItemReader<CompanyToEnrich> {

  private static final int PAGE_SIZE = 10;
  private static final Map<String, Object> FILTER = Map.of("enrich", false);

  private final SupabaseClient supabase;
  private final Queue<CompanyToEnrich> buffer = new ArrayDeque<>();
  private int page = 1;
  private boolean exhausted = false;

  @Override
  public CompanyToEnrich read() throws Exception {
    if (buffer.isEmpty() && !exhausted) {
      var nodes = supabase.list("crm_companies", FILTER, page, PAGE_SIZE);
      log.info("crm_companies_page page={} count={}", page, nodes.size());
      nodes.forEach(
          n -> buffer.add(new CompanyToEnrich(n.path("id").asInt(), n.path("name").asText(""))));
      page++;
      if (nodes.size() < PAGE_SIZE) {
        exhausted = true;
      }
    }
    return buffer.poll();
  }
}
