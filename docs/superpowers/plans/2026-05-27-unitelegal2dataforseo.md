# unitelegal2dataforseo Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a new Spring Batch Maven module `unitelegal2dataforseo` that converts an INSEE Sirene CSV (e.g. `sources/insee/echantillon.csv`) into a `dataforseo-queries.yml` file consumable by the existing `seo-news-search` module, applying a substring-aware dedup across the 5 dénomination columns and per-run defaults loaded from a YAML config.

**Architecture:** Single Spring Batch job `unitelegal2dataforseo` with one chunk-oriented step `convertStep`: `FlatFileItemReader` (streams CSV row → `InseeUniteLegale` record) → `KeywordDedupProcessor` (returns `KeywordBatch` with deduplicated keywords) → `DataForSeoYamlWriter` (custom `ItemStreamWriter` that opens the output YAML once, writes the `queries:` header, then appends one entry per keyword as chunks arrive). Defaults (`language_code`, `depth`, `location_code`, `location_name`, `file_prefix`) are loaded via `@ConfigurationProperties` from `query-defaults.yml`. Paths configurable via CLI args.

**Tech Stack:** Java 21, Spring Boot 3.4.5, Spring Batch (`FlatFileItemReader`, chunk-oriented step), Jackson YAML (`jackson-dataformat-yaml`), H2 in-memory (JobRepository only), Lombok, JUnit 5 + AssertJ. Build via Maven (inherits root `pom.xml`), runtime via `task default` (Taskfile.yml).

**Spec:** `docs/superpowers/specs/2026-05-27-unitelegal2dataforseo-design.md`

**Reference module:** `seo-news-search/` (mirror its structure: `batch/{reader,processor,writer}`, `config/`, `model/`, `Application.java`, `application.yml`, `Taskfile.yml`, `pom.xml`).

---

## Task 1: Bootstrap module scaffolding

**Files:**
- Create: `unitelegal2dataforseo/pom.xml`
- Modify: `pom.xml` (root, line 12 — add module)

- [ ] **Step 1: Create module directory tree**

Run:
```bash
mkdir -p unitelegal2dataforseo/src/main/java/com/synapsedx/mailing/unitelegal2dataforseo/{batch/{reader,processor,writer},config,model}
mkdir -p unitelegal2dataforseo/src/main/resources
mkdir -p unitelegal2dataforseo/src/test/java/com/synapsedx/mailing/unitelegal2dataforseo/{batch/{reader,processor,writer}}
mkdir -p unitelegal2dataforseo/src/test/resources
```

- [ ] **Step 2: Create module `pom.xml`**

Create `unitelegal2dataforseo/pom.xml`:

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd">
  <modelVersion>4.0.0</modelVersion>
  <artifactId>unitelegal2dataforseo</artifactId>

  <parent>
    <groupId>com.synapsedx</groupId>
    <artifactId>mailing</artifactId>
    <version>1.0-SNAPSHOT</version>
  </parent>

  <dependencies>
    <dependency>
      <groupId>org.projectlombok</groupId>
      <artifactId>lombok</artifactId>
      <scope>provided</scope>
    </dependency>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-batch</artifactId>
    </dependency>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-jdbc</artifactId>
    </dependency>
    <dependency>
      <groupId>com.h2database</groupId>
      <artifactId>h2</artifactId>
      <scope>runtime</scope>
    </dependency>
    <dependency>
      <groupId>com.fasterxml.jackson.dataformat</groupId>
      <artifactId>jackson-dataformat-yaml</artifactId>
    </dependency>
    <dependency>
      <groupId>com.fasterxml.jackson.core</groupId>
      <artifactId>jackson-databind</artifactId>
    </dependency>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-test</artifactId>
      <scope>test</scope>
    </dependency>
    <dependency>
      <groupId>org.springframework.batch</groupId>
      <artifactId>spring-batch-test</artifactId>
      <scope>test</scope>
    </dependency>
  </dependencies>

  <build>
    <plugins>
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-compiler-plugin</artifactId>
        <version>${maven-compiler-plugin.version}</version>
        <configuration>
          <annotationProcessorPaths>
            <path>
              <groupId>org.projectlombok</groupId>
              <artifactId>lombok</artifactId>
              <version>${lombok.version}</version>
            </path>
          </annotationProcessorPaths>
        </configuration>
      </plugin>
      <plugin>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-maven-plugin</artifactId>
        <version>${spring-boot.version}</version>
        <executions>
          <execution>
            <goals>
              <goal>repackage</goal>
            </goals>
          </execution>
        </executions>
      </plugin>
      <plugin>
        <groupId>com.diffplug.spotless</groupId>
        <artifactId>spotless-maven-plugin</artifactId>
        <version>${spotless-maven-plugin.version}</version>
        <configuration>
          <java>
            <googleJavaFormat>
              <version>${google-java-format.version}</version>
              <style>GOOGLE</style>
              <reflowLongStrings>true</reflowLongStrings>
            </googleJavaFormat>
            <removeUnusedImports/>
            <importOrder/>
          </java>
        </configuration>
        <executions>
          <execution>
            <id>spotless-apply</id>
            <phase>process-sources</phase>
            <goals>
              <goal>apply</goal>
            </goals>
          </execution>
        </executions>
      </plugin>
    </plugins>
  </build>
