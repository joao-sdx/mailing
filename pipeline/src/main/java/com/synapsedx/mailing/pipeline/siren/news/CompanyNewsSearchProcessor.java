package com.synapsedx.mailing.pipeline.siren.news;

import com.synapsedx.mailing.pipeline.dataforseo.DataForSeoPort;
import com.synapsedx.mailing.pipeline.siren.news.model.CompanyNewsResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

/**
 * Calls DataForSEO Google News for the single denomination carried by a {@link CompanySearchQuery}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CompanyNewsSearchProcessor
    implements ItemProcessor<CompanySearchQuery, CompanyNewsResult> {

  private final DataForSeoPort dataForSeo;

  @Override
  public CompanyNewsResult process(CompanySearchQuery query) {
    var data = dataForSeo.searchNews(query.denomination());
    log.info("company_news_searched rcs={} denomination={}", query.rcs(), query.denomination());
    return new CompanyNewsResult(query.rcs(), query.denomination(), query.seq(), data);
  }
}
