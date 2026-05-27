package com.synapsedx.mailing.unitelegal2dataforseo.batch.processor;

import com.synapsedx.mailing.unitelegal2dataforseo.model.InseeUniteLegale;
import com.synapsedx.mailing.unitelegal2dataforseo.model.KeywordBatch;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class KeywordDedupProcessor implements ItemProcessor<InseeUniteLegale, KeywordBatch> {

  @Override
  public KeywordBatch process(InseeUniteLegale row) {
    var nonBlank =
        Stream.of(
                row.sigle(),
                row.denomination(),
                row.denominationUsuelle1(),
                row.denominationUsuelle2(),
                row.denominationUsuelle3())
            .filter(v -> v != null && !v.isBlank())
            .map(String::trim)
            .sorted(Comparator.comparingInt(String::length).reversed())
            .toList();

    var kept = new ArrayList<String>();
    for (var v : nonBlank) {
      var upper = v.toUpperCase(Locale.ROOT);
      boolean alreadyIncluded =
          kept.stream().anyMatch(k -> k.toUpperCase(Locale.ROOT).contains(upper));
      if (!alreadyIncluded) {
        kept.add(v);
      }
    }

    if (kept.isEmpty()) {
      log.debug("dedup_skipped siren={} reason=all_columns_blank", row.siren());
      return null;
    }
    return new KeywordBatch(row.siren(), List.copyOf(kept));
  }
}
