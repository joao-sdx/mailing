package com.synapsedx.mailing.seo.batch.reader;

import com.synapsedx.mailing.seo.batch.QueryStore;
import com.synapsedx.mailing.seo.model.SearchQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.item.ItemReader;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class QueryStoreReader implements ItemReader<SearchQuery> {

  private final QueryStore queryStore;

  @Override
  public SearchQuery read() {
    return queryStore.next();
  }
}
