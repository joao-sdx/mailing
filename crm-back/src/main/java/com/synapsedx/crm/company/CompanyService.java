package com.synapsedx.crm.company;

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
public class CompanyService {

  private final CompanyRepository repo;

  public Page<CompanyDto> search(String q, int page, int size) {
    var pageable = PageRequest.of(page, size, Sort.by("name"));
    var result =
        q.isBlank() ? repo.findAll(pageable) : repo.findByNameContainingIgnoreCase(q, pageable);
    return result.map(CompanyDto::from);
  }

  public CompanyDto get(Long id) {
    return repo.findById(id)
        .map(CompanyDto::from)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
  }

  @Transactional
  public CompanyDto create(CompanySaveRequest req) {
    return CompanyDto.from(
        repo.save(Company.builder().name(req.name()).website(req.website()).phone(req.phone()).build()));
  }

  @Transactional
  public CompanyDto update(Long id, CompanySaveRequest req) {
    var company =
        repo.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    company.setName(req.name());
    company.setWebsite(req.website());
    company.setPhone(req.phone());
    return CompanyDto.from(repo.save(company));
  }

  @Transactional
  public void delete(Long id) {
    if (!repo.existsById(id)) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    repo.deleteById(id);
  }
}
