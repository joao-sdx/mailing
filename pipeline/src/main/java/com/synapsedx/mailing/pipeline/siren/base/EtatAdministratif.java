package com.synapsedx.mailing.pipeline.siren.base;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * État administratif d'une unité légale ou d'un établissement selon le répertoire SIRENE.
 * Correspond au champ {@code etatAdministratifUniteLegale} ou {@code etatAdministratif}.
 */
public enum EtatAdministratif {

  /** Unité légale ou établissement actif. */
  ACTIF("A", "Actif"),

  /** Unité légale cessée ou établissement fermé. */
  CESSE("C", "Cessé");

  private static final Map<String, EtatAdministratif> BY_CODE =
      Arrays.stream(values()).collect(Collectors.toMap(e -> e.code, e -> e));

  private final String code;
  private final String libelle;

  EtatAdministratif(String code, String libelle) {
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
  public static EtatAdministratif fromCode(String code) {
    if (code == null) {
      return null;
    }
    return BY_CODE.get(code);
  }
}
