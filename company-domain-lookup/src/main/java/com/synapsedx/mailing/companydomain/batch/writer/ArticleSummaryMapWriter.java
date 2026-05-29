package com.synapsedx.mailing.companydomain.batch.writer;

import com.synapsedx.mailing.companydomain.batch.support.ArticleSummaryMap;
import com.synapsedx.mailing.companydomain.model.ArticleSummary;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ArticleSummaryMapWriter implements ItemWriter<ArticleSummary> {

  private final ArticleSummaryMap map;

  @Override
  public void write(Chunk<? extends ArticleSummary> chunk) {
    for (var item : chunk.getItems()) {
      map.put(item);
    }
  }
}
