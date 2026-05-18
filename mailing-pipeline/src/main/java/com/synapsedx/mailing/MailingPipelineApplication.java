package com.synapsedx.mailing;

import com.synapsedx.mailing.seo.config.DataForSeoProperties;
import com.synapsedx.mailing.seo.config.SeoidProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({DataForSeoProperties.class, SeoidProperties.class})
public class MailingPipelineApplication {

  public static void main(String[] args) {
    SpringApplication.run(MailingPipelineApplication.class, args);
  }
}
