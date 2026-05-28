package com.synapsedx.mailing.seonewsparse.batch.writer;

import com.synapsedx.mailing.seonewsparse.config.SeoNewsParseProperties;
import com.synapsedx.mailing.seonewsparse.model.ArticleContacts;
import com.synapsedx.mailing.seonewsparse.model.PersonRow;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ContactsCsvWriter implements ItemWriter<ArticleContacts>, StepExecutionListener {

  private static final String HEADER = "first_name,last_name,role,company,article_id";

  private final SeoNewsParseProperties properties;
  private boolean headerWritten = false;

  @Override
  public void beforeStep(StepExecution stepExecution) {
    headerWritten = false;
  }

  @Override
  public void write(Chunk<? extends ArticleContacts> chunk) throws Exception {
    var rows = new ArrayList<PersonRow>();
    for (var item : chunk.getItems()) {
      rows.addAll(item.rows());
    }
    if (rows.isEmpty()) {
      return;
    }

    var csvPath = Path.of(properties.outputCsv());
    var parent = csvPath.getParent();
    var destDir = parent != null ? parent : Path.of("");
    if (parent != null) {
      Files.createDirectories(parent);
    }

    writeRows(csvPath, rows);
    copySourceArticles(chunk, destDir);
  }

  private void writeRows(Path csvPath, List<PersonRow> rows) throws Exception {
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
          .append(escapeCsv(row.role()))
          .append(",")
          .append(escapeCsv(row.company()))
          .append(",")
          .append(escapeCsv(row.articleId()))
          .append("\n");
    }
    Files.writeString(csvPath, sb.toString(), StandardOpenOption.APPEND);
    log.info("csv_written file={} rows={}", csvPath.getFileName(), rows.size());
  }

  private void copySourceArticles(Chunk<? extends ArticleContacts> chunk, Path destDir)
      throws Exception {
    for (var item : chunk.getItems()) {
      if (item.rows().isEmpty()) {
        continue;
      }
      var dest = destDir.resolve(item.sourceArticle().getFileName().toString());
      Files.copy(item.sourceArticle(), dest, StandardCopyOption.REPLACE_EXISTING);
      log.info("article_copied src={} dest={}", item.sourceArticle().getFileName(), dest);
    }
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
