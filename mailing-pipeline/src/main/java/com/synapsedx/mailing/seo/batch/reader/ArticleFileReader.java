package com.synapsedx.mailing.seo.batch.reader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@StepScope
public class ArticleFileReader implements ItemReader<Path> {

  private final Iterator<Path> fileIterator;

  public ArticleFileReader(@Value("#{jobParameters['articlesDir']}") String articlesDir)
      throws IOException {
    var dir = Path.of("output/seo").resolve(articlesDir);
    try (var stream = Files.list(dir)) {
      this.fileIterator =
          stream
              .filter(p -> p.toString().endsWith(".md"))
              .sorted()
              .collect(Collectors.toList())
              .iterator();
    }
    log.info("article_reader_init dir={}", dir.toAbsolutePath());
  }

  @Override
  public Path read() {
    return fileIterator.hasNext() ? fileIterator.next() : null;
  }
}
