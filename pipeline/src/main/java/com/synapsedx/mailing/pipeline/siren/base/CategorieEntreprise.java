package com.synapsedx.mailing.pipeline.siren.base;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Catégorie d'entreprise selon le décret n°2008-1354 du 18 décembre 2008. Correspond au champ
 * {@code categorieEntreprise} du répertoire SIRENE.
 */
public enum CategorieEntreprise {

  /**
   * Micro-entreprise ou petite et moyenne entreprise : moins de 250 salariés, CA ≤ 50M€ ou bilan ≤
   * 43M€.
   */
  PME("PME", "Petite et moyenne entreprise"),

  /** Entreprise de taille intermédiaire : 250 à 4 999 salariés, CA ≤ 1,5Md€ ou bilan ≤ 2Md€. */
  ETI("ETI", "Entreprise de taille intermédiaire"),

  /** Grande entreprise : 5 000 salariés ou plus, ou CA > 1,5Md€ et bilan > 2Md€. */
  GE("GE", "Grande entreprise");

  private static final Map<String, CategorieEntreprise> BY_CODE =
      Arrays.stream(values()).collect(Collectors.toMap(c -> c.code, c -> c));

  private final String code;
  private final String libelle;

  CategorieEntreprise(String code, String libelle) {
    this.code = code;
    this.libelle = libelle;
  }

  public String getCode() {
    return code;
  }

  public String getLibelle() {
    return libelle;
  }

  /** Retourne l'enum correspondant au code INSEE, ou {@code null} si inconnu ou null. */
  public static CategorieEntreprise fromCode(String code) {
    if (code == null) {
      return null;
    }
    return BY_CODE.get(code);
  }
}
