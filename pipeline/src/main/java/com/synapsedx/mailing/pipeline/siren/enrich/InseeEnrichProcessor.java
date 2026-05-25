package com.synapsedx.mailing.pipeline.siren.enrich;

import com.synapsedx.mailing.pipeline.siren.base.CategorieEntreprise;
import com.synapsedx.mailing.pipeline.siren.base.CategorieJuridique;
import com.synapsedx.mailing.pipeline.siren.base.EtatAdministratif;
import com.synapsedx.mailing.pipeline.siren.base.InseeRecord;
import com.synapsedx.mailing.pipeline.siren.base.TrancheEffectif;
import com.synapsedx.mailing.pipeline.siren.enrich.model.enrich.CompanyRecord;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

/** Reformats a raw {@link InseeRecord} into a {@link CompanyRecord} with enum conversions. */
@Component
public class InseeEnrichProcessor implements ItemProcessor<InseeRecord, CompanyRecord> {

  @Override
  public CompanyRecord process(InseeRecord raw) {
    return new CompanyRecord(
        "FR-" + raw.siren(),
        raw.dateCreationUniteLegale(),
        raw.sigleUniteLegale(),
        raw.pseudonymeUniteLegale(),
        raw.identifiantAssociationUniteLegale(),
        TrancheEffectif.fromCode(raw.trancheEffectifsUniteLegale()),
        CategorieEntreprise.fromCode(raw.categorieEntreprise()),
        EtatAdministratif.fromCode(raw.etatAdministratifUniteLegale()),
        raw.nomUniteLegale(),
        raw.nomUsageUniteLegale(),
        raw.denominationUniteLegale(),
        raw.denominationUsuelle1UniteLegale(),
        raw.denominationUsuelle2UniteLegale(),
        raw.denominationUsuelle3UniteLegale(),
        CategorieJuridique.fromCode(raw.categorieJuridiqueUniteLegale()),
        raw.activitePrincipaleUniteLegale(),
        raw.activitePrincipaleNAF25UniteLegale(),
        null,
        null,
        null);
  }
}
