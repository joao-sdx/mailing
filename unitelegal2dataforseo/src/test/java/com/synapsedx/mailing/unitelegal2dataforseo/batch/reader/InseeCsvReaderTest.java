package com.synapsedx.mailing.unitelegal2dataforseo.batch.reader;

import static org.assertj.core.api.Assertions.assertThat;

import com.synapsedx.mailing.unitelegal2dataforseo.config.Unitelegal2DataforseoProperties;
import com.synapsedx.mailing.unitelegal2dataforseo.model.InseeUniteLegale;
import org.junit.jupiter.api.Test;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.core.io.ClassPathResource;

class InseeCsvReaderTest {

  @Test
  void readsAllRowsMappingFiveDenominationColumns() throws Exception {
    var csvPath = new ClassPathResource("echantillon-mini.csv").getFile().getAbsolutePath();
    var props = new Unitelegal2DataforseoProperties(csvPath, "ignored.yml");
    var reader = new InseeCsvReader(props);
    reader.init();
    reader.open(new ExecutionContext());

    var row1 = reader.read();
    var row2 = reader.read();
    var row3 = reader.read();
    var row4 = reader.read();

    reader.close();

    assertThat(row1)
        .isEqualTo(new InseeUniteLegale("016750697", "", "ETS J VIRLY S A", "", "", ""));
    assertThat(row2)
        .isEqualTo(
            new InseeUniteLegale(
                "054501754",
                "SET",
                "SET - HUILLIER - SOCIETE D'ENTREPOSAGE ET DE TRANSPORTS",
                "",
                "",
                ""));
    assertThat(row3)
        .isEqualTo(
            new InseeUniteLegale("099999999", "EDF", "ELECTRICITE DE FRANCE", "EDF", "", ""));
    assertThat(row4).isNull();
  }
}
