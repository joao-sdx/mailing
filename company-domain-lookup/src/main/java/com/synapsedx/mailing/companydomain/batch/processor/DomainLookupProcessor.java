package com.synapsedx.mailing.companydomain.batch.processor;

import com.synapsedx.mailing.companydomain.client.DataForSeoSerpClient;
import com.synapsedx.mailing.companydomain.client.LmStudioClient;
import com.synapsedx.mailing.companydomain.config.CompanyDomainProperties;
import com.synapsedx.mailing.companydomain.model.CompanyDomain;
import com.synapsedx.mailing.companydomain.util.Domains;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DomainLookupProcessor implements ItemProcessor<String, CompanyDomain> {

  private final DataForSeoSerpClient serpClient;
  private final LmStudioClient lmStudioClient;
  private final CompanyDomainProperties properties;

  private long throttleMillis = 500;

  void setThrottleMillis(long ms) {
    this.throttleMillis = ms;
  }

  @Override
  public CompanyDomain process(String company) {
    var key = company.trim().toUpperCase(Locale.ROOT);
    log.info("domain_lookup_start company={}", company);
    try {
      throttle();
      var results = serpClient.searchOrganic(company, properties.serpDepth());
      if (results.isEmpty()) {
        log.info("domain_lookup_done company={} domain=", company);
        return new CompanyDomain(key, "");
      }
      var topN = results.subList(0, Math.min(properties.serpTopN(), results.size()));
      var picked = lmStudioClient.pickOfficialDomain(company, topN);
      var domain = picked.map(Domains::extractHost).orElse("");
      log.info("domain_lookup_done company={} domain={}", company, domain);
      return new CompanyDomain(key, domain);
    } catch (Exception e) {
      log.warn("domain_lookup_failed company={} reason={}", company, e.getMessage());
      return new CompanyDomain(key, "");
    }
  }

  private void throttle() {
    if (throttleMillis <= 0) {
      return;
    }
    try {
      Thread.sleep(throttleMillis);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }
}
