package com.synapsedx.mailing.companydomain.batch.writer;

import static org.assertj.core.api.Assertions.assertThat;

import com.synapsedx.mailing.companydomain.batch.support.CompanyDomainMap;
import com.synapsedx.mailing.companydomain.model.CompanyDomain;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.batch.item.Chunk;

class CompanyDomainMapWriterTest {

  @Test
  void writesEachDomainToMap() throws Exception {
    var map = new CompanyDomainMap();
    var writer = new CompanyDomainMapWriter(map);

    writer.write(
        new Chunk<>(
            List.of(
                new CompanyDomain("FACTOFRANCE", "factofrance.com"),
                new CompanyDomain("ARTZAINAK", ""))));

    assertThat(map.get("FACTOFRANCE")).isEqualTo("factofrance.com");
    assertThat(map.get("ARTZAINAK")).isEqualTo("");
    assertThat(map.get("UNKNOWN")).isEqualTo("");
  }
}
