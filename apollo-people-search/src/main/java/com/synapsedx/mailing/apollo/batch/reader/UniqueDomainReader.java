package com.synapsedx.mailing.apollo.batch.reader;

import com.synapsedx.mailing.apollo.config.ApolloProperties;
import com.synapsedx.mailing.apollo.csv.CsvColumnMapper;
import com.synapsedx.mailing.apollo.csv.CsvLineParser;
import com.synapsedx.mailing.apollo.model.CompanyRef;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.item.ItemReader;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class UniqueDomainReader implements ItemReader<CompanyRef>, StepExecutionListener {

  private final ApolloProperties properties;

  private Iterator<CompanyRef> entries;

  @Override
  public void beforeStep(StepExecution stepExecution) {
    entries = null;
  }

  @Override
  public CompanyRef read() throws Exception {
    if (entries == null) {
      entries = loadEntries().iterator();
    }
    return entries.hasNext() ? entries.next() : null;
  }

  private List<CompanyRef> loadEntries() throws Exception {
    var path = Path.of(properties.inputCsv());
    var lines = Files.readAllLines(path);
    if (lines.isEmpty()) {
      log.warn("apollo_reader_empty_file file={}", path);
      return List.of();
    }

    var header = CsvLineParser.parse(lines.getFirst());
    var indices = CsvColumnMapper.resolve(header, properties.columnMapping());

    var seen = new HashSet<String>();
    var result = new ArrayList<CompanyRef>();

    for (var i = 1; i < lines.size(); i++) {
      var fields = CsvLineParser.parse(lines.get(i));
      var companyIdx = indices.get("company");
      var domainIdx = indices.get("domain");
      if (fields.size() <= Math.max(companyIdx, domainIdx)) {
        continue;
      }
      var domain = fields.get(domainIdx).trim();
      if (domain.isBlank()) {
        continue;
      }
      var key = domain.toLowerCase(Locale.ROOT);
      if (seen.add(key)) {
        result.add(new CompanyRef(fields.get(companyIdx).trim(), domain));
      }
    }

    log.info("apollo_reader_loaded unique_domains={} file={}", result.size(), path.getFileName());
    return result;
  }
}
