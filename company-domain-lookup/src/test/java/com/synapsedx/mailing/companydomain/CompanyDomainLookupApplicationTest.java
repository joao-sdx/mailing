package com.synapsedx.mailing.companydomain;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(
    properties = {
      "dataforseo.api.user=test",
      "dataforseo.api.key=test",
      "spring.batch.job.enabled=false"
    })
class CompanyDomainLookupApplicationTest {

  @Test
  void contextLoads() {}
}
