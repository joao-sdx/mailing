package com.synapsedx.mailing.seo.batch.reader;

import com.synapsedx.mailing.seo.batch.SeoidContactStore;
import com.synapsedx.mailing.seo.model.TargetContact;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.item.ItemReader;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SeoidContactReader implements ItemReader<TargetContact> {

  private final SeoidContactStore store;

  @Override
  public TargetContact read() {
    return store.next();
  }
}
