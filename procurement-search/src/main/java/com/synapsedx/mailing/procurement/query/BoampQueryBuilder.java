package com.synapsedx.mailing.procurement.query;

import com.synapsedx.mailing.procurement.model.ProcurementQuery;
import com.synapsedx.mailing.procurement.model.SearchFilter;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class BoampQueryBuilder {

  public record BoampQuery(String where, List<String> refineParams) {}

  /**
   * Returns a BoampQuery. If rawQuery is set on the ProcurementQuery, uses it as the where clause
   * (no refine params). Otherwise builds from filter.
   */
  public BoampQuery build(ProcurementQuery query) {
    if (query.rawQuery() != null && !query.rawQuery().isBlank()) {
      return new BoampQuery(query.rawQuery(), List.of());
    }
    return buildFromFilter(query.filter());
  }

  private BoampQuery buildFromFilter(SearchFilter filter) {
    var whereClauses = new ArrayList<String>();

    if (filter.keywords() != null) {
      for (var kw : filter.keywords()) {
        whereClauses.add("search(objet,\"" + kw + "\")");
      }
    }

    if (filter.publicationDateFrom() != null) {
      whereClauses.add("dateparution>=\"" + filter.publicationDateFrom() + "\"");
    }

    var where = String.join(" AND ", whereClauses);

    var refineParams = new ArrayList<String>();
    if (filter.departments() != null) {
      for (var dept : filter.departments()) {
        refineParams.add("code_departement:" + dept);
      }
    }

    return new BoampQuery(where, List.copyOf(refineParams));
  }
}
