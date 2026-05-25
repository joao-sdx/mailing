package com.synapsedx.mailing.pipeline.siren.enrich;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.synapsedx.mailing.pipeline.siren.enrich.model.enrich.CompanyRecord;
import java.nio.file.Files;
import java.nio.file.Path;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

/** Writes each {@link CompanyRecord} as {@code {rcs}.json} in the output directory. */
@Slf4j
@Component
@RequiredArgsConstructor
public class InseeEnrichWriter implements ItemWriter<CompanyRecord> {

  private final InseeEnrichProperties properties;
  private final ObjectMapper objectMapper;

  @Override
  public void write(Chunk<? extends CompanyRecord> chunk) throws Exception {
    var outputDir = Path.of(properties.getOutputDir());
    Files.createDirectories(outputDir);

    for (var record : chunk.getItems()) {
      var file = outputDir.resolve(record.rcs() + ".json");
      objectMapper.writerWithDefaultPrettyPrinter().writeValue(file.toFile(), record);
      log.debug("insee_enrich_written rcs={}", record.rcs());
    }
    log.info("insee_enrich_chunk_written count={}", chunk.size());
  }
}
