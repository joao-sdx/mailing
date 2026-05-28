package com.synapsedx.mailing.companydomain.batch.support;

import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class CompanyDomainMap {

  private final ConcurrentHashMap<String, String> entries = new ConcurrentHashMap<>();

  public void put(String key, String domain) {
    entries.put(key, domain == null ? "" : domain);
  }

  public String get(String key) {
    return entries.getOrDefault(key, "");
  }
}
