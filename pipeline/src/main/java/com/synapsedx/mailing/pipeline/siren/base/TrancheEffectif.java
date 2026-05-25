package com.synapsedx.mailing.pipeline.siren.base;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

/** Tranche d'effectifs salariés selon la codification INSEE (répertoire SIRENE). */
public enum TrancheEffectif {
  UNKNOWN("NN", "Non renseigné", 0),
  T00000("00", "0 salarié", 0),
  T00001("01", "1 à 2", 1),
  T00003("02", "3 à 5", 3),
  T00006("03", "6 à 9", 6),
  T00010("11", "10 à 19", 10),
  T00020("12", "20 à 49", 20),
  T00050("21", "50 à 99", 50),
  T00100("22", "100 à 199", 100),
  T00200("31", "200 à 249", 200),
  T00250("32", "250 à 499", 250),
  T00500("41", "500 à 999", 500),
  T01000("42", "1 000 à 1 999", 1000),
  T02000("51", "2 000 à 4 999", 2000),
  T05000("52", "5 000 à 9 999", 5000),
  T10000("53", "10 000 et plus", 10000);

  private static final Map<String, TrancheEffectif> BY_CODE =
      Arrays.stream(values()).collect(Collectors.toMap(t -> t.code, t -> t));

  private final String code;
  private final String libelle;
  private final int minEmployes;

  TrancheEffectif(String code, String libelle, int minEmployes) {
    this.code = code;
    this.libelle = libelle;
    this.minEmployes = minEmployes;
  }

  public String getCode() {
    return code;
  }

  public String getLibelle() {
    return libelle;
  }

  /** Nombre minimum de salariés dans la tranche (0 si non renseigné). */
  public int getMinEmployes() {
    return minEmployes;
  }

  /** Retourne l'enum correspondant au code INSEE, ou {@link #UNKNOWN} si inconnu ou null. */
  public static TrancheEffectif fromCode(String code) {
    if (code == null) {
      return UNKNOWN;
    }
    return BY_CODE.getOrDefault(code, UNKNOWN);
  }
}
