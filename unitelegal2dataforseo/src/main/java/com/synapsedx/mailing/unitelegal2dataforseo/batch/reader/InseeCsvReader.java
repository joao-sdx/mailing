package com.synapsedx.mailing.unitelegal2dataforseo.batch.reader;

import com.synapsedx.mailing.unitelegal2dataforseo.config.Unitelegal2DataforseoProperties;
import com.synapsedx.mailing.unitelegal2dataforseo.model.InseeUniteLegale;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.batch.item.ItemStreamReader;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class InseeCsvReader implements ItemStreamReader<InseeUniteLegale> {

  private static final String[] CSV_COLUMNS = {
    "siren",
    "statutDiffusionUniteLegale",
    "unitePurgeeUniteLegale",
    "dateCreationUniteLegale",
    "sigleUniteLegale",
    "sexeUniteLegale",
    "prenom1UniteLegale",
    "prenom2UniteLegale",
    "prenom3UniteLegale",
    "prenom4UniteLegale",
    "prenomUsuelUniteLegale",
    "pseudonymeUniteLegale",
    "identifiantAssociationUniteLegale",
    "trancheEffectifsUniteLegale",
    "anneeEffectifsUniteLegale",
    "dateDernierTraitementUniteLegale",
    "nombrePeriodesUniteLegale",
    "categorieEntreprise",
    "anneeCategorieEntreprise",
    "dateDebut",
    "etatAdministratifUniteLegale",
    "nomUniteLegale",
    "nomUsageUniteLegale",
    "denominationUniteLegale",
    "denominationUsuelle1UniteLegale",
    "denominationUsuelle2UniteLegale",
    "denominationUsuelle3UniteLegale",
    "categorieJuridiqueUniteLegale",
    "activitePrincipaleUniteLegale",
    "nomenclatureActivitePrincipaleUniteLegale",
    "nicSiegeUniteLegale",
    "economieSocialeSolidaireUniteLegale",
    "societeMissionUniteLegale",
    "caractereEmployeurUniteLegale",
    "activitePrincipaleNAF25UniteLegale"
  };

  private final Unitelegal2DataforseoProperties properties;
  private FlatFileItemReader<InseeUniteLegale> delegate;

  @PostConstruct
  public void init() {
    delegate =
        new FlatFileItemReaderBuilder<InseeUniteLegale>()
            .name("inseeCsvReader")
            .resource(new FileSystemResource(properties.inputCsv()))
            .linesToSkip(1)
            .delimited()
            .names(CSV_COLUMNS)
            .fieldSetMapper(
                fs ->
                    new InseeUniteLegale(
                        fs.readString("siren"),
                        fs.readString("sigleUniteLegale"),
                        fs.readString("denominationUniteLegale"),
                        fs.readString("denominationUsuelle1UniteLegale"),
                        fs.readString("denominationUsuelle2UniteLegale"),
                        fs.readString("denominationUsuelle3UniteLegale")))
            .build();
  }

  @Override
  public InseeUniteLegale read() throws Exception {
    return delegate.read();
  }

  @Override
  public void open(ExecutionContext executionContext) throws ItemStreamException {
    delegate.open(executionContext);
  }

  @Override
  public void update(ExecutionContext executionContext) throws ItemStreamException {
    delegate.update(executionContext);
  }

  @Override
  public void close() throws ItemStreamException {
    delegate.close();
  }
}
