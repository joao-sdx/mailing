package com.synapsedx.mailing.seonewsparse.batch.writer;

import static org.assertj.core.api.Assertions.assertThat;

import com.synapsedx.mailing.seonewsparse.config.SeoNewsParseProperties;
import com.synapsedx.mailing.seonewsparse.model.ArticleContacts;
import com.synapsedx.mailing.seonewsparse.model.PersonRow;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.batch.item.Chunk;

class ContactsCsvWriterTest {

  @TempDir Path tempDir;

  private Path writeArticle(String filename, String body) throws Exception {
    var src = tempDir.resolve(filename);
    Files.writeString(src, body);
    return src;
  }

  @Test
  void writesHeaderOnceAndRoleColumn() throws Exception {
    var csvPath = tempDir.resolve("out").resolve("contacts.csv").toString();
    var writer = new ContactsCsvWriter(new SeoNewsParseProperties("input", csvPath));

    var article1 = writeArticle("a1.md", "body 1");
    var article2 = writeArticle("a2.md", "body 2");
    var article3 = writeArticle("a3.md", "body 3");

    var firstChunk =
        new Chunk<>(
            List.of(
                new ArticleContacts(
                    article1,
                    List.of(
                        new PersonRow("Jean", "Dupont", "Directeur", "BNP", "a1.md"),
                        new PersonRow("Marie", "Martin", "DG", "AXA", "a1.md"))),
                new ArticleContacts(
                    article2,
                    List.of(new PersonRow("Pierre", "Bernard", "DSI", "LVMH", "a2.md")))));
    writer.write(firstChunk);

    var secondChunk =
        new Chunk<>(
            List.of(
                new ArticleContacts(
                    article3, List.of(new PersonRow("Alice", "Durand", "CFO", "Total", "a3.md")))));
    writer.write(secondChunk);

    var content = Files.readString(Path.of(csvPath));
    var lines = content.lines().toList();

    assertThat(lines.getFirst()).isEqualTo("first_name,last_name,role,company,article_id");
    assertThat(
            lines.stream()
                .filter(l -> l.equals("first_name,last_name,role,company,article_id"))
                .count())
        .isEqualTo(1);
    assertThat(content).contains("Jean,Dupont,Directeur,BNP,a1.md");
    assertThat(content).contains("Marie,Martin,DG,AXA,a1.md");
    assertThat(content).contains("Pierre,Bernard,DSI,LVMH,a2.md");
    assertThat(content).contains("Alice,Durand,CFO,Total,a3.md");
  }

  @Test
  void escapesCommaInFields() throws Exception {
    var csvPath = tempDir.resolve("contacts.csv").toString();
    var writer = new ContactsCsvWriter(new SeoNewsParseProperties("input", csvPath));
    var article = writeArticle("a.md", "body");

    var chunk =
        new Chunk<>(
            List.of(
                new ArticleContacts(
                    article,
                    List.of(
                        new PersonRow("Jean", "Dupont", "Dir, Adj.", "BNP, Paribas", "a.md")))));
    writer.write(chunk);

    var content = Files.readString(Path.of(csvPath));
    assertThat(content).contains("\"Dir, Adj.\"");
    assertThat(content).contains("\"BNP, Paribas\"");
  }

  @Test
  void copiesArticleNextToCsvWhenRowsPresent() throws Exception {
    var outDir = tempDir.resolve("out");
    var csvPath = outDir.resolve("contacts.csv").toString();
    var writer = new ContactsCsvWriter(new SeoNewsParseProperties("input", csvPath));
    var article = writeArticle("kept.md", "article body kept");

    var chunk =
        new Chunk<>(
            List.of(
                new ArticleContacts(
                    article, List.of(new PersonRow("X", "Y", "Role", "Co", "kept.md")))));
    writer.write(chunk);

    var copied = outDir.resolve("kept.md");
    assertThat(copied).exists();
    assertThat(Files.readString(copied)).isEqualTo("article body kept");
  }

  @Test
  void doesNotCopyArticleWhenRowsEmpty() throws Exception {
    var outDir = tempDir.resolve("out");
    var csvPath = outDir.resolve("contacts.csv").toString();
    var writer = new ContactsCsvWriter(new SeoNewsParseProperties("input", csvPath));
    var skipped = writeArticle("skipped.md", "should not be copied");
    var kept = writeArticle("kept.md", "should be copied");

    var chunk =
        new Chunk<>(
            List.of(
                new ArticleContacts(skipped, List.of()),
                new ArticleContacts(
                    kept, List.of(new PersonRow("X", "Y", "Role", "Co", "kept.md")))));
    writer.write(chunk);

    assertThat(outDir.resolve("skipped.md")).doesNotExist();
    assertThat(outDir.resolve("kept.md")).exists();
  }

  @Test
  void emptyChunkIsSkipped() throws Exception {
    var csvPath = tempDir.resolve("out").resolve("contacts.csv").toString();
    var writer = new ContactsCsvWriter(new SeoNewsParseProperties("input", csvPath));
    var article = writeArticle("a.md", "body");

    var chunk = new Chunk<>(List.of(new ArticleContacts(article, List.<PersonRow>of())));
    writer.write(chunk);

    assertThat(Path.of(csvPath)).doesNotExist();
    assertThat(tempDir.resolve("out").resolve("a.md")).doesNotExist();
  }

  @Test
  void overwritesArticleOnReRun() throws Exception {
    var outDir = tempDir.resolve("out");
    var csvPath = outDir.resolve("contacts.csv").toString();
    Files.createDirectories(outDir);
    Files.writeString(outDir.resolve("a.md"), "stale content");

    var writer = new ContactsCsvWriter(new SeoNewsParseProperties("input", csvPath));
    var article = writeArticle("a.md", "fresh content");

    var chunk =
        new Chunk<>(
            List.of(
                new ArticleContacts(
                    article, List.of(new PersonRow("X", "Y", "Role", "Co", "a.md")))));
    writer.write(chunk);

    assertThat(Files.readString(outDir.resolve("a.md"))).isEqualTo("fresh content");
  }
}
