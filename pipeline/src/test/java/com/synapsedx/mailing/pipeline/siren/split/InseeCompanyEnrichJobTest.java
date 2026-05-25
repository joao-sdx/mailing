package com.synapsedx.mailing.pipeline.siren.split;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.synapsedx.mailing.pipeline.siren.enrich.InseeAnnuairePort;
import com.synapsedx.mailing.pipeline.siren.enrich.model.annuaire.AnnuaireDirigeant;
import com.synapsedx.mailing.pipeline.siren.enrich.model.annuaire.AnnuaireEntreprise;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
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
      "batch.company-enrich.input-dir=target/company-enrich-test/02-siren-line",
      "batch.company-enrich.done-dir=target/company-enrich-test/02-siren-line/done",
      "batch.company-enrich.company-output-dir=target/company-enrich-test/03-company",
      "batch.company-enrich.contact-output-dir=target/company-enrich-test/04-contact",
      "batch.company-enrich.relation-output-dir=target/company-enrich-test/05-relation"
    })
class InseeCompanyEnrichJobTest {

  private static final Path INPUT_DIR = Path.of("target/company-enrich-test/02-siren-line");
  private static final Path DONE_DIR = Path.of("target/company-enrich-test/02-siren-line/done");
  private static final Path COMPANY_DIR = Path.of("target/company-enrich-test/03-company");
  private static final Path CONTACT_DIR = Path.of("target/company-enrich-test/04-contact");
  private static final Path RELATION_DIR = Path.of("target/company-enrich-test/05-relation");
  private static final Path SAMPLE_JSON = Path.of("src/test/resources/FR-016750697.json");

  @Autowired private JobLauncherTestUtils jobLauncherTestUtils;

  @Autowired private Job companyEnrichJob;

  @MockBean private InseeAnnuairePort annuaireClient;

  @BeforeEach
  void setUp() throws IOException {
    deleteRecursively(Path.of("target/company-enrich-test"));
    Files.createDirectories(INPUT_DIR);
    Files.createDirectories(DONE_DIR);
    Files.createDirectories(COMPANY_DIR);
    Files.createDirectories(CONTACT_DIR);
    Files.createDirectories(RELATION_DIR);

    Files.copy(
        SAMPLE_JSON, INPUT_DIR.resolve("FR-016750697.json"), StandardCopyOption.REPLACE_EXISTING);

    var dirigeant =
        new AnnuaireDirigeant(
            "Dupont",
            "Jean",
            "1975",
            "1975-03",
            "Président",
            "Française",
            "personne physique",
            null,
            null);
    var entreprise =
        new AnnuaireEntreprise(
            "016750697",
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            List.of(dirigeant),
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null);
    when(annuaireClient.findBySiren(anyString())).thenReturn(Optional.of(entreprise));

    jobLauncherTestUtils.setJob(companyEnrichJob);
  }

  @Test
  void jobCompletesAndProducesExpectedFiles() throws Exception {
    var params =
        new JobParametersBuilder().addLong("run.id", System.currentTimeMillis()).toJobParameters();

    var execution = jobLauncherTestUtils.launchJob(params);

    assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

    assertThat(INPUT_DIR.resolve("FR-016750697.json")).doesNotExist();
    assertThat(DONE_DIR.resolve("FR-016750697.json")).exists();
    assertThat(COMPANY_DIR.resolve("FR-016750697.json")).exists();
    assertThat(CONTACT_DIR.resolve("FR-016750697-0.json")).exists();
  }

  @Test
  void noContactsWhenAnnuaireReturnsEmpty() throws Exception {
    when(annuaireClient.findBySiren(anyString())).thenReturn(Optional.empty());

    var params =
        new JobParametersBuilder().addLong("run.id", System.currentTimeMillis()).toJobParameters();

    jobLauncherTestUtils.launchJob(params);

    assertThat(COMPANY_DIR.resolve("FR-016750697.json")).exists();
    try (var files = Files.list(CONTACT_DIR)) {
      assertThat(files.toList()).isEmpty();
    }
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
