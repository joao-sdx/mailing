package com.synapsedx.mailing.pipeline.siren.split;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.synapsedx.mailing.pipeline.siren.enrich.model.enrich.CompanyRecord;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemStream;
import org.springframework.stereotype.Component;

/**
 * Reads {@code *.json} files from the input directory and deserializes each as a {@link
 * CompanyRecord}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InseeCompanyEnrichReader implements ItemReader<CompanyRecord>, ItemStream {

  private final InseeCompanyEnrichProperties properties;
  private final ObjectMapper objectMapper;

  private Queue<Path> pendingFiles;
  private final List<Path> processedFiles = new ArrayList<>();

  @Override
  public void open(ExecutionContext ctx) {
    var inputDir = Path.of(properties.getInputDir());
    pendingFiles = new LinkedList<>();
    try (var stream =
        Files.find(
            inputDir, 1, (p, attr) -> attr.isRegularFile() && p.toString().endsWith(".json"))) {
      stream.sorted().forEach(pendingFiles::add);
    } catch (IOException e) {
      throw new IllegalStateException("Cannot scan input dir: " + inputDir, e);
    }
    log.info("company_enrich_reader_open input_dir={} files={}", inputDir, pendingFiles.size());
  }

  @Override
  public CompanyRecord read() throws Exception {
    if (pendingFiles == null || pendingFiles.isEmpty()) {
      return null;
    }
    var file = pendingFiles.poll();
    var record = objectMapper.readValue(file.toFile(), CompanyRecord.class);
    processedFiles.add(file);
    log.debug("company_enrich_read rcs={}", record.rcs());
    return record;
  }

  @Override
  public void update(ExecutionContext ctx) {}

  @Override
  public void close() {}

  public List<Path> getProcessedFiles() {
    return List.copyOf(processedFiles);
  }
}
