package com.synapsedx.mailing.companydomain.batch.processor;

import com.synapsedx.mailing.companydomain.client.LmStudioClient;
import com.synapsedx.mailing.companydomain.config.CompanyDomainProperties;
import com.synapsedx.mailing.companydomain.model.ArticleSummary;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ArticleSummaryProcessor implements ItemProcessor<String, ArticleSummary> {

  private final CompanyDomainProperties properties;
  private final LmStudioClient lmStudioClient;

  @Override
  public ArticleSummary process(String articleId) {
    var path = resolveArticlePath(articleId);
    if (!Files.exists(path)) {
      throw new IllegalStateException("article file not found: " + path);
    }
    log.info("article_summary_start article={}", articleId);
    var body = stripFrontmatter(readFile(path));
    var summary = lmStudioClient.summarizeArticle(body).orElse("");
    log.info("article_summary_done article={} words={}", articleId, wordCount(summary));
    return new ArticleSummary(articleId, summary);
  }

  private Path resolveArticlePath(String articleId) {
    var dir = properties.articlesDir();
    if (dir != null && !dir.isBlank()) {
      return Path.of(dir).resolve(articleId);
    }
    return Path.of(properties.inputCsv()).toAbsolutePath().getParent().resolve(articleId);
  }

  private String readFile(Path path) {
    try {
      return Files.readString(path);
    } catch (Exception e) {
      throw new IllegalStateException("failed to read article file: " + path, e);
    }
  }

  private String stripFrontmatter(String content) {
    var lines = content.split("\n", -1);
    if (lines.length > 0 && lines[0].strip().equals("---")) {
      for (var i = 1; i < lines.length; i++) {
        if (lines[i].strip().equals("---")) {
          return String.join("\n", Arrays.asList(lines).subList(i + 1, lines.length)).strip();
        }
      }
    }
    return content.strip();
  }

  private int wordCount(String summary) {
    return summary.isBlank() ? 0 : summary.trim().split("\\s+").length;
  }
}
