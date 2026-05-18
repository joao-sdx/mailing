package com.synapsedx.mailing.seo.batch.writer;

import com.synapsedx.mailing.seo.batch.SeoidContactStore;
import com.synapsedx.mailing.seo.model.TargetContact;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SeoidContactCollector implements ItemWriter<List<TargetContact>> {

  private final SeoidContactStore store;

  @Override
  public void write(Chunk<? extends List<TargetContact>> chunk) throws Exception {
    for (var contacts : chunk.getItems()) {
      store.addAll(contacts);
    }
  }
}
