package com.synapsedx.mailing.companydomain.batch.processor;

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

  private final CompanyDomainMap map;

  @Override
  public EnrichedContactRow process(ContactRow row) {
    var key = row.company().trim().toUpperCase(Locale.ROOT);
    return new EnrichedContactRow(row, map.get(key));
  }
}
