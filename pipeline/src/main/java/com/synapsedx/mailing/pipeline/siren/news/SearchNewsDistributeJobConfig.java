package com.synapsedx.mailing.pipeline.siren.news;

import java.nio.file.Path;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

/** Distributes enriched records to search-news input queues (10-company, 11-person). */
@Configuration
@RequiredArgsConstructor
public class SearchNewsDistributeJobConfig {

  private final JobRepository jobRepository;
  private final PlatformTransactionManager transactionManager;
  private final SearchNewsDistributeProperties properties;

  @Bean
  public Job searchNewsDistributeJob() {
    return new JobBuilder("search-news-distribute", jobRepository)
        .start(distributeCompanyStep())
        .next(distributePersonStep())
        .build();
  }

  @Bean
  public Step distributeCompanyStep() {
    return new StepBuilder("distributeCompanyStep", jobRepository)
        .tasklet(
            new FileCopyTasklet(
                Path.of(properties.getCompanyInputDir()),
                Path.of(properties.getCompanyOutputDir()),
                Path.of(properties.getCompanyDoneDir())),
            transactionManager)
        .build();
  }

  @Bean
  public Step distributePersonStep() {
    return new StepBuilder("distributePersonStep", jobRepository)
        .tasklet(
            new FileCopyTasklet(
                Path.of(properties.getPersonInputDir()),
                Path.of(properties.getPersonOutputDir()),
                Path.of(properties.getPersonDoneDir())),
            transactionManager)
        .build();
  }
}
