package com.synapsedx.mailing.companydomain.batch.reader;

import static org.assertj.core.api.Assertions.assertThat;

import com.synapsedx.mailing.companydomain.config.CompanyDomainProperties;
import com.synapsedx.mailing.companydomain.model.ContactRow;
import java.util.ArrayList;
import org.junit.jupiter.api.Test;

class ContactsCsvReaderTest {

  @Test
  void emitsEveryRowWithHeadersAndCompany() throws Exception {
    var props =
        new CompanyDomainProperties(
            "src/test/resources/fixtures/contacts-sample.csv", "out.csv", 10, 5);
    var reader = new ContactsCsvReader(props);

    var all = new ArrayList<ContactRow>();
    ContactRow next;
    while ((next = reader.read()) != null) {
      all.add(next);
    }

    assertThat(all).hasSize(6);
    var first = all.get(0);
    assertThat(first.headers()).containsExactly("first_name", "last_name", "company", "article_id");
    assertThat(first.values()).containsExactly("Beñat", "Cazanave", "ARTZAINAK", "result-10-01.md");
    assertThat(first.company()).isEqualTo("ARTZAINAK");

    var rowWithEmptyCompany = all.get(5);
    assertThat(rowWithEmptyCompany.company()).isEqualTo("");
  }
}