</project>
```

- [ ] **Step 3: Add module to root `pom.xml`**

Edit root `pom.xml` — in the `<modules>` block (around line 8–12), add the new module so the section becomes:

```xml
    <modules>
        <module>mailing-pipeline</module>
        <module>seo-news-search</module>
        <module>seo-news-parse</module>
        <module>unitelegal2dataforseo</module>
    </modules>
```

- [ ] **Step 4: Verify Maven sees the module**

Run: `./mvnw -pl unitelegal2dataforseo validate -q`
Expected: BUILD SUCCESS (no compile yet, only POM validation).

- [ ] **Step 5: Commit**

```bash
git add pom.xml unitelegal2dataforseo/pom.xml
git commit -m "chore: scaffold unitelegal2dataforseo module"
```

---

## Task 2: Spring Boot application class + config properties

**Files:**
- Create: `unitelegal2dataforseo/src/main/java/com/synapsedx/mailing/unitelegal2dataforseo/Unitelegal2DataforseoApplication.java`
- Create: `unitelegal2dataforseo/src/main/java/com/synapsedx/mailing/unitelegal2dataforseo/config/Unitelegal2DataforseoProperties.java`
- Create: `unitelegal2dataforseo/src/main/java/com/synapsedx/mailing/unitelegal2dataforseo/config/QueryDefaults.java`
- Create: `unitelegal2dataforseo/src/main/resources/application.yml`
- Create: `unitelegal2dataforseo/src/main/resources/query-defaults.yml`
- Test: `unitelegal2dataforseo/src/test/java/com/synapsedx/mailing/unitelegal2dataforseo/ContextLoadsTest.java`

- [ ] **Step 1: Write the failing context-loads test**

Create `unitelegal2dataforseo/src/test/java/com/synapsedx/mailing/unitelegal2dataforseo/ContextLoadsTest.java`:

```java
package com.synapsedx.mailing.unitelegal2dataforseo;

import static org.assertj.core.api.Assertions.assertThat;

import com.synapsedx.mailing.unitelegal2dataforseo.config.QueryDefaults;
import com.synapsedx.mailing.unitelegal2dataforseo.config.Unitelegal2DataforseoProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = "spring.batch.job.enabled=false")
class ContextLoadsTest {

  @Autowired Unitelegal2DataforseoProperties properties;
  @Autowired QueryDefaults defaults;

  @Test
  void propertiesAreLoaded() {
    assertThat(properties.inputCsv()).isNotBlank();
    assertThat(properties.outputYml()).isNotBlank();
    assertThat(defaults.languageCode()).isEqualTo("fr");
    assertThat(defaults.depth()).isEqualTo(2);
    assertThat(defaults.locationCode()).isEqualTo(2250);
    assertThat(defaults.locationName()).isEqualTo("France");
    assertThat(defaults.filePrefix()).isEqualTo("assurance-fr");
  }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./mvnw -pl unitelegal2dataforseo test -q`
Expected: FAIL — Spring cannot load context (no `@SpringBootApplication` class).

- [ ] **Step 3: Create `Unitelegal2DataforseoProperties`**

Create `unitelegal2dataforseo/src/main/java/com/synapsedx/mailing/unitelegal2dataforseo/config/Unitelegal2DataforseoProperties.java`:

```java
package com.synapsedx.mailing.unitelegal2dataforseo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("unitelegal2dataforseo")
public record Unitelegal2DataforseoProperties(String inputCsv, String outputYml) {}
```

- [ ] **Step 4: Create `QueryDefaults`**

Create `unitelegal2dataforseo/src/main/java/com/synapsedx/mailing/unitelegal2dataforseo/config/QueryDefaults.java`:

```java
package com.synapsedx.mailing.unitelegal2dataforseo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("query-defaults")
public record QueryDefaults(
    String languageCode,
    int depth,
    int locationCode,
    String locationName,
    String filePrefix) {}
```

- [ ] **Step 5: Create Spring Boot application class**

Create `unitelegal2dataforseo/src/main/java/com/synapsedx/mailing/unitelegal2dataforseo/Unitelegal2DataforseoApplication.java`:

```java
package com.synapsedx.mailing.unitelegal2dataforseo;

import com.synapsedx.mailing.unitelegal2dataforseo.config.QueryDefaults;
import com.synapsedx.mailing.unitelegal2dataforseo.config.Unitelegal2DataforseoProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({Unitelegal2DataforseoProperties.class, QueryDefaults.class})
public class Unitelegal2DataforseoApplication {

