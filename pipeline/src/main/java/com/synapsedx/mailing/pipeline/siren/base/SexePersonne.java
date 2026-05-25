package com.synapsedx.mailing.pipeline.siren.base;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Sexe d'une personne physique dans le répertoire SIRENE. Correspond au champ {@code
 * sexeUniteLegale}. Null pour les personnes morales.
 */
public enum SexePersonne {
  MASCULIN("M", "Masculin"),
  FEMININ("F", "Féminin");

  private static final Map<String, SexePersonne> BY_CODE =
      Arrays.stream(values()).collect(Collectors.toMap(s -> s.code, s -> s));

  private final String code;
  private final String libelle;

  SexePersonne(String code, String libelle) {
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
  public static SexePersonne fromCode(String code) {
    if (code == null) {
      return null;
    }
    return BY_CODE.get(code);
  }
}
