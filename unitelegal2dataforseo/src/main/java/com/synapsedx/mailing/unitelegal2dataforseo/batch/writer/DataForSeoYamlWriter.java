package com.synapsedx.mailing.unitelegal2dataforseo.batch.writer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.synapsedx.mailing.unitelegal2dataforseo.config.QueryDefaults;
import com.synapsedx.mailing.unitelegal2dataforseo.config.Unitelegal2DataforseoProperties;
import com.synapsedx.mailing.unitelegal2dataforseo.model.DataForSeoQuery;
import com.synapsedx.mailing.unitelegal2dataforseo.model.KeywordBatch;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.batch.item.ItemStreamWriter;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataForSeoYamlWriter implements ItemStreamWriter<KeywordBatch> {

  private final Unitelegal2DataforseoProperties properties;
  private final QueryDefaults defaults;

  private final ObjectMapper yaml =
      new ObjectMapper(
              new YAMLFactory()
                  .disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER)
                  .configure(YAMLGenerator.Feature.SPLIT_LINES, false))
          .findAndRegisterModules();

  private BufferedWriter out;
  private int written;

  @Override
  public void open(ExecutionContext executionContext) throws ItemStreamException {
    try {
      var path = Path.of(properties.outputYml());
      if (path.getParent() != null) {
        Files.createDirectories(path.getParent());
      }
      out = Files.newBufferedWriter(path, StandardCharsets.UTF_8);
      out.write("queries:\n");
      written = 0;
      log.info("yaml_output_opened path={}", path.toAbsolutePath());
    } catch (IOException e) {
      throw new ItemStreamException("Unable to open output YAML: " + properties.outputYml(), e);
    }
  }

  @Override
  public void write(Chunk<? extends KeywordBatch> chunk) throws Exception {
    for (var batch : chunk.getItems()) {
      for (var keyword : batch.keywords()) {
        var query =
            new DataForSeoQuery(
                keyword,
                defaults.languageCode(),
                defaults.depth(),
                defaults.locationCode(),
                defaults.locationName(),
                defaults.filePrefix());
        writeListItem(query);
        written++;
      }
    }
  }

  private void writeListItem(DataForSeoQuery query) throws IOException {
    var block = yaml.writeValueAsString(query);
    var lines = block.split("\n");
    var sb = new StringBuilder();
    for (int i = 0; i < lines.length; i++) {
      if (lines[i].isBlank()) {
        continue;
      }
      sb.append(i == 0 ? "  - " : "    ").append(lines[i]).append('\n');
    }
    out.write(sb.toString());
  }

  @Override
  public void update(ExecutionContext executionContext) throws ItemStreamException {
    try {
      if (out != null) {
        out.flush();
      }
    } catch (IOException e) {
      throw new ItemStreamException("Flush failed", e);
    }
  }

  @Override
  public void close() throws ItemStreamException {
    try {
      if (out != null) {
        out.close();
        log.info("yaml_output_closed total_queries={}", written);
        out = null;
      }
    } catch (IOException e) {
      throw new ItemStreamException("Close failed", e);
    }
  }
}
