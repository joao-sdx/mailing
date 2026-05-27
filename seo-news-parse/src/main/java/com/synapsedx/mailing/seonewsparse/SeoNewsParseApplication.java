package com.synapsedx.mailing.seonewsparse;

import com.synapsedx.mailing.seonewsparse.config.LmStudioProperties;
import com.synapsedx.mailing.seonewsparse.config.SeoNewsParseProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({LmStudioProperties.class, SeoNewsParseProperties.class})
public class SeoNewsParseApplication {

  public static void main(String[] args) {
    SpringApplication.run(SeoNewsParseApplication.class, args);
  }
}
