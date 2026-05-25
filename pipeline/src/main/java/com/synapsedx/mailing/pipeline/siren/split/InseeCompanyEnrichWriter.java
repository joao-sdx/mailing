package com.synapsedx.mailing.pipeline.siren.split;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

/**
 * Writes {@link SplitResult} items to {@code 03-company/}, {@code 04-contact/}, {@code
 * 05-relation/}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InseeCompanyEnrichWriter implements ItemWriter<SplitResult> {

  private final InseeCompanyEnrichProperties properties;
  private final ObjectMapper objectMapper;

  @Override
  public void write(Chunk<? extends SplitResult> chunk) throws Exception {
    var companyDir = Path.of(properties.getCompanyOutputDir());
    var contactDir = Path.of(properties.getContactOutputDir());
    var relationDir = Path.of(properties.getRelationOutputDir());
    Files.createDirectories(companyDir);
    Files.createDirectories(contactDir);
    Files.createDirectories(relationDir);

    for (var result : chunk.getItems()) {
      var rcs = result.company().rcs();
      var pretty = objectMapper.writerWithDefaultPrettyPrinter();

      pretty.writeValue(companyDir.resolve(rcs + ".json").toFile(), result.company());
      log.debug("company_enrich_written_company rcs={}", rcs);

      writeIndexed(pretty, contactDir, rcs, result.contacts());
      writeIndexed(pretty, relationDir, rcs, result.parentCorps());

      log.debug(
          "company_enrich_written rcs={} contacts={} relations={}",
          rcs,
          result.contacts().size(),
          result.parentCorps().size());
    }
  }

  private static <T> void writeIndexed(
      com.fasterxml.jackson.databind.ObjectWriter writer, Path dir, String rcs, List<T> items)
      throws Exception {
    for (int i = 0; i < items.size(); i++) {
      writer.writeValue(dir.resolve(rcs + "-" + i + ".json").toFile(), items.get(i));
    }
  }
}
