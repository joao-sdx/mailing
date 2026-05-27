package com.synapsedx.mailing.unitelegal2dataforseo.batch;

import com.synapsedx.mailing.unitelegal2dataforseo.batch.processor.KeywordDedupProcessor;
import com.synapsedx.mailing.unitelegal2dataforseo.batch.reader.InseeCsvReader;
import com.synapsedx.mailing.unitelegal2dataforseo.batch.writer.DataForSeoYamlWriter;
import com.synapsedx.mailing.unitelegal2dataforseo.model.InseeUniteLegale;
import com.synapsedx.mailing.unitelegal2dataforseo.model.KeywordBatch;
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
public class Unitelegal2DataforseoJobConfig {

  private final JobRepository jobRepository;
  private final PlatformTransactionManager transactionManager;
  private final InseeCsvReader inseeCsvReader;
  private final KeywordDedupProcessor keywordDedupProcessor;
  private final DataForSeoYamlWriter dataForSeoYamlWriter;

  @Bean
  public Job unitelegal2dataforseoJob() {
    return new JobBuilder("unitelegal2dataforseo", jobRepository).start(convertStep()).build();
  }

  @Bean
  public Step convertStep() {
    return new StepBuilder("convertStep", jobRepository)
        .<InseeUniteLegale, KeywordBatch>chunk(100, transactionManager)
        .reader(inseeCsvReader)
        .processor(keywordDedupProcessor)
        .writer(dataForSeoYamlWriter)
        .build();
  }
}
