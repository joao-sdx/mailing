package com.synapsedx.mailing.pipeline.siren.split;

import com.synapsedx.mailing.pipeline.siren.enrich.InseeAnnuairePort;
import com.synapsedx.mailing.pipeline.siren.enrich.model.annuaire.AnnuaireDirigeant;
import com.synapsedx.mailing.pipeline.siren.enrich.model.enrich.CompanyRecord;
import com.synapsedx.mailing.pipeline.siren.enrich.model.enrich.ContactRecord;
import com.synapsedx.mailing.pipeline.siren.enrich.model.enrich.ParentCorporationRecord;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

/** Calls Annuaire API and splits a {@link CompanyRecord} into company + contact records. */
@Slf4j
@Component
@RequiredArgsConstructor
public class InseeCompanyEnrichProcessor implements ItemProcessor<CompanyRecord, SplitResult> {

  private static final String FRENCH_RCS_PREFIX = "FR-";

  private final InseeAnnuairePort annuaireClient;

  @Override
  public SplitResult process(CompanyRecord company) {
    var rcs = company.rcs();
    if (!rcs.startsWith(FRENCH_RCS_PREFIX)) {
      throw new IllegalArgumentException("Unsupported RCS format (expected FR-): " + rcs);
    }
    var siren = rcs.substring(FRENCH_RCS_PREFIX.length());

    List<ContactRecord> contacts = new ArrayList<>();
    List<ParentCorporationRecord> parentCorps = new ArrayList<>();
    Integer nombreEtablissementsOuverts = null;
    Long ca = null;
    Long resultatNet = null;

    try {
      var annuaire = annuaireClient.findBySiren(siren);
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
        var dirigeants = e.dirigeants();
        if (dirigeants != null) {
          for (var d : dirigeants) {
            if ("personne morale".equals(d.typeDirigeant())) {
              parentCorps.add(toParentCorp(rcs, d));
            } else {
              contacts.add(toContact(rcs, d));
            }
          }
        }
      } else {
        log.warn("company_enrich_no_annuaire rcs={}", rcs);
      }
    } catch (Exception e) {
      log.warn("company_enrich_annuaire_failed rcs={} reason={}", rcs, e.toString());
    }

    var enriched =
        new CompanyRecord(
            rcs,
            company.dateCreationUniteLegale(),
            company.sigleUniteLegale(),
            company.pseudonymeUniteLegale(),
            company.identifiantAssociationUniteLegale(),
            company.trancheEffectifsUniteLegale(),
            company.categorieEntreprise(),
            company.etatAdministratifUniteLegale(),
            company.nomUniteLegale(),
            company.nomUsageUniteLegale(),
            company.denominationUniteLegale(),
            company.denominationUsuelle1UniteLegale(),
            company.denominationUsuelle2UniteLegale(),
            company.denominationUsuelle3UniteLegale(),
            company.categorieJuridiqueUniteLegale(),
            company.activitePrincipaleUniteLegale(),
            company.activitePrincipaleNAF25UniteLegale(),
            nombreEtablissementsOuverts,
            ca,
            resultatNet);

    return new SplitResult(enriched, contacts, parentCorps);
  }

  private static ContactRecord toContact(String rcs, AnnuaireDirigeant d) {
    return new ContactRecord(
        rcs,
        d.nom(),
        d.prenoms(),
        d.anneeDeNaissance(),
        d.dateDeNaissance(),
        d.qualite(),
        d.nationalite(),
        d.typeDirigeant());
  }

  private static ParentCorporationRecord toParentCorp(String rcs, AnnuaireDirigeant d) {
    var parentRcs = d.siren() != null ? FRENCH_RCS_PREFIX + d.siren() : null;
    return new ParentCorporationRecord(rcs, d.qualite(), parentRcs);
  }
}
