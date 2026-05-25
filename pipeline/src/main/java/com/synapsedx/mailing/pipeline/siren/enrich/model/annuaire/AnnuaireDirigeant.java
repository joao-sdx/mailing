package com.synapsedx.mailing.pipeline.siren.enrich.model.annuaire;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Dirigeant ou mandataire social d'une unité légale.
 *
 * @param nom Nom de famille (personne physique) ou null.
 * @param prenoms Prénoms (personne physique) ou null.
 * @param anneeDeNaissance Année de naissance (personne physique).
 * @param dateDeNaissance Date de naissance au format {@code YYYY-MM} (personne physique).
 * @param qualite Titre ou mandat (ex : Président de SAS, Gérant).
 * @param nationalite Nationalité (personne physique).
 * @param typeDirigeant {@code personne physique} ou {@code personne morale}.
 * @param siren SIREN de la personne morale dirigeante.
 * @param denomination Dénomination sociale de la personne morale dirigeante.
 */
public record AnnuaireDirigeant(
    @JsonProperty("nom") String nom,
    @JsonProperty("prenoms") String prenoms,
    @JsonProperty("annee_de_naissance") String anneeDeNaissance,
    @JsonProperty("date_de_naissance") String dateDeNaissance,
    @JsonProperty("qualite") String qualite,
    @JsonProperty("nationalite") String nationalite,
    @JsonProperty("type_dirigeant") String typeDirigeant,
    @JsonProperty("siren") String siren,
    @JsonProperty("denomination") String denomination) {}
