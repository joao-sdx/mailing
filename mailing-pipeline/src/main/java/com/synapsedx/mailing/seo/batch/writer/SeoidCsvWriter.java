package com.synapsedx.mailing.seo.batch.writer;

import com.synapsedx.mailing.seo.batch.SeoJobContext;
import com.synapsedx.mailing.seo.model.TargetContact;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SeoidCsvWriter implements ItemWriter<TargetContact> {

  private static final String HEADER = "nom,role,societe,email,article";

  private final SeoJobContext jobContext;
  private boolean headerWritten = false;

  @Override
  public void write(Chunk<? extends TargetContact> chunk) throws Exception {
    if (chunk.isEmpty()) {
      return;
    }

    var csvPath = Path.of("output/seo/targets-" + jobContext.jobStartTime() + ".csv");
    Files.createDirectories(csvPath.getParent());

    if (!headerWritten) {
      Files.writeString(
          csvPath, HEADER + "\n", StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
      headerWritten = true;
    }

    var sb = new StringBuilder();
    for (var contact : chunk.getItems()) {
      sb.append(escapeCsv(contact.nom()))
          .append(",")
          .append(escapeCsv(contact.role()))
          .append(",")
          .append(escapeCsv(contact.societe()))
          .append(",")
          .append(escapeCsv(contact.email()))
          .append(",")
          .append(escapeCsv(contact.article()))
          .append("\n");
    }
    Files.writeString(csvPath, sb.toString(), StandardOpenOption.APPEND);
    log.info("csv_written file={} rows={}", csvPath.getFileName(), chunk.size());
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
