package com.synapsedx.mailing.companydomain.batch.writer;

import static org.assertj.core.api.Assertions.assertThat;

import com.synapsedx.mailing.companydomain.batch.support.ArticleSummaryMap;
import com.synapsedx.mailing.companydomain.model.ArticleSummary;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.batch.item.Chunk;

class ArticleSummaryMapWriterTest {

  @Test
  void putsEachSummaryIntoMap() throws Exception {
    var map = new ArticleSummaryMap();
    var writer = new ArticleSummaryMapWriter(map);

    writer.write(
        new Chunk<>(
            List.of(
                new ArticleSummary("r1.md", "Résumé un", "true"),
                new ArticleSummary("r2.md", "", ""))));

    assertThat(map.summary("r1.md")).isEqualTo("Résumé un");
    assertThat(map.relevant("r1.md")).isEqualTo("true");
    assertThat(map.summary("r2.md")).isEqualTo("");
    assertThat(map.relevant("r2.md")).isEqualTo("");
    assertThat(map.summary("absent.md")).isEqualTo("");
    assertThat(map.relevant("absent.md")).isEqualTo("");
  }
}
