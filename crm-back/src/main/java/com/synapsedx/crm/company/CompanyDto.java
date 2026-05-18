package com.synapsedx.crm.company;

public record CompanyDto(Long id, String name, String website, String phone) {

  public static CompanyDto from(Company c) {
    return new CompanyDto(c.getId(), c.getName(), c.getWebsite(), c.getPhone());
  }
}
