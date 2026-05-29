package com.synapsedx.mailing.procurement.batch.reader;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.synapsedx.mailing.procurement.config.ProcurementProperties;
import com.synapsedx.mailing.procurement.model.ProcurementQuery;
import com.synapsedx.mailing.procurement.model.ProcurementQueryList;
import java.util.Iterator;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.item.ItemReader;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class QueryReader implements ItemReader<ProcurementQuery> {

  private final ObjectMapper mapper =
      new ObjectMapper(new YAMLFactory())
          .registerModule(new JavaTimeModule())
          .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

  private final ProcurementProperties properties;
  private final ResourceLoader resourceLoader;
  private Iterator<ProcurementQuery> iterator;

  @Override
  public ProcurementQuery read() throws Exception {
    if (iterator == null) {
      var resource = resourceLoader.getResource(resolveLocation(properties.inputYml()));
      try (var in = resource.getInputStream()) {
        iterator = mapper.readValue(in, ProcurementQueryList.class).queries().iterator();
      }
    }
    return iterator.hasNext() ? iterator.next() : null;
  }

  private static String resolveLocation(String location) {
    if (location.startsWith("classpath:") || location.contains("://")) {
      return location;
    }
    return "file:" + location;
  }
}
