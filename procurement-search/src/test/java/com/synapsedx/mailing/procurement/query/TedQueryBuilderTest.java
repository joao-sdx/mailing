package com.synapsedx.mailing.procurement.query;

import static org.assertj.core.api.Assertions.assertThat;

import com.synapsedx.mailing.procurement.model.ProcurementQuery;
import com.synapsedx.mailing.procurement.model.SearchFilter;
import com.synapsedx.mailing.procurement.model.Source;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class TedQueryBuilderTest {

  private final TedQueryBuilder builder = new TedQueryBuilder();

  @Test
  void buildsQueryFromKeywordAndCountry() {
    var filter = new SearchFilter(List.of("logiciel"), null, List.of("FRA"), null, false);
    var query = new ProcurementQuery(Source.TED, filter, null);

    assertThat(builder.build(query)).isEqualTo("FT ~ logiciel AND buyer-country=FRA");
  }

  @Test
  void buildsQueryWithMultipleCountries() {
    var filter = new SearchFilter(null, null, List.of("FRA", "LUX"), null, false);
    var query = new ProcurementQuery(Source.TED, filter, null);

    assertThat(builder.build(query)).isEqualTo("buyer-country IN (FRA LUX)");
  }

  @Test
  void buildsQueryWithDateFilter() {
    var filter = new SearchFilter(null, LocalDate.of(2026, 1, 1), null, null, false);
    var query = new ProcurementQuery(Source.TED, filter, null);

    assertThat(builder.build(query)).isEqualTo("publication-date>=20260101");
  }

  @Test
  void buildsQueryWithAllFilters() {
    var filter =
        new SearchFilter(
            List.of("logiciel", "ERP"),
            LocalDate.of(2026, 1, 1),
            List.of("FRA", "DEU"),
            null,
            false);
    var query = new ProcurementQuery(Source.TED, filter, null);

    assertThat(builder.build(query))
        .isEqualTo(
            "FT ~ logiciel AND FT ~ ERP AND buyer-country IN (FRA DEU) AND"
                + " publication-date>=20260101");
  }

  @Test
  void rawQueryPassthrough() {
    var filter = new SearchFilter(List.of("logiciel"), null, List.of("FRA"), null, false);
    var query = new ProcurementQuery(Source.TED, filter, "my-raw-query");

    assertThat(builder.build(query)).isEqualTo("my-raw-query");
  }

  @Test
  void emptyFilterProducesEmptyString() {
    var filter = new SearchFilter(null, null, null, null, false);
    var query = new ProcurementQuery(Source.TED, filter, null);

    assertThat(builder.build(query)).isEmpty();
  }
}
