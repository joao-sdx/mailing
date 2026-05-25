package com.synapsedx.mailing.pipeline.siren.split;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** Configuration paths for the company-enrich batch job. */
@Component
@ConfigurationProperties("batch.company-enrich")
public class InseeCompanyEnrichProperties {

  private String inputDir = "workdir/02-siren-line";
  private String doneDir = "workdir/02-siren-line/done";
  private String companyOutputDir = "workdir/03-company";
  private String contactOutputDir = "workdir/04-contact";
  private String relationOutputDir = "workdir/05-relation";

  public String getInputDir() {
    return inputDir;
  }

  public void setInputDir(String inputDir) {
    this.inputDir = inputDir;
  }

  public String getDoneDir() {
    return doneDir;
  }

  public void setDoneDir(String doneDir) {
    this.doneDir = doneDir;
  }

  public String getCompanyOutputDir() {
    return companyOutputDir;
  }

  public void setCompanyOutputDir(String companyOutputDir) {
    this.companyOutputDir = companyOutputDir;
  }

  public String getContactOutputDir() {
    return contactOutputDir;
  }

  public void setContactOutputDir(String contactOutputDir) {
    this.contactOutputDir = contactOutputDir;
  }

  public String getRelationOutputDir() {
    return relationOutputDir;
  }

  public void setRelationOutputDir(String relationOutputDir) {
    this.relationOutputDir = relationOutputDir;
  }
}
