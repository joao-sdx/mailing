package com.synapsedx.mailing.pipeline.siren.news;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** Configuration paths for the company-news-search batch job. */
@Component
@ConfigurationProperties("batch.company-news-search")
public class CompanyNewsSearchProperties {

  private String inputDir = "workdir/10-company-search-news";
  private String doneDir = "workdir/10-company-search-news/done";
  private String outputDir = "workdir/12-company-news";

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

  public String getOutputDir() {
    return outputDir;
  }

  public void setOutputDir(String outputDir) {
    this.outputDir = outputDir;
  }
}
