package com.synapsedx.mailing.seonews.batch.writer;

import static org.assertj.core.api.Assertions.assertThat;

import com.synapsedx.mailing.seonews.config.SeoNewsProperties;
import com.synapsedx.mailing.seonews.model.NewsArticle;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.batch.item.Chunk;

class MarkdownFileWriterTest {

  @TempDir Path tempDir;

  MarkdownFileWriter writer;

  @BeforeEach
  void setUp() throws Exception {
    writer = new MarkdownFileWriter(new SeoNewsProperties(tempDir.toString()));
    writer.init();
  }

  @Test
  void writesFileWithFrontmatterAndContent() throws Exception {
    var article =
        new NewsArticle(
            "Test Title",
            "https://ex.com/article",
            "ex.com",
            "2026-05-01T00:00:00Z",
            "banque digitale",
            "banque-fr",
            "## Section\n\nContent paragraph.");
    var chunk = new Chunk<>(List.of(List.of(article)));

    writer.write(chunk);

    var file = tempDir.resolve("banque-fr-00.md");
    assertThat(file).exists();
    var content = Files.readString(file);
    assertThat(content).startsWith("---\n");
    assertThat(content).contains("title: \"Test Title\"");
    assertThat(content).contains("url: https://ex.com/article");
    assertThat(content).contains("domain: ex.com");
    assertThat(content).contains("published: 2026-05-01T00:00:00Z");
    assertThat(content).contains("keyword: banque digitale");
    assertThat(content).contains("---\n");
    assertThat(content).contains("## Section\n\nContent paragraph.");
  }

  @Test
  void writesMultipleFilesWithZeroPaddedIndex() throws Exception {
    var articles =
        List.of(
            new NewsArticle("A", "https://a.com/1", "a.com", null, "kw", "prefix", ""),
            new NewsArticle("B", "https://a.com/2", "a.com", null, "kw", "prefix", ""));
    var chunk = new Chunk<>(List.of(articles));

    writer.write(chunk);

    assertThat(tempDir.resolve("prefix-00.md")).exists();
    assertThat(tempDir.resolve("prefix-01.md")).exists();
  }

  @Test
  void writesFileWithEmptyContentWhenArticleBodyIsBlank() throws Exception {
    var article = new NewsArticle("Title", "https://ex.com", "ex.com", null, "kw", "test", "");
    var chunk = new Chunk<>(List.of(List.of(article)));

    writer.write(chunk);

    var file = tempDir.resolve("test-00.md");
    assertThat(file).exists();
    var content = Files.readString(file);
    assertThat(content).contains("title: \"Title\"");
    assertThat(content).doesNotContain("null");
  }
}
