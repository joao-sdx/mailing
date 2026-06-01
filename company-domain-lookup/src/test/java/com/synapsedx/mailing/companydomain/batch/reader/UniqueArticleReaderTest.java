package com.synapsedx.mailing.companydomain.batch.reader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.synapsedx.mailing.companydomain.config.CompanyDomainProperties;
import java.util.ArrayList;
import java.util.Map;
import org.junit.jupiter.api.Test;

class UniqueArticleReaderTest {

  @Test
  void emitsUniqueArticleIdsPreservingFirstSeenOrder() throws Exception {
    var props =
        new CompanyDomainProperties(
            "src/test/resources/fixtures/contacts-sample.csv",
            "out.csv",
            "",
            10,
            5,
            Map.of("company", "company", "article_id", "article_id"));
    var reader = new UniqueArticleReader(props);

    var seen = new ArrayList<String>();
    String next;
    while ((next = reader.read()) != null) {
      seen.add(next);
    }

    assertThat(seen)
        .containsExactly(
            "result-10-01.md",
            "result-10-02.md",
            "result-10-03.md",
            "result-10-04.md",
            "result-10-05.md");
  }

  @Test
  void failsFastWhenArticleIdColumnMissing() {
    var props =
        new CompanyDomainProperties(
            "src/test/resources/fixtures/no-article-id-header.csv",
            "out.csv",
            "",
            10,
            5,
            Map.of("company", "company", "article_id", "article_id"));
    var reader = new UniqueArticleReader(props);
    assertThatThrownBy(reader::read)
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("article_id");
  }
}
