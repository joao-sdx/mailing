package com.synapsedx.mailing.unitelegal2dataforseo;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBatchTest
@SpringBootTest
class Unitelegal2DataforseoJobIT {

  @TempDir static Path tempDir;

  @DynamicPropertySource
  static void overrideProps(DynamicPropertyRegistry registry) throws Exception {
    var csv = new ClassPathResource("echantillon-mini.csv").getFile().getAbsolutePath();
    registry.add("unitelegal2dataforseo.input-csv", () -> csv);
    registry.add(
        "unitelegal2dataforseo.output-yml", () -> tempDir.resolve("queries.yml").toString());
    registry.add("spring.batch.job.enabled", () -> "false");
  }

  @Autowired JobLauncherTestUtils jobLauncherTestUtils;

  @Test
  void runsEndToEndAndProducesExpectedYaml() throws Exception {
    var execution = jobLauncherTestUtils.launchJob();
    assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

    var output = tempDir.resolve("queries.yml");
    assertThat(output).exists();

    var mapper = new ObjectMapper(new YAMLFactory());
    @SuppressWarnings("unchecked")
    var parsed = mapper.readValue(Files.newInputStream(output), Map.class);
    @SuppressWarnings("unchecked")
    var queries = (List<Map<String, Object>>) parsed.get("queries");

    assertThat(queries).hasSize(4);
    assertThat(queries.get(0))
        .containsEntry("keyword", "ETS J VIRLY S A")
        .containsEntry("language_code", "fr")
        .containsEntry("depth", 2)
        .containsEntry("location_code", 2250)
        .containsEntry("location_name", "France")
        .hasEntrySatisfying("file_prefix", v -> assertThat(v).asString().matches(".+-\\d+"));
    assertThat(queries.get(1))
        .containsEntry("keyword", "SET - HUILLIER - SOCIETE D'ENTREPOSAGE ET DE TRANSPORTS");
    // Row 3 has two distinct keywords (EDF, ELECTRICITE DE FRANCE) — both kept.
    assertThat(queries).extracting(q -> q.get("keyword")).contains("EDF", "ELECTRICITE DE FRANCE");
  }
}
