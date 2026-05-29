package com.synapsedx.mailing.procurement.batch.processor;

import com.synapsedx.mailing.procurement.client.TenderSource;
import com.synapsedx.mailing.procurement.config.ProcurementProperties;
import com.synapsedx.mailing.procurement.model.ProcurementQuery;
import com.synapsedx.mailing.procurement.model.Source;
import com.synapsedx.mailing.procurement.model.Tender;
import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TenderSearchProcessor implements ItemProcessor<ProcurementQuery, List<Tender>> {

  private final List<TenderSource> sources;
  private final ProcurementProperties properties;

  private Map<Source, TenderSource> sourceMap;
  private long throttleMillis;

  @PostConstruct
  void init() {
    sourceMap = sources.stream().collect(Collectors.toMap(TenderSource::source, s -> s));
    throttleMillis = properties.throttleMillis();
  }

  @Override
  public List<Tender> process(ProcurementQuery query) {
    throttle();
    try {
      var source = sourceMap.get(query.source());
      if (source == null) {
        log.warn("tender_source_not_found source={}", query.source());
        return null;
      }
      var tenders = source.search(query);
      log.info("tender_search_done source={} count={}", query.source(), tenders.size());
      return tenders.isEmpty() ? null : tenders;
    } catch (Exception e) {
      log.error("tender_search_failed source={}", query.source(), e);
      return null;
    }
  }

  void setThrottleMillis(long ms) {
    this.throttleMillis = ms;
  }

  private void throttle() {
    if (throttleMillis <= 0) {
      return;
    }
    try {
      Thread.sleep(throttleMillis);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }
}
