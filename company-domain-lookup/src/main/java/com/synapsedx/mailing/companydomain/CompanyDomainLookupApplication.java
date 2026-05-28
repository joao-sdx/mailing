package com.synapsedx.mailing.companydomain;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class CompanyDomainLookupApplication {

  public static void main(String[] args) {
    SpringApplication.run(CompanyDomainLookupApplication.class, args);
  }
}
