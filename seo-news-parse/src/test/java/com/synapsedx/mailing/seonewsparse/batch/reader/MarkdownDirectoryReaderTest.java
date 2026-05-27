package com.synapsedx.mailing.seonewsparse.batch.reader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.synapsedx.mailing.seonewsparse.config.SeoNewsParseProperties;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MarkdownDirectoryReaderTest {

  @Test
  void readsAllMarkdownFiles(@TempDir Path tempDir) throws Exception {
    Files.createFile(tempDir.resolve("article-a.md"));
    Files.createFile(tempDir.resolve("article-b.md"));

    var properties = new SeoNewsParseProperties(tempDir.toString(), "output/contacts.csv");
    var reader = new MarkdownDirectoryReader(properties);
    reader.init();

    var first = reader.read();
    var second = reader.read();
    var third = reader.read();

    assertThat(first).isNotNull();
    assertThat(second).isNotNull();
    assertThat(third).isNull();
    assertThat(first.getFileName().toString()).endsWith(".md");
    assertThat(second.getFileName().toString()).endsWith(".md");
  }

  @Test
  void missingDirThrowsOnInit(@TempDir Path tempDir) {
    var nonExistent = tempDir.resolve("does-not-exist").toString();
    var properties = new SeoNewsParseProperties(nonExistent, "output/contacts.csv");
    var reader = new MarkdownDirectoryReader(properties);

    assertThatThrownBy(reader::init).isInstanceOf(IllegalStateException.class);
  }
}
