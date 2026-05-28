package com.synapsedx.mailing.companydomain.batch.writer;

import static org.assertj.core.api.Assertions.assertThat;

import com.synapsedx.mailing.companydomain.config.CompanyDomainProperties;
import com.synapsedx.mailing.companydomain.model.ContactRow;
import com.synapsedx.mailing.companydomain.model.EnrichedContactRow;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.item.Chunk;

class EnrichedContactsCsvWriterTest {

  @Test
  void writesHeaderOnceAcrossChunksAndAppendsDomain(@TempDir Path tmp) throws Exception {
    var out = tmp.resolve("out.csv");
    var props = new CompanyDomainProperties("ignored", out.toString(), "", 10, 5);
    var writer = new EnrichedContactsCsvWriter(props);
    writer.beforeStep(new StepExecution("step", null));

    var headers = List.of("first_name", "last_name", "company", "article_id");
    writer.write(
        new Chunk<>(
            List.of(
                new EnrichedContactRow(
                    new ContactRow(
                        headers, List.of("A", "B", "Factofrance", "r1.md"), "Factofrance"),
                    "factofrance.com"),
                new EnrichedContactRow(
                    new ContactRow(headers, List.of("C", "D", "Unknown", "r2.md"), "Unknown"),
                    ""))));
    writer.write(
        new Chunk<>(
            List.of(
                new EnrichedContactRow(
                    new ContactRow(
                        headers, List.of("E,F", "G\"H", "Crédit Mutuel", "r3.md"), "Crédit Mutuel"),
                    "creditmutuel.fr"))));

    var lines = Files.readAllLines(out);
    assertThat(lines).hasSize(4);
    assertThat(lines.get(0)).isEqualTo("first_name,last_name,company,article_id,domain");
    assertThat(lines.get(1)).isEqualTo("A,B,Factofrance,r1.md,factofrance.com");
    assertThat(lines.get(2)).isEqualTo("C,D,Unknown,r2.md,");
    assertThat(lines.get(3)).isEqualTo("\"E,F\",\"G\"\"H\",Crédit Mutuel,r3.md,creditmutuel.fr");
  }
}
