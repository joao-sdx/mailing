package com.synapsedx.mailing.companydomain.batch.processor;

import static org.assertj.core.api.Assertions.assertThat;

import com.synapsedx.mailing.companydomain.batch.support.CompanyDomainMap;
import com.synapsedx.mailing.companydomain.model.ContactRow;
import java.util.List;
import org.junit.jupiter.api.Test;

class ContactEnrichProcessorTest {

  @Test
  void enrichesWithMappedDomainCaseInsensitive() {
    var map = new CompanyDomainMap();
    map.put("FACTOFRANCE", "factofrance.com");
    var processor = new ContactEnrichProcessor(map);

    var row =
        new ContactRow(
            List.of("first_name", "last_name", "company", "article_id"),
            List.of("Philippe", "Mutin", " factofrance ", "r.md"),
            " factofrance ");

    var enriched = processor.process(row);

    assertThat(enriched.contact()).isSameAs(row);
    assertThat(enriched.domain()).isEqualTo("factofrance.com");
  }

  @Test
  void emptyDomainWhenNotInMap() {
    var map = new CompanyDomainMap();
    var processor = new ContactEnrichProcessor(map);
    var row =
        new ContactRow(
            List.of("first_name", "last_name", "company", "article_id"),
            List.of("a", "b", "Unknown", "r.md"),
            "Unknown");

    assertThat(processor.process(row).domain()).isEqualTo("");
  }
}
