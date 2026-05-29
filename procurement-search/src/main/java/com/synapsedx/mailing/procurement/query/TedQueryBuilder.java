package com.synapsedx.mailing.procurement.query;

import com.synapsedx.mailing.procurement.model.ProcurementQuery;
import com.synapsedx.mailing.procurement.model.SearchFilter;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import org.springframework.stereotype.Component;

@Component
public class TedQueryBuilder {

  private static final DateTimeFormatter TED_DATE = DateTimeFormatter.BASIC_ISO_DATE;

  /**
   * Returns the rawQuery verbatim if set, otherwise builds an Expert-Search query from the filter.
   */
  public String build(ProcurementQuery query) {
    if (query.rawQuery() != null && !query.rawQuery().isBlank()) {
      return query.rawQuery();
    }
    return buildFromFilter(query.filter());
  }

  private String buildFromFilter(SearchFilter filter) {
    var clauses = new ArrayList<String>();

    if (filter.keywords() != null) {
      for (var kw : filter.keywords()) {
        clauses.add("FT ~ " + kw);
      }
    }

    if (filter.countries() != null && !filter.countries().isEmpty()) {
      if (filter.countries().size() == 1) {
        clauses.add("buyer-country=" + filter.countries().getFirst());
      } else {
        clauses.add("buyer-country IN (" + String.join(" ", filter.countries()) + ")");
      }
    }

    if (filter.publicationDateFrom() != null) {
      clauses.add("publication-date>=" + filter.publicationDateFrom().format(TED_DATE));
    }

    return String.join(" AND ", clauses);
  }
}
