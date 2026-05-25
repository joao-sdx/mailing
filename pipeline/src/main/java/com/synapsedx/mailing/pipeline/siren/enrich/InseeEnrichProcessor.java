package com.synapsedx.mailing.pipeline.siren.enrich;

import com.synapsedx.mailing.pipeline.siren.base.CategorieEntreprise;
import com.synapsedx.mailing.pipeline.siren.base.CategorieJuridique;
import com.synapsedx.mailing.pipeline.siren.base.EtatAdministratif;
import com.synapsedx.mailing.pipeline.siren.base.InseeRecord;
import com.synapsedx.mailing.pipeline.siren.base.TrancheEffectif;
import com.synapsedx.mailing.pipeline.siren.enrich.model.enrich.CompanyRecord;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

/** Maps {@link InseeRecord} to {@link CompanyRecord} and enriches via Annuaire API. */
@Slf4j
@Component
@RequiredArgsConstructor
public class InseeEnrichProcessor implements ItemProcessor<InseeRecord, CompanyRecord> {

  private final InseeAnnuairePort annuaireClient;

  @Override
  public CompanyRecord process(InseeRecord raw) {
    var annuaire = annuaireClient.findBySiren(raw.siren());

    Integer nombreEtablissementsOuverts = null;
    Long ca = null;
    Long resultatNet = null;

    if (annuaire.isPresent()) {
      var e = annuaire.get();
      nombreEtablissementsOuverts = e.nombreEtablissementsOuverts();
      if (e.finances() != null && !e.finances().isEmpty()) {
        var latestYear = e.finances().keySet().stream().max(String::compareTo).orElse(null);
        if (latestYear != null) {
          var finances = e.finances().get(latestYear);
          ca = finances.ca();
          resultatNet = finances.resultatNet();
        }
      }
    } else {
      log.debug("insee_enrich_no_annuaire siren={}", raw.siren());
    }

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
        nombreEtablissementsOuverts,
        ca,
        resultatNet);
  }
}
