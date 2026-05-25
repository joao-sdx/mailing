package com.synapsedx.mailing.pipeline.siren.base;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Catégorie juridique des unités légales selon la nomenclature INSEE (CJ 2020). Correspond au champ
 * {@code categorieJuridiqueUniteLegale} du répertoire SIRENE.
 */
public enum CategorieJuridique {

  // -------------------------------------------------------------------------
  // 1xxx — Personne physique
  // -------------------------------------------------------------------------
  ENTREPRENEUR_INDIVIDUEL("1000", "Entrepreneur individuel"),

  // -------------------------------------------------------------------------
  // 2xxx — Groupements de droit privé sans personnalité morale
  // -------------------------------------------------------------------------
  INDIVISION_PP("2110", "Indivision entre personnes physiques"),
  INDIVISION_PM("2120", "Indivision avec personne morale"),
  SOCIETE_FAIT_PP("2210", "Société créée de fait entre personnes physiques"),
  SOCIETE_FAIT_PM("2220", "Société créée de fait avec personne morale"),
  PARTICIPATION_PP("2310", "Société en participation entre personnes physiques"),
  PARTICIPATION_PM("2320", "Société en participation avec personne morale"),
  PARTICIPATION_LIBERALE("2385", "Société en participation de professions libérales"),
  FIDUCIE("2400", "Fiducie"),
  PAROISSE_HORS_CONCORDAT("2700", "Paroisse hors zone concordataire"),
  AUTRE_GROUPEMENT_SANS_PM("2900", "Autre groupement de droit privé sans personnalité morale"),

  // -------------------------------------------------------------------------
  // 3xxx — Personnes morales de droit étranger
  // -------------------------------------------------------------------------
  AGENCE_ETRANGER_RCS(
      "3110",
      "Représentation ou agence commerciale d'état ou organisme étranger immatriculé au RCS"),
  SOCIETE_ETRANGERE_RCS("3120", "Société étrangère immatriculée au RCS"),
  ORGANISATION_INTERNATIONALE("3205", "Organisation internationale"),

  // -------------------------------------------------------------------------
  // 4xxx — Établissements publics soumis au droit commercial
  // -------------------------------------------------------------------------
  EPIC_NATIONAL(
      "4110", "Établissement public national à caractère industriel et commercial (EPIC)"),
  EPIC_LOCAL("4120", "Établissement public local à caractère industriel et commercial"),

  // -------------------------------------------------------------------------
  // 5xxx — Sociétés commerciales
  // -------------------------------------------------------------------------
  CAUTION_MUTUELLE("5191", "Société de caution mutuelle"),
  SNC("5202", "Société en nom collectif (SNC)"),
  COMMANDITE_SIMPLE("5306", "Société en commandite simple"),
  COMMANDITE_ACTIONS("5308", "Société en commandite par actions"),
  EURL("5370", "SARL à associé unique (EURL)"),
  SARL("5399", "Société à responsabilité limitée (SARL)"),
  SA_DIRECTOIRE("5410", "Société anonyme à directoire"),
  SA_CONSEIL("5415", "Société anonyme à conseil d'administration"),
  SA_SMIA("5422", "Société anonyme mixte d'intérêt agricole (SMIA)"),
  SICAV("5426", "Société d'investissement à capital variable (SICAV)"),
  SEML("5441", "Société d'économie mixte locale (SEML)"),
  UNION_COOPERATIVES("5451", "Union de sociétés coopératives"),
  CAISSE_EPARGNE("5460", "Caisse d'épargne et de prévoyance"),
  SCOP_SA("5470", "Société anonyme coopérative ouvrière de production (SCOP)"),
  SCIC_SA("5485", "Société coopérative d'intérêt collectif (SCIC)"),
  SA_PARTICIPATION_OUVRIERE("5505", "Société anonyme à participation ouvrière"),
  SAS("5510", "Société par actions simplifiée (SAS)"),
  SASU("5515", "Société par actions simplifiée à associé unique (SASU)"),
  SA_AUTRE("5499", "Société anonyme (autre)"),
  SE("5540", "Société européenne (SE)"),
  SCE("5542", "Société coopérative européenne (SCE)"),
  GEIE("5547", "Groupement européen d'intérêt économique (GEIE)"),
  SCOP("5551", "Société coopérative ouvrière de production (SCOP)"),
  SCOP_ARTISANALE("5552", "Société coopérative artisanale"),
  COOPERATIVE_MARITIME("5553", "Société coopérative maritime"),
  COOPERATIVE_TRANSPORT("5554", "Société coopérative de transport"),
  CAISSE_CREDIT_MUNICIPAL("5560", "Caisse de crédit municipal"),
  SOCIETE_CIVILE("5570", "Société civile"),
  SCPI("5585", "Société civile de placement collectif immobilier (SCPI)"),
  GIE("5595", "Groupement d'intérêt économique (GIE)"),
  SA_NON_PRECISEE("5599", "Société anonyme (forme non précisée)"),
  BANQUE_POPULAIRE("5605", "SA coopérative de crédit (Banque Populaire)"),
  CREDIT_MUTUEL("5610", "SA coopérative de crédit mutuel"),
  CREDIT_AGRICOLE("5615", "SA coopérative de crédit agricole"),
  COOPERATIVE_HLM("5620", "SA coopérative d'HLM"),
  SICA("5640", "Société d'intérêt collectif agricole (SICA)"),
  GAEC("5650", "Groupement agricole d'exploitation en commun (GAEC)"),

