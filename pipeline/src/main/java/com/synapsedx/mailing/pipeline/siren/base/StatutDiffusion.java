package com.synapsedx.mailing.pipeline.siren.base;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Statut de diffusion d'une unité légale ou d'un établissement dans le répertoire SIRENE.
 * Correspond au champ {@code statutDiffusionUniteLegale} ou {@code statutDiffusionEtablissement}.
 *
 * <p>Les unités non diffusibles ne doivent pas être rendues publiques (personnes physiques ayant
 * exercé leur droit d'opposition).
 */
public enum StatutDiffusion {

  /** Données diffusibles publiquement. */
  DIFFUSIBLE("O", "Diffusible"),

  /** Données non diffusibles — opposition de la personne physique. */
  NON_DIFFUSIBLE("N", "Non diffusible"),

  /** Données partiellement diffusibles (nom et prénom masqués, données économiques visibles). */
  PARTIELLEMENT_DIFFUSIBLE("P", "Partiellement diffusible");

  private static final Map<String, StatutDiffusion> BY_CODE =
      Arrays.stream(values()).collect(Collectors.toMap(s -> s.code, s -> s));

  private final String code;
  private final String libelle;

  StatutDiffusion(String code, String libelle) {
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
  public static StatutDiffusion fromCode(String code) {
    if (code == null) {
      return null;
    }
    return BY_CODE.get(code);
  }
}
