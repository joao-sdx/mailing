package com.synapsedx.mailing.pipeline.siren.split;

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
public class InseeCompanyEnrichJobConfig {

  private final JobRepository jobRepository;
  private final PlatformTransactionManager transactionManager;
  private final InseeCompanyEnrichReader reader;
  private final InseeCompanyEnrichProcessor processor;
  private final InseeCompanyEnrichWriter writer;
  private final InseeCompanyEnrichProperties properties;

  @Bean
  public Job companyEnrichJob() {
    return new JobBuilder("company-enrich", jobRepository).start(companyEnrichStep()).build();
  }

  @Bean
  public Step companyEnrichStep() {
    return new StepBuilder("companyEnrichStep", jobRepository)
        .<com.synapsedx.mailing.pipeline.siren.enrich.model.enrich.CompanyRecord, SplitResult>chunk(
            1, transactionManager)
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
            var target = doneDir.resolve(file.getFileName());
            Files.move(file, target, StandardCopyOption.REPLACE_EXISTING);
            log.info("company_enrich_file_moved file={} -> done", file.getFileName());
          }
        } catch (Exception e) {
          log.error("company_enrich_file_move_failed reason={}", e.getMessage(), e);
        }
        return stepExecution.getExitStatus();
      }
    };
  }
}
