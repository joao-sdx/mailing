package com.synapsedx.mailing.seo.batch.processor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.synapsedx.mailing.seo.model.DataForSeoRequest;
import com.synapsedx.mailing.seo.model.SearchQuery;
import java.util.List;
import java.util.Map;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

@Component
public class DataForSeoRequestProcessor implements ItemProcessor<SearchQuery, DataForSeoRequest> {

  private final ObjectMapper mapper = new ObjectMapper();

  @Override
  public DataForSeoRequest process(SearchQuery query) throws Exception {
    var payload =
        List.of(
            Map.of(
                "keyword", query.keyword(),
                "language_code", query.languageCode(),
                "depth", query.depth(),
                "location_code", query.locationCode(),
                "location_name", query.locationName(),
                "offset", query.offset()));
    return new DataForSeoRequest(query, mapper.writeValueAsString(payload));
  }
}
