package com.synapsedx.mailing.unitelegal2dataforseo;

import com.synapsedx.mailing.unitelegal2dataforseo.config.QueryDefaults;
import com.synapsedx.mailing.unitelegal2dataforseo.config.Unitelegal2DataforseoProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({Unitelegal2DataforseoProperties.class, QueryDefaults.class})
public class Unitelegal2DataforseoApplication {

  public static void main(String[] args) {
    SpringApplication.run(Unitelegal2DataforseoApplication.class, args);
  }
}
