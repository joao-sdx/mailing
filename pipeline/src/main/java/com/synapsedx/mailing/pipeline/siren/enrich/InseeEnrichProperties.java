package com.synapsedx.mailing.pipeline.siren.enrich;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** Configuration paths for the INSEE enrichment batch job. */
@Component
@ConfigurationProperties("batch.siren")
public class InseeEnrichProperties {

  private String inputDir = "workdir/01-siren";
  private String doneDir = "workdir/01-siren/done";
  private String outputDir = "workdir/02-siren-line";

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
