package com.synapsedx.mailing.sedia.batch;

import com.synapsedx.mailing.sedia.batch.processor.RelevanceProcessor;
import com.synapsedx.mailing.sedia.batch.reader.SediaCallReader;
import com.synapsedx.mailing.sedia.batch.writer.FundingCallsCsvWriter;
import com.synapsedx.mailing.sedia.model.FundingCall;
import com.synapsedx.mailing.sedia.model.ScoredCall;
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
public class SediaJobConfig {

  private final JobRepository jobRepository;
  private final PlatformTransactionManager transactionManager;
  private final SediaCallReader sediaCallReader;
  private final RelevanceProcessor relevanceProcessor;
  private final FundingCallsCsvWriter fundingCallsCsvWriter;

  @Bean
  public Job sediaFundingJob() {
    return new JobBuilder("sedia-funding", jobRepository).start(fetchAndScoreStep()).build();
  }

  @Bean
  public Step fetchAndScoreStep() {
    return new StepBuilder("fetchAndScoreStep", jobRepository)
        .<FundingCall, ScoredCall>chunk(1, transactionManager)
        .reader(sediaCallReader)
        .processor(relevanceProcessor)
        .writer(fundingCallsCsvWriter)
        .listener(sediaCallReader)
        .listener(fundingCallsCsvWriter)
        .build();
  }
}
