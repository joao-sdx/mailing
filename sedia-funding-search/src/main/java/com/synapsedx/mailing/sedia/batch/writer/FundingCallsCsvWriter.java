package com.synapsedx.mailing.sedia.batch.writer;

import com.synapsedx.mailing.sedia.config.SediaProperties;
import com.synapsedx.mailing.sedia.model.ScoredCall;
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
public class FundingCallsCsvWriter implements ItemWriter<ScoredCall>, StepExecutionListener {

  private static final String HEADER =
      "identifier,call_identifier,title,programme,status,deadline,start_date,budget,url,relevant,summary\n";

  private final SediaProperties properties;
  private boolean headerWritten = false;

  @Override
  public void beforeStep(StepExecution stepExecution) {
    headerWritten = false;
  }

  @Override
  public void write(Chunk<? extends ScoredCall> chunk) throws Exception {
    if (chunk.getItems().isEmpty()) {
      return;
    }

    var csvPath = Path.of(properties.outputCsv());
    var parent = csvPath.getParent();
    if (parent != null) {
      Files.createDirectories(parent);
    }

    if (!headerWritten) {
      Files.writeString(
          csvPath, HEADER, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
      headerWritten = true;
    }

    var sb = new StringBuilder();
    for (var scored : chunk.getItems()) {
      var c = scored.call();
      sb.append(escapeCsv(c.identifier())).append(",");
      sb.append(escapeCsv(c.callIdentifier())).append(",");
      sb.append(escapeCsv(c.title())).append(",");
      sb.append(escapeCsv(c.programme())).append(",");
      sb.append(escapeCsv(c.status())).append(",");
      sb.append(escapeCsv(c.deadline())).append(",");
      sb.append(escapeCsv(c.startDate())).append(",");
      sb.append(escapeCsv(c.budget())).append(",");
      sb.append(escapeCsv(c.url())).append(",");
      sb.append(escapeCsv(scored.relevant())).append(",");
      sb.append(escapeCsv(scored.summary())).append("\n");
    }
    Files.writeString(csvPath, sb.toString(), StandardOpenOption.APPEND);
    log.info("funding_calls_csv_written file={} rows={}", csvPath.getFileName(), chunk.size());
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
