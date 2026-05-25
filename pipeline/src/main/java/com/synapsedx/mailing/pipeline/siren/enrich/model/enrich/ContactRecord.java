package com.synapsedx.mailing.pipeline.siren.enrich.model.enrich;

/**
 * @param rcs RCS de la personne morale dirigée. For french companies rcs is 'FR-(siren)"
 * @param nom Nom de famille (personne physique) ou null.
 * @param prenoms Prénoms (personne physique) ou null.
 * @param anneeDeNaissance Année de naissance (personne physique).
 * @param dateDeNaissance Date de naissance au format {@code YYYY-MM} (personne physique).
 * @param qualite Titre ou mandat (ex : Président de SAS, Gérant).
 * @param nationalite Nationalité (personne physique).
 * @param typeDirigeant {@code personne physique} ou {@code personne morale}.
 */
public record ContactRecord(
    //
    String rcs,
    // from AnnuaireDirigeant
    String nom,
    String prenoms,
    String anneeDeNaissance,
    String dateDeNaissance,
    String qualite,
    String nationalite,
    String typeDirigeant) {}
