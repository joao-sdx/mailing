package com.synapsedx.mailing.seo.batch.writer;

import com.synapsedx.mailing.seo.AnnuaireEntreprisesClient;
import com.synapsedx.mailing.seo.SupabaseClient;
import com.synapsedx.mailing.seo.model.CompanyToEnrich;
import java.util.LinkedHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CompanyEnrichWriter implements ItemWriter<CompanyToEnrich> {

  private final SupabaseClient supabase;
  private final AnnuaireEntreprisesClient annuaire;

  @Override
  public void write(Chunk<? extends CompanyToEnrich> chunk) throws Exception {
    for (var company : chunk.getItems()) {
      var fields = new LinkedHashMap<String, Object>();
      try {
        var result = annuaire.enrich(company.name());
        if (result.isPresent()) {
          var e = result.get();
          if (e.siren() != null) fields.put("siren", e.siren());
          if (e.nafCode() != null) fields.put("naf_code", e.nafCode());
          if (e.sector() != null) fields.put("sector", e.sector());
          if (e.employeeRange() != null) fields.put("employee_range", e.employeeRange());
          if (e.category() != null) fields.put("category", e.category());
          if (e.city() != null) fields.put("city", e.city());
          fields.put("country", "France");
          log.info(
              "crm_company_enriched id={} name={} sector={} category={}",
              company.id(),
              company.name(),
              e.sector(),
              e.category());
        } else {
          log.info("crm_company_enrich_not_found id={} name={}", company.id(), company.name());
        }
      } catch (Exception e) {
        log.warn(
            "crm_company_enrich_failed id={} name={} reason={}",
            company.id(),
            company.name(),
            e.getMessage());
      }
      fields.put("enrich", true);
      supabase.update("crm_companies", company.id(), fields);
    }
  }
}
