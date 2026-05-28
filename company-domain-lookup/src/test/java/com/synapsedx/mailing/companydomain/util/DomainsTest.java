package com.synapsedx.mailing.companydomain.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class DomainsTest {

  @Test
  void extractsHostFromFullUrl() {
    assertThat(Domains.extractHost("https://www.factofrance.com/contact"))
        .isEqualTo("factofrance.com");
  }

  @Test
  void stripsLeadingWww() {
    assertThat(Domains.extractHost("https://www.example.org")).isEqualTo("example.org");
  }

  @Test
  void acceptsBareHost() {
    assertThat(Domains.extractHost("factofrance.com")).isEqualTo("factofrance.com");
  }

  @Test
  void lowercasesHost() {
    assertThat(Domains.extractHost("https://Example.COM/")).isEqualTo("example.com");
  }

  @Test
  void returnsEmptyForBlankInput() {
    assertThat(Domains.extractHost("")).isEqualTo("");
    assertThat(Domains.extractHost(null)).isEqualTo("");
  }

  @Test
  void returnsEmptyForUnparseableInput() {
    assertThat(Domains.extractHost("not a url at all")).isEqualTo("");
  }
}
