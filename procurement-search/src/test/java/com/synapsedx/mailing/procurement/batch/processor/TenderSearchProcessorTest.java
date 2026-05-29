package com.synapsedx.mailing.procurement.batch.processor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.synapsedx.mailing.procurement.client.LmStudioClient;
import com.synapsedx.mailing.procurement.client.TenderSource;
import com.synapsedx.mailing.procurement.config.ProcurementProperties;
import com.synapsedx.mailing.procurement.model.ProcurementQuery;
import com.synapsedx.mailing.procurement.model.Source;
import com.synapsedx.mailing.procurement.model.Tender;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TenderSearchProcessorTest {

  @Mock private TenderSource tedSource;
  @Mock private TenderSource boampSource;
  @Mock private ProcurementProperties properties;
  @Mock private LmStudioClient lmStudioClient;

  private TenderSearchProcessor processor;

  @BeforeEach
  void setUp() {
    when(tedSource.source()).thenReturn(Source.TED);
    when(boampSource.source()).thenReturn(Source.BOAMP);
    when(properties.throttleMillis()).thenReturn(0L);

    processor =
        new TenderSearchProcessor(List.of(tedSource, boampSource), properties, lmStudioClient);
    processor.init();
    processor.setThrottleMillis(0);
  }

  @Test
  void dispatchesToCorrectSourceAndAttachesRelevance() throws Exception {
    var query = new ProcurementQuery(Source.TED, null, null);
    when(tedSource.search(query)).thenReturn(List.of(tender("T-1", "TED")));
    when(lmStudioClient.assessPostmasterFit(any())).thenReturn(Optional.of(true));

    var result = processor.process(query);

    assertThat(result).hasSize(1);
    assertThat(result.getFirst().id()).isEqualTo("T-1");
    assertThat(result.getFirst().relevant()).isEqualTo("true");
    verify(boampSource, never()).search(query);
  }

  @Test
  void returnsNullWhenSourceThrows() throws Exception {
    var query = new ProcurementQuery(Source.BOAMP, null, null);
    when(boampSource.search(query)).thenThrow(new RuntimeException("network error"));

    var result = processor.process(query);

    assertThat(result).isNull();
  }

  @Test
  void returnsNullForUnknownSource() throws Exception {
    // Build a processor that only knows about BOAMP; a TED query should return null
    var boampOnly = org.mockito.Mockito.mock(TenderSource.class);
    when(boampOnly.source()).thenReturn(Source.BOAMP);
    var localProcessor = new TenderSearchProcessor(List.of(boampOnly), properties, lmStudioClient);
    localProcessor.init();
    localProcessor.setThrottleMillis(0);

    var query = new ProcurementQuery(Source.TED, null, null);

    var result = localProcessor.process(query);

    assertThat(result).isNull();
    verify(boampOnly, never()).search(query);
  }

  @Test
  void returnsNullForEmptyResults() throws Exception {
    var query = new ProcurementQuery(Source.TED, null, null);
    when(tedSource.search(query)).thenReturn(List.of());

    var result = processor.process(query);

    assertThat(result).isNull();
  }

  @Test
  void setsEmptyRelevantWhenLlmFails() throws Exception {
    var query = new ProcurementQuery(Source.TED, null, null);
    when(tedSource.search(query)).thenReturn(List.of(tender("T-1", "TED")));
    when(lmStudioClient.assessPostmasterFit(any())).thenReturn(Optional.empty());

    var result = processor.process(query);

    assertThat(result).hasSize(1);
    assertThat(result.getFirst().relevant()).isEmpty();
  }

  private Tender tender(String id, String source) {
    return new Tender(
        source,
        id,
        "Title",
        "Buyer",
        "FRA",
        "CPV-1",
        "1000",
        LocalDate.now(),
        null,
        "https://example.com",
        "");
  }
}
