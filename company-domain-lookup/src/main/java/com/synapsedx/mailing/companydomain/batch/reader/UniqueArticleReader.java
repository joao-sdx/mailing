package com.synapsedx.mailing.companydomain.batch.reader;

import com.synapsedx.mailing.companydomain.config.CompanyDomainProperties;
import com.synapsedx.mailing.companydomain.csv.CsvLineParser;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemReader;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class UniqueArticleReader implements ItemReader<String> {

  private final CompanyDomainProperties properties;
  private Iterator<String> iterator;

  @Override
  public String read() throws Exception {
    if (iterator == null) {
      iterator = loadUniqueArticleIds().iterator();
    }
    return iterator.hasNext() ? iterator.next() : null;
  }

  private List<String> loadUniqueArticleIds() throws Exception {
    var path = Path.of(properties.inputCsv());
    var lines = Files.readAllLines(path);
    if (lines.isEmpty()) {
      throw new IllegalStateException("input CSV is empty: " + path);
    }
    var headers = CsvLineParser.parse(lines.getFirst());
    var articleIdx = headers.indexOf("article_id");
    if (articleIdx < 0) {
      throw new IllegalStateException(
          "input CSV missing 'article_id' column; headers=" + headers + " file=" + path);
    }

    var unique = new ArrayList<String>();
    var seen = new HashSet<String>();
    for (var i = 1; i < lines.size(); i++) {
      var fields = CsvLineParser.parse(lines.get(i));
      if (articleIdx >= fields.size()) {
        continue;
      }
      var value = fields.get(articleIdx).trim();
      if (value.isEmpty()) {
        continue;
      }
      if (seen.add(value)) {
        unique.add(value);
      }
    }
    log.info("unique_articles_loaded file={} count={}", path.getFileName(), unique.size());
    return unique;
  }
}
