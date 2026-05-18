package com.synapsedx.mailing.seo.batch;

import com.synapsedx.mailing.seo.model.TargetContact;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class SeoidContactStore {

  private final List<TargetContact> contacts = new ArrayList<>();
  private int readIndex = 0;

  public void addAll(List<TargetContact> items) {
    contacts.addAll(items);
  }

  public TargetContact next() {
    return readIndex < contacts.size() ? contacts.get(readIndex++) : null;
  }
}
