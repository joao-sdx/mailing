package com.synapsedx.mailing.companydomain.batch.processor;

import com.synapsedx.mailing.companydomain.batch.support.ArticleSummaryMap;
import com.synapsedx.mailing.companydomain.batch.support.CompanyDomainMap;
import com.synapsedx.mailing.companydomain.model.ContactRow;
import com.synapsedx.mailing.companydomain.model.EnrichedContactRow;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ContactEnrichProcessor implements ItemProcessor<ContactRow, EnrichedContactRow> {

  private final CompanyDomainMap domainMap;
  private final ArticleSummaryMap summaryMap;

  @Override
  public EnrichedContactRow process(ContactRow row) {
    var key = row.company().trim().toUpperCase(Locale.ROOT);
    var domain = domainMap.get(key);
    var summary = summaryMap.summary(row.articleId());
    var relevant = summaryMap.relevant(row.articleId());
    return new EnrichedContactRow(row, domain, summary, relevant);
  }
}
