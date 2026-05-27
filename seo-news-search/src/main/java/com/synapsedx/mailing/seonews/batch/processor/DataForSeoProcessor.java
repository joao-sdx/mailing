package com.synapsedx.mailing.seonews.batch.processor;

import com.synapsedx.mailing.seonews.client.DataForSeoClient;
import com.synapsedx.mailing.seonews.model.NewsArticle;
import com.synapsedx.mailing.seonews.model.SearchQuery;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataForSeoProcessor implements ItemProcessor<SearchQuery, List<NewsArticle>> {

  private final DataForSeoClient client;

  @Override
  public List<NewsArticle> process(SearchQuery query) {
    try {
      var rawItems = client.searchNews(query);
      var articles = new ArrayList<NewsArticle>();
      for (var item : rawItems) {
        var content = client.fetchContent(item.url());
        articles.add(
            new NewsArticle(
                item.title(),
                item.url(),
                item.domain(),
                item.published(),
                query.keyword(),
                query.filePrefix(),
                content));
      }
      return articles;
    } catch (Exception e) {
      log.error("dataforseo_processor_failed keyword={}", query.keyword(), e);
      return null;
    }
  }
}
