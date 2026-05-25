package com.synapsedx.mailing.pipeline.siren.enrich;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;

@SpringBatchTest
@SpringBootTest
@TestPropertySource(
    properties = {
      "batch.siren.input-dir=target/insee-enrich-test/01-siren",
      "batch.siren.done-dir=target/insee-enrich-test/01-siren/done",
      "batch.siren.output-dir=target/insee-enrich-test/02-siren-line"
    })
class InseeEnrichJobTest {

  private static final Path INPUT_DIR = Path.of("target/insee-enrich-test/01-siren");
  private static final Path DONE_DIR = Path.of("target/insee-enrich-test/01-siren/done");
  private static final Path OUTPUT_DIR = Path.of("target/insee-enrich-test/02-siren-line");
  private static final Path TEST_CSV = Path.of("src/test/resources/insee.csv");

  /** Number of data rows in insee.csv (excluding header). */
  private static final int EXPECTED_RECORD_COUNT = 7;

  @Autowired private JobLauncherTestUtils jobLauncherTestUtils;

  @Autowired private Job inseeEnrichPrepareJob;

  @MockBean private InseeAnnuairePort annuaireClient;

  @BeforeEach
  void setUp() throws IOException {
    // Clean and recreate directory structure
    deleteRecursively(Path.of("target/insee-enrich-test"));
    Files.createDirectories(INPUT_DIR);
    Files.createDirectories(DONE_DIR);
    Files.createDirectories(OUTPUT_DIR);

    // Copy test CSV into input dir
    Files.copy(TEST_CSV, INPUT_DIR.resolve("insee.csv"), StandardCopyOption.REPLACE_EXISTING);

    // Stub API to return empty (no external calls)
    when(annuaireClient.findBySiren(anyString())).thenReturn(Optional.empty());

    jobLauncherTestUtils.setJob(inseeEnrichPrepareJob);
  }

  @Test
  void jobCompletesAndProducesExpectedFiles() throws Exception {
    var params =
        new JobParametersBuilder().addLong("run.id", System.currentTimeMillis()).toJobParameters();

    var execution = jobLauncherTestUtils.launchJob(params);

    assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

    // Input file must be moved to done/
    assertThat(INPUT_DIR.resolve("insee.csv")).doesNotExist();
    assertThat(DONE_DIR.resolve("insee.csv")).exists();

    // One JSON per record in 02-siren-line/
    try (var files = Files.list(OUTPUT_DIR)) {
      var jsonFiles = files.filter(p -> p.toString().endsWith(".json")).toList();
      assertThat(jsonFiles).hasSize(EXPECTED_RECORD_COUNT);
    }
  }

  @Test
  void eachOutputFileIsNamedBySiren() throws Exception {
    var params =
        new JobParametersBuilder().addLong("run.id", System.currentTimeMillis()).toJobParameters();

    jobLauncherTestUtils.launchJob(params);

    // Verify a known SIREN from insee.csv has its JSON file
    assertThat(OUTPUT_DIR.resolve("FR-046620266.json")).exists();
    assertThat(OUTPUT_DIR.resolve("FR-016750697.json")).exists();
  }

  private static void deleteRecursively(Path path) throws IOException {
    if (!Files.exists(path)) {
      return;
    }
    try (var stream = Files.walk(path)) {
      stream
          .sorted(java.util.Comparator.reverseOrder())
          .forEach(
              p -> {
                try {
                  Files.delete(p);
                } catch (IOException e) {
                  throw new RuntimeException("Cannot delete: " + p, e);
                }
              });
    }
  }
}
