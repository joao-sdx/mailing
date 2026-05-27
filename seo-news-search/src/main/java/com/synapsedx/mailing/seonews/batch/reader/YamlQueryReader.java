package com.synapsedx.mailing.seonews.batch.reader;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.synapsedx.mailing.seonews.model.QueryList;
import com.synapsedx.mailing.seonews.model.SearchQuery;
import java.util.Iterator;
import org.springframework.batch.item.ItemReader;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

@Component
public class YamlQueryReader implements ItemReader<SearchQuery> {

  private final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
  private Iterator<SearchQuery> iterator;

  @Override
  public SearchQuery read() throws Exception {
    if (iterator == null) {
      try (var in = new ClassPathResource("dataforseo-queries.yml").getInputStream()) {
        iterator = mapper.readValue(in, QueryList.class).queries().iterator();
      }
    }
    return iterator.hasNext() ? iterator.next() : null;
  }
}
