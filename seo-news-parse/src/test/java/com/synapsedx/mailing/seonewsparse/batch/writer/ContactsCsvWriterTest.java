package com.synapsedx.mailing.seonewsparse.batch.writer;

import static org.assertj.core.api.Assertions.assertThat;

import com.synapsedx.mailing.seonewsparse.config.SeoNewsParseProperties;
import com.synapsedx.mailing.seonewsparse.model.PersonRow;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.batch.item.Chunk;

class ContactsCsvWriterTest {

  @TempDir Path tempDir;

  @Test
  void writesHeaderOnce() throws Exception {
    var csvPath = tempDir.resolve("contacts.csv").toString();
    var writer = new ContactsCsvWriter(new SeoNewsParseProperties("input", csvPath));

    var firstChunk =
        new Chunk<>(
            List.of(
                List.of(
                    new PersonRow("Jean", "Dupont", "BNP", "Article 1"),
                    new PersonRow("Marie", "Martin", "AXA", "Article 2"))));
    writer.write(firstChunk);

    var secondChunk =
        new Chunk<>(List.of(List.of(new PersonRow("Pierre", "Bernard", "LVMH", "Article 3"))));
    writer.write(secondChunk);

    var content = Files.readString(Path.of(csvPath));
    var lines = content.lines().toList();

    assertThat(lines.getFirst()).isEqualTo("first_name,last_name,company,article_id");
    assertThat(
            lines.stream().filter(l -> l.equals("first_name,last_name,company,article_id")).count())
        .isEqualTo(1);
    assertThat(content).contains("Jean,Dupont,BNP,Article 1");
    assertThat(content).contains("Marie,Martin,AXA,Article 2");
    assertThat(content).contains("Pierre,Bernard,LVMH,Article 3");
  }

  @Test
  void escapesCommaInFields() throws Exception {
    var csvPath = tempDir.resolve("contacts.csv").toString();
    var writer = new ContactsCsvWriter(new SeoNewsParseProperties("input", csvPath));

    var chunk =
        new Chunk<>(List.of(List.of(new PersonRow("Jean", "Dupont", "BNP, Paribas", "Article"))));
    writer.write(chunk);

    var content = Files.readString(Path.of(csvPath));
    assertThat(content).contains("\"BNP, Paribas\"");
  }

  @Test
  void emptyChunkIsSkipped() throws Exception {
    var csvPath = tempDir.resolve("contacts.csv").toString();
    var writer = new ContactsCsvWriter(new SeoNewsParseProperties("input", csvPath));

    var chunk = new Chunk<>(List.of(List.<PersonRow>of()));
    writer.write(chunk);

    assertThat(Path.of(csvPath)).doesNotExist();
  }
}
