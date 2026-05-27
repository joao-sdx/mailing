package com.synapsedx.mailing.unitelegal2dataforseo.batch.processor;

import static org.assertj.core.api.Assertions.assertThat;

import com.synapsedx.mailing.unitelegal2dataforseo.model.InseeUniteLegale;
import org.junit.jupiter.api.Test;

class KeywordDedupProcessorTest {

  private final KeywordDedupProcessor processor = new KeywordDedupProcessor();

  @Test
  void dropsSigleWhenIncludedInDenomination() throws Exception {
    var row =
        new InseeUniteLegale(
            "054501754",
            "SET",
            "SET - HUILLIER - SOCIETE D'ENTREPOSAGE ET DE TRANSPORTS",
            "",
            "",
            "");

    var result = processor.process(row);

    assertThat(result).isNotNull();
    assertThat(result.siren()).isEqualTo("054501754");
    assertThat(result.keywords())
        .containsExactly("SET - HUILLIER - SOCIETE D'ENTREPOSAGE ET DE TRANSPORTS");
  }

  @Test
  void keepsSigleAndDenominationWhenDistinct() throws Exception {
    var row = new InseeUniteLegale("111111111", "EDF", "ELECTRICITE DE FRANCE", null, null, null);

    var result = processor.process(row);

    assertThat(result.keywords()).containsExactlyInAnyOrder("EDF", "ELECTRICITE DE FRANCE");
  }

  @Test
  void singleDenominationWhenSigleBlank() throws Exception {
    var row = new InseeUniteLegale("016750697", "", "ETS J VIRLY S A", "", "", "");

    var result = processor.process(row);

    assertThat(result.keywords()).containsExactly("ETS J VIRLY S A");
  }

  @Test
  void returnsNullWhenAllColumnsBlank() throws Exception {
    var row = new InseeUniteLegale("999999999", "", "", null, "  ", "\t");

    var result = processor.process(row);

    assertThat(result).isNull();
  }

  @Test
  void caseInsensitiveSubstringMatch() throws Exception {
    var row =
        new InseeUniteLegale("222222222", "edf", "ELECTRICITE DE FRANCE EDF", null, null, null);

    var result = processor.process(row);

    assertThat(result.keywords()).containsExactly("ELECTRICITE DE FRANCE EDF");
  }

  @Test
  void trimsWhitespace() throws Exception {
    var row = new InseeUniteLegale("333333333", "  ABC  ", "  XYZ COMPANY  ", null, null, null);

    var result = processor.process(row);

    assertThat(result.keywords()).containsExactlyInAnyOrder("ABC", "XYZ COMPANY");
  }

  @Test
  void deduplicatesAcrossUsuelleColumns() throws Exception {
    var row =
        new InseeUniteLegale(
            "444444444", "ACME", "ACME CORPORATION", "ACME", "ACME CORP", "GLOBAL HOLDINGS");

    var result = processor.process(row);

    // "ACME CORPORATION" kept first (longest), absorbs "ACME", "ACME CORP".
    // "GLOBAL HOLDINGS" kept (distinct).
    assertThat(result.keywords()).containsExactlyInAnyOrder("ACME CORPORATION", "GLOBAL HOLDINGS");
  }
}
