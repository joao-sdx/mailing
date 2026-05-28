package com.synapsedx.mailing.companydomain.batch;

import com.synapsedx.mailing.companydomain.batch.processor.ArticleSummaryProcessor;
import com.synapsedx.mailing.companydomain.batch.processor.ContactEnrichProcessor;
import com.synapsedx.mailing.companydomain.batch.processor.DomainLookupProcessor;
import com.synapsedx.mailing.companydomain.batch.reader.ContactsCsvReader;
import com.synapsedx.mailing.companydomain.batch.reader.UniqueArticleReader;
import com.synapsedx.mailing.companydomain.batch.reader.UniqueCompanyReader;
import com.synapsedx.mailing.companydomain.batch.writer.ArticleSummaryMapWriter;
import com.synapsedx.mailing.companydomain.batch.writer.CompanyDomainMapWriter;
import com.synapsedx.mailing.companydomain.batch.writer.EnrichedContactsCsvWriter;
import com.synapsedx.mailing.companydomain.model.ArticleSummary;
import com.synapsedx.mailing.companydomain.model.CompanyDomain;
import com.synapsedx.mailing.companydomain.model.ContactRow;
import com.synapsedx.mailing.companydomain.model.EnrichedContactRow;
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
public class CompanyDomainLookupJobConfig {

  private final JobRepository jobRepository;
  private final PlatformTransactionManager transactionManager;
  private final UniqueCompanyReader uniqueCompanyReader;
  private final DomainLookupProcessor domainLookupProcessor;
  private final CompanyDomainMapWriter companyDomainMapWriter;
  private final UniqueArticleReader uniqueArticleReader;
  private final ArticleSummaryProcessor articleSummaryProcessor;
  private final ArticleSummaryMapWriter articleSummaryMapWriter;
  private final ContactsCsvReader contactsCsvReader;
  private final ContactEnrichProcessor contactEnrichProcessor;
  private final EnrichedContactsCsvWriter enrichedContactsCsvWriter;

  @Bean
  public Job companyDomainLookupJob() {
    return new JobBuilder("company-domain-lookup-job", jobRepository)
        .start(resolveDomainsStep())
        .next(resolveSummariesStep())
        .next(enrichContactsStep())
        .build();
  }

  @Bean
  public Step resolveDomainsStep() {
    return new StepBuilder("resolveDomainsStep", jobRepository)
        .<String, CompanyDomain>chunk(1, transactionManager)
        .reader(uniqueCompanyReader)
        .processor(domainLookupProcessor)
        .writer(companyDomainMapWriter)
        .build();
  }

  @Bean
  public Step resolveSummariesStep() {
    return new StepBuilder("resolveSummariesStep", jobRepository)
        .<String, ArticleSummary>chunk(1, transactionManager)
        .reader(uniqueArticleReader)
        .processor(articleSummaryProcessor)
        .writer(articleSummaryMapWriter)
        .build();
  }

  @Bean
  public Step enrichContactsStep() {
    return new StepBuilder("enrichContactsStep", jobRepository)
        .<ContactRow, EnrichedContactRow>chunk(100, transactionManager)
        .reader(contactsCsvReader)
        .processor(contactEnrichProcessor)
        .writer(enrichedContactsCsvWriter)
        .build();
  }
}
