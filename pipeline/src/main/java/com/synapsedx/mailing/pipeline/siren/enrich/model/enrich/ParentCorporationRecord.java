package com.synapsedx.mailing.pipeline.siren.enrich.model.enrich;

/**
 * @param rcs RCS de la personne morale dirigée. For french companies rcs is 'FR-(siren)"
 * @param qualite Titre ou mandat (ex : Président de SAS, Gérant).
 * @param parentCorporationRcs RCS de la personne morale dirigeante. For french companies rcs is
 *     'FR-(siren)"
 */
public record ParentCorporationRecord(
    // For french companies rcs is 'FR-(siren)"
    String rcs,
    //
    String qualite,
    // For french companies rcs is 'FR-(siren)"
    String parentCorporationRcs) {}
