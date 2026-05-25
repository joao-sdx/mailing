package com.synapsedx.mailing.pipeline.siren.enrich.model.annuaire;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Réponse paginée de l'API recherche-entreprises.api.gouv.fr.
 *
 * @param results Liste des unités légales correspondant à la recherche.
 * @param totalResults Nombre total de résultats pour la requête.
 * @param page Numéro de la page courante (commence à 1).
 * @param perPage Nombre de résultats par page.
 * @param totalPages Nombre total de pages.
 */
public record AnnuaireResponse(
    @JsonProperty("results") List<AnnuaireEntreprise> results,
    @JsonProperty("total_results") Integer totalResults,
    @JsonProperty("page") Integer page,
    @JsonProperty("per_page") Integer perPage,
    @JsonProperty("total_pages") Integer totalPages) {}