  public static void main(String[] args) {
    SpringApplication.run(Unitelegal2DataforseoApplication.class, args);
  }
}
```

- [ ] **Step 6: Create `application.yml`**

Create `unitelegal2dataforseo/src/main/resources/application.yml`:

```yaml
spring:
  config:
    import: optional:classpath:query-defaults.yml
  datasource:
    url: jdbc:h2:mem:batchdb
  batch:
    job:
      enabled: true
    jdbc:
      initialize-schema: always

unitelegal2dataforseo:
  input-csv: ../sources/insee/echantillon.csv
  output-yml: output/dataforseo-queries.yml
```

- [ ] **Step 7: Create `query-defaults.yml`**

Create `unitelegal2dataforseo/src/main/resources/query-defaults.yml`:

```yaml
query-defaults:
  language-code: fr
  depth: 2
  location-code: 2250
  location-name: France
  file-prefix: assurance-fr
```

- [ ] **Step 8: Run test to verify it passes**

Run: `./mvnw -pl unitelegal2dataforseo test -q`
Expected: PASS — context loads, both `@ConfigurationProperties` records populated.

- [ ] **Step 9: Commit**

```bash
git add unitelegal2dataforseo/src
git commit -m "feat(unitelegal2dataforseo): add Spring Boot app + config properties"
```

---

## Task 3: Domain records

**Files:**
- Create: `unitelegal2dataforseo/src/main/java/com/synapsedx/mailing/unitelegal2dataforseo/model/InseeUniteLegale.java`
- Create: `unitelegal2dataforseo/src/main/java/com/synapsedx/mailing/unitelegal2dataforseo/model/KeywordBatch.java`
- Create: `unitelegal2dataforseo/src/main/java/com/synapsedx/mailing/unitelegal2dataforseo/model/DataForSeoQuery.java`

- [ ] **Step 1: Create `InseeUniteLegale` record**

Create `unitelegal2dataforseo/src/main/java/com/synapsedx/mailing/unitelegal2dataforseo/model/InseeUniteLegale.java`:

```java
package com.synapsedx.mailing.unitelegal2dataforseo.model;

public record InseeUniteLegale(
    String siren,
    String sigle,
    String denomination,
    String denominationUsuelle1,
    String denominationUsuelle2,
    String denominationUsuelle3) {}
```

- [ ] **Step 2: Create `KeywordBatch` record**

Create `unitelegal2dataforseo/src/main/java/com/synapsedx/mailing/unitelegal2dataforseo/model/KeywordBatch.java`:

```java
package com.synapsedx.mailing.unitelegal2dataforseo.model;

import java.util.List;

public record KeywordBatch(String siren, List<String> keywords) {}
```

- [ ] **Step 3: Create `DataForSeoQuery` record**

Mirror the field naming of `seo-news-search` `SearchQuery` (snake_case via `@JsonProperty`) so the produced YAML is byte-compatible with the existing format consumed downstream.

Create `unitelegal2dataforseo/src/main/java/com/synapsedx/mailing/unitelegal2dataforseo/model/DataForSeoQuery.java`:

```java
package com.synapsedx.mailing.unitelegal2dataforseo.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder({"keyword", "language_code", "depth", "location_code", "location_name", "file_prefix"})
public record DataForSeoQuery(
    String keyword,
    @JsonProperty("language_code") String languageCode,
    int depth,
    @JsonProperty("location_code") int locationCode,
    @JsonProperty("location_name") String locationName,
    @JsonProperty("file_prefix") String filePrefix) {}
```

- [ ] **Step 4: Verify compile**

Run: `./mvnw -pl unitelegal2dataforseo compile -q`
Expected: BUILD SUCCESS.

- [ ] **Step 5: Commit**

```bash
git add unitelegal2dataforseo/src/main/java/com/synapsedx/mailing/unitelegal2dataforseo/model
git commit -m "feat(unitelegal2dataforseo): add domain records"
```

---

## Task 4: `KeywordDedupProcessor` — unit tests + implementation

**Files:**
- Create: `unitelegal2dataforseo/src/main/java/com/synapsedx/mailing/unitelegal2dataforseo/batch/processor/KeywordDedupProcessor.java`
- Test: `unitelegal2dataforseo/src/test/java/com/synapsedx/mailing/unitelegal2dataforseo/batch/processor/KeywordDedupProcessorTest.java`

- [ ] **Step 1: Write the failing test class**

Create `unitelegal2dataforseo/src/test/java/com/synapsedx/mailing/unitelegal2dataforseo/batch/processor/KeywordDedupProcessorTest.java`:

```java
package com.synapsedx.mailing.unitelegal2dataforseo.batch.processor;

import static org.assertj.core.api.Assertions.assertThat;

import com.synapsedx.mailing.unitelegal2dataforseo.model.InseeUniteLegale;
import org.junit.jupiter.api.Test;

class KeywordDedupProcessorTest {

  private final KeywordDedupProcessor processor = new KeywordDedupProcessor();

  @Test
  void dropsSigleWhenIncludedInDenomination() throws Exception {
    var row = new InseeUniteLegale(
        "054501754", "SET", "SET - HUILLIER - SOCIETE D'ENTREPOSAGE ET DE TRANSPORTS",
        "", "", "");

    var result = processor.process(row);

    assertThat(result).isNotNull();
    assertThat(result.siren()).isEqualTo("054501754");
    assertThat(result.keywords())
        .containsExactly("SET - HUILLIER - SOCIETE D'ENTREPOSAGE ET DE TRANSPORTS");
  }

  @Test
  void keepsSigleAndDenominationWhenDistinct() throws Exception {
    var row = new InseeUniteLegale(
        "111111111", "EDF", "ELECTRICITE DE FRANCE", null, null, null);

    var result = processor.process(row);

    assertThat(result.keywords())
        .containsExactlyInAnyOrder("EDF", "ELECTRICITE DE FRANCE");
  }

  @Test
  void singleDenominationWhenSigleBlank() throws Exception {
    var row = new InseeUniteLegale(
        "016750697", "", "ETS J VIRLY S A", "", "", "");

    var result = processor.process(row);

    assertThat(result.keywords()).containsExactly("ETS J VIRLY S A");
  }

