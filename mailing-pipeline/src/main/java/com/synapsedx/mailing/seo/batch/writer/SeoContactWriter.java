package com.synapsedx.mailing.seo.batch.writer;

import com.synapsedx.mailing.seo.SupabaseClient;
import com.synapsedx.mailing.seo.model.ExtractedContact;
import com.synapsedx.mailing.seo.model.SeoContactBatch;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SeoContactWriter implements ItemWriter<SeoContactBatch> {

  private final SupabaseClient nocobase;

  @Override
  public void write(Chunk<? extends SeoContactBatch> chunk) throws Exception {
    for (var batch : chunk.getItems()) {
      for (var contact : batch.contacts()) {
        try {
          var contactId = resolveContact(contact);
          nocobase.addRelation("crm_contacts", contactId, "seo_articles", batch.seoResultId());
          log.info("seo_article_linked contactId={} resultId={}", contactId, batch.seoResultId());
        } catch (Exception e) {
          log.warn(
              "seo_contact_failed resultId={} nom={} reason={}",
              batch.seoResultId(),
              contact.nom(),
              e.getMessage());
        }
      }
      nocobase.update("seo_result", batch.seoResultId(), Map.of("scan_status", "Yes"));
      log.info(
          "seo_result_scanned id={} contacts={}", batch.seoResultId(), batch.contacts().size());
    }
  }

  private int resolveContact(ExtractedContact contact) throws Exception {
    // 1. find by email
    if (!contact.email().isBlank()) {
      var found = nocobase.findFirst("crm_contacts", Map.of("email", contact.email()));
      if (found.isPresent()) {
        log.info("crm_contact_found_by_email email={}", contact.email());
        return found.get();
      }
    }

    // 2. find by name + first_name
    if (!contact.nom().isBlank()) {
      var nameFilter = new LinkedHashMap<String, Object>();
      nameFilter.put("name", contact.nom());
      if (!contact.prenom().isBlank()) {
        nameFilter.put("first_name", contact.prenom());
      }
      var found = nocobase.findFirst("crm_contacts", nameFilter);
      if (found.isPresent()) {
        log.info("crm_contact_found_by_name nom={} prenom={}", contact.nom(), contact.prenom());
        return found.get();
      }
    }

    // 3. resolve company
    var companyId = resolveCompany(contact.societe());

    // 4. create contact
    var fields = new LinkedHashMap<String, Object>();
    if (!contact.nom().isBlank()) {
      fields.put("name", contact.nom());
    }
    if (!contact.prenom().isBlank()) {
      fields.put("first_name", contact.prenom());
    }
    if (!contact.email().isBlank()) {
      fields.put("email", contact.email());
    }
    if (companyId > 0) {
      fields.put("company_id", companyId);
    }
    var contactId = nocobase.create("crm_contacts", fields);
    log.info(
        "crm_contact_created id={} nom={} prenom={} company_id={}",
        contactId,
        contact.nom(),
        contact.prenom(),
        companyId);
    return contactId;
  }

  private int resolveCompany(String societe) throws Exception {
    if (societe.isBlank()) {
      return 0;
    }
    var found = nocobase.findFirst("crm_companies", Map.of("name", societe));
    if (found.isPresent()) {
      log.info("crm_company_found name={}", societe);
      return found.get();
    }
    var companyId = nocobase.create("crm_companies", Map.of("name", societe));
    log.info("crm_company_created id={} name={}", companyId, societe);
    return companyId;
  }
}
