package com.synapsedx.mailing.apollo.batch.writer;

import static org.assertj.core.api.Assertions.assertThat;

import com.synapsedx.mailing.apollo.config.ApolloProperties;
import com.synapsedx.mailing.apollo.model.ApolloPerson;
import com.synapsedx.mailing.apollo.model.CompanyPeople;
import com.synapsedx.mailing.apollo.model.CompanyRef;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.batch.item.Chunk;

class ApolloPeopleCsvWriterTest {

  @TempDir Path tempDir;

  private ApolloPeopleCsvWriter writerFor(Path outputPath) {
    var props =
        new ApolloProperties(
            new ApolloProperties.Api("key"),
            null,
            "input/contacts.csv",
            outputPath.toString(),
            25,
            10,
            0,
            List.of("c_suite"),
            List.of(),
            Map.of("company", "company", "domain", "domain"));
    var writer = new ApolloPeopleCsvWriter(props);
    writer.beforeStep(null);
    return writer;
  }

  @Test
  void writesHeaderAndRows() throws Exception {
    var output = tempDir.resolve("people.csv");
    var writer = writerFor(output);

    var person = new ApolloPerson("id1", "Sophie", "B.", "CEO");
    var companyPeople =
        new CompanyPeople(new CompanyRef("Factofrance", "factofrance.com"), List.of(person));
    writer.write(new Chunk<>(List.of(companyPeople)));

    var lines = Files.readAllLines(output);
    assertThat(lines).hasSize(2);
    assertThat(lines.getFirst()).isEqualTo("apollo_id,company,domain,first_name,last_name,title");
    assertThat(lines.get(1)).isEqualTo("id1,Factofrance,factofrance.com,Sophie,B.,CEO");
  }

  @Test
  void writesHeaderOnce_acrossMultipleChunks() throws Exception {
    var output = tempDir.resolve("people.csv");
    var writer = writerFor(output);

    var p1 = new ApolloPerson("id1", "Sophie", "B.", "CEO");
    var p2 = new ApolloPerson("id2", "Pierre", "M.", "VP Sales");
    var chunk1 =
        new Chunk<>(List.of(new CompanyPeople(new CompanyRef("Acme", "acme.com"), List.of(p1))));
    var chunk2 =
        new Chunk<>(List.of(new CompanyPeople(new CompanyRef("Beta", "beta.com"), List.of(p2))));

    writer.write(chunk1);
    writer.write(chunk2);

    var lines = Files.readAllLines(output);
    assertThat(lines).hasSize(3); // header + 2 rows
    assertThat(lines.stream().filter(l -> l.startsWith("apollo_id,")).count()).isEqualTo(1);
  }

  @Test
  void escapesCommasInValues() throws Exception {
    var output = tempDir.resolve("people.csv");
    var writer = writerFor(output);

    var person = new ApolloPerson("id1", "Jean", "Du, Pont", "Director, Sales");
    var chunk =
        new Chunk<>(
            List.of(new CompanyPeople(new CompanyRef("Acme", "acme.com"), List.of(person))));
    writer.write(chunk);

    var lines = Files.readAllLines(output);
    assertThat(lines.get(1)).contains("\"Du, Pont\"").contains("\"Director, Sales\"");
  }

  @Test
  void skipsEmptyChunks() throws Exception {
    var output = tempDir.resolve("people.csv");
    var writer = writerFor(output);

    writer.write(new Chunk<>(List.of()));

    assertThat(output).doesNotExist();
  }

  @Test
  void writesNoRowsForCompanyWithNoPeople() throws Exception {
    var output = tempDir.resolve("people.csv");
    var writer = writerFor(output);

    var companyPeople = new CompanyPeople(new CompanyRef("Empty", "empty.com"), List.of());
    writer.write(new Chunk<>(List.of(companyPeople)));

    // header written but no data rows
    var lines = Files.readAllLines(output);
    assertThat(lines).hasSize(1);
  }

  @Test
  void resetsClearsHeaderWrittenFlag() throws Exception {
    var output = tempDir.resolve("people.csv");
    var writer = writerFor(output);

    var person = new ApolloPerson("id1", "A", "B.", "T");
    var chunk =
        new Chunk<>(List.of(new CompanyPeople(new CompanyRef("X", "x.com"), List.of(person))));
    writer.write(chunk);

    // reset and write again — should truncate and re-write header
    writer.beforeStep(null);
    writer.write(chunk);

    var lines = Files.readAllLines(output);
    assertThat(lines).hasSize(2); // still just header + 1 row (truncated)
  }
}
