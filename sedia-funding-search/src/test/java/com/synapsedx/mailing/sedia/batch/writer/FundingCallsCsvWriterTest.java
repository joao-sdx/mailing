package com.synapsedx.mailing.sedia.batch.writer;

import static org.assertj.core.api.Assertions.assertThat;

import com.synapsedx.mailing.sedia.config.SediaProperties;
import com.synapsedx.mailing.sedia.model.FundingCall;
import com.synapsedx.mailing.sedia.model.ScoredCall;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.batch.item.Chunk;

class FundingCallsCsvWriterTest {

  @TempDir Path tempDir;

  private FundingCallsCsvWriter writer;
  private Path outputCsv;

  @BeforeEach
  void setUp() {
    outputCsv = tempDir.resolve("sedia-calls.csv");
    var props =
        new SediaProperties(
            "http://localhost/search",
            "SEDIA",
            "***",
            List.of("43108390"),
            List.of("31094502"),
            List.of("1"),
            100,
            outputCsv.toString());
    writer = new FundingCallsCsvWriter(props);
    writer.beforeStep(null);
  }

  private ScoredCall scored(String id, String title, String relevant) {
    var call =
        new FundingCall(
            id,
            "CALL-01",
            title,
            "Horizon Europe",
            "Open",
            "2026-10-01T00:00:00Z",
            "2026-01-01T00:00:00Z",
            "1000000",
            "https://ec.europa.eu/topic/" + id,
            "A description.");
    return new ScoredCall(call, relevant);
  }

  @Test
  void writesHeaderOnFirstChunk() throws Exception {
    writer.write(new Chunk<>(List.of(scored("A", "Call A", "true"))));

    var lines = Files.readAllLines(outputCsv);
    assertThat(lines.getFirst())
        .isEqualTo(
            "identifier,call_identifier,title,programme,status,deadline,start_date,budget,url,relevant");
  }

  @Test
  void writesDataRowsWithRelevantColumn() throws Exception {
    writer.write(new Chunk<>(List.of(scored("EIC-01", "EIC Accelerator 2026", "true"))));

    var lines = Files.readAllLines(outputCsv);
    assertThat(lines).hasSize(2);
    assertThat(lines.get(1)).startsWith("EIC-01,CALL-01,EIC Accelerator 2026,").endsWith(",true");
  }

  @Test
  void appendsSubsequentChunksWithoutDuplicatingHeader() throws Exception {
    writer.write(new Chunk<>(List.of(scored("A", "Call A", "true"))));
    writer.write(new Chunk<>(List.of(scored("B", "Call B", "false"))));

    var lines = Files.readAllLines(outputCsv);
    assertThat(lines).hasSize(3);
    assertThat(lines.get(0)).contains("identifier");
    assertThat(lines.get(1)).startsWith("A,");
    assertThat(lines.get(2)).startsWith("B,");
  }

  @Test
  void escapesCsvSpecialCharactersInTitle() throws Exception {
    writer.write(new Chunk<>(List.of(scored("X", "Title, with comma", "true"))));

    var content = Files.readString(outputCsv);
    assertThat(content).contains("\"Title, with comma\"");
  }

  @Test
  void truncatesExistingFileOnNewStep() throws Exception {
    writer.write(new Chunk<>(List.of(scored("A", "Call A", "true"))));

    // Simulate a new step execution
    writer.beforeStep(null);
    writer.write(new Chunk<>(List.of(scored("B", "Call B", "false"))));

    var lines = Files.readAllLines(outputCsv);
    assertThat(lines).hasSize(2); // header + 1 row, no leftover from previous run
    assertThat(lines.get(1)).startsWith("B,");
  }
}
