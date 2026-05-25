package com.synapsedx.mailing.pipeline.siren.enrich;

import com.synapsedx.mailing.pipeline.siren.base.InseeRecord;
import com.synapsedx.mailing.pipeline.siren.enrich.model.enrich.CompanyRecord;
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
public class InseeEnrichJobConfig {

  private final JobRepository jobRepository;
  private final PlatformTransactionManager transactionManager;
  private final InseeEnrichReader reader;
  private final InseeEnrichProcessor processor;
  private final InseeEnrichWriter writer;
  private final InseeEnrichProperties properties;

  @Bean
  public Job inseeEnrichPrepareJob() {
    return new JobBuilder("insee-enrich-prepare", jobRepository).start(inseeEnrichStep()).build();
  }

  @Bean
  public Step inseeEnrichStep() {
    return new StepBuilder("inseeEnrichStep", jobRepository)
        .<InseeRecord, CompanyRecord>chunk(10, transactionManager)
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
            log.info("insee_enrich_file_moved file={} -> done", file.getFileName());
          }
        } catch (Exception e) {
          log.error("insee_enrich_file_move_failed reason={}", e.getMessage(), e);
        }
        return stepExecution.getExitStatus();
      }
    };
  }
}
