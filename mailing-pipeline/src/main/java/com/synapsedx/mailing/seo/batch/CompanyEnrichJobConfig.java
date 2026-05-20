package com.synapsedx.mailing.seo.batch;

import com.synapsedx.mailing.seo.batch.reader.CompanyEnrichReader;
import com.synapsedx.mailing.seo.batch.writer.CompanyEnrichWriter;
import com.synapsedx.mailing.seo.model.CompanyToEnrich;
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
public class CompanyEnrichJobConfig {

  private final JobRepository jobRepository;
  private final PlatformTransactionManager transactionManager;
  private final CompanyEnrichReader reader;
  private final CompanyEnrichWriter writer;

  @Bean
  public Job enrichCompaniesJob() {
    return new JobBuilder("enrich-companies", jobRepository).start(enrichStep()).build();
  }

  @Bean
  public Step enrichStep() {
    return new StepBuilder("enrichStep", jobRepository)
        .<CompanyToEnrich, CompanyToEnrich>chunk(5, transactionManager)
        .reader(reader)
        .writer(writer)
        .build();
  }
}
