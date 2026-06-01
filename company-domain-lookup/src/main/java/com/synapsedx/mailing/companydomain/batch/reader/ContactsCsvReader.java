package com.synapsedx.mailing.companydomain.batch.reader;

import com.synapsedx.mailing.companydomain.config.CompanyDomainProperties;
import com.synapsedx.mailing.companydomain.csv.CsvColumnMapper;
import com.synapsedx.mailing.companydomain.csv.CsvLineParser;
import com.synapsedx.mailing.companydomain.model.ContactRow;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemReader;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ContactsCsvReader implements ItemReader<ContactRow> {

  private final CompanyDomainProperties properties;
  private List<String> headers;
  private Map<String, Integer> columnIndices;
  private Iterator<String> dataLines;

  @Override
  public ContactRow read() throws Exception {
    if (dataLines == null) {
      init();
    }
    if (!dataLines.hasNext()) {
      return null;
    }
    var fields = CsvLineParser.parse(dataLines.next());
    var companyIdx = columnIndices.get("company");
    var articleIdIdx = columnIndices.get("article_id");
    var company = companyIdx < fields.size() ? fields.get(companyIdx).trim() : "";
    var articleId = articleIdIdx < fields.size() ? fields.get(articleIdIdx).trim() : "";
    return new ContactRow(headers, fields, company, articleId);
  }

  private void init() throws Exception {
    var path = Path.of(properties.inputCsv());
    var lines = Files.readAllLines(path);
    if (lines.isEmpty()) {
      throw new IllegalStateException("input CSV is empty: " + path);
    }
    headers = CsvLineParser.parse(lines.getFirst());
    columnIndices = CsvColumnMapper.resolve(headers, properties.columnMapping());
    dataLines = lines.subList(1, lines.size()).iterator();
    log.info(
        "contacts_csv_loaded file={} headers={} rows={}",
        path.getFileName(),
        headers,
        lines.size() - 1);
  }
}
