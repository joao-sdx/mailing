package com.synapsedx.mailing.procurement.batch.writer;

import com.synapsedx.mailing.procurement.config.ProcurementProperties;
import com.synapsedx.mailing.procurement.model.Tender;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
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
public class TenderCsvWriter implements ItemWriter<List<Tender>>, StepExecutionListener {

  private static final String HEADER =
      "source,id,title,buyer,country,classification,value,publication_date,deadline,url,relevant";

  private final ProcurementProperties properties;
  private boolean headerWritten = false;

  @Override
  public void beforeStep(StepExecution stepExecution) {
    headerWritten = false;
  }

  @Override
  public void write(Chunk<? extends List<Tender>> chunk) throws Exception {
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
          csvPath, HEADER + "\n", StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
      headerWritten = true;
    }

    var sb = new StringBuilder();
    for (var tenders : chunk.getItems()) {
      for (var tender : tenders) {
        sb.append(escapeCsv(tender.source())).append(",");
        sb.append(escapeCsv(tender.id())).append(",");
        sb.append(escapeCsv(tender.title())).append(",");
        sb.append(escapeCsv(tender.buyer())).append(",");
        sb.append(escapeCsv(tender.country())).append(",");
        sb.append(escapeCsv(tender.classification())).append(",");
        sb.append(escapeCsv(tender.value())).append(",");
        sb.append(tender.publicationDate() != null ? tender.publicationDate().toString() : "")
            .append(",");
        sb.append(tender.deadline() != null ? tender.deadline().toString() : "").append(",");
        sb.append(escapeCsv(tender.url())).append(",");
        sb.append(escapeCsv(tender.relevant())).append("\n");
      }
    }
    Files.writeString(csvPath, sb.toString(), StandardOpenOption.APPEND);
    log.info(
        "tenders_csv_written file={} rows={}",
        csvPath.getFileName(),
        chunk.getItems().stream().mapToInt(List::size).sum());
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
