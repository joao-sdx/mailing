package com.synapsedx.mailing.unitelegal2dataforseo.batch.writer;

import static org.assertj.core.api.Assertions.assertThat;

import com.synapsedx.mailing.unitelegal2dataforseo.config.QueryDefaults;
import com.synapsedx.mailing.unitelegal2dataforseo.config.Unitelegal2DataforseoProperties;
import com.synapsedx.mailing.unitelegal2dataforseo.model.KeywordBatch;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ExecutionContext;

class DataForSeoYamlWriterTest {

  @TempDir Path tempDir;

  private DataForSeoYamlWriter newWriter(Path outputYml) {
    var props = new Unitelegal2DataforseoProperties("ignored.csv", outputYml.toString());
    var defaults = new QueryDefaults("fr", 2, 2250, "France", "assurance-fr");
    return new DataForSeoYamlWriter(props, defaults);
  }

  @Test
  void writesYamlMatchingDataforseoQueriesFormat() throws Exception {
    var output = tempDir.resolve("queries.yml");
    var writer = newWriter(output);
    writer.open(new ExecutionContext());

    writer.write(
        new Chunk<>(
            List.of(
                new KeywordBatch("016750697", List.of("ETS J VIRLY S A")),
                new KeywordBatch("099999999", List.of("EDF", "ELECTRICITE DE FRANCE")))));
    writer.close();

    var content = Files.readString(output);
    assertThat(content)
        .startsWith("queries:")
        .contains("- keyword: \"ETS J VIRLY S A\"")
        .contains("language_code: \"fr\"")
        .contains("depth: 2")
        .contains("location_code: 2250")
        .contains("location_name: \"France\"")
        .contains("file_prefix: \"assurance-fr\"")
        .contains("- keyword: \"EDF\"")
        .contains("- keyword: \"ELECTRICITE DE FRANCE\"")
        .doesNotStartWith("---");
  }

  @Test
  void appendsAcrossMultipleChunks() throws Exception {
    var output = tempDir.resolve("queries.yml");
    var writer = newWriter(output);
    writer.open(new ExecutionContext());

    writer.write(new Chunk<>(List.of(new KeywordBatch("1", List.of("FIRST")))));
    writer.write(new Chunk<>(List.of(new KeywordBatch("2", List.of("SECOND")))));
    writer.close();

    var content = Files.readString(output);
    assertThat(content)
        .containsSubsequence("queries:", "- keyword: \"FIRST\"", "- keyword: \"SECOND\"");
  }

  @Test
  void createsOutputDirectoryIfMissing() throws Exception {
    var output = tempDir.resolve("nested/sub/queries.yml");
    var writer = newWriter(output);
    writer.open(new ExecutionContext());
    writer.write(new Chunk<>(List.of(new KeywordBatch("1", List.of("X")))));
    writer.close();

    assertThat(Files.exists(output)).isTrue();
  }
}
