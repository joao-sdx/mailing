package com.synapsedx.mailing.seo.batch;

import com.synapsedx.mailing.seo.batch.processor.SeoidLlmProcessor;
import com.synapsedx.mailing.seo.batch.reader.ArticleFileReader;
import com.synapsedx.mailing.seo.batch.reader.SeoidContactReader;
import com.synapsedx.mailing.seo.batch.writer.SeoidContactCollector;
import com.synapsedx.mailing.seo.batch.writer.SeoidCsvWriter;
import com.synapsedx.mailing.seo.model.TargetContact;
import java.nio.file.Path;
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
public class SeoidJob {

  private final JobRepository jobRepository;
  private final PlatformTransactionManager transactionManager;
  private final ArticleFileReader articleFileReader;
  private final SeoidLlmProcessor llmProcessor;
  private final SeoidContactCollector contactCollector;
  private final SeoidContactReader contactReader;
  private final SeoidCsvWriter csvWriter;

  @Bean
  public Job identifyTargetJob() {
    return new JobBuilder("seo-identify-target", jobRepository)
        .start(seoidIdentifyStep())
        .next(seoidWriteCsvStep())
        .build();
  }

  @Bean
  public Step seoidIdentifyStep() {
    return new StepBuilder("seoidIdentifyStep", jobRepository)
        .<Path, List<TargetContact>>chunk(1, transactionManager)
        .reader(articleFileReader)
        .processor(llmProcessor)
        .writer(contactCollector)
        .build();
  }

  @Bean
  public Step seoidWriteCsvStep() {
    return new StepBuilder("seoidWriteCsvStep", jobRepository)
        .<TargetContact, TargetContact>chunk(50, transactionManager)
        .reader(contactReader)
        .writer(csvWriter)
        .build();
  }
}
