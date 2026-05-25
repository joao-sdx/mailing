package com.synapsedx.mailing.pipeline;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.job.DefaultJobParametersExtractor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Orchestrates all INSEE jobs sequentially: prepare → company-enrich → search-news-distribute →
 * company-news-search.
 */
@Configuration
@RequiredArgsConstructor
public class IseePipelineJobConfig {

  private final JobRepository jobRepository;
  private final JobLauncher jobLauncher;

  @Bean
  public Job iseePipelineJob(
      @Qualifier("pipelinePrepareStep") Step pipelinePrepareStep,
      @Qualifier("pipelineCompanyEnrichStep") Step pipelineCompanyEnrichStep,
      @Qualifier("pipelineSearchNewsDistributeStep") Step pipelineSearchNewsDistributeStep,
      @Qualifier("pipelineCompanyNewsSearchStep") Step pipelineCompanyNewsSearchStep) {
    return new JobBuilder("insee-pipeline", jobRepository)
        .start(pipelinePrepareStep)
        .next(pipelineCompanyEnrichStep)
        .next(pipelineSearchNewsDistributeStep)
        .next(pipelineCompanyNewsSearchStep)
        .build();
  }

  @Bean
  public Step pipelinePrepareStep(@Qualifier("inseeEnrichPrepareJob") Job inseeEnrichPrepareJob) {
    return new StepBuilder("pipelinePrepareStep", jobRepository)
        .job(inseeEnrichPrepareJob)
        .launcher(jobLauncher)
        .parametersExtractor(new DefaultJobParametersExtractor())
        .build();
  }

  @Bean
  public Step pipelineCompanyEnrichStep(@Qualifier("companyEnrichJob") Job companyEnrichJob) {
    return new StepBuilder("pipelineCompanyEnrichStep", jobRepository)
        .job(companyEnrichJob)
        .launcher(jobLauncher)
        .parametersExtractor(new DefaultJobParametersExtractor())
        .build();
  }

  @Bean
  public Step pipelineSearchNewsDistributeStep(
      @Qualifier("searchNewsDistributeJob") Job searchNewsDistributeJob) {
    return new StepBuilder("pipelineSearchNewsDistributeStep", jobRepository)
        .job(searchNewsDistributeJob)
        .launcher(jobLauncher)
        .parametersExtractor(new DefaultJobParametersExtractor())
        .build();
  }

  @Bean
  public Step pipelineCompanyNewsSearchStep(
      @Qualifier("companyNewsSearchJob") Job companyNewsSearchJob) {
    return new StepBuilder("pipelineCompanyNewsSearchStep", jobRepository)
        .job(companyNewsSearchJob)
        .launcher(jobLauncher)
        .parametersExtractor(new DefaultJobParametersExtractor())
        .build();
  }
}
