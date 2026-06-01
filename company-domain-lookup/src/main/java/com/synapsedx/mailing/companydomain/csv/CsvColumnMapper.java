package com.synapsedx.mailing.companydomain.csv;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CsvColumnMapper {

  public static Map<String, Integer> resolve(
      List<String> headers, Map<String, String> columnMapping) {
    if (columnMapping == null || columnMapping.isEmpty()) {
      throw new IllegalStateException("column-mapping must be configured");
    }
    var result = new LinkedHashMap<String, Integer>();
    for (var entry : columnMapping.entrySet()) {
      var idx = headers.indexOf(entry.getValue());
      if (idx < 0) {
        throw new IllegalArgumentException(
            "CSV column '%s' (mapped to '%s') not found in headers: %s"
                .formatted(entry.getValue(), entry.getKey(), headers));
      }
      result.put(entry.getKey(), idx);
    }
    return result;
  }
}
