package com.synapsedx.mailing.pipeline.siren.enrich.annuaire;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Données financières annuelles issues des comptes déposés. */
public record AnnuaireFinances(
    /** Chiffre d'affaires en euros. */
    @JsonProperty("ca") Long ca,
    /** Résultat net en euros. */
    @JsonProperty("resultat_net") Long resultatNet) {}
