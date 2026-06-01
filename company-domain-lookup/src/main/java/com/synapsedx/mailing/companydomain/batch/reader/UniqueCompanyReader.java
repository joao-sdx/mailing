package com.synapsedx.mailing.companydomain.batch.reader;

import com.synapsedx.mailing.companydomain.config.CompanyDomainProperties;
import com.synapsedx.mailing.companydomain.csv.CsvColumnMapper;
import com.synapsedx.mailing.companydomain.csv.CsvLineParser;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemReader;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class UniqueCompanyReader implements ItemReader<String> {

  private final CompanyDomainProperties properties;
  private Iterator<String> iterator;

  @Override
  public String read() throws Exception {
    if (iterator == null) {
      iterator = loadUniqueCompanies().iterator();
    }
    return iterator.hasNext() ? iterator.next() : null;
  }

  private List<String> loadUniqueCompanies() throws Exception {
    var path = Path.of(properties.inputCsv());
    var lines = Files.readAllLines(path);
    if (lines.isEmpty()) {
      throw new IllegalStateException("input CSV is empty: " + path);
    }
    var headers = CsvLineParser.parse(lines.getFirst());
    var resolved = CsvColumnMapper.resolve(headers, properties.columnMapping());
    var companyIdx = resolved.get("company");
    if (companyIdx == null) {
      throw new IllegalStateException("column-mapping must include key 'company'");
    }

    var unique = new ArrayList<String>();
    var seenKeys = new HashSet<String>();
    for (var i = 1; i < lines.size(); i++) {
      var fields = CsvLineParser.parse(lines.get(i));
      if (companyIdx >= fields.size()) {
        continue;
      }
      var raw = fields.get(companyIdx);
      var key = raw.trim().toUpperCase(Locale.ROOT);
      if (key.isEmpty()) {
        continue;
      }
      if (seenKeys.add(key)) {
        unique.add(raw.trim());
      }
    }
    log.info("unique_companies_loaded file={} count={}", path.getFileName(), unique.size());
    return unique;
  }
}