  @Test
  void returnsNullWhenAllColumnsBlank() throws Exception {
    var row = new InseeUniteLegale("999999999", "", "", null, "  ", "\t");

    var result = processor.process(row);

    assertThat(result).isNull();
  }

  @Test
  void caseInsensitiveSubstringMatch() throws Exception {
    var row = new InseeUniteLegale(
        "222222222", "edf", "ELECTRICITE DE FRANCE EDF", null, null, null);

    var result = processor.process(row);

    assertThat(result.keywords()).containsExactly("ELECTRICITE DE FRANCE EDF");
  }

  @Test
  void trimsWhitespace() throws Exception {
    var row = new InseeUniteLegale(
        "333333333", "  ABC  ", "  XYZ COMPANY  ", null, null, null);

    var result = processor.process(row);

    assertThat(result.keywords()).containsExactlyInAnyOrder("ABC", "XYZ COMPANY");
  }

  @Test
  void deduplicatesAcrossUsuelleColumns() throws Exception {
    var row = new InseeUniteLegale(
        "444444444",
        "ACME",
        "ACME CORPORATION",
        "ACME",
        "ACME CORP",
        "GLOBAL HOLDINGS");

    var result = processor.process(row);

    // "ACME CORPORATION" kept first (longest), absorbs "ACME", "ACME CORP".
    // "GLOBAL HOLDINGS" kept (distinct).
    assertThat(result.keywords())
        .containsExactlyInAnyOrder("ACME CORPORATION", "GLOBAL HOLDINGS");
  }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./mvnw -pl unitelegal2dataforseo test -q -Dtest=KeywordDedupProcessorTest`
Expected: FAIL — `KeywordDedupProcessor` class does not exist (compilation error).

- [ ] **Step 3: Implement `KeywordDedupProcessor`**

Create `unitelegal2dataforseo/src/main/java/com/synapsedx/mailing/unitelegal2dataforseo/batch/processor/KeywordDedupProcessor.java`:

```java
package com.synapsedx.mailing.unitelegal2dataforseo.batch.processor;

import com.synapsedx.mailing.unitelegal2dataforseo.model.InseeUniteLegale;
import com.synapsedx.mailing.unitelegal2dataforseo.model.KeywordBatch;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class KeywordDedupProcessor implements ItemProcessor<InseeUniteLegale, KeywordBatch> {

  @Override
  public KeywordBatch process(InseeUniteLegale row) {
    var nonBlank = Stream.of(
            row.sigle(),
            row.denomination(),
            row.denominationUsuelle1(),
            row.denominationUsuelle2(),
            row.denominationUsuelle3())
        .filter(v -> v != null && !v.isBlank())
        .map(String::trim)
        .sorted(Comparator.comparingInt(String::length).reversed())
        .toList();

    var kept = new ArrayList<String>();
    for (var v : nonBlank) {
      var upper = v.toUpperCase(Locale.ROOT);
      boolean alreadyIncluded =
          kept.stream().anyMatch(k -> k.toUpperCase(Locale.ROOT).contains(upper));
      if (!alreadyIncluded) {
        kept.add(v);
      }
    }

    if (kept.isEmpty()) {
      log.debug("dedup_skipped siren={} reason=all_columns_blank", row.siren());
      return null;
    }
    return new KeywordBatch(row.siren(), List.copyOf(kept));
  }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./mvnw -pl unitelegal2dataforseo test -q -Dtest=KeywordDedupProcessorTest`
Expected: PASS — all 7 tests green.

- [ ] **Step 5: Commit**

```bash
git add unitelegal2dataforseo/src/main/java/com/synapsedx/mailing/unitelegal2dataforseo/batch/processor unitelegal2dataforseo/src/test/java/com/synapsedx/mailing/unitelegal2dataforseo/batch/processor
git commit -m "feat(unitelegal2dataforseo): add KeywordDedupProcessor with substring dedup"
```

---

## Task 5: `InseeCsvReader` — unit test + FlatFileItemReader wrapper

**Files:**
- Create: `unitelegal2dataforseo/src/main/java/com/synapsedx/mailing/unitelegal2dataforseo/batch/reader/InseeCsvReader.java`
- Create: `unitelegal2dataforseo/src/test/resources/echantillon-mini.csv`
- Test: `unitelegal2dataforseo/src/test/java/com/synapsedx/mailing/unitelegal2dataforseo/batch/reader/InseeCsvReaderTest.java`

- [ ] **Step 1: Create test CSV fixture**

Create `unitelegal2dataforseo/src/test/resources/echantillon-mini.csv` — header + 3 rows; the rows mirror the 35-column schema of `sources/insee/echantillon.csv` exactly so the reader is exercised with realistic data:

```csv
siren,statutDiffusionUniteLegale,unitePurgeeUniteLegale,dateCreationUniteLegale,sigleUniteLegale,sexeUniteLegale,prenom1UniteLegale,prenom2UniteLegale,prenom3UniteLegale,prenom4UniteLegale,prenomUsuelUniteLegale,pseudonymeUniteLegale,identifiantAssociationUniteLegale,trancheEffectifsUniteLegale,anneeEffectifsUniteLegale,dateDernierTraitementUniteLegale,nombrePeriodesUniteLegale,categorieEntreprise,anneeCategorieEntreprise,dateDebut,etatAdministratifUniteLegale,nomUniteLegale,nomUsageUniteLegale,denominationUniteLegale,denominationUsuelle1UniteLegale,denominationUsuelle2UniteLegale,denominationUsuelle3UniteLegale,categorieJuridiqueUniteLegale,activitePrincipaleUniteLegale,nomenclatureActivitePrincipaleUniteLegale,nicSiegeUniteLegale,economieSocialeSolidaireUniteLegale,societeMissionUniteLegale,caractereEmployeurUniteLegale,activitePrincipaleNAF25UniteLegale
016750697,O,,1967-01-01,,,,,,,,,,31,2023,2025-12-06T05:25:38,7,ETI,2023,2022-10-01,A,,,ETS J VIRLY S A,,,,5710,77.39Z,NAFRev2,00050,N,,,77.39Y
054501754,O,,1954-01-01,SET,,,,,,,,,12,2023,2025-12-06T08:35:21,5,ETI,2023,2008-01-01,A,,,SET - HUILLIER - SOCIETE D'ENTREPOSAGE ET DE TRANSPORTS,,,,5699,64.20Z,NAFRev2,00011,N,,,64.21Y
099999999,O,,2000-01-01,EDF,,,,,,,,,53,2023,2025-12-06T08:35:21,5,GE,2023,2008-01-01,A,,,ELECTRICITE DE FRANCE,EDF,,,5699,35.11Z,NAFRev2,00001,N,,,35.11Z
```

- [ ] **Step 2: Write the failing reader test**

Create `unitelegal2dataforseo/src/test/java/com/synapsedx/mailing/unitelegal2dataforseo/batch/reader/InseeCsvReaderTest.java`:

```java
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
            new InseeUniteLegale(
                "099999999", "EDF", "ELECTRICITE DE FRANCE", "EDF", "", ""));
    assertThat(row4).isNull();
  }
}
```

- [ ] **Step 3: Run test to verify it fails**

Run: `./mvnw -pl unitelegal2dataforseo test -q -Dtest=InseeCsvReaderTest`
Expected: FAIL — `InseeCsvReader` not found (compilation error).

- [ ] **Step 4: Implement `InseeCsvReader`**

Create `unitelegal2dataforseo/src/main/java/com/synapsedx/mailing/unitelegal2dataforseo/batch/reader/InseeCsvReader.java`:

```java
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
```

- [ ] **Step 5: Run test to verify it passes**

Run: `./mvnw -pl unitelegal2dataforseo test -q -Dtest=InseeCsvReaderTest`
Expected: PASS — 4 reads return the 3 expected rows then `null`.

- [ ] **Step 6: Commit**

```bash
git add unitelegal2dataforseo/src/main/java/com/synapsedx/mailing/unitelegal2dataforseo/batch/reader unitelegal2dataforseo/src/test/java/com/synapsedx/mailing/unitelegal2dataforseo/batch/reader unitelegal2dataforseo/src/test/resources/echantillon-mini.csv
git commit -m "feat(unitelegal2dataforseo): add InseeCsvReader (FlatFileItemReader wrapper)"
```

---

## Task 6: `DataForSeoYamlWriter` — unit tests + ItemStreamWriter implementation

**Files:**
- Create: `unitelegal2dataforseo/src/main/java/com/synapsedx/mailing/unitelegal2dataforseo/batch/writer/DataForSeoYamlWriter.java`
- Test: `unitelegal2dataforseo/src/test/java/com/synapsedx/mailing/unitelegal2dataforseo/batch/writer/DataForSeoYamlWriterTest.java`

- [ ] **Step 1: Write the failing writer tests**

Create `unitelegal2dataforseo/src/test/java/com/synapsedx/mailing/unitelegal2dataforseo/batch/writer/DataForSeoYamlWriterTest.java`:

```java
package com.synapsedx.mailing.unitelegal2dataforseo.batch.writer;

