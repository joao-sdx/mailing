package com.synapsedx.mailing.sedia.batch.reader;

import com.synapsedx.mailing.sedia.client.SediaSearchClient;
import com.synapsedx.mailing.sedia.config.SediaProperties;
import com.synapsedx.mailing.sedia.model.FundingCall;
import java.util.Iterator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.item.ItemReader;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SediaCallReader implements ItemReader<FundingCall>, StepExecutionListener {

  private static final long PAGE_DELAY_MS = 500L;

  private final SediaSearchClient client;
  private final SediaProperties properties;

  private Iterator<FundingCall> buffer;
  private int nextPage;
  private int totalResults;
  private int fetched;

  @Override
  public void beforeStep(StepExecution stepExecution) {
    buffer = null;
    nextPage = 1;
    totalResults = -1;
    fetched = 0;
  }

  @Override
  public FundingCall read() throws Exception {
    if (buffer == null) {
      fetchPage(1);
    }
    while (!buffer.hasNext()) {
      if (fetched >= totalResults) {
        log.info("sedia_reader_done total={}", totalResults);
        return null;
      }
      Thread.sleep(PAGE_DELAY_MS);
      fetchPage(nextPage);
    }
    return buffer.next();
  }

  private void fetchPage(int page) throws Exception {
    var searchPage = client.search(page);
    if (totalResults < 0) {
      totalResults = searchPage.totalResults();
    }
    buffer = searchPage.calls().iterator();
    fetched += searchPage.calls().size();
    nextPage = page + 1;
    log.info(
        "sedia_page_fetched page={} calls={} fetched={} total={}",
        page,
        searchPage.calls().size(),
        fetched,
        totalResults);
  }
}
