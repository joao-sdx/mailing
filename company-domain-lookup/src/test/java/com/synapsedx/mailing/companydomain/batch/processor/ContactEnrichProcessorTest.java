package com.synapsedx.mailing.companydomain.batch.processor;

import static org.assertj.core.api.Assertions.assertThat;

import com.synapsedx.mailing.companydomain.batch.support.ArticleSummaryMap;
import com.synapsedx.mailing.companydomain.batch.support.CompanyDomainMap;
import com.synapsedx.mailing.companydomain.model.ArticleSummary;
import com.synapsedx.mailing.companydomain.model.ContactRow;
import java.util.List;
import org.junit.jupiter.api.Test;

class ContactEnrichProcessorTest {

  @Test
  void enrichesWithMappedDomainAndSummary() {
    var domainMap = new CompanyDomainMap();
    domainMap.put("FACTOFRANCE", "factofrance.com");
    var summaryMap = new ArticleSummaryMap();
    summaryMap.put(new ArticleSummary("r.md", "Résumé de l'article.", "true"));
    var processor = new ContactEnrichProcessor(domainMap, summaryMap);

    var row =
        new ContactRow(
            List.of("first_name", "last_name", "company", "article_id"),
            List.of("Philippe", "Mutin", " factofrance ", "r.md"),
            " factofrance ",
            "r.md");

    var enriched = processor.process(row);

    assertThat(enriched.contact()).isSameAs(row);
    assertThat(enriched.domain()).isEqualTo("factofrance.com");
    assertThat(enriched.summary()).isEqualTo("Résumé de l'article.");
    assertThat(enriched.relevant()).isEqualTo("true");
  }

  @Test
  void emptyDomainAndSummaryWhenNotInMaps() {
    var processor = new ContactEnrichProcessor(new CompanyDomainMap(), new ArticleSummaryMap());
    var row =
        new ContactRow(
            List.of("first_name", "last_name", "company", "article_id"),
            List.of("a", "b", "Unknown", "x.md"),
            "Unknown",
            "x.md");

    var enriched = processor.process(row);
    assertThat(enriched.domain()).isEqualTo("");
    assertThat(enriched.summary()).isEqualTo("");
    assertThat(enriched.relevant()).isEqualTo("");
  }
}
