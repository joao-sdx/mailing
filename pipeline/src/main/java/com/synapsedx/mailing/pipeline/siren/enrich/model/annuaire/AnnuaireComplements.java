package com.synapsedx.mailing.pipeline.siren.enrich.model.annuaire;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Informations complémentaires et labels qualité d'une unité légale.
 *
 * @param collectiviteTerritoriale Identifiant si l'unité est une collectivité territoriale.
 * @param conventionCollectiveRenseignee {@code true} si une convention collective est renseignée.
 * @param listeIdcc Liste des identifiants de conventions collectives (IDCC).
 * @param listeFiness Identifiants FINESS (établissements de santé/social) de la personne morale.
 * @param egaproRenseignee {@code true} si l'index Egapro (égalité professionnelle) est renseigné.
 * @param estAchatsResponsables {@code true} si labellisée achats responsables.
 * @param estAlimConfiance {@code true} si référencée Alim'confiance (restauration).
 * @param estAssociation {@code true} si l'unité est une association (RNA).
 * @param estAvocat {@code true} si l'unité est un cabinet d'avocats.
 * @param estBio {@code true} si l'unité est certifiée agriculture biologique.
 * @param estEntrepreneurIndividuel {@code true} si entrepreneur individuel.
 * @param estEntrepreneurSpectacle {@code true} si licenciée entrepreneur de spectacle.
 * @param estEss {@code true} si appartient à l'Économie Sociale et Solidaire.
 * @param estFiness {@code true} si référencée dans le répertoire FINESS.
 * @param estOrganismeFormation {@code true} si déclarée organisme de formation (Qualiopi ou non).
 * @param estQualiopi {@code true} si certifiée Qualiopi (qualité formation).
 * @param listeIdOrganismeFormation Identifiants du Datadock / Qualiopi.
 * @param estRge {@code true} si titulaire d'une mention RGE (Reconnu Garant de l'Environnement).
 * @param estSiae {@code true} si structure d'insertion par l'activité économique.
 * @param estSocieteMission {@code true} si société à mission (loi PACTE).
 * @param estUai {@code true} si référencée dans le répertoire UAI (établissement d'enseignement).
 * @param estPatrimoineVivant {@code true} si labellisée Entreprise du Patrimoine Vivant.
 * @param bilanGesRenseigne {@code true} si un bilan GES (gaz à effet de serre) est renseigné.
 * @param identifiantAssociation Numéro RNA pour les associations.
 * @param statutEntrepreneurSpectacle Statut de la licence entrepreneur de spectacle.
 * @param typeSiae Type de SIAE (ex : EI, AI, ETTI, ACI).
 * @param aAideMinimis {@code true} si bénéficiaire d'une aide de minimis.
 * @param aAideAdeme {@code true} si bénéficiaire d'une aide ADEME.
 * @param estAdministration {@code true} si entité publique ou administrative.
 * @param estServicePublic {@code true} si service public.
 * @param estL1003 {@code true} si soumise à l'article L100-3 du Code de commerce.
 */
public record AnnuaireComplements(
    @JsonProperty("collectivite_territoriale") String collectiviteTerritoriale,
    @JsonProperty("convention_collective_renseignee") Boolean conventionCollectiveRenseignee,
    @JsonProperty("liste_idcc") List<String> listeIdcc,
    @JsonProperty("liste_finess_juridique") List<String> listeFiness,
    @JsonProperty("egapro_renseignee") Boolean egaproRenseignee,
    @JsonProperty("est_achats_responsables") Boolean estAchatsResponsables,
    @JsonProperty("est_alim_confiance") Boolean estAlimConfiance,
    @JsonProperty("est_association") Boolean estAssociation,
    @JsonProperty("est_avocat") Boolean estAvocat,
    @JsonProperty("est_bio") Boolean estBio,
    @JsonProperty("est_entrepreneur_individuel") Boolean estEntrepreneurIndividuel,
    @JsonProperty("est_entrepreneur_spectacle") Boolean estEntrepreneurSpectacle,
    @JsonProperty("est_ess") Boolean estEss,
    @JsonProperty("est_finess") Boolean estFiness,
    @JsonProperty("est_organisme_formation") Boolean estOrganismeFormation,
    @JsonProperty("est_qualiopi") Boolean estQualiopi,
    @JsonProperty("liste_id_organisme_formation") List<String> listeIdOrganismeFormation,
    @JsonProperty("est_rge") Boolean estRge,
    @JsonProperty("est_siae") Boolean estSiae,
    @JsonProperty("est_societe_mission") Boolean estSocieteMission,
    @JsonProperty("est_uai") Boolean estUai,
    @JsonProperty("est_patrimoine_vivant") Boolean estPatrimoineVivant,
    @JsonProperty("bilan_ges_renseigne") Boolean bilanGesRenseigne,
    @JsonProperty("identifiant_association") String identifiantAssociation,
    @JsonProperty("statut_entrepreneur_spectacle") String statutEntrepreneurSpectacle,
    @JsonProperty("type_siae") String typeSiae,
    @JsonProperty("a_aide_minimis") Boolean aAideMinimis,
    @JsonProperty("a_aide_ademe") Boolean aAideAdeme,
    @JsonProperty("est_administration") Boolean estAdministration,
    @JsonProperty("est_service_public") Boolean estServicePublic,
    @JsonProperty("est_l100_3") Boolean estL1003) {}
