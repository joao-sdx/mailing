package com.synapsedx.mailing.seo.batch.writer;

import com.synapsedx.mailing.seo.batch.QueryStore;
import com.synapsedx.mailing.seo.model.SearchQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class QueryStoreWriter implements ItemWriter<SearchQuery> {

  private final QueryStore queryStore;

  @Override
  public void write(Chunk<? extends SearchQuery> chunk) {
    queryStore.addAll(chunk.getItems());
  }
}
