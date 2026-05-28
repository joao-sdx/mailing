package com.synapsedx.mailing.companydomain.batch.reader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.synapsedx.mailing.companydomain.config.CompanyDomainProperties;
import java.util.ArrayList;
import org.junit.jupiter.api.Test;

class UniqueCompanyReaderTest {

  @Test
  void emitsUniqueCompaniesPreservingFirstSeenOrder() throws Exception {
    var props =
        new CompanyDomainProperties(
            "src/test/resources/fixtures/contacts-sample.csv", "out.csv", "", 10, 5);
    var reader = new UniqueCompanyReader(props);

    var seen = new ArrayList<String>();
    String next;
    while ((next = reader.read()) != null) {
      seen.add(next);
    }

    assertThat(seen).containsExactly("ARTZAINAK", "Factofrance", "Crédit Mutuel Alliance Fédérale");
  }

  @Test
  void failsFastWhenCompanyColumnMissing() {
    var props =
        new CompanyDomainProperties(
            "src/test/resources/fixtures/no-company-header.csv", "out.csv", "", 10, 5);
    var reader = new UniqueCompanyReader(props);
    assertThatThrownBy(reader::read)
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("company");
  }
}
