package com.synapsedx.mailing.sedia.batch.processor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

import com.synapsedx.mailing.sedia.client.LmStudioClient;
import com.synapsedx.mailing.sedia.model.FundingCall;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

class RelevanceProcessorTest {

  private LmStudioClient lmStudioClient;
  private RelevanceProcessor processor;

  @BeforeEach
  void setUp() {
    lmStudioClient = mock(LmStudioClient.class);
    processor = new RelevanceProcessor(lmStudioClient);
  }

  private FundingCall call(String id, String title, String summary) {
    return new FundingCall(
        id,
        "CALL-01",
        title,
        "Horizon Europe",
        "Open",
        "2026-10-01T00:00:00Z",
        "2026-01-01T00:00:00Z",
        "1000000",
        "https://ec.europa.eu/topic/" + id,
        summary);
  }

  @Test
  void mapsRelevantCallToTrue() {
    when(lmStudioClient.assessRelevance(contains("EIC Accelerator"))).thenReturn(Optional.of(true));
    when(lmStudioClient.summarize(any())).thenReturn(Optional.of("some summary"));
    var call = call("EIC-01", "EIC Accelerator Open 2026", "Support for deep tech startups.");

    var result = processor.process(call);

    assertThat(result.call()).isEqualTo(call);
    assertThat(result.relevant()).isEqualTo("true");
    assertThat(result.summary()).isEqualTo("some summary");
  }

  @Test
  void mapsIrrelevantCallToFalse() {
    when(lmStudioClient.assessRelevance(contains("Fisheries"))).thenReturn(Optional.of(false));
    var call = call("FISH-01", "Fisheries Research 2026", "Study of Atlantic cod populations.");

    var result = processor.process(call);

    assertThat(result.relevant()).isEqualTo("false");
    assertThat(result.summary()).isEmpty();
  }

  @Test
  void mapsEmptyLlmResponseToEmptyString() {
    when(lmStudioClient.assessRelevance(contains("Unknown Call"))).thenReturn(Optional.empty());
    var call = call("UNKNOWN-01", "Unknown Call", "");

    var result = processor.process(call);

    assertThat(result.relevant()).isEmpty();
    assertThat(result.summary()).isEmpty();
  }

  @Test
  void includesTitleAndSummaryInPromptText() {
    var title = "Digital SME Innovation";
    var summary = "Accelerating cloud adoption for SMEs.";
    when(lmStudioClient.assessRelevance(contains(title))).thenReturn(Optional.of(true));
    when(lmStudioClient.assessRelevance(contains(summary))).thenReturn(Optional.of(true));
    when(lmStudioClient.summarize(any())).thenReturn(Optional.of("some summary"));

    var call = call("DIGITAL-01", title, summary);
    processor.process(call);

    // verify the combined text was passed (both title and summary present)
    var captor = ArgumentCaptor.forClass(String.class);
    Mockito.verify(lmStudioClient).assessRelevance(captor.capture());
    assertThat(captor.getValue()).contains(title).contains(summary);
  }

  @Test
  void summarizesCallWhenRelevant() {
    when(lmStudioClient.assessRelevance(any())).thenReturn(Optional.of(true));
    when(lmStudioClient.summarize(any()))
        .thenReturn(Optional.of("This call targets SMEs in cloud."));
    var call = call("SME-01", "SME Cloud Innovation", "Cloud adoption for small businesses.");

    var result = processor.process(call);

    assertThat(result.summary()).isEqualTo("This call targets SMEs in cloud.");
    Mockito.verify(lmStudioClient).summarize(any());
  }

  @Test
  void doesNotSummarizeWhenIrrelevant() {
    when(lmStudioClient.assessRelevance(any())).thenReturn(Optional.of(false));
    var call = call("FISH-02", "Fisheries Monitoring 2026", "Ocean fish stock monitoring.");

    var result = processor.process(call);

    assertThat(result.summary()).isEmpty();
    Mockito.verify(lmStudioClient, never()).summarize(any());
  }

  @Test
  void doesNotSummarizeWhenRelevanceEmpty() {
    when(lmStudioClient.assessRelevance(any())).thenReturn(Optional.empty());
    var call = call("UNKNOWN-02", "Unknown Topic 2026", "Some unclassifiable call.");

    var result = processor.process(call);

    assertThat(result.summary()).isEmpty();
    Mockito.verify(lmStudioClient, never()).summarize(any());
  }
}
