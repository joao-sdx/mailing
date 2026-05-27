package com.synapsedx.mailing.unitelegal2dataforseo;

import static org.assertj.core.api.Assertions.assertThat;

import com.synapsedx.mailing.unitelegal2dataforseo.config.QueryDefaults;
import com.synapsedx.mailing.unitelegal2dataforseo.config.Unitelegal2DataforseoProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = "spring.batch.job.enabled=false")
class ContextLoadsTest {

  @Autowired Unitelegal2DataforseoProperties properties;
  @Autowired QueryDefaults defaults;

  @Test
  void propertiesAreLoaded() {
    assertThat(properties.inputCsv()).isNotBlank();
    assertThat(properties.outputYml()).isNotBlank();
    assertThat(defaults.languageCode()).isEqualTo("fr");
    assertThat(defaults.depth()).isEqualTo(2);
    assertThat(defaults.locationCode()).isEqualTo(2250);
    assertThat(defaults.locationName()).isEqualTo("France");
    assertThat(defaults.filePrefix()).isEqualTo("assurance-fr");
  }
}
