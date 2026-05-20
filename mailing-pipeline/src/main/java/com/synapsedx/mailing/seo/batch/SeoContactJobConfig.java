package com.synapsedx.mailing.seo.batch;

import com.synapsedx.mailing.seo.batch.processor.SeoContactProcessor;
import com.synapsedx.mailing.seo.batch.reader.SeoResultReader;
import com.synapsedx.mailing.seo.batch.writer.SeoContactWriter;
import com.synapsedx.mailing.seo.model.SeoContactBatch;
import com.synapsedx.mailing.seo.model.SeoResultItem;
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
public class SeoContactJobConfig {

  private final JobRepository jobRepository;
  private final PlatformTransactionManager transactionManager;
  private final SeoResultReader seoResultReader;
  private final SeoContactProcessor seoContactProcessor;
  private final SeoContactWriter seoContactWriter;

  @Bean
  public Job seoContactJob() {
    return new JobBuilder("seo-contact", jobRepository).start(scanContactsStep()).build();
  }

  @Bean
  public Step scanContactsStep() {
    return new StepBuilder("scanContactsStep", jobRepository)
        .<SeoResultItem, SeoContactBatch>chunk(1, transactionManager)
        .reader(seoResultReader)
        .processor(seoContactProcessor)
        .writer(seoContactWriter)
        .build();
  }
}
