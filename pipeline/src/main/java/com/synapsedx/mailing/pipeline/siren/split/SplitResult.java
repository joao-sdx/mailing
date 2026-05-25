package com.synapsedx.mailing.pipeline.siren.split;

import com.synapsedx.mailing.pipeline.siren.enrich.model.enrich.CompanyRecord;
import com.synapsedx.mailing.pipeline.siren.enrich.model.enrich.ContactRecord;
import com.synapsedx.mailing.pipeline.siren.enrich.model.enrich.ParentCorporationRecord;
import java.util.List;

record SplitResult(
    CompanyRecord company,
    List<ContactRecord> contacts,
    List<ParentCorporationRecord> parentCorps) {}