  // -------------------------------------------------------------------------
  // 6xxx — Mutuelles et organismes de prévoyance
  // -------------------------------------------------------------------------
  CAISSE_RETRAITE("6100", "Caisse de retraite et de prévoyance"),
  MUTUELLE("6210", "Mutuelle"),
  MUTUELLE_AGRICOLE("6220", "Mutuelle agricole"),
  INSTITUTION_PREVOYANCE("6230", "Institution de prévoyance"),
  AUTRE_MUTUALISTE("6290", "Autre organisme mutualiste"),

  // -------------------------------------------------------------------------
  // 7xxx — Personnes morales de droit public
  // -------------------------------------------------------------------------
  COMMUNE("7111", "Commune"),
  DEPARTEMENT("7112", "Département"),
  REGION("7113", "Région"),
  COLLECTIVITE_STATUT_PARTICULIER("7120", "Collectivité territoriale à statut particulier"),
  EPA_NATIONAL("7150", "Établissement public national administratif (EPA)"),
  EPST("7160", "Établissement public national scientifique et technologique (EPST)"),
  EPSCP("7170", "Établissement public à caractère scientifique, culturel et professionnel"),
  EPIC_NAT_ADMIN("7180", "Établissement public national à caractère industriel ou commercial"),
  EPA_AUTRE("7190", "Autre établissement public national administratif"),
  ETABLISSEMENT_ENSEIGNEMENT("7340", "Établissement public local d'enseignement"),
  EPA_LOCAL_AUTRE("7350", "Autre établissement public local administratif"),
  EPIC_LOCAL_ADM("7381", "Établissement public local à caractère industriel ou commercial"),
  GROUPEMENT_COLLECTIVITES("7410", "Groupement de collectivités territoriales"),
  COMMUNAUTE_COMMUNES("7430", "Communauté de communes"),
  METROPOLE("7450", "Métropole"),
  AUTRE_SYNDICAT_COLLECTIVITES("7490", "Autre syndicat de collectivités locales"),

  // -------------------------------------------------------------------------
  // 8xxx — Organismes privés spécialisés
  // -------------------------------------------------------------------------
  ORGANISME_SECURITE_SOCIALE(
      "8110", "Organisme de droit privé chargé de la gestion de la sécurité sociale"),
  ORGANISME_RETRAITE_COMPLEMENTAIRE("8120", "Organisme de retraite complémentaire"),
  CSE("8130", "Comité social et économique (CSE)"),
  ORGANISME_HLM("8220", "Organisme HLM"),

  // -------------------------------------------------------------------------
  // 9xxx — Associations, syndicats, fondations
  // -------------------------------------------------------------------------
  SYNDICAT_SALARIES("9110", "Syndicat de salariés"),
  SYNDICAT_EMPLOYEURS("9120", "Syndicat d'employeurs"),
  ASSOCIATION_LOI_1901("9210", "Association loi 1901"),
  ASSOCIATION_ALSACE_MOSELLE("9220", "Association loi 1908 d'Alsace-Moselle"),
  CONGREGATION("9240", "Congrégation"),
  FONDATION("9300", "Fondation"),
  AUTRE_PM_DROIT_PRIVE("9900", "Autre personne morale de droit privé");

  private static final Map<String, CategorieJuridique> BY_CODE =
      Arrays.stream(values()).collect(Collectors.toMap(c -> c.code, c -> c));

  private final String code;
  private final String libelle;

  CategorieJuridique(String code, String libelle) {
    this.code = code;
    this.libelle = libelle;
  }

  public String getCode() {
    return code;
  }

  public String getLibelle() {
    return libelle;
  }

  /**
   * Retourne l'enum correspondant au code INSEE sur 4 chiffres, ou {@code null} si le code est
   * inconnu ou null.
   */
  public static CategorieJuridique fromCode(String code) {
    if (code == null) {
      return null;
    }
    return BY_CODE.get(code);
  }
}
