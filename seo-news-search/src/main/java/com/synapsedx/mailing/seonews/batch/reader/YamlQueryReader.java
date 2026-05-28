package com.synapsedx.mailing.seonews.batch.reader;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.synapsedx.mailing.seonews.config.SeoNewsProperties;
import com.synapsedx.mailing.seonews.model.QueryList;
import com.synapsedx.mailing.seonews.model.SearchQuery;
import java.util.Iterator;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.item.ItemReader;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class YamlQueryReader implements ItemReader<SearchQuery> {

  private final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
  private final SeoNewsProperties properties;
  private final ResourceLoader resourceLoader;
  private Iterator<SearchQuery> iterator;

  @Override
  public SearchQuery read() throws Exception {
    if (iterator == null) {
      var resource = resourceLoader.getResource(resolveLocation(properties.inputYml()));
      try (var in = resource.getInputStream()) {
        iterator = mapper.readValue(in, QueryList.class).queries().iterator();
      }
    }
    return iterator.hasNext() ? iterator.next() : null;
  }

  private static String resolveLocation(String location) {
    if (location.startsWith("classpath:") || location.contains(":/")) {
      return location;
    }
    return "file:" + location;
  }
}
