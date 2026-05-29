package com.synapsedx.mailing.sedia;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class SediaFundingApplication {

  public static void main(String[] args) {
    SpringApplication.run(SediaFundingApplication.class, args);
  }
}
