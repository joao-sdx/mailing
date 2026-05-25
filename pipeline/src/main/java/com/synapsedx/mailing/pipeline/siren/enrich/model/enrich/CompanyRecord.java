package com.synapsedx.mailing.pipeline.siren.enrich.model.enrich;

import com.synapsedx.mailing.pipeline.siren.base.CategorieEntreprise;
import com.synapsedx.mailing.pipeline.siren.base.CategorieJuridique;
import com.synapsedx.mailing.pipeline.siren.base.EtatAdministratif;
import com.synapsedx.mailing.pipeline.siren.base.TrancheEffectif;

public record CompanyRecord(
    // For french companies rcs is 'FR-(siren)"
    String rcs,
    // from InseeRecord
    String dateCreationUniteLegale,
    String sigleUniteLegale,
    String pseudonymeUniteLegale,
    String identifiantAssociationUniteLegale,
    TrancheEffectif trancheEffectifsUniteLegale,
    CategorieEntreprise categorieEntreprise,
    EtatAdministratif etatAdministratifUniteLegale,
    String nomUniteLegale,
    String nomUsageUniteLegale,
    String denominationUniteLegale,
    String denominationUsuelle1UniteLegale,
    String denominationUsuelle2UniteLegale,
    String denominationUsuelle3UniteLegale,
    CategorieJuridique categorieJuridiqueUniteLegale,
    String activitePrincipaleUniteLegale,
    String activitePrincipaleNAF25UniteLegale,
    // from AnnuaireResponse
    Integer nombreEtablissementsOuverts,
    Long ca,
    Long resultatNet) {}