import static org.assertj.core.api.Assertions.assertThat;

import com.synapsedx.mailing.unitelegal2dataforseo.config.QueryDefaults;
import com.synapsedx.mailing.unitelegal2dataforseo.config.Unitelegal2DataforseoProperties;
import com.synapsedx.mailing.unitelegal2dataforseo.model.KeywordBatch;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ExecutionContext;

class DataForSeoYamlWriterTest {

  @TempDir Path tempDir;

  private DataForSeoYamlWriter newWriter(Path outputYml) {
    var props = new Unitelegal2DataforseoProperties("ignored.csv", outputYml.toString());
    var defaults = new QueryDefaults("fr", 2, 2250, "France", "assurance-fr");
    return new DataForSeoYamlWriter(props, defaults);
  }

  @Test
  void writesYamlMatchingDataforseoQueriesFormat() throws Exception {
    var output = tempDir.resolve("queries.yml");
    var writer = newWriter(output);
    writer.open(new ExecutionContext());

    writer.write(
        new Chunk<>(
            List.of(
                new KeywordBatch("016750697", List.of("ETS J VIRLY S A")),
                new KeywordBatch("099999999", List.of("EDF", "ELECTRICITE DE FRANCE")))));
    writer.close();

    var content = Files.readString(output);
    assertThat(content)
        .startsWith("queries:")
        .contains("- keyword: \"ETS J VIRLY S A\"")
        .contains("language_code: \"fr\"")
        .contains("depth: 2")
        .contains("location_code: 2250")
        .contains("location_name: \"France\"")
        .contains("file_prefix: \"assurance-fr\"")
        .contains("- keyword: \"EDF\"")
        .contains("- keyword: \"ELECTRICITE DE FRANCE\"")
        .doesNotStartWith("---");
  }

