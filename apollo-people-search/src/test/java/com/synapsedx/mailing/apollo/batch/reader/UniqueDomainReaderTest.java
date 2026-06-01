package com.synapsedx.mailing.apollo.batch.reader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.synapsedx.mailing.apollo.config.ApolloProperties;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class UniqueDomainReaderTest {

  private UniqueDomainReader readerFor(String classpathCsv) throws Exception {
    var path = new ClassPathResource(classpathCsv).getFile().getAbsolutePath();
    var props =
        new ApolloProperties(
            new ApolloProperties.Api("key"),
            null,
            path,
            "output/people.csv",
            25,
            10,
            0,
            List.of("c_suite"),
            List.of(),
            Map.of("company", "company", "domain", "domain"));
    return new UniqueDomainReader(props);
  }

  @Test
  void deduplicatesDomainsCaseInsensitively() throws Exception {
    var reader = readerFor("fixtures/contacts-with-domain.csv");
    reader.beforeStep(null);

    var refs = new ArrayList<>();
    Object item;
    while ((item = reader.read()) != null) {
      refs.add(item);
    }

    // fixture has factofrance.com twice, artzainak.com once, and one blank domain
    assertThat(refs).hasSize(2);
  }

  @Test
  void skipsBlankDomains() throws Exception {
    var reader = readerFor("fixtures/contacts-with-domain.csv");
    reader.beforeStep(null);

    var domains = new ArrayList<String>();
    Object item;
    while ((item = reader.read()) != null) {
      domains.add(((com.synapsedx.mailing.apollo.model.CompanyRef) item).domain());
    }

    assertThat(domains).doesNotContain("").doesNotContainNull();
  }

  @Test
  void preservesOriginalCaseOfDomain() throws Exception {
    var reader = readerFor("fixtures/contacts-with-domain.csv");
    reader.beforeStep(null);

    var first = (com.synapsedx.mailing.apollo.model.CompanyRef) reader.read();
    assertThat(first.domain()).isEqualTo("factofrance.com");
    assertThat(first.company()).isEqualTo("Factofrance");
  }

  @Test
  void failsFastOnMissingDomainHeader() throws Exception {
    var path =
        new ClassPathResource("fixtures/contacts-with-domain.csv").getFile().getAbsolutePath();
    // Build a reader pointing at a file that has no domain column
    // We test this by pointing at a non-existent file path — the real
    // missing-header case is covered by an inline fixture CSV below
    var tmpFile = java.nio.file.Files.createTempFile("no-domain", ".csv");
    java.nio.file.Files.writeString(tmpFile, "first_name,last_name,company\nJean,Dupont,Acme\n");

    var props =
        new ApolloProperties(
            new ApolloProperties.Api("key"),
            null,
            tmpFile.toString(),
            "output/people.csv",
            25,
            10,
            0,
            List.of("c_suite"),
            List.of(),
            Map.of("company", "company", "domain", "domain"));
    var reader = new UniqueDomainReader(props);
    reader.beforeStep(null);

    assertThatThrownBy(reader::read)
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("domain");
  }

  @Test
  void resetsBetweenStepExecutions() throws Exception {
    var reader = readerFor("fixtures/contacts-with-domain.csv");

    // first run
    reader.beforeStep(null);
    var first = new ArrayList<>();
    Object item;
    while ((item = reader.read()) != null) {
      first.add(item);
    }

    // second run after reset
    reader.beforeStep(null);
    var second = new ArrayList<>();
    while ((item = reader.read()) != null) {
      second.add(item);
    }

    assertThat(first).hasSize(second.size());
  }
}
