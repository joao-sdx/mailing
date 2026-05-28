package com.synapsedx.mailing.companydomain.csv;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CsvLineParserTest {

  @Test
  void parsesPlainFields() {
    assertThat(CsvLineParser.parse("a,b,c")).containsExactly("a", "b", "c");
  }

  @Test
  void parsesQuotedFieldWithComma() {
    assertThat(CsvLineParser.parse("a,\"b,c\",d")).containsExactly("a", "b,c", "d");
  }

  @Test
  void parsesDoubledQuoteInsideQuotedField() {
    assertThat(CsvLineParser.parse("a,\"b\"\"c\",d")).containsExactly("a", "b\"c", "d");
  }

  @Test
  void parsesEmptyFields() {
    assertThat(CsvLineParser.parse("a,,c")).containsExactly("a", "", "c");
  }

  @Test
  void parsesTrailingEmptyField() {
    assertThat(CsvLineParser.parse("a,b,")).containsExactly("a", "b", "");
  }

  @Test
  void parsesUnicode() {
    assertThat(CsvLineParser.parse("Beñat,Cazanave,ARTZAINAK,result-10-01.md"))
        .containsExactly("Beñat", "Cazanave", "ARTZAINAK", "result-10-01.md");
  }
}
