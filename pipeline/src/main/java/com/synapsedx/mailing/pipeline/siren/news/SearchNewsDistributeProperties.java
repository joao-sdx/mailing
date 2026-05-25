package com.synapsedx.mailing.pipeline.siren.news;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** Configuration paths for the search-news-distribute batch job. */
@Component
@ConfigurationProperties("batch.search-news-distribute")
public class SearchNewsDistributeProperties {

  private String companyInputDir = "workdir/03-company";
  private String companyDoneDir = "workdir/03-company/done";
  private String companyOutputDir = "workdir/10-company-search-news";
  private String personInputDir = "workdir/04-contact";
  private String personDoneDir = "workdir/04-contact/done";
  private String personOutputDir = "workdir/11-person-search-news";

  public String getCompanyInputDir() {
    return companyInputDir;
  }

  public void setCompanyInputDir(String companyInputDir) {
    this.companyInputDir = companyInputDir;
  }

  public String getCompanyDoneDir() {
    return companyDoneDir;
  }

  public void setCompanyDoneDir(String companyDoneDir) {
    this.companyDoneDir = companyDoneDir;
  }

  public String getCompanyOutputDir() {
    return companyOutputDir;
  }

  public void setCompanyOutputDir(String companyOutputDir) {
    this.companyOutputDir = companyOutputDir;
  }

  public String getPersonInputDir() {
    return personInputDir;
  }

  public void setPersonInputDir(String personInputDir) {
    this.personInputDir = personInputDir;
  }

  public String getPersonDoneDir() {
    return personDoneDir;
  }

  public void setPersonDoneDir(String personDoneDir) {
    this.personDoneDir = personDoneDir;
  }

  public String getPersonOutputDir() {
    return personOutputDir;
  }

  public void setPersonOutputDir(String personOutputDir) {
    this.personOutputDir = personOutputDir;
  }
}
