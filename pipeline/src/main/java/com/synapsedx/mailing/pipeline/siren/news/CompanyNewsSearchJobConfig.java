package com.synapsedx.mailing.pipeline.siren.news;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class CompanyNewsSearchJobConfig {

  private final JobRepository jobRepository;
  private final PlatformTransactionManager transactionManager;
  private final CompanyNewsSearchReader reader;
  private final CompanyNewsSearchProcessor processor;
  private final CompanyNewsSearchWriter writer;
  private final CompanyNewsSearchProperties properties;

  @Bean
  public Job companyNewsSearchJob() {
    return new JobBuilder("company-news-search", jobRepository)
        .start(companyNewsSearchStep())
        .build();
  }

  @Bean
  public Step companyNewsSearchStep() {
    return new StepBuilder("companyNewsSearchStep", jobRepository)
        .<CompanySearchQuery, com.synapsedx.mailing.pipeline.siren.news.model.CompanyNewsResult>
            chunk(1, transactionManager)
        .reader(reader)
        .processor(processor)
        .writer(writer)
        .listener(fileMoveListener())
        .build();
  }

  private StepExecutionListener fileMoveListener() {
    return new StepExecutionListener() {
      @Override
      public void beforeStep(StepExecution stepExecution) {}

      @Override
      public org.springframework.batch.core.ExitStatus afterStep(StepExecution stepExecution) {
        var doneDir = Path.of(properties.getDoneDir());
        try {
          Files.createDirectories(doneDir);
          for (var file : reader.getProcessedFiles()) {
            Files.move(
                file, doneDir.resolve(file.getFileName()), StandardCopyOption.REPLACE_EXISTING);
            log.info("company_news_file_moved file={} -> done", file.getFileName());
          }
        } catch (Exception e) {
          log.error("company_news_file_move_failed reason={}", e.getMessage(), e);
        }
        return stepExecution.getExitStatus();
      }
    };
  }
}
