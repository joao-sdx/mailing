package com.synapsedx.mailing.pipeline.siren.base;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Nomenclature utilisée pour coder l'activité principale d'une unité légale ou d'un établissement.
 * Correspond au champ {@code nomenclatureActivitePrincipaleUniteLegale}.
 */
public enum NomenclatureActivite {

  /** Nomenclature d'Activités Française révision 1 (en vigueur de 1993 à 2007). */
  NAF_REV1("NAFRev1", "NAF révision 1 (1993-2007)"),

  /** Nomenclature d'Activités Française révision 2 (en vigueur depuis 2008). */
  NAF_REV2("NAFRev2", "NAF révision 2 (depuis 2008)"),

  /** Nomenclature d'Activités Française 2025 (en vigueur depuis 2025). */
  NAF_2025("NAF2025", "NAF 2025");

  private static final Map<String, NomenclatureActivite> BY_CODE =
      Arrays.stream(values()).collect(Collectors.toMap(n -> n.code, n -> n));

  private final String code;
  private final String libelle;

  NomenclatureActivite(String code, String libelle) {
    this.code = code;
    this.libelle = libelle;
  }

  public String getCode() {
    return code;
  }

  public String getLibelle() {
    return libelle;
  }

  /** Retourne l'enum correspondant au code, ou {@code null} si inconnu ou null. */
  public static NomenclatureActivite fromCode(String code) {
    if (code == null) {
      return null;
    }
    return BY_CODE.get(code);
  }
}
