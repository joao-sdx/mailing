package com.synapsedx.mailing.companydomain.batch.processor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.synapsedx.mailing.companydomain.client.DataForSeoSerpClient;
import com.synapsedx.mailing.companydomain.client.LmStudioClient;
import com.synapsedx.mailing.companydomain.config.CompanyDomainProperties;
import com.synapsedx.mailing.companydomain.model.SerpResult;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class DomainLookupProcessorTest {

  private final CompanyDomainProperties props =
      new CompanyDomainProperties("in.csv", "out.csv", 10, 5);

  @Test
  void emptySerpReturnsEmptyDomain() throws Exception {
    var serp = mock(DataForSeoSerpClient.class);
    var llm = mock(LmStudioClient.class);
    when(serp.searchOrganic(anyString(), anyInt())).thenReturn(List.of());
    var p = new DomainLookupProcessor(serp, llm, props);
    p.setThrottleMillis(0);

    var result = p.process("Factofrance");

    assertThat(result.companyKey()).isEqualTo("FACTOFRANCE");
    assertThat(result.domain()).isEqualTo("");
  }

  @Test
  void llmEmptyReturnsEmptyDomain() throws Exception {
    var serp = mock(DataForSeoSerpClient.class);
    var llm = mock(LmStudioClient.class);
    when(serp.searchOrganic(eq("Factofrance"), anyInt()))
        .thenReturn(List.of(new SerpResult("t", "https://x.com", "s")));
    when(llm.pickOfficialDomain(eq("Factofrance"), any())).thenReturn(Optional.empty());
    var p = new DomainLookupProcessor(serp, llm, props);
    p.setThrottleMillis(0);

    assertThat(p.process("Factofrance").domain()).isEqualTo("");
  }

  @Test
  void normalisesLlmUrlToBareHost() throws Exception {
    var serp = mock(DataForSeoSerpClient.class);
    var llm = mock(LmStudioClient.class);
    when(serp.searchOrganic(eq("Factofrance"), anyInt()))
        .thenReturn(List.of(new SerpResult("t", "https://www.factofrance.com", "s")));
    when(llm.pickOfficialDomain(eq("Factofrance"), any()))
        .thenReturn(Optional.of("https://www.factofrance.com/contact"));
    var p = new DomainLookupProcessor(serp, llm, props);
    p.setThrottleMillis(0);

    assertThat(p.process("Factofrance").domain()).isEqualTo("factofrance.com");
  }

  @Test
  void serpThrowsReturnsEmptyDomain() throws Exception {
    var serp = mock(DataForSeoSerpClient.class);
    var llm = mock(LmStudioClient.class);
    when(serp.searchOrganic(anyString(), anyInt())).thenThrow(new RuntimeException("boom"));
    var p = new DomainLookupProcessor(serp, llm, props);
    p.setThrottleMillis(0);

    var result = p.process("Factofrance");
    assertThat(result.companyKey()).isEqualTo("FACTOFRANCE");
    assertThat(result.domain()).isEqualTo("");
  }

  @Test
  void topNTruncatesResultsBeforeSendingToLlm() throws Exception {
    var serp = mock(DataForSeoSerpClient.class);
    var llm = mock(LmStudioClient.class);
    when(serp.searchOrganic(eq("X"), anyInt()))
        .thenReturn(
            List.of(
                new SerpResult("a", "https://a.com", ""),
                new SerpResult("b", "https://b.com", ""),
                new SerpResult("c", "https://c.com", ""),
                new SerpResult("d", "https://d.com", ""),
                new SerpResult("e", "https://e.com", ""),
                new SerpResult("f", "https://f.com", "")));
    when(llm.pickOfficialDomain(eq("X"), any())).thenReturn(Optional.empty());

    var smallProps = new CompanyDomainProperties("in.csv", "out.csv", 10, 3);
    var p = new DomainLookupProcessor(serp, llm, smallProps);
    p.setThrottleMillis(0);
    p.process("X");

    org.mockito.ArgumentCaptor<List<SerpResult>> captor =
        org.mockito.ArgumentCaptor.forClass(List.class);
    org.mockito.Mockito.verify(llm).pickOfficialDomain(eq("X"), captor.capture());
    assertThat(captor.getValue()).hasSize(3);
  }
}
