package com.synapsedx.mailing.sedia.batch.reader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.synapsedx.mailing.sedia.client.SediaSearchClient;
import com.synapsedx.mailing.sedia.config.SediaProperties;
import com.synapsedx.mailing.sedia.model.FundingCall;
import com.synapsedx.mailing.sedia.model.SearchPage;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SediaCallReaderTest {

  private SediaSearchClient client;
  private SediaCallReader reader;

  @BeforeEach
  void setUp() {
    client = mock(SediaSearchClient.class);
    var props =
        new SediaProperties(
            "http://localhost/search",
            "SEDIA",
            "***",
            List.of("43108390"),
            List.of("31094502"),
            List.of("1"),
            2,
            "output/test.csv");
    reader = new SediaCallReader(client, props);
    reader.beforeStep(null);
  }

  private FundingCall call(String id) {
    return new FundingCall(
        id,
        "CALL-01",
        "Title",
        "Horizon",
        "Open",
        "2026-10-01T00:00:00Z",
        "2026-01-01T00:00:00Z",
        "1000000",
        "https://ec.europa.eu/topic/" + id,
        "A description.");
  }

  @Test
  void returnsEachCallFromSinglePage() throws Exception {
    when(client.search(1)).thenReturn(new SearchPage(2, 1, List.of(call("A"), call("B"))));

    assertThat(reader.read().identifier()).isEqualTo("A");
    assertThat(reader.read().identifier()).isEqualTo("B");
    assertThat(reader.read()).isNull();
    verify(client, times(1)).search(anyInt());
  }

  @Test
  void paginatesWhenBufferExhausted() throws Exception {
    when(client.search(1)).thenReturn(new SearchPage(3, 1, List.of(call("A"), call("B"))));
    when(client.search(2)).thenReturn(new SearchPage(3, 2, List.of(call("C"))));

    assertThat(reader.read().identifier()).isEqualTo("A");
    assertThat(reader.read().identifier()).isEqualTo("B");
    assertThat(reader.read().identifier()).isEqualTo("C");
    assertThat(reader.read()).isNull();

    verify(client).search(1);
    verify(client).search(2);
  }

  @Test
  void returnsNullImmediatelyWhenNoResults() throws Exception {
    when(client.search(1)).thenReturn(new SearchPage(0, 1, List.of()));

    assertThat(reader.read()).isNull();
    verify(client, times(1)).search(1);
  }

  @Test
  void resetsStateOnBeforeStep() throws Exception {
    when(client.search(1)).thenReturn(new SearchPage(1, 1, List.of(call("A"))));

    reader.read(); // consume all
    assertThat(reader.read()).isNull();

    // reset and re-run
    reader.beforeStep(null);
    when(client.search(1)).thenReturn(new SearchPage(1, 1, List.of(call("B"))));
    assertThat(reader.read().identifier()).isEqualTo("B");
  }
}
