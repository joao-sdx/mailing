package com.synapsedx.mailing.pipeline.siren.base;

/**
 * Unité légale issue du répertoire 01-siren (fichier <a
 * href="https://files.data.gouv.fr/insee-sirene/StockUniteLegale_utf8.zip">StockUniteLegale</a>).
 *
 * @param siren Identifiant unique de l'unité légale (9 chiffres).
 * @param statutDiffusionUniteLegale Statut de diffusion : {@code O} = diffusible, {@code N} = non
 *     diffusible, {@code P} = partiellement non diffusible.
 * @param unitePurgeeUniteLegale {@code true} si l'unité légale a été purgée du répertoire (données
 *     personnelles supprimées).
 * @param dateCreationUniteLegale Date de création de l'unité légale au format {@code YYYY-MM-DD}.
 * @param sigleUniteLegale Sigle officiel de l'unité légale.
 * @param sexeUniteLegale Sexe de la personne physique : {@code M} = masculin, {@code F} = féminin.
 *     Null pour les personnes morales.
 * @param prenom1UniteLegale Premier prénom déclaré à l'état civil pour une personne physique.
 * @param prenom2UniteLegale Deuxième prénom déclaré à l'état civil.
 * @param prenom3UniteLegale Troisième prénom déclaré à l'état civil.
 * @param prenom4UniteLegale Quatrième prénom déclaré à l'état civil.
 * @param prenomUsuelUniteLegale Prénom usuel de la personne physique (peut différer du premier
 *     prénom).
 * @param pseudonymeUniteLegale Pseudonyme de la personne physique.
 * @param identifiantAssociationUniteLegale Numéro RNA (Répertoire National des Associations) pour
 *     les unités légales de type association.
 * @param trancheEffectifsUniteLegale Tranche d'effectifs salariés : {@code NN} = non renseigné,
 *     {@code 00} = 0, {@code 01} = 1-2, {@code 02} = 3-5, {@code 03} = 6-9, {@code 11} = 10-19,
 *     {@code 12} = 20-49, {@code 21} = 50-99, {@code 22} = 100-199, {@code 31} = 200-249, {@code
 *     32} = 250-499, {@code 41} = 500-999, {@code 42} = 1000-1999, {@code 51} = 2000-4999, {@code
 *     52} = 5000-9999, {@code 53} = 10000+.
 * @param anneeEffectifsUniteLegale Année de validité de la tranche d'effectifs.
 * @param dateDernierTraitementUniteLegale Date et heure du dernier traitement dans le répertoire
 *     SIRENE (format ISO 8601).
 * @param nombrePeriodesUniteLegale Nombre de périodes dans l'historique de l'unité légale.
 * @param categorieEntreprise Catégorie de l'entreprise selon le décret 2008-1354 : {@code PME},
 *     {@code ETI} (Entreprise de Taille Intermédiaire), {@code GE} (Grande Entreprise).
 * @param anneeCategorieEntreprise Année de validité de la catégorie d'entreprise.
 * @param dateDebut Date de début de la période en cours pour les variables historisées (format
 *     {@code YYYY-MM-DD}).
 * @param etatAdministratifUniteLegale État administratif de l'unité légale : {@code A} = active,
 *     {@code C} = cessée.
 * @param nomUniteLegale Nom de naissance de la personne physique. Null pour les personnes morales.
 * @param nomUsageUniteLegale Nom d'usage (peut différer du nom de naissance) pour une personne
 *     physique.
 * @param denominationUniteLegale Dénomination sociale (raison sociale) de la personne morale.
 * @param denominationUsuelle1UniteLegale Première dénomination usuelle (nom commercial).
 * @param denominationUsuelle2UniteLegale Deuxième dénomination usuelle.
 * @param denominationUsuelle3UniteLegale Troisième dénomination usuelle.
 * @param categorieJuridiqueUniteLegale Code de la catégorie juridique sur 4 chiffres selon la
 *     nomenclature INSEE (ex : {@code 5710} = SAS, {@code 5499} = SARL, {@code 1000} = personne
 *     physique).
 * @param activitePrincipaleUniteLegale Code APE (Activité Principale Exercée) selon la nomenclature
 *     NAFRev2 (ex : {@code 64.91Z}).
 * @param nomenclatureActivitePrincipaleUniteLegale Nomenclature utilisée pour l'activité principale
 *     : {@code NAFRev2} ou {@code NAFRev1}.
 * @param nicSiegeUniteLegale NIC (Numéro Interne de Classement) du siège social sur 5 chiffres.
 *     Concaténé au SIREN forme le SIRET du siège.
 * @param economieSocialeSolidaireUniteLegale Appartenance au secteur de l'ESS : {@code O} = oui,
 *     {@code N} = non.
 * @param societeMissionUniteLegale Qualité de société à mission (loi PACTE) : {@code O} = oui,
 *     {@code N} = non.
 * @param caractereEmployeurUniteLegale Caractère employeur : {@code O} = emploie des salariés,
 *     {@code N} = non employeur.
 * @param activitePrincipaleNAF25UniteLegale Code APE selon la nouvelle nomenclature NAF 2025 (ex :
 *     {@code 64.91Y}).
 */
public record InseeRecord(
    String siren,
    String statutDiffusionUniteLegale,
    String unitePurgeeUniteLegale,
    String dateCreationUniteLegale,
    String sigleUniteLegale,
    String sexeUniteLegale,
    String prenom1UniteLegale,
    String prenom2UniteLegale,
    String prenom3UniteLegale,
    String prenom4UniteLegale,
    String prenomUsuelUniteLegale,
    String pseudonymeUniteLegale,
    String identifiantAssociationUniteLegale,
    String trancheEffectifsUniteLegale,
    String anneeEffectifsUniteLegale,
    String dateDernierTraitementUniteLegale,
    String nombrePeriodesUniteLegale,
    String categorieEntreprise,
    String anneeCategorieEntreprise,
    String dateDebut,
    String etatAdministratifUniteLegale,
    String nomUniteLegale,
    String nomUsageUniteLegale,
    String denominationUniteLegale,
    String denominationUsuelle1UniteLegale,
    String denominationUsuelle2UniteLegale,
    String denominationUsuelle3UniteLegale,
    String categorieJuridiqueUniteLegale,
    String activitePrincipaleUniteLegale,
    String nomenclatureActivitePrincipaleUniteLegale,
    String nicSiegeUniteLegale,
    String economieSocialeSolidaireUniteLegale,
    String societeMissionUniteLegale,
    String caractereEmployeurUniteLegale,
    String activitePrincipaleNAF25UniteLegale) {}
