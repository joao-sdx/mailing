package com.synapsedx.mailing.pipeline.siren.news;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.synapsedx.mailing.pipeline.siren.news.model.CompanyNewsResult;
import java.nio.file.Files;
import java.nio.file.Path;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

/** Writes {@link CompanyNewsResult} items to {@code 12-company-news/}. */
@Slf4j
@Component
@RequiredArgsConstructor
public class CompanyNewsSearchWriter implements ItemWriter<CompanyNewsResult> {

  private final CompanyNewsSearchProperties properties;
  private final ObjectMapper objectMapper;

  @Override
  public void write(Chunk<? extends CompanyNewsResult> chunk) throws Exception {
    var outputDir = Path.of(properties.getOutputDir());
    Files.createDirectories(outputDir);

    var pretty = objectMapper.writerWithDefaultPrettyPrinter();
    for (var result : chunk.getItems()) {
      var items = result.data().path("items");
      var i = 0;
      for (var item : items) {
        var file = outputDir.resolve(result.rcs() + "-" + result.seq() + "-" + i + ".json");
        pretty.writeValue(file.toFile(), item);
        i++;
      }
      log.debug(
          "company_news_written rcs={} keyword={} files={}", result.rcs(), result.keyword(), i);
    }
  }
}
