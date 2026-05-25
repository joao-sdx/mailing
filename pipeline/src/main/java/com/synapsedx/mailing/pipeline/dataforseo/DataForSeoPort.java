package com.synapsedx.mailing.pipeline.dataforseo;

import com.fasterxml.jackson.databind.JsonNode;

public interface DataForSeoPort {

  /** Searches Google News via DataForSEO and returns the raw {@code tasks[0].result[0]} object. */
  JsonNode searchNews(String keyword);
}
