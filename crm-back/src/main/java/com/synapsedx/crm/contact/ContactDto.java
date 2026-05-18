package com.synapsedx.crm.contact;

import com.synapsedx.crm.company.CompanyDto;

public record ContactDto(
    Long id,
    String firstName,
    String lastName,
    String email,
    String phone,
    String jobTitle,
    CompanyDto company) {

  public static ContactDto from(Contact c) {
    return new ContactDto(
        c.getId(),
        c.getFirstName(),
        c.getLastName(),
        c.getEmail(),
        c.getPhone(),
        c.getJobTitle(),
        c.getCompany() != null ? CompanyDto.from(c.getCompany()) : null);
  }
}
