package com.synapsedx.mailing.procurement.query;

import static org.assertj.core.api.Assertions.assertThat;

import com.synapsedx.mailing.procurement.model.ProcurementQuery;
import com.synapsedx.mailing.procurement.model.SearchFilter;
import com.synapsedx.mailing.procurement.model.Source;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class BoampQueryBuilderTest {

  private final BoampQueryBuilder builder = new BoampQueryBuilder();

  @Test
  void buildsWhereFromKeyword() {
    var filter = new SearchFilter(List.of("logiciel"), null, null, null, false);
    var query = new ProcurementQuery(Source.BOAMP, filter, null);

    var result = builder.build(query);

    assertThat(result.where()).isEqualTo("search(objet,\"logiciel\")");
    assertThat(result.refineParams()).isEmpty();
  }

  @Test
  void buildsWhereWithDateFilter() {
    var filter = new SearchFilter(List.of("logiciel"), LocalDate.of(2026, 1, 1), null, null, false);
    var query = new ProcurementQuery(Source.BOAMP, filter, null);

    var result = builder.build(query);

    assertThat(result.where())
        .isEqualTo("search(objet,\"logiciel\") AND dateparution>=\"2026-01-01\"");
    assertThat(result.refineParams()).isEmpty();
  }

  @Test
  void buildsRefineParamsFromDepartments() {
    var filter = new SearchFilter(null, null, null, List.of("75", "92"), false);
    var query = new ProcurementQuery(Source.BOAMP, filter, null);

    var result = builder.build(query);

    assertThat(result.where()).isEmpty();
    assertThat(result.refineParams()).containsExactly("code_departement:75", "code_departement:92");
  }

  @Test
  void rawQueryPassthrough() {
    var filter = new SearchFilter(List.of("logiciel"), null, null, null, false);
    var query = new ProcurementQuery(Source.BOAMP, filter, "my-raw-where");

    var result = builder.build(query);

    assertThat(result.where()).isEqualTo("my-raw-where");
    assertThat(result.refineParams()).isEmpty();
  }

  @Test
  void emptyFilterProducesEmptyResults() {
    var filter = new SearchFilter(null, null, null, null, false);
    var query = new ProcurementQuery(Source.BOAMP, filter, null);

    var result = builder.build(query);

    assertThat(result.where()).isEmpty();
    assertThat(result.refineParams()).isEmpty();
  }
}
