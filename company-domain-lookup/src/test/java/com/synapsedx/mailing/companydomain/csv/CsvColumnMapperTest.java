package com.synapsedx.mailing.companydomain.csv;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class CsvColumnMapperTest {

  @Test
  void resolvesConfiguredColumns() {
    var mapping = Map.of("company", "organizationName", "article_id", "ref");
    var headers = List.of("id", "organizationName", "ref", "extra");

    var indices = CsvColumnMapper.resolve(headers, mapping);

    assertThat(indices.get("company")).isEqualTo(1);
    assertThat(indices.get("article_id")).isEqualTo(2);
  }

  @Test
  void ignoresExtraColumnsInCsv() {
    var mapping = Map.of("company", "company");
    var headers = List.of("company", "extra1", "extra2");

    var indices = CsvColumnMapper.resolve(headers, mapping);

    assertThat(indices).hasSize(1);
    assertThat(indices.get("company")).isEqualTo(0);
  }

  @Test
  void throwsWhenConfiguredColumnMissingFromHeaders() {
    var mapping = Map.of("company", "organizationName");
    var headers = List.of("id", "article_id");

    assertThatThrownBy(() -> CsvColumnMapper.resolve(headers, mapping))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("organizationName")
        .hasMessageContaining("company");
  }

  @Test
  void throwsWhenMappingIsNull() {
    assertThatThrownBy(() -> CsvColumnMapper.resolve(List.of("company"), null))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("column-mapping");
  }

  @Test
  void throwsWhenMappingIsEmpty() {
    assertThatThrownBy(() -> CsvColumnMapper.resolve(List.of("company"), Map.of()))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("column-mapping");
  }
}
