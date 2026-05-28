package com.synapsedx.mailing.companydomain.csv;

import java.util.ArrayList;
import java.util.List;

public final class CsvLineParser {

  private CsvLineParser() {}

  public static List<String> parse(String line) {
    var fields = new ArrayList<String>();
    var current = new StringBuilder();
    var inQuotes = false;
    for (var i = 0; i < line.length(); i++) {
      var c = line.charAt(i);
      if (inQuotes) {
        if (c == '"') {
          if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
            current.append('"');
            i++;
          } else {
            inQuotes = false;
          }
        } else {
          current.append(c);
        }
      } else {
        if (c == ',') {
          fields.add(current.toString());
          current.setLength(0);
        } else if (c == '"' && current.length() == 0) {
          inQuotes = true;
        } else {
          current.append(c);
        }
      }
    }
    fields.add(current.toString());
    return fields;
  }
}
