package com.synapsedx.mailing.companydomain.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(
    properties = {
      "dataforseo.api.user=u",
      "dataforseo.api.key=k",
      "lmstudio.server=http://x:1",
      "lmstudio.model=m",
      "lmstudio.key=lm-studio",
      "lmstudio.connect-timeout-seconds=5",
      "lmstudio.request-timeout-seconds=15",
      "company-domain.input-csv=in.csv",
      "company-domain.output-csv=out.csv",
      "company-domain.articles-dir=/tmp/articles",
      "company-domain.serp-depth=20",
      "company-domain.serp-top-n=7",
      "spring.batch.job.enabled=false"
    })
class PropertiesBindingTest {

  @Autowired DataForSeoProperties dataforseo;
  @Autowired LmStudioProperties lmstudio;
  @Autowired CompanyDomainProperties companyDomain;

  @Test
  void bindsAllProperties() {
    assertThat(dataforseo.api().user()).isEqualTo("u");
    assertThat(dataforseo.api().key()).isEqualTo("k");
    assertThat(lmstudio.server()).isEqualTo("http://x:1");
    assertThat(lmstudio.model()).isEqualTo("m");
    assertThat(lmstudio.key()).isEqualTo("lm-studio");
    assertThat(lmstudio.connectTimeoutSeconds()).isEqualTo(5);
    assertThat(lmstudio.requestTimeoutSeconds()).isEqualTo(15);
    assertThat(companyDomain.inputCsv()).isEqualTo("in.csv");
    assertThat(companyDomain.outputCsv()).isEqualTo("out.csv");
    assertThat(companyDomain.articlesDir()).isEqualTo("/tmp/articles");
    assertThat(companyDomain.serpDepth()).isEqualTo(20);
    assertThat(companyDomain.serpTopN()).isEqualTo(7);
    assertThat(dataforseo.serpOrganicEndpoint())
        .isEqualTo("https://api.dataforseo.com/v3/serp/google/organic/live/advanced");
  }
}
