package com.synapsedx.mailing.seo.batch;

import com.synapsedx.mailing.seo.batch.processor.DataForSeoRequestProcessor;
import com.synapsedx.mailing.seo.batch.processor.SeoSummaryProcessor;
import com.synapsedx.mailing.seo.batch.reader.QueryStoreReader;
import com.synapsedx.mailing.seo.batch.reader.SeoSummaryReader;
import com.synapsedx.mailing.seo.batch.reader.YamlQueryReader;
import com.synapsedx.mailing.seo.batch.writer.DataForSeoResponseWriter;
import com.synapsedx.mailing.seo.batch.writer.QueryStoreWriter;
import com.synapsedx.mailing.seo.batch.writer.SeoSummaryWriter;
import com.synapsedx.mailing.seo.model.DataForSeoRequest;
import com.synapsedx.mailing.seo.model.SearchQuery;
import com.synapsedx.mailing.seo.model.SeoSummaryResult;
import com.synapsedx.mailing.seo.model.SeoSummaryTask;
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
public class DataForSeoNewsJob {

  private final JobRepository jobRepository;
  private final PlatformTransactionManager transactionManager;
  private final YamlQueryReader yamlQueryReader;
  private final QueryStoreWriter queryStoreWriter;
  private final QueryStoreReader queryStoreReader;
  private final DataForSeoRequestProcessor processor;
  private final DataForSeoResponseWriter responseWriter;
  private final SeoSummaryReader summaryReader;
  private final SeoSummaryProcessor summaryProcessor;
  private final SeoSummaryWriter summaryWriter;

  @Bean
  public Job seoJob() {
    return new JobBuilder("seo", jobRepository)
        .start(readQueriesStep())
        .next(callDataForSeoStep())
        .next(summarizeStep())
        .build();
  }

  @Bean
  public Step readQueriesStep() {
    return new StepBuilder("readQueriesStep", jobRepository)
        .<SearchQuery, SearchQuery>chunk(10, transactionManager)
        .reader(yamlQueryReader)
        .writer(queryStoreWriter)
        .build();
  }

  @Bean
  public Step callDataForSeoStep() {
    return new StepBuilder("callDataForSeoStep", jobRepository)
        .<SearchQuery, DataForSeoRequest>chunk(1, transactionManager)
        .reader(queryStoreReader)
        .processor(processor)
        .writer(responseWriter)
        .build();
  }

  @Bean
  public Step summarizeStep() {
    return new StepBuilder("summarizeStep", jobRepository)
        .<SeoSummaryTask, SeoSummaryResult>chunk(1, transactionManager)
        .reader(summaryReader)
        .processor(summaryProcessor)
        .writer(summaryWriter)
        .build();
  }
}
