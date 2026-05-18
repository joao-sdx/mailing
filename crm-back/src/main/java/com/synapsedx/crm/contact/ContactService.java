package com.synapsedx.crm.contact;

import com.synapsedx.crm.company.Company;
import com.synapsedx.crm.company.CompanyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ContactService {

  private final ContactRepository repo;
  private final CompanyRepository companyRepo;

  public Page<ContactDto> search(String q, int page, int size) {
    var pageable = PageRequest.of(page, size, Sort.by("lastName", "firstName"));
    return repo.search(q, pageable).map(ContactDto::from);
  }

  public ContactDto get(Long id) {
    return repo.findById(id)
        .map(ContactDto::from)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
  }

  @Transactional
  public ContactDto create(ContactSaveRequest req) {
    var contact =
        Contact.builder()
            .firstName(req.firstName())
            .lastName(req.lastName())
            .email(req.email())
            .phone(req.phone())
            .jobTitle(req.jobTitle())
            .company(resolveCompany(req.companyId()))
            .build();
    return ContactDto.from(repo.save(contact));
  }

  @Transactional
  public ContactDto update(Long id, ContactSaveRequest req) {
    var contact =
        repo.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    contact.setFirstName(req.firstName());
    contact.setLastName(req.lastName());
    contact.setEmail(req.email());
    contact.setPhone(req.phone());
    contact.setJobTitle(req.jobTitle());
    contact.setCompany(resolveCompany(req.companyId()));
    return ContactDto.from(repo.save(contact));
  }

  @Transactional
  public void delete(Long id) {
    if (!repo.existsById(id)) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    repo.deleteById(id);
  }

  private Company resolveCompany(Long companyId) {
    if (companyId == null) return null;
    return companyRepo
        .findById(companyId)
        .orElseThrow(
            () -> new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Company not found"));
  }
}
