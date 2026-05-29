package com.synapsedx.mailing.procurement.batch;

import com.synapsedx.mailing.procurement.batch.processor.TenderSearchProcessor;
import com.synapsedx.mailing.procurement.batch.reader.QueryReader;
import com.synapsedx.mailing.procurement.batch.writer.TenderCsvWriter;
import com.synapsedx.mailing.procurement.model.ProcurementQuery;
import com.synapsedx.mailing.procurement.model.Tender;
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
public class ProcurementJobConfig {

  private final JobRepository jobRepository;
  private final PlatformTransactionManager transactionManager;
  private final QueryReader queryReader;
  private final TenderSearchProcessor tenderSearchProcessor;
  private final TenderCsvWriter tenderCsvWriter;

  @Bean
  public Job procurementSearchJob() {
    return new JobBuilder("procurement-search", jobRepository).start(searchAndWriteStep()).build();
  }

  @Bean
  public Step searchAndWriteStep() {
    return new StepBuilder("searchAndWriteStep", jobRepository)
        .<ProcurementQuery, List<Tender>>chunk(1, transactionManager)
        .reader(queryReader)
        .processor(tenderSearchProcessor)
        .writer(tenderCsvWriter)
        .build();
  }
}
