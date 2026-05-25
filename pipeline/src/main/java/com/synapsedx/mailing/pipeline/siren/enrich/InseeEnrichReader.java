package com.synapsedx.mailing.pipeline.siren.enrich;

import com.synapsedx.mailing.pipeline.siren.base.InseeRecord;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemStream;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.mapping.FieldSetMapper;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.batch.item.file.transform.FieldSet;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Component;

/** Reads all {@code *.csv} files from the configured input directory as {@link InseeRecord}s. */
@Slf4j
@Component
@RequiredArgsConstructor
public class InseeEnrichReader implements ItemReader<InseeRecord>, ItemStream {

  private final InseeEnrichProperties properties;

  private Queue<Path> pendingFiles;
  private FlatFileItemReader<InseeRecord> currentReader;
  private Path currentFile;
  private final List<Path> processedFiles = new ArrayList<>();

  @Override
  public void open(ExecutionContext ctx) {
    var inputDir = Path.of(properties.getInputDir());
    pendingFiles = new LinkedList<>();
    try (var stream =
        Files.find(
            inputDir, 1, (p, attr) -> attr.isRegularFile() && p.toString().endsWith(".csv"))) {
      stream.sorted().forEach(pendingFiles::add);
    } catch (IOException e) {
      throw new IllegalStateException("Cannot scan input dir: " + inputDir, e);
    }
    log.info("insee_enrich_reader_open input_dir={} files={}", inputDir, pendingFiles.size());
    openNextFile();
  }

  @Override
  public InseeRecord read() throws Exception {
    if (currentReader == null) {
      return null;
    }
    var item = currentReader.read();
    if (item != null) {
      return item;
    }
    // current file exhausted
    closeCurrentFile();
    openNextFile();
    if (currentReader == null) {
      return null;
    }
    return currentReader.read();
  }

  @Override
  public void update(ExecutionContext ctx) {}

  @Override
  public void close() {
    closeCurrentFile();
  }

  /** Files that have been fully read — to be moved to done by the job listener. */
  public List<Path> getProcessedFiles() {
    return List.copyOf(processedFiles);
  }

  private void openNextFile() {
    if (pendingFiles == null || pendingFiles.isEmpty()) {
      currentReader = null;
      currentFile = null;
      return;
    }
    currentFile = pendingFiles.poll();
    log.info("insee_enrich_opening_file file={}", currentFile.getFileName());
    currentReader = buildReader(currentFile);
    currentReader.open(new ExecutionContext());
  }

  private void closeCurrentFile() {
    if (currentReader != null) {
      currentReader.close();
      processedFiles.add(currentFile);
      log.info("insee_enrich_file_done file={}", currentFile.getFileName());
      currentReader = null;
      currentFile = null;
    }
  }

  private FlatFileItemReader<InseeRecord> buildReader(Path file) {
    var tokenizer = new DelimitedLineTokenizer(",");

    var lineMapper = new DefaultLineMapper<InseeRecord>();
    lineMapper.setLineTokenizer(tokenizer);
    lineMapper.setFieldSetMapper(new InseeRecordFieldSetMapper());

    var reader = new FlatFileItemReader<InseeRecord>();
    reader.setResource(new FileSystemResource(file));
    reader.setLinesToSkip(1); // header row
    reader.setLineMapper(lineMapper);
    return reader;
  }

  private static final class InseeRecordFieldSetMapper implements FieldSetMapper<InseeRecord> {

    @Override
    public InseeRecord mapFieldSet(FieldSet fs) {
      return new InseeRecord(
          fs.readString(0), // siren
          fs.readString(1), // statutDiffusionUniteLegale
          fs.readString(2), // unitePurgeeUniteLegale
          fs.readString(3), // dateCreationUniteLegale
          fs.readString(4), // sigleUniteLegale
          fs.readString(5), // sexeUniteLegale
          fs.readString(6), // prenom1UniteLegale
          fs.readString(7), // prenom2UniteLegale
          fs.readString(8), // prenom3UniteLegale
          fs.readString(9), // prenom4UniteLegale
          fs.readString(10), // prenomUsuelUniteLegale
          fs.readString(11), // pseudonymeUniteLegale
          fs.readString(12), // identifiantAssociationUniteLegale
          fs.readString(13), // trancheEffectifsUniteLegale
          fs.readString(14), // anneeEffectifsUniteLegale
          fs.readString(15), // dateDernierTraitementUniteLegale
          fs.readString(16), // nombrePeriodesUniteLegale
          fs.readString(17), // categorieEntreprise
          fs.readString(18), // anneeCategorieEntreprise
          fs.readString(19), // dateDebut
          fs.readString(20), // etatAdministratifUniteLegale
          fs.readString(21), // nomUniteLegale
          fs.readString(22), // nomUsageUniteLegale
          fs.readString(23), // denominationUniteLegale
          fs.readString(24), // denominationUsuelle1UniteLegale
          fs.readString(25), // denominationUsuelle2UniteLegale
          fs.readString(26), // denominationUsuelle3UniteLegale
          fs.readString(27), // categorieJuridiqueUniteLegale
          fs.readString(28), // activitePrincipaleUniteLegale
          fs.readString(29), // nomenclatureActivitePrincipaleUniteLegale
          fs.readString(30), // nicSiegeUniteLegale
          fs.readString(31), // economieSocialeSolidaireUniteLegale
          fs.readString(32), // societeMissionUniteLegale
          fs.readString(33), // caractereEmployeurUniteLegale
          fs.readString(34) // activitePrincipaleNAF25UniteLegale
          );
    }
  }
}
