package com.synapsedx.mailing.seo.batch.reader;

import com.synapsedx.mailing.seo.batch.SeoSummaryStore;
import com.synapsedx.mailing.seo.model.SeoSummaryTask;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.item.ItemReader;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SeoSummaryReader implements ItemReader<SeoSummaryTask> {

  private final SeoSummaryStore store;

  @Override
  public SeoSummaryTask read() {
    return store.next();
  }
}
