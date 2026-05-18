package com.synapsedx.mailing.seo.batch;

import com.synapsedx.mailing.seo.model.SearchQuery;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class QueryStore {

  private final List<SearchQuery> queries = new ArrayList<>();
  private int readIndex = 0;

  public void addAll(List<? extends SearchQuery> items) {
    queries.addAll(items);
  }

  public SearchQuery next() {
    return readIndex < queries.size() ? queries.get(readIndex++) : null;
  }
}
