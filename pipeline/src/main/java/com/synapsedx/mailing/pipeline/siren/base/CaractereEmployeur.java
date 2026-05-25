package com.synapsedx.mailing.pipeline.siren.base;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Caractère employeur d'une unité légale ou d'un établissement dans le répertoire SIRENE.
 * Correspond au champ {@code caractereEmployeurUniteLegale} ou {@code caractereEmployeur}.
 */
public enum CaractereEmployeur {

  /** Emploie au moins un salarié. */
  EMPLOYEUR("O", "Employeur"),

  /** N'emploie aucun salarié. */
  NON_EMPLOYEUR("N", "Non employeur");

  private static final Map<String, CaractereEmployeur> BY_CODE =
      Arrays.stream(values()).collect(Collectors.toMap(c -> c.code, c -> c));

  private final String code;
  private final String libelle;

  CaractereEmployeur(String code, String libelle) {
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
  public static CaractereEmployeur fromCode(String code) {
    if (code == null) {
      return null;
    }
    return BY_CODE.get(code);
  }
}
