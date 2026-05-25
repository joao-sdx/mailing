package com.synapsedx.mailing.pipeline;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class PipelineApplication {

  public static void main(String[] args) {
    SpringApplication.run(PipelineApplication.class, args);
  }
}
