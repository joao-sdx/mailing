package com.synapsedx.mailing.procurement;

import com.synapsedx.mailing.procurement.config.BoampProperties;
import com.synapsedx.mailing.procurement.config.LmStudioProperties;
import com.synapsedx.mailing.procurement.config.ProcurementProperties;
import com.synapsedx.mailing.procurement.config.TedProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({
  ProcurementProperties.class,
  TedProperties.class,
  BoampProperties.class,
  LmStudioProperties.class
})
public class ProcurementSearchApplication {

  public static void main(String[] args) {
    SpringApplication.run(ProcurementSearchApplication.class, args);
  }
}