  @Test
  void appendsAcrossMultipleChunks() throws Exception {
    var output = tempDir.resolve("queries.yml");
    var writer = newWriter(output);
    writer.open(new ExecutionContext());

    writer.write(new Chunk<>(List.of(new KeywordBatch("1", List.of("FIRST")))));
    writer.write(new Chunk<>(List.of(new KeywordBatch("2", List.of("SECOND")))));
    writer.close();

    var content = Files.readString(output);
    assertThat(content)
        .containsSubsequence("queries:", "- keyword: \"FIRST\"", "- keyword: \"SECOND\"");
  }

  @Test
  void createsOutputDirectoryIfMissing() throws Exception {
    var output = tempDir.resolve("nested/sub/queries.yml");
    var writer = newWriter(output);
    writer.open(new ExecutionContext());
    writer.write(new Chunk<>(List.of(new KeywordBatch("1", List.of("X")))));
    writer.close();

    assertThat(Files.exists(output)).isTrue();
  }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./mvnw -pl unitelegal2dataforseo test -q -Dtest=DataForSeoYamlWriterTest`
Expected: FAIL — `DataForSeoYamlWriter` not found (compilation error).

- [ ] **Step 3: Implement `DataForSeoYamlWriter`**

The writer:
- Opens the file in `open()`, writes the literal `queries:\n` header, keeps an `OutputStreamWriter` for the duration of the step.
- In `write()`: for each `KeywordBatch`, for each `keyword`, builds a `DataForSeoQuery` (applying defaults) and serializes it with Jackson YAML, then prefixes the resulting block with `  - ` (YAML list item) so it nests under `queries:`.

Create `unitelegal2dataforseo/src/main/java/com/synapsedx/mailing/unitelegal2dataforseo/batch/writer/DataForSeoYamlWriter.java`:

```java
package com.synapsedx.mailing.unitelegal2dataforseo.batch.writer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.synapsedx.mailing.unitelegal2dataforseo.config.QueryDefaults;
import com.synapsedx.mailing.unitelegal2dataforseo.config.Unitelegal2DataforseoProperties;
import com.synapsedx.mailing.unitelegal2dataforseo.model.DataForSeoQuery;
import com.synapsedx.mailing.unitelegal2dataforseo.model.KeywordBatch;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.batch.item.ItemStreamWriter;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataForSeoYamlWriter implements ItemStreamWriter<KeywordBatch> {

  private final Unitelegal2DataforseoProperties properties;
  private final QueryDefaults defaults;

  private final ObjectMapper yaml =
      new ObjectMapper(
              new YAMLFactory()
                  .disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER)
                  .enable(YAMLGenerator.Feature.MINIMIZE_QUOTES)
                  .configure(YAMLGenerator.Feature.SPLIT_LINES, false))
          .findAndRegisterModules();

  private BufferedWriter out;
  private int written;

  @Override
  public void open(ExecutionContext executionContext) throws ItemStreamException {
    try {
      var path = Path.of(properties.outputYml());
      if (path.getParent() != null) {
        Files.createDirectories(path.getParent());
      }
      out = Files.newBufferedWriter(path, StandardCharsets.UTF_8);
      out.write("queries:\n");
      written = 0;
      log.info("yaml_output_opened path={}", path.toAbsolutePath());
    } catch (IOException e) {
      throw new ItemStreamException("Unable to open output YAML: " + properties.outputYml(), e);
    }
  }

  @Override
  public void write(Chunk<? extends KeywordBatch> chunk) throws Exception {
    for (var batch : chunk.getItems()) {
      for (var keyword : batch.keywords()) {
        var query =
            new DataForSeoQuery(
                keyword,
                defaults.languageCode(),
                defaults.depth(),
                defaults.locationCode(),
                defaults.locationName(),
                defaults.filePrefix());
        writeListItem(query);
        written++;
      }
    }
  }

  private void writeListItem(DataForSeoQuery query) throws IOException {
    var block = yaml.writeValueAsString(query);
    // Jackson YAML emits each field on its own line. Prefix first line with "  - " then "    " for the rest.
    var lines = block.split("\n");
    var sb = new StringBuilder();
    for (int i = 0; i < lines.length; i++) {
      if (lines[i].isBlank()) {
        continue;
      }
      sb.append(i == 0 ? "  - " : "    ").append(lines[i]).append('\n');
    }
    out.write(sb.toString());
  }

  @Override
  public void update(ExecutionContext executionContext) throws ItemStreamException {
    try {
      if (out != null) {
        out.flush();
      }
    } catch (IOException e) {
      throw new ItemStreamException("Flush failed", e);
    }
  }

