package com.synapsedx.mailing.companydomain.batch.reader;

import com.synapsedx.mailing.companydomain.config.CompanyDomainProperties;
import com.synapsedx.mailing.companydomain.csv.CsvLineParser;
import com.synapsedx.mailing.companydomain.model.ContactRow;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;
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
  private int companyIdx = -1;
  private int articleIdIdx = -1;
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
    var company =
        companyIdx >= 0 && companyIdx < fields.size() ? fields.get(companyIdx).trim() : "";
    var articleId =
        articleIdIdx >= 0 && articleIdIdx < fields.size() ? fields.get(articleIdIdx).trim() : "";
    return new ContactRow(headers, fields, company, articleId);
  }

  private void init() throws Exception {
    var path = Path.of(properties.inputCsv());
    var lines = Files.readAllLines(path);
    if (lines.isEmpty()) {
      throw new IllegalStateException("input CSV is empty: " + path);
    }
    headers = CsvLineParser.parse(lines.getFirst());
    companyIdx = headers.indexOf("company");
    if (companyIdx < 0) {
      throw new IllegalStateException(
          "input CSV missing 'company' column; headers=" + headers + " file=" + path);
    }
    articleIdIdx = headers.indexOf("article_id");
    if (articleIdIdx < 0) {
      throw new IllegalStateException(
          "input CSV missing 'article_id' column; headers=" + headers + " file=" + path);
    }
    dataLines = lines.subList(1, lines.size()).iterator();
    log.info(
        "contacts_csv_loaded file={} headers={} rows={}",
        path.getFileName(),
        headers,
        lines.size() - 1);
  }
}
