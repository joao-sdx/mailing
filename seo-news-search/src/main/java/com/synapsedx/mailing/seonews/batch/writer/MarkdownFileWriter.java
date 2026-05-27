package com.synapsedx.mailing.seonews.batch.writer;

import com.synapsedx.mailing.seonews.config.SeoNewsProperties;
import com.synapsedx.mailing.seonews.model.NewsArticle;
import jakarta.annotation.PostConstruct;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MarkdownFileWriter implements ItemWriter<List<NewsArticle>> {

  private final SeoNewsProperties properties;

  @PostConstruct
  void init() throws Exception {
    var dir = Path.of(properties.outputDir());
    if (!Files.exists(dir)) {
      Files.createDirectories(dir);
      log.info("output_dir_created path={}", dir.toAbsolutePath());
    }
    if (!Files.isWritable(dir)) {
      throw new IllegalStateException("Output directory is not writable: " + dir.toAbsolutePath());
    }
  }

  @Override
  public void write(Chunk<? extends List<NewsArticle>> chunk) throws Exception {
    for (var articles : chunk.getItems()) {
      for (int i = 0; i < articles.size(); i++) {
        writeArticle(articles.get(i), i);
      }
    }
  }

  private void writeArticle(NewsArticle article, int index) throws Exception {
    var filename = String.format("%s-%02d.md", article.filePrefix(), index);
    var file = Path.of(properties.outputDir(), filename);
    var sb = new StringBuilder();
    sb.append("---\n");
    sb.append("title: \"").append(escape(article.title())).append("\"\n");
    sb.append("url: ").append(nullToEmpty(article.url())).append("\n");
    sb.append("domain: ").append(nullToEmpty(article.domain())).append("\n");
    sb.append("published: ").append(nullToEmpty(article.published())).append("\n");
    sb.append("keyword: ").append(nullToEmpty(article.keyword())).append("\n");
    sb.append("---\n");
    if (article.content() != null && !article.content().isBlank()) {
      sb.append("\n").append(article.content());
    }
    Files.writeString(file, sb.toString());
    log.info("article_written file={}", filename);
  }

  private String escape(String value) {
    return value == null ? "" : value.replace("\"", "\\\"");
  }

  private String nullToEmpty(String value) {
    return value == null ? "" : value;
  }
}
