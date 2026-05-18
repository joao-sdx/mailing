package com.synapsedx.mailing.seo.batch;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.stereotype.Component;

@Component
public class SeoJobContext {

  private final long jobStartTime = Instant.now().getEpochSecond();
  private final AtomicInteger articleCounter = new AtomicInteger(1);

  public long jobStartTime() {
    return jobStartTime;
  }

  public int nextArticleId() {
    return articleCounter.getAndIncrement();
  }
}
