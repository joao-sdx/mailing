package com.synapsedx.mailing.pipeline.siren.enrich;

import com.synapsedx.mailing.pipeline.siren.enrich.annuaire.AnnuaireEntreprise;
import java.util.Optional;

/** Contract for looking up a company by SIREN from the Annuaire des Entreprises. */
public interface InseeAnnuairePort {

  Optional<AnnuaireEntreprise> findBySiren(String siren);
}
