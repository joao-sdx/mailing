package com.synapsedx.mailing.seo.batch;

import com.synapsedx.mailing.seo.model.SeoSummaryTask;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class SeoSummaryStore {

  private final List<SeoSummaryTask> tasks = new ArrayList<>();
  private int readIndex = 0;

  public void add(SeoSummaryTask task) {
    tasks.add(task);
  }

  public SeoSummaryTask next() {
    return readIndex < tasks.size() ? tasks.get(readIndex++) : null;
  }
}
