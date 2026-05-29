package com.synapsedx.mailing.procurement.model.boamp;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record BoampRecord(
    @JsonProperty("idweb") String idweb,
    @JsonProperty("id") String id,
    @JsonProperty("objet") String objet,
    @JsonProperty("nomacheteur") String nomacheteur,
    @JsonProperty("dateparution") String dateparution, // "YYYY-MM-DD"
    @JsonProperty("datelimitereponse") String datelimitereponse, // ISO datetime or null
    @JsonProperty("descripteur_libelle") List<String> descripteurLibelle,
    @JsonProperty("url_avis") String urlAvis) {}
