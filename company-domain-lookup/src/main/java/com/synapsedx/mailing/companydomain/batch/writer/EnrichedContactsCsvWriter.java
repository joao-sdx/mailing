package com.synapsedx.mailing.companydomain.batch.writer;

import com.synapsedx.mailing.companydomain.config.CompanyDomainProperties;
import com.synapsedx.mailing.companydomain.model.EnrichedContactRow;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
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
public class EnrichedContactsCsvWriter
    implements ItemWriter<EnrichedContactRow>, StepExecutionListener {

  private final CompanyDomainProperties properties;
  private boolean headerWritten = false;

  @Override
  public void beforeStep(StepExecution stepExecution) {
    headerWritten = false;
  }

  @Override
  public void write(Chunk<? extends EnrichedContactRow> chunk) throws Exception {
    if (chunk.getItems().isEmpty()) {
      return;
    }

    var csvPath = Path.of(properties.outputCsv());
    var parent = csvPath.getParent();
    if (parent != null) {
      Files.createDirectories(parent);
    }

    if (!headerWritten) {
      var headers = chunk.getItems().iterator().next().contact().headers();
      var headerLine = String.join(",", headers) + ",domain,summary,relevant\n";
      Files.writeString(
          csvPath, headerLine, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
      headerWritten = true;
    }

    var sb = new StringBuilder();
    for (var row : chunk.getItems()) {
      var first = true;
      for (var v : row.contact().values()) {
        if (!first) {
          sb.append(",");
        }
        sb.append(escapeCsv(v));
        first = false;
      }
      sb.append(",").append(escapeCsv(row.domain()));
      sb.append(",").append(escapeCsv(row.summary()));
      sb.append(",").append(escapeCsv(row.relevant())).append("\n");
    }
    Files.writeString(csvPath, sb.toString(), StandardOpenOption.APPEND);
    log.info("enriched_csv_written file={} rows={}", csvPath.getFileName(), chunk.size());
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
