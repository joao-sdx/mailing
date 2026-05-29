package com.synapsedx.mailing.procurement.client;

import com.synapsedx.mailing.procurement.model.ProcurementQuery;
import com.synapsedx.mailing.procurement.model.Source;
import com.synapsedx.mailing.procurement.model.Tender;
import java.util.List;

/** Strategy interface for a procurement data source. One implementation per Source enum value. */
public interface TenderSource {

  Source source();

  List<Tender> search(ProcurementQuery query) throws Exception;
}
