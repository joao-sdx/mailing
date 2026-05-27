package com.synapsedx.mailing.seonews;

import com.synapsedx.mailing.seonews.config.DataForSeoProperties;
import com.synapsedx.mailing.seonews.config.SeoNewsProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({DataForSeoProperties.class, SeoNewsProperties.class})
public class SeoNewsApplication {

  public static void main(String[] args) {
    SpringApplication.run(SeoNewsApplication.class, args);
  }
}
