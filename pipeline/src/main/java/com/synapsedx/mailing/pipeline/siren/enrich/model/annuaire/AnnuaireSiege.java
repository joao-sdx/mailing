package com.synapsedx.mailing.pipeline.siren.enrich.annuaire;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Établissement siège social d'une unité légale.
 *
 * @param siret SIRET du siège (SIREN + NIC, 14 chiffres).
 * @param activitePrincipale Code APE NAFRev2 du siège (ex : {@code 64.91Z}).
 * @param activitePrincipaleNaf25 Code APE NAF 2025 du siège (ex : {@code 64.91Y}).
 * @param activitePrincipaleRegistreMetier Activité principale au Registre des Métiers (artisans).
 * @param adresse Adresse postale complète du siège.
 * @param complementAdresse Complément d'adresse.
 * @param numeroVoie Numéro dans la voie.
 * @param dernierNumeroVoie Dernier numéro de voie (pour les intervalles).
 * @param indiceRepetition Indice de répétition (bis, ter…).
 * @param typeVoie Type de voie (RUE, AV, BD…).
 * @param libelleVoie Libellé de la voie.
 * @param codePostal Code postal (5 chiffres).
 * @param libelleCommune Libellé de la commune.
 * @param commune Code INSEE de la commune (5 chiffres).
 * @param cedex Code CEDEX.
 * @param libelleCedex Libellé CEDEX.
 * @param distributionSpeciale Mention de distribution spéciale (BP, TSA…).
 * @param codePaysEtranger Code pays pour les adresses étrangères.
 * @param libelleCommuneEtranger Libellé de la commune étrangère.
 * @param libellePaysEtranger Libellé du pays étranger.
 * @param departement Code département (ex : {@code 75}, {@code 2A}).
 * @param region Code région INSEE (ex : {@code 11} = Île-de-France).
 * @param epci Code de l'EPCI (établissement public de coopération intercommunale).
 * @param coordonnees Coordonnées géographiques au format {@code latitude,longitude}.
 * @param latitude Latitude WGS84.
 * @param longitude Longitude WGS84.
 * @param geoAdresse Adresse géocodée normalisée.
 * @param geoId Identifiant BAN de l'adresse géocodée.
 * @param nomCommercial Nom commercial de l'établissement siège.
 * @param listeEnseignes Liste des enseignes commerciales.
 * @param dateCreation Date de création du siège ({@code YYYY-MM-DD}).
 * @param dateDebutActivite Date de début de l'activité en cours ({@code YYYY-MM-DD}).
 * @param dateFermeture Date de fermeture du siège ({@code YYYY-MM-DD}), null si actif.
 * @param dateMiseAJour Date de dernière mise à jour dans l'API.
 * @param dateMiseAJourInsee Date de dernière mise à jour INSEE (format ISO 8601).
 * @param etatAdministratif État administratif : {@code A} = actif, {@code F} = fermé.
 * @param estSiege {@code true} si cet établissement est le siège de l'unité légale.
 * @param caractereEmployeur Caractère employeur : {@code O} = employeur, {@code N} = non.
 * @param trancheEffectifSalarie Code tranche d'effectifs du siège (même codification qu'unité
 *     légale).
 * @param anneeTrancheEffectifSalarie Année de validité de la tranche d'effectifs.
 * @param statutDiffusionEtablissement Statut de diffusion de l'établissement.
 * @param listeIdcc Identifiants des conventions collectives applicables (IDCC).
 * @param listeFiness Identifiants FINESS de l'établissement.
 * @param listeIdBio Identifiants de certification bio.
 * @param listeRge Mentions RGE (Reconnu Garant de l'Environnement).
 * @param listeUai Identifiants UAI (établissement d'enseignement).
 * @param listeIdOrganismeFormation Identifiants organisme de formation.
 */
public record AnnuaireSiege(
    @JsonProperty("siret") String siret,
    @JsonProperty("activite_principale") String activitePrincipale,
    @JsonProperty("activite_principale_naf25") String activitePrincipaleNaf25,
    @JsonProperty("activite_principale_registre_metier") String activitePrincipaleRegistreMetier,
    @JsonProperty("adresse") String adresse,
    @JsonProperty("complement_adresse") String complementAdresse,
    @JsonProperty("numero_voie") String numeroVoie,
    @JsonProperty("dernier_numero_voie") String dernierNumeroVoie,
    @JsonProperty("indice_repetition") String indiceRepetition,
    @JsonProperty("type_voie") String typeVoie,
    @JsonProperty("libelle_voie") String libelleVoie,
    @JsonProperty("code_postal") String codePostal,
    @JsonProperty("libelle_commune") String libelleCommune,
    @JsonProperty("commune") String commune,
    @JsonProperty("cedex") String cedex,
    @JsonProperty("libelle_cedex") String libelleCedex,
    @JsonProperty("distribution_speciale") String distributionSpeciale,
    @JsonProperty("code_pays_etranger") String codePaysEtranger,
    @JsonProperty("libelle_commune_etranger") String libelleCommuneEtranger,
    @JsonProperty("libelle_pays_etranger") String libellePaysEtranger,
    @JsonProperty("departement") String departement,
    @JsonProperty("region") String region,
    @JsonProperty("epci") String epci,
    @JsonProperty("coordonnees") String coordonnees,
    @JsonProperty("latitude") String latitude,
    @JsonProperty("longitude") String longitude,
    @JsonProperty("geo_adresse") String geoAdresse,
    @JsonProperty("geo_id") String geoId,
    @JsonProperty("nom_commercial") String nomCommercial,
    @JsonProperty("liste_enseignes") List<String> listeEnseignes,
    @JsonProperty("date_creation") String dateCreation,
    @JsonProperty("date_debut_activite") String dateDebutActivite,
    @JsonProperty("date_fermeture") String dateFermeture,
    @JsonProperty("date_mise_a_jour") String dateMiseAJour,
    @JsonProperty("date_mise_a_jour_insee") String dateMiseAJourInsee,
    @JsonProperty("etat_administratif") String etatAdministratif,
    @JsonProperty("est_siege") Boolean estSiege,
    @JsonProperty("caractere_employeur") String caractereEmployeur,
    @JsonProperty("tranche_effectif_salarie") String trancheEffectifSalarie,
    @JsonProperty("annee_tranche_effectif_salarie") String anneeTrancheEffectifSalarie,
    @JsonProperty("statut_diffusion_etablissement") String statutDiffusionEtablissement,
    @JsonProperty("liste_idcc") List<String> listeIdcc,
    @JsonProperty("liste_finess") List<String> listeFiness,
    @JsonProperty("liste_id_bio") List<String> listeIdBio,
    @JsonProperty("liste_rge") List<String> listeRge,
    @JsonProperty("liste_uai") List<String> listeUai,
    @JsonProperty("liste_id_organisme_formation") List<String> listeIdOrganismeFormation) {}
