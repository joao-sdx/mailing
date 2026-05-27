package com.synapsedx.mailing.seonews.batch.processor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.synapsedx.mailing.seonews.client.DataForSeoClient;
import com.synapsedx.mailing.seonews.model.RawNewsItem;
import com.synapsedx.mailing.seonews.model.SearchQuery;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DataForSeoProcessorTest {

  @Mock DataForSeoClient client;

  @InjectMocks DataForSeoProcessor processor;

  private final SearchQuery query =
      new SearchQuery("banque digitale", "fr", 2, 2250, "France", "banque-fr");

  @Test
  void buildsArticlesWithKeywordAndFilePrefix() throws Exception {
    var rawItems =
        List.of(
            new RawNewsItem("Article 1", "https://ex.com/1", "ex.com", "2026-05-01T00:00:00Z"),
            new RawNewsItem("Article 2", "https://ex.com/2", "ex.com", "2026-05-02T00:00:00Z"));
    when(client.searchNews(query)).thenReturn(rawItems);
    when(client.fetchContent("https://ex.com/1")).thenReturn("## Heading\n\nContent.");
    when(client.fetchContent("https://ex.com/2")).thenReturn("");

    var result = processor.process(query);

    assertThat(result).hasSize(2);
    assertThat(result.get(0).title()).isEqualTo("Article 1");
    assertThat(result.get(0).keyword()).isEqualTo("banque digitale");
    assertThat(result.get(0).filePrefix()).isEqualTo("banque-fr");
    assertThat(result.get(0).content()).isEqualTo("## Heading\n\nContent.");
    assertThat(result.get(1).content()).isEmpty();
  }

  @Test
  void returnsNullWhenNewsApiFails() throws Exception {
    when(client.searchNews(query)).thenThrow(new RuntimeException("API error"));

    var result = processor.process(query);

    assertThat(result).isNull();
  }
}