  @Override
  public void close() throws ItemStreamException {
    try {
      if (out != null) {
        out.close();
        log.info("yaml_output_closed total_queries={}", written);
        out = null;
      }
    } catch (IOException e) {
      throw new ItemStreamException("Close failed", e);
    }
  }
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `./mvnw -pl unitelegal2dataforseo test -q -Dtest=DataForSeoYamlWriterTest`
Expected: PASS — 3 tests green.

- [ ] **Step 5: Commit**

```bash
git add unitelegal2dataforseo/src/main/java/com/synapsedx/mailing/unitelegal2dataforseo/batch/writer unitelegal2dataforseo/src/test/java/com/synapsedx/mailing/unitelegal2dataforseo/batch/writer
git commit -m "feat(unitelegal2dataforseo): add DataForSeoYamlWriter (streaming YAML output)"
```

---

## Task 7: `Unitelegal2DataforseoJobConfig` — wire reader/processor/writer into a Job

**Files:**
- Create: `unitelegal2dataforseo/src/main/java/com/synapsedx/mailing/unitelegal2dataforseo/batch/Unitelegal2DataforseoJobConfig.java`

- [ ] **Step 1: Create the JobConfig**

Create `unitelegal2dataforseo/src/main/java/com/synapsedx/mailing/unitelegal2dataforseo/batch/Unitelegal2DataforseoJobConfig.java`:

```java
package com.synapsedx.mailing.unitelegal2dataforseo.batch;

import com.synapsedx.mailing.unitelegal2dataforseo.batch.processor.KeywordDedupProcessor;
import com.synapsedx.mailing.unitelegal2dataforseo.batch.reader.InseeCsvReader;
import com.synapsedx.mailing.unitelegal2dataforseo.batch.writer.DataForSeoYamlWriter;
import com.synapsedx.mailing.unitelegal2dataforseo.model.InseeUniteLegale;
import com.synapsedx.mailing.unitelegal2dataforseo.model.KeywordBatch;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@RequiredArgsConstructor
public class Unitelegal2DataforseoJobConfig {

  private final JobRepository jobRepository;
  private final PlatformTransactionManager transactionManager;
  private final InseeCsvReader inseeCsvReader;
  private final KeywordDedupProcessor keywordDedupProcessor;
  private final DataForSeoYamlWriter dataForSeoYamlWriter;

  @Bean
  public Job unitelegal2dataforseoJob() {
    return new JobBuilder("unitelegal2dataforseo", jobRepository).start(convertStep()).build();
  }

  @Bean
  public Step convertStep() {
    return new StepBuilder("convertStep", jobRepository)
        .<InseeUniteLegale, KeywordBatch>chunk(100, transactionManager)
        .reader(inseeCsvReader)
        .processor(keywordDedupProcessor)
        .writer(dataForSeoYamlWriter)
        .build();
  }
}
```

- [ ] **Step 2: Run full test suite to verify wiring**

Run: `./mvnw -pl unitelegal2dataforseo test -q`
Expected: PASS — all previous tests still green, no new failures.

- [ ] **Step 3: Commit**

```bash
git add unitelegal2dataforseo/src/main/java/com/synapsedx/mailing/unitelegal2dataforseo/batch/Unitelegal2DataforseoJobConfig.java
git commit -m "feat(unitelegal2dataforseo): wire convert job (reader/processor/writer)"
```

---

## Task 8: End-to-end job integration test

**Files:**
- Create: `unitelegal2dataforseo/src/test/java/com/synapsedx/mailing/unitelegal2dataforseo/Unitelegal2DataforseoJobIT.java`

- [ ] **Step 1: Write the failing end-to-end test**

Uses `spring-batch-test`'s `JobLauncherTestUtils` (already declared in Task 1 pom). The test points `unitelegal2dataforseo.input-csv` at the fixture CSV from Task 5 and writes the output YAML to a JUnit `@TempDir`, then asserts the file is valid YAML and matches expected entries.

Create `unitelegal2dataforseo/src/test/java/com/synapsedx/mailing/unitelegal2dataforseo/Unitelegal2DataforseoJobIT.java`:

```java
package com.synapsedx.mailing.unitelegal2dataforseo;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBatchTest
@SpringBootTest
class Unitelegal2DataforseoJobIT {

  @TempDir static Path tempDir;

  @DynamicPropertySource
  static void overrideProps(DynamicPropertyRegistry registry) throws Exception {
    var csv = new ClassPathResource("echantillon-mini.csv").getFile().getAbsolutePath();
    registry.add("unitelegal2dataforseo.input-csv", () -> csv);
    registry.add(
        "unitelegal2dataforseo.output-yml", () -> tempDir.resolve("queries.yml").toString());
    registry.add("spring.batch.job.enabled", () -> "false");
  }

  @Autowired JobLauncherTestUtils jobLauncherTestUtils;

  @Test
  void runsEndToEndAndProducesExpectedYaml() throws Exception {
    var execution = jobLauncherTestUtils.launchJob();
    assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

    var output = tempDir.resolve("queries.yml");
    assertThat(output).exists();

    var mapper = new ObjectMapper(new YAMLFactory());
    @SuppressWarnings("unchecked")
    var parsed = mapper.readValue(Files.newInputStream(output), Map.class);
    @SuppressWarnings("unchecked")
    var queries = (List<Map<String, Object>>) parsed.get("queries");

    assertThat(queries).hasSize(3);
    assertThat(queries.get(0))
        .containsEntry("keyword", "ETS J VIRLY S A")
        .containsEntry("language_code", "fr")
        .containsEntry("depth", 2)
        .containsEntry("location_code", 2250)
        .containsEntry("location_name", "France")
        .containsEntry("file_prefix", "assurance-fr");
    assertThat(queries.get(1))
        .containsEntry("keyword", "SET - HUILLIER - SOCIETE D'ENTREPOSAGE ET DE TRANSPORTS");
    // Row 3 has two distinct keywords (EDF, ELECTRICITE DE FRANCE) — both kept.
    assertThat(queries)
        .extracting(q -> q.get("keyword"))
        .contains("EDF", "ELECTRICITE DE FRANCE");
  }
}
```

- [ ] **Step 2: Run test to verify it fails first (sanity check)**

Run: `./mvnw -pl unitelegal2dataforseo test -q -Dtest=Unitelegal2DataforseoJobIT`
Expected: It should actually PASS if Tasks 1–7 were implemented correctly. If it fails, inspect the produced YAML at `target/test-output/queries.yml` (via a temporary `System.out.println(Files.readString(output))`) — most likely the YAML indentation is off in the writer.

If failing only because of YAML indent mismatch, adjust `DataForSeoYamlWriter.writeListItem` until Jackson's parse round-trips it cleanly. Re-run.

- [ ] **Step 3: Commit**

```bash
git add unitelegal2dataforseo/src/test/java/com/synapsedx/mailing/unitelegal2dataforseo/Unitelegal2DataforseoJobIT.java
git commit -m "test(unitelegal2dataforseo): add end-to-end job integration test"
```

---

## Task 9: Taskfile for local runs

**Files:**
- Create: `unitelegal2dataforseo/Taskfile.yml`

- [ ] **Step 1: Create the Taskfile**

Create `unitelegal2dataforseo/Taskfile.yml`:

```yaml
version: "3"

vars:
  MVN_EXEC: "../mvnw"

tasks:
  default:
    desc: Convert ../sources/insee/echantillon.csv into output/dataforseo-queries.yml
    cmd: '{{.MVN_EXEC}} spring-boot:run'

  run:
    desc: "Convert a custom CSV; pass INPUT_CSV and OUTPUT_YML"
    cmd: |
      {{.MVN_EXEC}} spring-boot:run \
        -Dspring-boot.run.arguments="--unitelegal2dataforseo.input-csv={{.INPUT_CSV}} --unitelegal2dataforseo.output-yml={{.OUTPUT_YML}}"
    requires:
      vars: [INPUT_CSV, OUTPUT_YML]

  build:
    desc: Build the jar
    cmd: "{{.MVN_EXEC}} package -DskipTests"

  test:
    desc: Run unit + integration tests
    cmd: "{{.MVN_EXEC}} test"

  clean:
    desc: Remove build artifacts and output files
    cmds:
      - "{{.MVN_EXEC}} clean"
      - rm -rf output/
```

- [ ] **Step 2: Commit**

```bash
git add unitelegal2dataforseo/Taskfile.yml
git commit -m "chore(unitelegal2dataforseo): add Taskfile"
```

---

## Task 10: Smoke run on real `echantillon.csv` + verify output

**No files created — just validation.**

- [ ] **Step 1: Build the whole project to catch cross-module regressions**

Run: `./mvnw -q -DskipTests package`
Expected: BUILD SUCCESS for all 4 modules.

- [ ] **Step 2: Run the job on the real sample CSV**

From the `unitelegal2dataforseo/` directory:
```bash
cd unitelegal2dataforseo
task default
```

Expected:
- Logs: `yaml_output_opened path=…/unitelegal2dataforseo/output/dataforseo-queries.yml`
- Logs: `yaml_output_closed total_queries=N` where N ≥ 4 (echantillon.csv has 9 data rows; expect ~9–12 queries depending on EDF-style splits)
- Spring Batch step `convertStep` finished COMPLETED.

- [ ] **Step 2 alternative if `task` is not installed**

```bash
../mvnw -pl unitelegal2dataforseo spring-boot:run
```

- [ ] **Step 3: Inspect produced YAML manually**

Read: `unitelegal2dataforseo/output/dataforseo-queries.yml`

Verify by visual diff against `seo-news-search/src/main/resources/dataforseo-queries.yml`:
- Starts with `queries:` (no `---`)
- Each item begins with `  - keyword: "..."`
- Subsequent fields indented 4 spaces
- All 5 default fields present with the configured values (`fr`, `2`, `2250`, `France`, `assurance-fr`)

- [ ] **Step 4: Round-trip parse with the downstream reader (sanity check)**

Drop the produced file into a Jackson YAML parser to confirm it deserializes into `QueryList` (the model `seo-news-search` consumes):

```bash
../mvnw -pl seo-news-search test -q -Dtest=YamlQueryReaderTest
```

If the existing test still passes after temporarily symlinking/copying our output over `seo-news-search/src/test/resources/dataforseo-queries.yml`, the format is fully compatible. (Not required — visual diff is sufficient.)

- [ ] **Step 5: Commit (only if there were any small fixes during smoke run)**

If steps 1–4 surfaced any tweaks, commit them:
```bash
git status
git add -p <files>
git commit -m "fix(unitelegal2dataforseo): <describe>"
```

Otherwise skip — Task 10 is purely validation.

---

## Done

The module is complete when:
- All tests pass: `./mvnw -pl unitelegal2dataforseo test -q`
- Whole-project build clean: `./mvnw package -DskipTests`
- `task default` produces a valid `dataforseo-queries.yml` that visually matches the format of `seo-news-search/src/main/resources/dataforseo-queries.yml`.