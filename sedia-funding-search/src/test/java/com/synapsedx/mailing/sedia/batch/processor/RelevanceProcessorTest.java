package com.synapsedx.mailing.sedia.batch.processor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.synapsedx.mailing.sedia.client.LmStudioClient;
import com.synapsedx.mailing.sedia.model.FundingCall;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
    var call = call("EIC-01", "EIC Accelerator Open 2026", "Support for deep tech startups.");

    var result = processor.process(call);

    assertThat(result.call()).isEqualTo(call);
    assertThat(result.relevant()).isEqualTo("true");
  }

  @Test
  void mapsIrrelevantCallToFalse() {
    when(lmStudioClient.assessRelevance(contains("Fisheries"))).thenReturn(Optional.of(false));
    var call = call("FISH-01", "Fisheries Research 2026", "Study of Atlantic cod populations.");

    var result = processor.process(call);

    assertThat(result.relevant()).isEqualTo("false");
  }

  @Test
  void mapsEmptyLlmResponseToEmptyString() {
    when(lmStudioClient.assessRelevance(contains("Unknown Call"))).thenReturn(Optional.empty());
    var call = call("UNKNOWN-01", "Unknown Call", "");

    var result = processor.process(call);

    assertThat(result.relevant()).isEmpty();
  }

  @Test
  void includesTitleAndSummaryInPromptText() {
    var title = "Digital SME Innovation";
    var summary = "Accelerating cloud adoption for SMEs.";
    when(lmStudioClient.assessRelevance(contains(title))).thenReturn(Optional.of(true));
    when(lmStudioClient.assessRelevance(contains(summary))).thenReturn(Optional.of(true));

    var call = call("DIGITAL-01", title, summary);
    processor.process(call);

    // verify the combined text was passed (both title and summary present)
    var captor = org.mockito.ArgumentCaptor.forClass(String.class);
    org.mockito.Mockito.verify(lmStudioClient).assessRelevance(captor.capture());
    assertThat(captor.getValue()).contains(title).contains(summary);
  }
}
