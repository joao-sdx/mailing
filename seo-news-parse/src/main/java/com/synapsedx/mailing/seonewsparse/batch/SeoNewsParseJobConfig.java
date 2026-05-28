package com.synapsedx.mailing.seonewsparse.batch;

import com.synapsedx.mailing.seonewsparse.batch.processor.LmStudioExtractProcessor;
import com.synapsedx.mailing.seonewsparse.batch.reader.MarkdownDirectoryReader;
import com.synapsedx.mailing.seonewsparse.batch.writer.ContactsCsvWriter;
import com.synapsedx.mailing.seonewsparse.model.ArticleContacts;
import java.nio.file.Path;
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
public class SeoNewsParseJobConfig {

  private final JobRepository jobRepository;
  private final PlatformTransactionManager transactionManager;
  private final MarkdownDirectoryReader markdownDirectoryReader;
  private final LmStudioExtractProcessor lmStudioExtractProcessor;
  private final ContactsCsvWriter contactsCsvWriter;

  @Bean
  public Job seoNewsParseJob() {
    return new JobBuilder("seo-news-parse", jobRepository).start(extractStep()).build();
  }

  @Bean
  public Step extractStep() {
    return new StepBuilder("extractStep", jobRepository)
        .<Path, ArticleContacts>chunk(1, transactionManager)
        .reader(markdownDirectoryReader)
        .processor(lmStudioExtractProcessor)
        .writer(contactsCsvWriter)
        .listener(contactsCsvWriter)
        .build();
  }
}
