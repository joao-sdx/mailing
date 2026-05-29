package com.synapsedx.mailing.procurement.batch.writer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.synapsedx.mailing.procurement.config.ProcurementProperties;
import com.synapsedx.mailing.procurement.model.Tender;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.item.Chunk;

class TenderCsvWriterTest {

  @TempDir Path tempDir;

  private TenderCsvWriter writer;
  private Path csvPath;

  @BeforeEach
  void setUp() {
    csvPath = tempDir.resolve("tenders.csv");
    var properties = mock(ProcurementProperties.class);
    org.mockito.Mockito.when(properties.outputCsv()).thenReturn(csvPath.toString());
    writer = new TenderCsvWriter(properties);
    writer.beforeStep(mock(StepExecution.class));
  }

  @Test
  void writesHeaderAndRows() throws Exception {
    var t1 =
        tender(
            "TED",
            "T-001",
            "Road construction",
            "Ministry",
            "FRA",
            "45000000",
            "500000",
            LocalDate.of(2025, 1, 10),
            LocalDate.of(2025, 3, 1),
            "https://ted.europa.eu/1");
    var t2 =
        tender(
            "BOAMP",
            "B-002",
            "IT services",
            "Prefecture",
            "FRA",
            "CPV-72",
            "",
            LocalDate.of(2025, 1, 15),
            null,
            "https://boamp.fr/2");

    writer.write(new Chunk<>(List.of(List.of(t1, t2))));

    var lines = Files.readAllLines(csvPath);
    assertThat(lines).hasSize(3);
    assertThat(lines.getFirst())
        .isEqualTo(
            "source,id,title,buyer,country,classification,value,publication_date,deadline,url");
    assertThat(lines.get(1)).startsWith("TED,T-001,Road construction,Ministry,FRA");
    assertThat(lines.get(2)).startsWith("BOAMP,B-002,IT services,Prefecture,FRA");
  }

  @Test
  void escapesCommasInFields() throws Exception {
    var t =
        tender(
            "TED",
            "T-001",
            "Roads, bridges",
            "Dept, FR",
            "FRA",
            "45000000",
            "",
            LocalDate.of(2025, 1, 1),
            null,
            "https://ted.europa.eu/1");

    writer.write(new Chunk<>(List.of(List.of(t))));

    var content = Files.readString(csvPath);
    assertThat(content).contains("\"Roads, bridges\"");
    assertThat(content).contains("\"Dept, FR\"");
  }

  @Test
  void appendsAcrossMultipleChunks() throws Exception {
    var t1 =
        tender(
            "TED",
            "T-001",
            "Title 1",
            "Buyer 1",
            "FRA",
            "CPV-1",
            "",
            LocalDate.of(2025, 1, 1),
            null,
            "https://ted.europa.eu/1");
    var t2 =
        tender(
            "BOAMP",
            "B-002",
            "Title 2",
            "Buyer 2",
            "FRA",
            "CPV-2",
            "",
            LocalDate.of(2025, 2, 1),
            null,
            "https://boamp.fr/2");

    writer.write(new Chunk<>(List.of(List.of(t1))));
    writer.write(new Chunk<>(List.of(List.of(t2))));

    var lines = Files.readAllLines(csvPath);
    // Header written once + 2 data rows
    assertThat(lines).hasSize(3);
    assertThat(lines.getFirst()).startsWith("source,id");
    assertThat(lines.get(1)).contains("T-001");
    assertThat(lines.get(2)).contains("B-002");
  }

  @Test
  void handlesNullFieldsGracefully() throws Exception {
    var t = new Tender("TED", "T-001", null, null, "FRA", null, null, null, null, null);

    writer.write(new Chunk<>(List.of(List.of(t))));

    var lines = Files.readAllLines(csvPath);
    assertThat(lines).hasSize(2);
    // Fields are empty, no NPE
    var dataLine = lines.get(1);
    assertThat(dataLine).startsWith("TED,T-001,,");
  }

  private Tender tender(
      String source,
      String id,
      String title,
      String buyer,
      String country,
      String classification,
      String value,
      LocalDate publicationDate,
      LocalDate deadline,
      String url) {
    return new Tender(
        source, id, title, buyer, country, classification, value, publicationDate, deadline, url);
  }
}
