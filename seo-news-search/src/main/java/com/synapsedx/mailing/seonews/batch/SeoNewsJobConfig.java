package com.synapsedx.mailing.seonews.batch;

import com.synapsedx.mailing.seonews.batch.processor.DataForSeoProcessor;
import com.synapsedx.mailing.seonews.batch.reader.YamlQueryReader;
import com.synapsedx.mailing.seonews.batch.writer.MarkdownFileWriter;
import com.synapsedx.mailing.seonews.model.NewsArticle;
import com.synapsedx.mailing.seonews.model.SearchQuery;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@RequiredArgsConstructor
public class SeoNewsJobConfig {

  private final JobRepository jobRepository;
  private final PlatformTransactionManager transactionManager;
  private final YamlQueryReader yamlQueryReader;
  private final DataForSeoProcessor dataForSeoProcessor;
  private final MarkdownFileWriter markdownFileWriter;

  @Bean
  public Job seoNewsJob() {
    return new JobBuilder("seo-news", jobRepository).start(searchAndWriteStep()).build();
  }

  @Bean
  public Step searchAndWriteStep() {
    return new StepBuilder("searchAndWriteStep", jobRepository)
        .<SearchQuery, List<NewsArticle>>chunk(1, transactionManager)
        .reader(yamlQueryReader)
        .processor(dataForSeoProcessor)
        .writer(markdownFileWriter)
        .build();
  }
}
