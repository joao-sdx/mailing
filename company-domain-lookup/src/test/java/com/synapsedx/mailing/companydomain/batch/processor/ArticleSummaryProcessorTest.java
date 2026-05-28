package com.synapsedx.mailing.companydomain.batch.processor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.synapsedx.mailing.companydomain.client.LmStudioClient;
import com.synapsedx.mailing.companydomain.config.CompanyDomainProperties;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

class ArticleSummaryProcessorTest {

  private CompanyDomainProperties propsForDir(Path dir) {
    return new CompanyDomainProperties("ignored.csv", "out.csv", dir.toString(), 10, 5);
  }

  @Test
  void stripsFrontmatterBeforeSendingBodyToLlm(@TempDir Path dir) throws Exception {
    Files.writeString(
        dir.resolve("a.md"), "---\ntitle: T\nurl: http://x\n---\nCorps réel de l'article.\n");
    var llm = mock(LmStudioClient.class);
    when(llm.summarizeArticle(anyString())).thenReturn(Optional.of("Un résumé."));
    var processor = new ArticleSummaryProcessor(propsForDir(dir), llm);

    var result = processor.process("a.md");

    assertThat(result.articleId()).isEqualTo("a.md");
    assertThat(result.summary()).isEqualTo("Un résumé.");
    var captor = ArgumentCaptor.forClass(String.class);
    Mockito.verify(llm).summarizeArticle(captor.capture());
    assertThat(captor.getValue()).isEqualTo("Corps réel de l'article.");
  }

  @Test
  void usesWholeContentWhenNoFrontmatter(@TempDir Path dir) throws Exception {
    Files.writeString(dir.resolve("b.md"), "Pas de frontmatter ici.");
    var llm = mock(LmStudioClient.class);
    when(llm.summarizeArticle(anyString())).thenReturn(Optional.of("ok"));
    var processor = new ArticleSummaryProcessor(propsForDir(dir), llm);

    processor.process("b.md");

    var captor = ArgumentCaptor.forClass(String.class);
    Mockito.verify(llm).summarizeArticle(captor.capture());
    assertThat(captor.getValue()).isEqualTo("Pas de frontmatter ici.");
  }

  @Test
  void throwsWhenArticleFileMissing(@TempDir Path dir) {
    var llm = mock(LmStudioClient.class);
    var processor = new ArticleSummaryProcessor(propsForDir(dir), llm);

    assertThatThrownBy(() -> processor.process("missing.md"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("missing.md");
  }

  @Test
  void emptySummaryWhenLlmReturnsEmpty(@TempDir Path dir) throws Exception {
    Files.writeString(dir.resolve("c.md"), "Contenu.");
    var llm = mock(LmStudioClient.class);
    when(llm.summarizeArticle(anyString())).thenReturn(Optional.empty());
    var processor = new ArticleSummaryProcessor(propsForDir(dir), llm);

    assertThat(processor.process("c.md").summary()).isEqualTo("");
  }

  @Test
  void returnsSummaryVerbatimWithoutTruncatingLongOutput(@TempDir Path dir) throws Exception {
    Files.writeString(dir.resolve("d.md"), "Contenu.");
    var longSummary = "mot ".repeat(50).trim(); // 50 words, well over 30
    var llm = mock(LmStudioClient.class);
    when(llm.summarizeArticle(anyString())).thenReturn(Optional.of(longSummary));
    var processor = new ArticleSummaryProcessor(propsForDir(dir), llm);

    assertThat(processor.process("d.md").summary()).isEqualTo(longSummary);
  }
}
