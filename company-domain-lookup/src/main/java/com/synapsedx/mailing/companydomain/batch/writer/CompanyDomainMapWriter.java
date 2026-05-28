package com.synapsedx.mailing.companydomain.batch.writer;

import com.synapsedx.mailing.companydomain.batch.support.CompanyDomainMap;
import com.synapsedx.mailing.companydomain.model.CompanyDomain;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CompanyDomainMapWriter implements ItemWriter<CompanyDomain> {

  private final CompanyDomainMap map;

  @Override
  public void write(Chunk<? extends CompanyDomain> chunk) {
    for (var item : chunk.getItems()) {
      map.put(item.companyKey(), item.domain());
    }
  }
}
