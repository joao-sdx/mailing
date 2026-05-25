package com.synapsedx.mailing.pipeline.siren.enrich.model.annuaire;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

/**
 * Unité légale retournée par l'API recherche-entreprises.api.gouv.fr.
 *
 * @param siren Identifiant unique de l'unité légale (9 chiffres).
 * @param nomComplet Nom complet reconstitué (dénomination ou nom + prénom).
 * @param nomRaisonSociale Dénomination sociale ou nom de naissance.
 * @param sigle Sigle officiel.
 * @param nombreEtablissements Nombre total d'établissements (ouverts + fermés).
 * @param nombreEtablissementsOuverts Nombre d'établissements actuellement actifs.
 * @param siege Établissement siège social.
 * @param activitePrincipale Code APE NAFRev2 de l'unité légale (ex : {@code 64.91Z}).
 * @param activitePrincipaleNaf25 Code APE NAF 2025 (ex : {@code 64.91Y}).
 * @param categorieEntreprise Catégorie selon décret 2008-1354 : {@code PME}, {@code ETI}, {@code
 *     GE}.
 * @param caractereEmployeur Caractère employeur au niveau de l'unité légale.
 * @param anneeCategorieEntreprise Année de validité de la catégorie d'entreprise.
 * @param dateCreation Date de création ({@code YYYY-MM-DD}).
 * @param dateFermeture Date de cessation ({@code YYYY-MM-DD}), null si active.
 * @param dateMiseAJour Date de dernière mise à jour dans l'API.
 * @param dateMiseAJourInsee Date de dernière mise à jour INSEE (ISO 8601).
 * @param dateMiseAJourRne Date de dernière mise à jour RNE (ISO 8601).
 * @param dirigeants Liste des dirigeants et mandataires sociaux.
 * @param etatAdministratif État administratif : {@code A} = active, {@code C} = cessée.
 * @param natureJuridique Code catégorie juridique sur 4 chiffres (ex : {@code 5710} = SAS).
 * @param sectionActivitePrincipale Section NAF (lettre, ex : {@code K} = Finance et assurance).
 * @param trancheEffectifSalarie Code tranche d'effectifs salariés (même codification que SIRENE).
 * @param anneeTrancheEffectifSalarie Année de validité de la tranche d'effectifs.
 * @param statutDiffusion Statut de diffusion : {@code O} = diffusible, {@code N} = non diffusible.
 * @param finances Données financières annuelles indexées par année (ex : {@code "2024"}).
 * @param complements Labels qualité et informations complémentaires.
 */
public record AnnuaireEntreprise(
    @JsonProperty("siren") String siren,
    @JsonProperty("nom_complet") String nomComplet,
    @JsonProperty("nom_raison_sociale") String nomRaisonSociale,
    @JsonProperty("sigle") String sigle,
    @JsonProperty("nombre_etablissements") Integer nombreEtablissements,
    @JsonProperty("nombre_etablissements_ouverts") Integer nombreEtablissementsOuverts,
    @JsonProperty("siege") AnnuaireSiege siege,
    @JsonProperty("activite_principale") String activitePrincipale,
    @JsonProperty("activite_principale_naf25") String activitePrincipaleNaf25,
    @JsonProperty("categorie_entreprise") String categorieEntreprise,
    @JsonProperty("caractere_employeur") String caractereEmployeur,
    @JsonProperty("annee_categorie_entreprise") String anneeCategorieEntreprise,
    @JsonProperty("date_creation") String dateCreation,
    @JsonProperty("date_fermeture") String dateFermeture,
    @JsonProperty("date_mise_a_jour") String dateMiseAJour,
    @JsonProperty("date_mise_a_jour_insee") String dateMiseAJourInsee,
    @JsonProperty("date_mise_a_jour_rne") String dateMiseAJourRne,
    @JsonProperty("dirigeants") List<AnnuaireDirigeant> dirigeants,
    @JsonProperty("etat_administratif") String etatAdministratif,
    @JsonProperty("nature_juridique") String natureJuridique,
    @JsonProperty("section_activite_principale") String sectionActivitePrincipale,
    @JsonProperty("tranche_effectif_salarie") String trancheEffectifSalarie,
    @JsonProperty("annee_tranche_effectif_salarie") String anneeTrancheEffectifSalarie,
    @JsonProperty("statut_diffusion") String statutDiffusion,
    @JsonProperty("finances") Map<String, AnnuaireFinances> finances,
    @JsonProperty("complements") AnnuaireComplements complements) {}
