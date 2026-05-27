package com.synapsedx.mailing.seonewsparse.batch.writer;

import com.synapsedx.mailing.seonewsparse.config.SeoNewsParseProperties;
import com.synapsedx.mailing.seonewsparse.model.PersonRow;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ContactsCsvWriter implements ItemWriter<List<PersonRow>> {

  private static final String HEADER = "first_name,last_name,company,article_name";

  private final SeoNewsParseProperties properties;
  private boolean headerWritten = false;

  @Override
  public void write(Chunk<? extends List<PersonRow>> chunk) throws Exception {
    var rows = new ArrayList<PersonRow>();
    for (var list : chunk.getItems()) {
      rows.addAll(list);
    }
    if (rows.isEmpty()) {
      return;
    }

    var csvPath = Path.of(properties.outputCsv());
    Files.createDirectories(csvPath.getParent());

    if (!headerWritten) {
      Files.writeString(
          csvPath, HEADER + "\n", StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
      headerWritten = true;
    }

    var sb = new StringBuilder();
    for (var row : rows) {
      sb.append(escapeCsv(row.firstName()))
          .append(",")
          .append(escapeCsv(row.lastName()))
          .append(",")
          .append(escapeCsv(row.company()))
          .append(",")
          .append(escapeCsv(row.articleName()))
          .append("\n");
    }
    Files.writeString(csvPath, sb.toString(), StandardOpenOption.APPEND);
    log.info("csv_written file={} rows={}", csvPath.getFileName(), rows.size());
  }

  private String escapeCsv(String value) {
    if (value == null || value.isEmpty()) {
      return "";
    }
    if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
      return "\"" + value.replace("\"", "\"\"") + "\"";
    }
    return value;
  }
}
