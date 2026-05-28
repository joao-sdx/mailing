# company-domain-lookup Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a new standalone Spring Batch module `company-domain-lookup` that reads a contacts CSV produced by `seo-news-parse`, deduplicates company names, looks up each company's official domain via DataForSEO Google organic SERP + LM Studio LLM filter, and writes an enriched CSV with all original columns plus a `domain` column.

**Architecture:** Two-step Spring Batch job. Step 1 emits each unique company once, looks up the domain (DataForSEO call → LLM filter), and stores `companyKey → domain` in a Spring-managed in-memory map. Step 2 streams the original CSV row-by-row, joins the domain from the map, and writes the enriched CSV. The job is header-driven (locates `company` column by name) so it works with both the current 4-column CSV and a future 5-column variant.

**Tech Stack:** Java 21 records, Spring Boot 3.4.5, Spring Batch, Jackson, Lombok (`@Slf4j`, `@RequiredArgsConstructor`), JUnit 5, AssertJ, WireMock (new test dependency), H2 (test job repository).

**Reference spec:** `docs/superpowers/specs/2026-05-28-company-domain-lookup-design.md`

---

## Task 1: Module bootstrap

**Files:**
- Create: `company-domain-lookup/pom.xml`
- Modify: `pom.xml` (root, add to `<modules>`)
- Create: `company-domain-lookup/src/main/java/com/synapsedx/mailing/companydomain/CompanyDomainLookupApplication.java`
- Create: `company-domain-lookup/src/main/resources/application.yml`
- Create: `company-domain-lookup/src/test/java/com/synapsedx/mailing/companydomain/CompanyDomainLookupApplicationTest.java`

- [ ] **Step 1: Create the module pom.xml**

`company-domain-lookup/pom.xml`:

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd">
  <modelVersion>4.0.0</modelVersion>
  <artifactId>company-domain-lookup</artifactId>

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
    <dependency>
      <groupId>com.github.tomakehurst</groupId>
      <artifactId>wiremock-jre8-standalone</artifactId>
      <version>3.0.1</version>
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

- [ ] **Step 2: Register module in root pom.xml**

Edit `pom.xml` (root), find `<modules>` section (line 8–13). Change:

```xml
<modules>
    <module>mailing-pipeline</module>
    <module>seo-news-search</module>
    <module>seo-news-parse</module>
    <module>unitelegal2dataforseo</module>
</modules>
```

to:

```xml
<modules>
    <module>mailing-pipeline</module>
    <module>seo-news-search</module>
    <module>seo-news-parse</module>
    <module>unitelegal2dataforseo</module>
    <module>company-domain-lookup</module>
</modules>
```

- [ ] **Step 3: Create the Spring Boot application class**

`company-domain-lookup/src/main/java/com/synapsedx/mailing/companydomain/CompanyDomainLookupApplication.java`:

```java
package com.synapsedx.mailing.companydomain;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class CompanyDomainLookupApplication {

  public static void main(String[] args) {
    SpringApplication.run(CompanyDomainLookupApplication.class, args);
  }
}
```

- [ ] **Step 4: Create application.yml**

`company-domain-lookup/src/main/resources/application.yml`:

```yaml
spring:
  config:
    import: optional:file:.env[.properties]
  batch:
    job:
      enabled: true
    jdbc:
      initialize-schema: always

dataforseo:
  api:
    user: ${dataforseo.api.user}
    key: ${dataforseo.api.key}

lmstudio:
  server: http://127.0.0.1:1234
  model: nvidia/nemotron-3-nano-4b
  key: ${openai.key:lm-studio}
  connect-timeout-seconds: 10
  request-timeout-seconds: 60

company-domain:
  input-csv: input/contacts.csv
  output-csv: output/contacts-with-domain.csv
  serp-depth: 10
  serp-top-n: 5
```

- [ ] **Step 5: Write context-loads smoke test**

`company-domain-lookup/src/test/java/com/synapsedx/mailing/companydomain/CompanyDomainLookupApplicationTest.java`:

```java
package com.synapsedx.mailing.companydomain;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(
    properties = {
      "dataforseo.api.user=test",
      "dataforseo.api.key=test",
      "spring.batch.job.enabled=false"
    })
class CompanyDomainLookupApplicationTest {

  @Test
  void contextLoads() {}
}
```

- [ ] **Step 6: Run the test — expected to FAIL (no @ConfigurationProperties classes yet, Spring will still boot but only if no bean wiring is broken)**

Run: `./mvnw -pl company-domain-lookup test -Dtest=CompanyDomainLookupApplicationTest`
Expected: PASS (no beans require properties yet; the test only verifies the Spring context starts).

If it fails, fix the issue before continuing.

- [ ] **Step 7: Commit**

```bash
git add pom.xml company-domain-lookup/
git commit -m "feat(company-domain-lookup): bootstrap module with Spring Boot skeleton"
```

---

## Task 2: Configuration properties

**Files:**
- Create: `company-domain-lookup/src/main/java/com/synapsedx/mailing/companydomain/config/CompanyDomainProperties.java`
- Create: `company-domain-lookup/src/main/java/com/synapsedx/mailing/companydomain/config/DataForSeoProperties.java`
- Create: `company-domain-lookup/src/main/java/com/synapsedx/mailing/companydomain/config/LmStudioProperties.java`
- Create: `company-domain-lookup/src/test/java/com/synapsedx/mailing/companydomain/config/PropertiesBindingTest.java`

- [ ] **Step 1: Write the failing test**

`company-domain-lookup/src/test/java/com/synapsedx/mailing/companydomain/config/PropertiesBindingTest.java`:

```java
package com.synapsedx.mailing.companydomain.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(
    properties = {
      "dataforseo.api.user=u",
      "dataforseo.api.key=k",
      "lmstudio.server=http://x:1",
      "lmstudio.model=m",
      "lmstudio.key=lm-studio",
      "lmstudio.connect-timeout-seconds=5",
      "lmstudio.request-timeout-seconds=15",
      "company-domain.input-csv=in.csv",
      "company-domain.output-csv=out.csv",
      "company-domain.serp-depth=20",
      "company-domain.serp-top-n=7",
      "spring.batch.job.enabled=false"
    })
class PropertiesBindingTest {

  @Autowired DataForSeoProperties dataforseo;
  @Autowired LmStudioProperties lmstudio;
  @Autowired CompanyDomainProperties companyDomain;

  @Test
  void bindsAllProperties() {
    assertThat(dataforseo.api().user()).isEqualTo("u");
    assertThat(dataforseo.api().key()).isEqualTo("k");
    assertThat(lmstudio.server()).isEqualTo("http://x:1");
    assertThat(lmstudio.model()).isEqualTo("m");
    assertThat(lmstudio.connectTimeoutSeconds()).isEqualTo(5);
    assertThat(lmstudio.requestTimeoutSeconds()).isEqualTo(15);
    assertThat(companyDomain.inputCsv()).isEqualTo("in.csv");
    assertThat(companyDomain.outputCsv()).isEqualTo("out.csv");
    assertThat(companyDomain.serpDepth()).isEqualTo(20);
    assertThat(companyDomain.serpTopN()).isEqualTo(7);
  }
}
```

- [ ] **Step 2: Run test — expected to FAIL (classes do not exist)**

Run: `./mvnw -pl company-domain-lookup test -Dtest=PropertiesBindingTest`
Expected: compilation error — `DataForSeoProperties`, `LmStudioProperties`, `CompanyDomainProperties` not found.

- [ ] **Step 3: Implement the three property records**

`company-domain-lookup/src/main/java/com/synapsedx/mailing/companydomain/config/DataForSeoProperties.java`:

```java
package com.synapsedx.mailing.companydomain.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("dataforseo")
public record DataForSeoProperties(Api api) {
  public record Api(String user, String key) {}
}
```

`company-domain-lookup/src/main/java/com/synapsedx/mailing/companydomain/config/LmStudioProperties.java`:

```java
package com.synapsedx.mailing.companydomain.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("lmstudio")
public record LmStudioProperties(
    String server,
    String model,
    String key,
    int connectTimeoutSeconds,
    int requestTimeoutSeconds) {}
```

`company-domain-lookup/src/main/java/com/synapsedx/mailing/companydomain/config/CompanyDomainProperties.java`:

```java
package com.synapsedx.mailing.companydomain.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("company-domain")
public record CompanyDomainProperties(
    String inputCsv, String outputCsv, int serpDepth, int serpTopN) {}
```

- [ ] **Step 4: Run test — expected to PASS**

Run: `./mvnw -pl company-domain-lookup test -Dtest=PropertiesBindingTest`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add company-domain-lookup/src/main/java/com/synapsedx/mailing/companydomain/config/ \
        company-domain-lookup/src/test/java/com/synapsedx/mailing/companydomain/config/
git commit -m "feat(company-domain-lookup): add @ConfigurationProperties records"
```

---

## Task 3: Domain model records

**Files:**
- Create: `company-domain-lookup/src/main/java/com/synapsedx/mailing/companydomain/model/SerpResult.java`
- Create: `company-domain-lookup/src/main/java/com/synapsedx/mailing/companydomain/model/CompanyDomain.java`
- Create: `company-domain-lookup/src/main/java/com/synapsedx/mailing/companydomain/model/ContactRow.java`
- Create: `company-domain-lookup/src/main/java/com/synapsedx/mailing/companydomain/model/EnrichedContactRow.java`

These are pure records with no behavior — no test required.

- [ ] **Step 1: Create SerpResult**

`company-domain-lookup/src/main/java/com/synapsedx/mailing/companydomain/model/SerpResult.java`:

```java
package com.synapsedx.mailing.companydomain.model;

public record SerpResult(String title, String url, String snippet) {}
```

- [ ] **Step 2: Create CompanyDomain**

`company-domain-lookup/src/main/java/com/synapsedx/mailing/companydomain/model/CompanyDomain.java`:

```java
package com.synapsedx.mailing.companydomain.model;

public record CompanyDomain(String companyKey, String domain) {}
```

- [ ] **Step 3: Create ContactRow**

`company-domain-lookup/src/main/java/com/synapsedx/mailing/companydomain/model/ContactRow.java`:

```java
package com.synapsedx.mailing.companydomain.model;

import java.util.List;

public record ContactRow(List<String> headers, List<String> values, String company) {}
```

- [ ] **Step 4: Create EnrichedContactRow**

`company-domain-lookup/src/main/java/com/synapsedx/mailing/companydomain/model/EnrichedContactRow.java`:

```java
package com.synapsedx.mailing.companydomain.model;

public record EnrichedContactRow(ContactRow contact, String domain) {}
```

- [ ] **Step 5: Verify it compiles**

Run: `./mvnw -pl company-domain-lookup test-compile`
Expected: BUILD SUCCESS.

- [ ] **Step 6: Commit**

```bash
git add company-domain-lookup/src/main/java/com/synapsedx/mailing/companydomain/model/
git commit -m "feat(company-domain-lookup): add domain model records"
```

---

## Task 4: CSV line parser utility

A small CSV parser inverting the project's `escapeCsv` rules — handles quoted fields containing commas, doubled quotes, and embedded newlines for **single-line** records. Embedded newlines inside quotes are out of scope for the reader, which reads line-by-line; the parser handles them only when given a pre-assembled string with `\n` inside quotes (so the helper is correct in isolation).

**Files:**
- Create: `company-domain-lookup/src/main/java/com/synapsedx/mailing/companydomain/csv/CsvLineParser.java`
- Create: `company-domain-lookup/src/test/java/com/synapsedx/mailing/companydomain/csv/CsvLineParserTest.java`

- [ ] **Step 1: Write the failing test**

`company-domain-lookup/src/test/java/com/synapsedx/mailing/companydomain/csv/CsvLineParserTest.java`:

```java
package com.synapsedx.mailing.companydomain.csv;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CsvLineParserTest {

  @Test
  void parsesPlainFields() {
    assertThat(CsvLineParser.parse("a,b,c")).containsExactly("a", "b", "c");
  }

  @Test
  void parsesQuotedFieldWithComma() {
    assertThat(CsvLineParser.parse("a,\"b,c\",d")).containsExactly("a", "b,c", "d");
  }

  @Test
  void parsesDoubledQuoteInsideQuotedField() {
    assertThat(CsvLineParser.parse("a,\"b\"\"c\",d")).containsExactly("a", "b\"c", "d");
  }

  @Test
  void parsesEmptyFields() {
    assertThat(CsvLineParser.parse("a,,c")).containsExactly("a", "", "c");
  }

  @Test
  void parsesTrailingEmptyField() {
    assertThat(CsvLineParser.parse("a,b,")).containsExactly("a", "b", "");
  }

  @Test
  void parsesUnicode() {
    assertThat(CsvLineParser.parse("Beñat,Cazanave,ARTZAINAK,result-10-01.md"))
        .containsExactly("Beñat", "Cazanave", "ARTZAINAK", "result-10-01.md");
  }
}
```

- [ ] **Step 2: Run test — expected to FAIL (class missing)**

Run: `./mvnw -pl company-domain-lookup test -Dtest=CsvLineParserTest`
Expected: compilation error.

- [ ] **Step 3: Implement the parser**

`company-domain-lookup/src/main/java/com/synapsedx/mailing/companydomain/csv/CsvLineParser.java`:

```java
package com.synapsedx.mailing.companydomain.csv;

import java.util.ArrayList;
import java.util.List;

public final class CsvLineParser {

  private CsvLineParser() {}

  public static List<String> parse(String line) {
    var fields = new ArrayList<String>();
    var current = new StringBuilder();
    var inQuotes = false;
    for (var i = 0; i < line.length(); i++) {
      var c = line.charAt(i);
      if (inQuotes) {
        if (c == '"') {
          if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
            current.append('"');
            i++;
          } else {
            inQuotes = false;
          }
        } else {
          current.append(c);
        }
      } else {
        if (c == ',') {
          fields.add(current.toString());
          current.setLength(0);
        } else if (c == '"' && current.length() == 0) {
          inQuotes = true;
        } else {
          current.append(c);
        }
      }
    }
    fields.add(current.toString());
    return fields;
  }
}
```

- [ ] **Step 4: Run test — expected to PASS**

Run: `./mvnw -pl company-domain-lookup test -Dtest=CsvLineParserTest`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add company-domain-lookup/src/main/java/com/synapsedx/mailing/companydomain/csv/ \
        company-domain-lookup/src/test/java/com/synapsedx/mailing/companydomain/csv/
git commit -m "feat(company-domain-lookup): add CSV line parser utility"
```

---

## Task 5: UniqueCompanyReader (Step 1 reader)

Reads the input CSV once, locates the `company` column by header name, dedups case-insensitively (trim + uppercase), preserves first-seen order, emits original-case company names.

**Files:**
- Create: `company-domain-lookup/src/main/java/com/synapsedx/mailing/companydomain/batch/reader/UniqueCompanyReader.java`
- Create: `company-domain-lookup/src/test/java/com/synapsedx/mailing/companydomain/batch/reader/UniqueCompanyReaderTest.java`
- Create test fixture: `company-domain-lookup/src/test/resources/fixtures/contacts-sample.csv`

- [ ] **Step 1: Create the test fixture**

`company-domain-lookup/src/test/resources/fixtures/contacts-sample.csv`:

```
first_name,last_name,company,article_id
Beñat,Cazanave,ARTZAINAK,result-10-01.md
Philippe,Mutin,Factofrance,result-10-01.md
Marc,Tyan,Factofrance,result-10-02.md
Isabelle,Gautier,Crédit Mutuel Alliance Fédérale,result-10-03.md
Jean,Test, factofrance ,result-10-04.md
Empty,Company,,result-10-05.md
```

- [ ] **Step 2: Write the failing test**

`company-domain-lookup/src/test/java/com/synapsedx/mailing/companydomain/batch/reader/UniqueCompanyReaderTest.java`:

```java
package com.synapsedx.mailing.companydomain.batch.reader;

import static org.assertj.core.api.Assertions.assertThat;

import com.synapsedx.mailing.companydomain.config.CompanyDomainProperties;
import java.util.ArrayList;
import org.junit.jupiter.api.Test;

class UniqueCompanyReaderTest {

  @Test
  void emitsUniqueCompaniesPreservingFirstSeenOrder() throws Exception {
    var props =
        new CompanyDomainProperties(
            "src/test/resources/fixtures/contacts-sample.csv", "out.csv", 10, 5);
    var reader = new UniqueCompanyReader(props);

    var seen = new ArrayList<String>();
    String next;
    while ((next = reader.read()) != null) {
      seen.add(next);
    }

    assertThat(seen)
        .containsExactly("ARTZAINAK", "Factofrance", "Crédit Mutuel Alliance Fédérale");
  }

  @Test
  void failsFastWhenCompanyColumnMissing() {
    var props =
        new CompanyDomainProperties(
            "src/test/resources/fixtures/no-company-header.csv", "out.csv", 10, 5);
    var reader = new UniqueCompanyReader(props);
    org.assertj.core.api.Assertions.assertThatThrownBy(reader::read)
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("company");
  }
}
```

- [ ] **Step 3: Create the no-company-header fixture**

`company-domain-lookup/src/test/resources/fixtures/no-company-header.csv`:

```
first_name,last_name,article_id
A,B,c
```

- [ ] **Step 4: Run test — expected to FAIL (reader missing)**

Run: `./mvnw -pl company-domain-lookup test -Dtest=UniqueCompanyReaderTest`
Expected: compilation error.

- [ ] **Step 5: Implement UniqueCompanyReader**

`company-domain-lookup/src/main/java/com/synapsedx/mailing/companydomain/batch/reader/UniqueCompanyReader.java`:

```java
package com.synapsedx.mailing.companydomain.batch.reader;

import com.synapsedx.mailing.companydomain.config.CompanyDomainProperties;
import com.synapsedx.mailing.companydomain.csv.CsvLineParser;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemReader;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class UniqueCompanyReader implements ItemReader<String> {

  private final CompanyDomainProperties properties;
  private Iterator<String> iterator;

  @Override
  public String read() throws Exception {
    if (iterator == null) {
      iterator = loadUniqueCompanies().iterator();
    }
    return iterator.hasNext() ? iterator.next() : null;
  }

  private List<String> loadUniqueCompanies() throws Exception {
    var path = Path.of(properties.inputCsv());
    var lines = Files.readAllLines(path);
    if (lines.isEmpty()) {
      throw new IllegalStateException("input CSV is empty: " + path);
    }
    var headers = CsvLineParser.parse(lines.get(0));
    var companyIdx = headers.indexOf("company");
    if (companyIdx < 0) {
      throw new IllegalStateException(
          "input CSV missing 'company' column; headers=" + headers + " file=" + path);
    }

    var unique = new ArrayList<String>();
    var seenKeys = new HashSet<String>();
    for (var i = 1; i < lines.size(); i++) {
      var fields = CsvLineParser.parse(lines.get(i));
      if (companyIdx >= fields.size()) {
        continue;
      }
      var raw = fields.get(companyIdx);
      var key = raw.trim().toUpperCase(Locale.ROOT);
      if (key.isEmpty()) {
        continue;
      }
      if (seenKeys.add(key)) {
        unique.add(raw.trim());
      }
    }
    log.info("unique_companies_loaded file={} count={}", path.getFileName(), unique.size());
    return unique;
  }
}
```

Note on test expectation: the third unique company `" factofrance "` (with leading/trailing space) has the same key as `Factofrance` after trim+upper, so it's deduped. `Empty,Company,,...` has empty company key and is skipped. Final emitted list: `[ARTZAINAK, Factofrance, Crédit Mutuel Alliance Fédérale]` — matching the test.

- [ ] **Step 6: Run test — expected to PASS**

Run: `./mvnw -pl company-domain-lookup test -Dtest=UniqueCompanyReaderTest`
Expected: PASS (both methods).

- [ ] **Step 7: Commit**

```bash
git add company-domain-lookup/src/main/java/com/synapsedx/mailing/companydomain/batch/reader/UniqueCompanyReader.java \
        company-domain-lookup/src/test/java/com/synapsedx/mailing/companydomain/batch/reader/UniqueCompanyReaderTest.java \
        company-domain-lookup/src/test/resources/fixtures/
git commit -m "feat(company-domain-lookup): add UniqueCompanyReader with header-driven dedup"
```

---

## Task 6: DataForSeoSerpClient

Copy of the auth + post pattern from `seo-news-search`'s `DataForSeoClient`, but pointing at the organic SERP endpoint. No unit test for the HTTP call; covered by the integration test in Task 14. Mirrors the upstream pattern, which also has no unit test.

**Files:**
- Create: `company-domain-lookup/src/main/java/com/synapsedx/mailing/companydomain/client/DataForSeoSerpClient.java`

- [ ] **Step 1: Implement the client**

`company-domain-lookup/src/main/java/com/synapsedx/mailing/companydomain/client/DataForSeoSerpClient.java`:

```java
package com.synapsedx.mailing.companydomain.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.synapsedx.mailing.companydomain.config.DataForSeoProperties;
import com.synapsedx.mailing.companydomain.model.SerpResult;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataForSeoSerpClient {

  @Value(
      "${dataforseo.serp-organic-endpoint:https://api.dataforseo.com/v3/serp/google/organic/live/advanced}")
  private String organicEndpoint;

  private final DataForSeoProperties properties;
  private final ObjectMapper objectMapper = new ObjectMapper();
  private final HttpClient httpClient =
      HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build();

  public List<SerpResult> searchOrganic(String company, int depth) throws Exception {
    var body =
        objectMapper.writeValueAsString(
            List.of(
                Map.of(
                    "keyword", company,
                    "language_code", "fr",
                    "depth", depth,
                    "location_code", 2250,
                    "location_name", "France")));
    var raw = post(organicEndpoint, body);
    var items =
        objectMapper.readTree(raw).path("tasks").path(0).path("result").path(0).path("items");
    var results = new ArrayList<SerpResult>();
    for (var item : items) {
      if (!"organic".equals(item.path("type").asText(""))) {
        continue;
      }
      var url = item.path("url").asText(null);
      if (url == null) {
        continue;
      }
      results.add(
          new SerpResult(
              item.path("title").asText(""),
              url,
              item.path("description").asText("")));
    }
    log.info("dataforseo_serp_organic company={} count={}", company, results.size());
    return results;
  }

  private String post(String endpoint, String body) throws Exception {
    var credentials = properties.api().user() + ":" + properties.api().key();
    var auth = Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
    var request =
        HttpRequest.newBuilder()
            .uri(URI.create(endpoint))
            .header("Authorization", "Basic " + auth)
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build();
    var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    log.debug("dataforseo_post endpoint={} status={}", endpoint, response.statusCode());
    if (response.statusCode() != 200) {
      throw new IllegalStateException("DataForSEO error status=" + response.statusCode());
    }
    return response.body();
  }
}
```

For testability against WireMock in Task 14, the endpoint will be overridden via reflection in the IT (see Task 14). No production seam is added.

- [ ] **Step 2: Verify compilation**

Run: `./mvnw -pl company-domain-lookup test-compile`
Expected: BUILD SUCCESS.

- [ ] **Step 3: Commit**

```bash
git add company-domain-lookup/src/main/java/com/synapsedx/mailing/companydomain/client/DataForSeoSerpClient.java
git commit -m "feat(company-domain-lookup): add DataForSEO organic SERP client"
```

---

## Task 7: LmStudioClient + prompt resource + extractHost helper

The LLM client posts a single-purpose JSON request and returns the picked domain. `extractHost` is a pure helper for normalising URLs to bare hosts — tested separately.

**Files:**
- Create: `company-domain-lookup/src/main/resources/domain-pick-prompt.md`
- Create: `company-domain-lookup/src/main/java/com/synapsedx/mailing/companydomain/client/LmStudioClient.java`
- Create: `company-domain-lookup/src/main/java/com/synapsedx/mailing/companydomain/util/Domains.java`
- Create: `company-domain-lookup/src/test/java/com/synapsedx/mailing/companydomain/util/DomainsTest.java`

- [ ] **Step 1: Write the failing test for extractHost**

`company-domain-lookup/src/test/java/com/synapsedx/mailing/companydomain/util/DomainsTest.java`:

```java
package com.synapsedx.mailing.companydomain.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class DomainsTest {

  @Test
  void extractsHostFromFullUrl() {
    assertThat(Domains.extractHost("https://www.factofrance.com/contact"))
        .isEqualTo("factofrance.com");
  }

  @Test
  void stripsLeadingWww() {
    assertThat(Domains.extractHost("https://www.example.org")).isEqualTo("example.org");
  }

  @Test
  void acceptsBareHost() {
    assertThat(Domains.extractHost("factofrance.com")).isEqualTo("factofrance.com");
  }

  @Test
  void lowercasesHost() {
    assertThat(Domains.extractHost("https://Example.COM/")).isEqualTo("example.com");
  }

  @Test
  void returnsEmptyForBlankInput() {
    assertThat(Domains.extractHost("")).isEqualTo("");
    assertThat(Domains.extractHost(null)).isEqualTo("");
  }

  @Test
  void returnsEmptyForUnparseableInput() {
    assertThat(Domains.extractHost("not a url at all")).isEqualTo("");
  }
}
```

- [ ] **Step 2: Run test — expected to FAIL (class missing)**

Run: `./mvnw -pl company-domain-lookup test -Dtest=DomainsTest`
Expected: compilation error.

- [ ] **Step 3: Implement Domains.extractHost**

`company-domain-lookup/src/main/java/com/synapsedx/mailing/companydomain/util/Domains.java`:

```java
package com.synapsedx.mailing.companydomain.util;

import java.net.URI;
import java.util.Locale;

public final class Domains {

  private Domains() {}

  public static String extractHost(String input) {
    if (input == null || input.isBlank()) {
      return "";
    }
    var trimmed = input.trim();
    if (!trimmed.contains("://")) {
      trimmed = "https://" + trimmed;
    }
    try {
      var uri = URI.create(trimmed);
      var host = uri.getHost();
      if (host == null || host.isBlank()) {
        return "";
      }
      if (host.startsWith("www.")) {
        host = host.substring(4);
      }
      return host.toLowerCase(Locale.ROOT);
    } catch (IllegalArgumentException e) {
      return "";
    }
  }
}
```

- [ ] **Step 4: Run test — expected to PASS**

Run: `./mvnw -pl company-domain-lookup test -Dtest=DomainsTest`
Expected: PASS (all six methods).

- [ ] **Step 5: Create the prompt resource**

`company-domain-lookup/src/main/resources/domain-pick-prompt.md`:

```
Tu reçois le nom d'une compagnie et une liste de résultats Google.
Renvoie UNIQUEMENT un JSON {"domain": "<domaine>"} où <domaine> est le
domaine racine (ex: "factofrance.com") du site officiel de cette compagnie
parmi les résultats. Si aucun résultat ne correspond à un site officiel
(annuaire, presse, agrégateur, réseau social), renvoie {"domain": null}.

Compagnie: {{company}}

Résultats:
{{results}}
```

- [ ] **Step 6: Implement LmStudioClient**

`company-domain-lookup/src/main/java/com/synapsedx/mailing/companydomain/client/LmStudioClient.java`:

```java
package com.synapsedx.mailing.companydomain.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.synapsedx.mailing.companydomain.config.LmStudioProperties;
import com.synapsedx.mailing.companydomain.model.SerpResult;
import jakarta.annotation.PostConstruct;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class LmStudioClient {

  private final LmStudioProperties properties;
  private final ObjectMapper mapper = new ObjectMapper();

  private HttpClient httpClient;
  private String promptTemplate;

  @PostConstruct
  void init() throws Exception {
    httpClient =
        HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(properties.connectTimeoutSeconds()))
            .build();
    promptTemplate =
        new ClassPathResource("domain-pick-prompt.md").getContentAsString(StandardCharsets.UTF_8);
  }

  public Optional<String> pickOfficialDomain(String company, List<SerpResult> results) {
    try {
      var rendered =
          promptTemplate
              .replace("{{company}}", company)
              .replace("{{results}}", renderResults(results));

      var messages = mapper.createArrayNode();
      messages.add(mapper.createObjectNode().put("role", "user").put("content", rendered));

      var requestNode = mapper.createObjectNode();
      requestNode.put("model", properties.model());
      requestNode.set("messages", messages);
      requestNode.put("temperature", 0);
      requestNode.put("max_tokens", 200);
      requestNode.set(
          "response_format", mapper.createObjectNode().put("type", "json_object"));

      var rawResponse = post(mapper.writeValueAsString(requestNode));
      var content =
          mapper
              .readTree(rawResponse)
              .path("choices")
              .path(0)
              .path("message")
              .path("content")
              .asText("");
      if (content.isBlank()) {
        log.warn("llm_empty_response company={}", company);
        return Optional.empty();
      }
      var domainNode = mapper.readTree(content).path("domain");
      if (domainNode.isMissingNode() || domainNode.isNull()) {
        return Optional.empty();
      }
      var domain = domainNode.asText("");
      return domain.isBlank() ? Optional.empty() : Optional.of(domain);
    } catch (Exception e) {
      log.warn("llm_pick_failed company={} reason={}", company, e.getMessage());
      return Optional.empty();
    }
  }

  private String renderResults(List<SerpResult> results) {
    var sb = new StringBuilder();
    for (var r : results) {
      sb.append("- titre: ").append(r.title()).append("\n");
      sb.append("  url: ").append(r.url()).append("\n");
      sb.append("  description: ").append(r.snippet()).append("\n");
    }
    return sb.toString();
  }

  private String post(String body) throws Exception {
    var request =
        HttpRequest.newBuilder()
            .uri(URI.create(properties.server().replaceAll("/+$", "") + "/v1/chat/completions"))
            .header("Authorization", "Bearer " + properties.key())
            .header("Content-Type", "application/json")
            .timeout(Duration.ofSeconds(properties.requestTimeoutSeconds()))
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build();
    var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    log.debug("llm_call status={}", response.statusCode());
    if (response.statusCode() != 200) {
      throw new IllegalStateException("LM Studio error status=" + response.statusCode());
    }
    return response.body();
  }
}
```

- [ ] **Step 7: Verify compilation**

Run: `./mvnw -pl company-domain-lookup test-compile`
Expected: BUILD SUCCESS.

- [ ] **Step 8: Commit**

```bash
git add company-domain-lookup/src/main/java/com/synapsedx/mailing/companydomain/client/LmStudioClient.java \
        company-domain-lookup/src/main/java/com/synapsedx/mailing/companydomain/util/ \
        company-domain-lookup/src/main/resources/domain-pick-prompt.md \
        company-domain-lookup/src/test/java/com/synapsedx/mailing/companydomain/util/
git commit -m "feat(company-domain-lookup): add LM Studio client + extractHost helper"
```

---

## Task 8: CompanyDomainMap support bean + writer

The map is a thin Spring bean wrapping a `ConcurrentHashMap<String,String>`. The writer just puts entries.

**Files:**
- Create: `company-domain-lookup/src/main/java/com/synapsedx/mailing/companydomain/batch/support/CompanyDomainMap.java`
- Create: `company-domain-lookup/src/main/java/com/synapsedx/mailing/companydomain/batch/writer/CompanyDomainMapWriter.java`
- Create: `company-domain-lookup/src/test/java/com/synapsedx/mailing/companydomain/batch/writer/CompanyDomainMapWriterTest.java`

- [ ] **Step 1: Write the failing test**

`company-domain-lookup/src/test/java/com/synapsedx/mailing/companydomain/batch/writer/CompanyDomainMapWriterTest.java`:

```java
package com.synapsedx.mailing.companydomain.batch.writer;

import static org.assertj.core.api.Assertions.assertThat;

import com.synapsedx.mailing.companydomain.batch.support.CompanyDomainMap;
import com.synapsedx.mailing.companydomain.model.CompanyDomain;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.batch.item.Chunk;

class CompanyDomainMapWriterTest {

  @Test
  void writesEachDomainToMap() throws Exception {
    var map = new CompanyDomainMap();
    var writer = new CompanyDomainMapWriter(map);

    writer.write(
        new Chunk<>(
            List.of(
                new CompanyDomain("FACTOFRANCE", "factofrance.com"),
                new CompanyDomain("ARTZAINAK", ""))));

    assertThat(map.get("FACTOFRANCE")).isEqualTo("factofrance.com");
    assertThat(map.get("ARTZAINAK")).isEqualTo("");
    assertThat(map.get("UNKNOWN")).isEqualTo("");
  }
}
```

- [ ] **Step 2: Run test — expected to FAIL (classes missing)**

Run: `./mvnw -pl company-domain-lookup test -Dtest=CompanyDomainMapWriterTest`
Expected: compilation error.

- [ ] **Step 3: Implement CompanyDomainMap**

`company-domain-lookup/src/main/java/com/synapsedx/mailing/companydomain/batch/support/CompanyDomainMap.java`:

```java
package com.synapsedx.mailing.companydomain.batch.support;

import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class CompanyDomainMap {

  private final ConcurrentHashMap<String, String> entries = new ConcurrentHashMap<>();

  public void put(String key, String domain) {
    entries.put(key, domain == null ? "" : domain);
  }

  public String get(String key) {
    return entries.getOrDefault(key, "");
  }
}
```

- [ ] **Step 4: Implement CompanyDomainMapWriter**

`company-domain-lookup/src/main/java/com/synapsedx/mailing/companydomain/batch/writer/CompanyDomainMapWriter.java`:

```java
package com.synapsedx.mailing.companydomain.batch.writer;

import com.synapsedx.mailing.companydomain.batch.support.CompanyDomainMap;
import com.synapsedx.mailing.companydomain.model.CompanyDomain;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CompanyDomainMapWriter implements ItemWriter<CompanyDomain> {

  private final CompanyDomainMap map;

  @Override
  public void write(Chunk<? extends CompanyDomain> chunk) {
    for (var item : chunk.getItems()) {
      map.put(item.companyKey(), item.domain());
    }
  }
}
```

- [ ] **Step 5: Run test — expected to PASS**

Run: `./mvnw -pl company-domain-lookup test -Dtest=CompanyDomainMapWriterTest`
Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add company-domain-lookup/src/main/java/com/synapsedx/mailing/companydomain/batch/support/ \
        company-domain-lookup/src/main/java/com/synapsedx/mailing/companydomain/batch/writer/CompanyDomainMapWriter.java \
        company-domain-lookup/src/test/java/com/synapsedx/mailing/companydomain/batch/writer/CompanyDomainMapWriterTest.java
git commit -m "feat(company-domain-lookup): add in-memory company→domain map + writer"
```

---

## Task 9: DomainLookupProcessor (Step 1 processor)

Maps `company → CompanyDomain` by calling DataForSEO then LM Studio. Applies a 500 ms throttle (2 req/s) before each DataForSEO call. Swallows exceptions to keep the job alive.

**Files:**
- Create: `company-domain-lookup/src/main/java/com/synapsedx/mailing/companydomain/batch/processor/DomainLookupProcessor.java`
- Create: `company-domain-lookup/src/test/java/com/synapsedx/mailing/companydomain/batch/processor/DomainLookupProcessorTest.java`

- [ ] **Step 1: Write the failing test**

`company-domain-lookup/src/test/java/com/synapsedx/mailing/companydomain/batch/processor/DomainLookupProcessorTest.java`:

```java
package com.synapsedx.mailing.companydomain.batch.processor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.synapsedx.mailing.companydomain.client.DataForSeoSerpClient;
import com.synapsedx.mailing.companydomain.client.LmStudioClient;
import com.synapsedx.mailing.companydomain.config.CompanyDomainProperties;
import com.synapsedx.mailing.companydomain.model.SerpResult;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class DomainLookupProcessorTest {

  private final CompanyDomainProperties props =
      new CompanyDomainProperties("in.csv", "out.csv", 10, 5);

  @Test
  void emptySerpReturnsEmptyDomain() throws Exception {
    var serp = mock(DataForSeoSerpClient.class);
    var llm = mock(LmStudioClient.class);
    when(serp.searchOrganic(anyString(), anyInt())).thenReturn(List.of());
    var p = new DomainLookupProcessor(serp, llm, props);
    p.setThrottleMillis(0);

    var result = p.process("Factofrance");

    assertThat(result.companyKey()).isEqualTo("FACTOFRANCE");
    assertThat(result.domain()).isEqualTo("");
  }

  @Test
  void llmEmptyReturnsEmptyDomain() throws Exception {
    var serp = mock(DataForSeoSerpClient.class);
    var llm = mock(LmStudioClient.class);
    when(serp.searchOrganic(eq("Factofrance"), anyInt()))
        .thenReturn(List.of(new SerpResult("t", "https://x.com", "s")));
    when(llm.pickOfficialDomain(eq("Factofrance"), any())).thenReturn(Optional.empty());
    var p = new DomainLookupProcessor(serp, llm, props);
    p.setThrottleMillis(0);

    assertThat(p.process("Factofrance").domain()).isEqualTo("");
  }

  @Test
  void normalisesLlmUrlToBareHost() throws Exception {
    var serp = mock(DataForSeoSerpClient.class);
    var llm = mock(LmStudioClient.class);
    when(serp.searchOrganic(eq("Factofrance"), anyInt()))
        .thenReturn(List.of(new SerpResult("t", "https://www.factofrance.com", "s")));
    when(llm.pickOfficialDomain(eq("Factofrance"), any()))
        .thenReturn(Optional.of("https://www.factofrance.com/contact"));
    var p = new DomainLookupProcessor(serp, llm, props);
    p.setThrottleMillis(0);

    assertThat(p.process("Factofrance").domain()).isEqualTo("factofrance.com");
  }

  @Test
  void serpThrowsReturnsEmptyDomain() throws Exception {
    var serp = mock(DataForSeoSerpClient.class);
    var llm = mock(LmStudioClient.class);
    when(serp.searchOrganic(anyString(), anyInt())).thenThrow(new RuntimeException("boom"));
    var p = new DomainLookupProcessor(serp, llm, props);
    p.setThrottleMillis(0);

    var result = p.process("Factofrance");
    assertThat(result.companyKey()).isEqualTo("FACTOFRANCE");
    assertThat(result.domain()).isEqualTo("");
  }

  @Test
  void topNTruncatesResultsBeforeSendingToLlm() throws Exception {
    var serp = mock(DataForSeoSerpClient.class);
    var llm = mock(LmStudioClient.class);
    when(serp.searchOrganic(eq("X"), anyInt()))
        .thenReturn(
            List.of(
                new SerpResult("a", "https://a.com", ""),
                new SerpResult("b", "https://b.com", ""),
                new SerpResult("c", "https://c.com", ""),
                new SerpResult("d", "https://d.com", ""),
                new SerpResult("e", "https://e.com", ""),
                new SerpResult("f", "https://f.com", "")));
    when(llm.pickOfficialDomain(eq("X"), any())).thenReturn(Optional.empty());

    var smallProps = new CompanyDomainProperties("in.csv", "out.csv", 10, 3);
    var p = new DomainLookupProcessor(serp, llm, smallProps);
    p.setThrottleMillis(0);
    p.process("X");

    org.mockito.ArgumentCaptor<List<SerpResult>> captor =
        org.mockito.ArgumentCaptor.forClass(List.class);
    org.mockito.Mockito.verify(llm).pickOfficialDomain(eq("X"), captor.capture());
    assertThat(captor.getValue()).hasSize(3);
  }
}
```

- [ ] **Step 2: Run test — expected to FAIL (processor missing)**

Run: `./mvnw -pl company-domain-lookup test -Dtest=DomainLookupProcessorTest`
Expected: compilation error.

- [ ] **Step 3: Implement DomainLookupProcessor**

`company-domain-lookup/src/main/java/com/synapsedx/mailing/companydomain/batch/processor/DomainLookupProcessor.java`:

```java
package com.synapsedx.mailing.companydomain.batch.processor;

import com.synapsedx.mailing.companydomain.client.DataForSeoSerpClient;
import com.synapsedx.mailing.companydomain.client.LmStudioClient;
import com.synapsedx.mailing.companydomain.config.CompanyDomainProperties;
import com.synapsedx.mailing.companydomain.model.CompanyDomain;
import com.synapsedx.mailing.companydomain.util.Domains;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DomainLookupProcessor implements ItemProcessor<String, CompanyDomain> {

  private final DataForSeoSerpClient serpClient;
  private final LmStudioClient lmStudioClient;
  private final CompanyDomainProperties properties;

  private long throttleMillis = 500;

  void setThrottleMillis(long ms) {
    this.throttleMillis = ms;
  }

  @Override
  public CompanyDomain process(String company) {
    var key = company.trim().toUpperCase(Locale.ROOT);
    log.info("domain_lookup_start company={}", company);
    try {
      throttle();
      var results = serpClient.searchOrganic(company, properties.serpDepth());
      if (results.isEmpty()) {
        log.info("domain_lookup_done company={} domain=", company);
        return new CompanyDomain(key, "");
      }
      var topN = results.subList(0, Math.min(properties.serpTopN(), results.size()));
      var picked = lmStudioClient.pickOfficialDomain(company, topN);
      var domain = picked.map(Domains::extractHost).orElse("");
      log.info("domain_lookup_done company={} domain={}", company, domain);
      return new CompanyDomain(key, domain);
    } catch (Exception e) {
      log.warn("domain_lookup_failed company={} reason={}", company, e.getMessage());
      return new CompanyDomain(key, "");
    }
  }

  private void throttle() {
    if (throttleMillis <= 0) {
      return;
    }
    try {
      Thread.sleep(throttleMillis);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }
}
```

- [ ] **Step 4: Run test — expected to PASS**

Run: `./mvnw -pl company-domain-lookup test -Dtest=DomainLookupProcessorTest`
Expected: PASS (all five methods).

- [ ] **Step 5: Commit**

```bash
git add company-domain-lookup/src/main/java/com/synapsedx/mailing/companydomain/batch/processor/DomainLookupProcessor.java \
        company-domain-lookup/src/test/java/com/synapsedx/mailing/companydomain/batch/processor/DomainLookupProcessorTest.java
git commit -m "feat(company-domain-lookup): add domain lookup processor (SERP + LLM filter)"
```

---

## Task 10: ContactsCsvReader (Step 2 reader)

Re-reads the CSV row-by-row, emitting `ContactRow` items that carry the headers, the row's values, and the extracted `company` value for downstream lookup.

**Files:**
- Create: `company-domain-lookup/src/main/java/com/synapsedx/mailing/companydomain/batch/reader/ContactsCsvReader.java`
- Create: `company-domain-lookup/src/test/java/com/synapsedx/mailing/companydomain/batch/reader/ContactsCsvReaderTest.java`

- [ ] **Step 1: Write the failing test**

`company-domain-lookup/src/test/java/com/synapsedx/mailing/companydomain/batch/reader/ContactsCsvReaderTest.java`:

```java
package com.synapsedx.mailing.companydomain.batch.reader;

import static org.assertj.core.api.Assertions.assertThat;

import com.synapsedx.mailing.companydomain.config.CompanyDomainProperties;
import com.synapsedx.mailing.companydomain.model.ContactRow;
import java.util.ArrayList;
import org.junit.jupiter.api.Test;

class ContactsCsvReaderTest {

  @Test
  void emitsEveryRowWithHeadersAndCompany() throws Exception {
    var props =
        new CompanyDomainProperties(
            "src/test/resources/fixtures/contacts-sample.csv", "out.csv", 10, 5);
    var reader = new ContactsCsvReader(props);

    var all = new ArrayList<ContactRow>();
    ContactRow next;
    while ((next = reader.read()) != null) {
      all.add(next);
    }

    assertThat(all).hasSize(6);
    var first = all.get(0);
    assertThat(first.headers()).containsExactly("first_name", "last_name", "company", "article_id");
    assertThat(first.values())
        .containsExactly("Beñat", "Cazanave", "ARTZAINAK", "result-10-01.md");
    assertThat(first.company()).isEqualTo("ARTZAINAK");

    var rowWithEmptyCompany = all.get(5);
    assertThat(rowWithEmptyCompany.company()).isEqualTo("");
  }
}
```

- [ ] **Step 2: Run test — expected to FAIL (reader missing)**

Run: `./mvnw -pl company-domain-lookup test -Dtest=ContactsCsvReaderTest`
Expected: compilation error.

- [ ] **Step 3: Implement ContactsCsvReader**

`company-domain-lookup/src/main/java/com/synapsedx/mailing/companydomain/batch/reader/ContactsCsvReader.java`:

```java
package com.synapsedx.mailing.companydomain.batch.reader;

import com.synapsedx.mailing.companydomain.config.CompanyDomainProperties;
import com.synapsedx.mailing.companydomain.csv.CsvLineParser;
import com.synapsedx.mailing.companydomain.model.ContactRow;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemReader;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ContactsCsvReader implements ItemReader<ContactRow> {

  private final CompanyDomainProperties properties;
  private List<String> headers;
  private int companyIdx = -1;
  private Iterator<String> dataLines;

  @Override
  public ContactRow read() throws Exception {
    if (dataLines == null) {
      init();
    }
    if (!dataLines.hasNext()) {
      return null;
    }
    var fields = CsvLineParser.parse(dataLines.next());
    var company =
        companyIdx >= 0 && companyIdx < fields.size() ? fields.get(companyIdx).trim() : "";
    return new ContactRow(headers, fields, company);
  }

  private void init() throws Exception {
    var path = Path.of(properties.inputCsv());
    var lines = Files.readAllLines(path);
    if (lines.isEmpty()) {
      throw new IllegalStateException("input CSV is empty: " + path);
    }
    headers = CsvLineParser.parse(lines.get(0));
    companyIdx = headers.indexOf("company");
    if (companyIdx < 0) {
      throw new IllegalStateException(
          "input CSV missing 'company' column; headers=" + headers + " file=" + path);
    }
    dataLines = lines.subList(1, lines.size()).iterator();
    log.info(
        "contacts_csv_loaded file={} headers={} rows={}",
        path.getFileName(),
        headers,
        lines.size() - 1);
  }
}
```

- [ ] **Step 4: Run test — expected to PASS**

Run: `./mvnw -pl company-domain-lookup test -Dtest=ContactsCsvReaderTest`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add company-domain-lookup/src/main/java/com/synapsedx/mailing/companydomain/batch/reader/ContactsCsvReader.java \
        company-domain-lookup/src/test/java/com/synapsedx/mailing/companydomain/batch/reader/ContactsCsvReaderTest.java
git commit -m "feat(company-domain-lookup): add ContactsCsvReader for enrichment step"
```

---

## Task 11: ContactEnrichProcessor

Looks up the company in `CompanyDomainMap` and produces an `EnrichedContactRow`.

**Files:**
- Create: `company-domain-lookup/src/main/java/com/synapsedx/mailing/companydomain/batch/processor/ContactEnrichProcessor.java`
- Create: `company-domain-lookup/src/test/java/com/synapsedx/mailing/companydomain/batch/processor/ContactEnrichProcessorTest.java`

- [ ] **Step 1: Write the failing test**

`company-domain-lookup/src/test/java/com/synapsedx/mailing/companydomain/batch/processor/ContactEnrichProcessorTest.java`:

```java
package com.synapsedx.mailing.companydomain.batch.processor;

import static org.assertj.core.api.Assertions.assertThat;

import com.synapsedx.mailing.companydomain.batch.support.CompanyDomainMap;
import com.synapsedx.mailing.companydomain.model.ContactRow;
import java.util.List;
import org.junit.jupiter.api.Test;

class ContactEnrichProcessorTest {

  @Test
  void enrichesWithMappedDomainCaseInsensitive() {
    var map = new CompanyDomainMap();
    map.put("FACTOFRANCE", "factofrance.com");
    var processor = new ContactEnrichProcessor(map);

    var row =
        new ContactRow(
            List.of("first_name", "last_name", "company", "article_id"),
            List.of("Philippe", "Mutin", " factofrance ", "r.md"),
            " factofrance ");

    var enriched = processor.process(row);

    assertThat(enriched.contact()).isSameAs(row);
    assertThat(enriched.domain()).isEqualTo("factofrance.com");
  }

  @Test
  void emptyDomainWhenNotInMap() {
    var map = new CompanyDomainMap();
    var processor = new ContactEnrichProcessor(map);
    var row =
        new ContactRow(
            List.of("first_name", "last_name", "company", "article_id"),
            List.of("a", "b", "Unknown", "r.md"),
            "Unknown");

    assertThat(processor.process(row).domain()).isEqualTo("");
  }
}
```

- [ ] **Step 2: Run test — expected to FAIL**

Run: `./mvnw -pl company-domain-lookup test -Dtest=ContactEnrichProcessorTest`
Expected: compilation error.

- [ ] **Step 3: Implement ContactEnrichProcessor**

`company-domain-lookup/src/main/java/com/synapsedx/mailing/companydomain/batch/processor/ContactEnrichProcessor.java`:

```java
package com.synapsedx.mailing.companydomain.batch.processor;

import com.synapsedx.mailing.companydomain.batch.support.CompanyDomainMap;
import com.synapsedx.mailing.companydomain.model.ContactRow;
import com.synapsedx.mailing.companydomain.model.EnrichedContactRow;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ContactEnrichProcessor implements ItemProcessor<ContactRow, EnrichedContactRow> {

  private final CompanyDomainMap map;

  @Override
  public EnrichedContactRow process(ContactRow row) {
    var key = row.company().trim().toUpperCase(Locale.ROOT);
    return new EnrichedContactRow(row, map.get(key));
  }
}
```

- [ ] **Step 4: Run test — expected to PASS**

Run: `./mvnw -pl company-domain-lookup test -Dtest=ContactEnrichProcessorTest`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add company-domain-lookup/src/main/java/com/synapsedx/mailing/companydomain/batch/processor/ContactEnrichProcessor.java \
        company-domain-lookup/src/test/java/com/synapsedx/mailing/companydomain/batch/processor/ContactEnrichProcessorTest.java
git commit -m "feat(company-domain-lookup): add ContactEnrichProcessor"
```

---

## Task 12: EnrichedContactsCsvWriter

Writes the output CSV: header once (original headers + `,domain`), then escaped rows preserving input column order, with `domain` appended.

**Files:**
- Create: `company-domain-lookup/src/main/java/com/synapsedx/mailing/companydomain/batch/writer/EnrichedContactsCsvWriter.java`
- Create: `company-domain-lookup/src/test/java/com/synapsedx/mailing/companydomain/batch/writer/EnrichedContactsCsvWriterTest.java`

- [ ] **Step 1: Write the failing test**

`company-domain-lookup/src/test/java/com/synapsedx/mailing/companydomain/batch/writer/EnrichedContactsCsvWriterTest.java`:

```java
package com.synapsedx.mailing.companydomain.batch.writer;

import static org.assertj.core.api.Assertions.assertThat;

import com.synapsedx.mailing.companydomain.config.CompanyDomainProperties;
import com.synapsedx.mailing.companydomain.model.ContactRow;
import com.synapsedx.mailing.companydomain.model.EnrichedContactRow;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.item.Chunk;

class EnrichedContactsCsvWriterTest {

  @Test
  void writesHeaderOnceAcrossChunksAndAppendsDomain(@TempDir Path tmp) throws Exception {
    var out = tmp.resolve("out.csv");
    var props =
        new CompanyDomainProperties("ignored", out.toString(), 10, 5);
    var writer = new EnrichedContactsCsvWriter(props);
    writer.beforeStep(new StepExecution("step", null));

    var headers = List.of("first_name", "last_name", "company", "article_id");
    writer.write(
        new Chunk<>(
            List.of(
                new EnrichedContactRow(
                    new ContactRow(headers, List.of("A", "B", "Factofrance", "r1.md"),
                        "Factofrance"),
                    "factofrance.com"),
                new EnrichedContactRow(
                    new ContactRow(headers, List.of("C", "D", "Unknown", "r2.md"), "Unknown"),
                    ""))));
    writer.write(
        new Chunk<>(
            List.of(
                new EnrichedContactRow(
                    new ContactRow(
                        headers, List.of("E,F", "G\"H", "Crédit Mutuel", "r3.md"), "Crédit Mutuel"),
                    "creditmutuel.fr"))));

    var lines = Files.readAllLines(out);
    assertThat(lines).hasSize(4);
    assertThat(lines.get(0)).isEqualTo("first_name,last_name,company,article_id,domain");
    assertThat(lines.get(1)).isEqualTo("A,B,Factofrance,r1.md,factofrance.com");
    assertThat(lines.get(2)).isEqualTo("C,D,Unknown,r2.md,");
    assertThat(lines.get(3)).isEqualTo("\"E,F\",\"G\"\"H\",Crédit Mutuel,r3.md,creditmutuel.fr");
  }
}
```

- [ ] **Step 2: Run test — expected to FAIL**

Run: `./mvnw -pl company-domain-lookup test -Dtest=EnrichedContactsCsvWriterTest`
Expected: compilation error.

- [ ] **Step 3: Implement EnrichedContactsCsvWriter**

`company-domain-lookup/src/main/java/com/synapsedx/mailing/companydomain/batch/writer/EnrichedContactsCsvWriter.java`:

```java
package com.synapsedx.mailing.companydomain.batch.writer;

import com.synapsedx.mailing.companydomain.config.CompanyDomainProperties;
import com.synapsedx.mailing.companydomain.model.EnrichedContactRow;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class EnrichedContactsCsvWriter
    implements ItemWriter<EnrichedContactRow>, StepExecutionListener {

  private final CompanyDomainProperties properties;
  private boolean headerWritten = false;

  @Override
  public void beforeStep(StepExecution stepExecution) {
    headerWritten = false;
  }

  @Override
  public void write(Chunk<? extends EnrichedContactRow> chunk) throws Exception {
    if (chunk.getItems().isEmpty()) {
      return;
    }

    var csvPath = Path.of(properties.outputCsv());
    var parent = csvPath.getParent();
    if (parent != null) {
      Files.createDirectories(parent);
    }

    if (!headerWritten) {
      var headers = chunk.getItems().iterator().next().contact().headers();
      var headerLine = String.join(",", headers) + ",domain\n";
      Files.writeString(
          csvPath, headerLine, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
      headerWritten = true;
    }

    var sb = new StringBuilder();
    for (var row : chunk.getItems()) {
      var first = true;
      for (var v : row.contact().values()) {
        if (!first) {
          sb.append(",");
        }
        sb.append(escapeCsv(v));
        first = false;
      }
      sb.append(",").append(escapeCsv(row.domain())).append("\n");
    }
    Files.writeString(csvPath, sb.toString(), StandardOpenOption.APPEND);
    log.info("enriched_csv_written file={} rows={}", csvPath.getFileName(), chunk.size());
  }

  private String escapeCsv(String value) {
    if (value == null || value.isEmpty()) {
      return "";
    }
    if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
      return "\"" + value.replace("\"", "\"\"") + "\"";
    }
    return value;
  }
}
```

- [ ] **Step 4: Run test — expected to PASS**

Run: `./mvnw -pl company-domain-lookup test -Dtest=EnrichedContactsCsvWriterTest`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add company-domain-lookup/src/main/java/com/synapsedx/mailing/companydomain/batch/writer/EnrichedContactsCsvWriter.java \
        company-domain-lookup/src/test/java/com/synapsedx/mailing/companydomain/batch/writer/EnrichedContactsCsvWriterTest.java
git commit -m "feat(company-domain-lookup): add EnrichedContactsCsvWriter"
```

---

## Task 13: Job configuration

Wire the two steps. Step 1 has chunk size 1 (each API-bound item is independent and we want progress per company). Step 2 has chunk size 100 (cheap, file-bound).

**Files:**
- Create: `company-domain-lookup/src/main/java/com/synapsedx/mailing/companydomain/batch/CompanyDomainLookupJobConfig.java`

- [ ] **Step 1: Implement the job config**

`company-domain-lookup/src/main/java/com/synapsedx/mailing/companydomain/batch/CompanyDomainLookupJobConfig.java`:

```java
package com.synapsedx.mailing.companydomain.batch;

import com.synapsedx.mailing.companydomain.batch.processor.ContactEnrichProcessor;
import com.synapsedx.mailing.companydomain.batch.processor.DomainLookupProcessor;
import com.synapsedx.mailing.companydomain.batch.reader.ContactsCsvReader;
import com.synapsedx.mailing.companydomain.batch.reader.UniqueCompanyReader;
import com.synapsedx.mailing.companydomain.batch.writer.CompanyDomainMapWriter;
import com.synapsedx.mailing.companydomain.batch.writer.EnrichedContactsCsvWriter;
import com.synapsedx.mailing.companydomain.model.CompanyDomain;
import com.synapsedx.mailing.companydomain.model.ContactRow;
import com.synapsedx.mailing.companydomain.model.EnrichedContactRow;
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
public class CompanyDomainLookupJobConfig {

  private final JobRepository jobRepository;
  private final PlatformTransactionManager transactionManager;
  private final UniqueCompanyReader uniqueCompanyReader;
  private final DomainLookupProcessor domainLookupProcessor;
  private final CompanyDomainMapWriter companyDomainMapWriter;
  private final ContactsCsvReader contactsCsvReader;
  private final ContactEnrichProcessor contactEnrichProcessor;
  private final EnrichedContactsCsvWriter enrichedContactsCsvWriter;

  @Bean
  public Job companyDomainLookupJob() {
    return new JobBuilder("company-domain-lookup-job", jobRepository)
        .start(resolveDomainsStep())
        .next(enrichContactsStep())
        .build();
  }

  @Bean
  public Step resolveDomainsStep() {
    return new StepBuilder("resolveDomainsStep", jobRepository)
        .<String, CompanyDomain>chunk(1, transactionManager)
        .reader(uniqueCompanyReader)
        .processor(domainLookupProcessor)
        .writer(companyDomainMapWriter)
        .build();
  }

  @Bean
  public Step enrichContactsStep() {
    return new StepBuilder("enrichContactsStep", jobRepository)
        .<ContactRow, EnrichedContactRow>chunk(100, transactionManager)
        .reader(contactsCsvReader)
        .processor(contactEnrichProcessor)
        .writer(enrichedContactsCsvWriter)
        .build();
  }
}
```

- [ ] **Step 2: Run all module tests — expected to PASS**

Run: `./mvnw -pl company-domain-lookup test`
Expected: BUILD SUCCESS, all tests green (context-loads, properties binding, CSV parser, both readers, domain lookup processor, contact enrich processor, both writers, Domains helper).

- [ ] **Step 3: Commit**

```bash
git add company-domain-lookup/src/main/java/com/synapsedx/mailing/companydomain/batch/CompanyDomainLookupJobConfig.java
git commit -m "feat(company-domain-lookup): wire two-step batch job"
```

---

## Task 14: End-to-end integration test with WireMock

Spins up two WireMock servers (one for DataForSEO, one for LM Studio), points the clients at them via Spring `@DynamicPropertySource` (LM Studio) and reflection on the static endpoint constant (DataForSEO), runs the job, asserts the output CSV.

**Files:**
- Create: `company-domain-lookup/src/test/resources/fixtures/it-contacts.csv`
- Create: `company-domain-lookup/src/test/resources/fixtures/dataforseo-organic-factofrance.json`
- Create: `company-domain-lookup/src/test/resources/fixtures/dataforseo-organic-empty.json`
- Create: `company-domain-lookup/src/test/java/com/synapsedx/mailing/companydomain/CompanyDomainLookupJobIT.java`

- [ ] **Step 1: Create the input fixture CSV**

`company-domain-lookup/src/test/resources/fixtures/it-contacts.csv`:

```
first_name,last_name,company,article_id
Philippe,Mutin,Factofrance,r1.md
Marc,Tyan,Factofrance,r2.md
Beñat,Cazanave,ARTZAINAK,r3.md
Isabelle,Gautier,Crédit Mutuel,r4.md
Jean,Test,,r5.md
```

3 unique companies (Factofrance, ARTZAINAK, Crédit Mutuel) — the empty-company row stays in step 2 output with an empty domain.

- [ ] **Step 2: Create the DataForSEO stub responses**

`company-domain-lookup/src/test/resources/fixtures/dataforseo-organic-factofrance.json`:

```json
{
  "tasks": [{
    "result": [{
      "items": [
        {"type": "organic", "title": "Factofrance — Official", "url": "https://www.factofrance.com/", "description": "Official site"},
        {"type": "organic", "title": "LinkedIn", "url": "https://www.linkedin.com/company/factofrance", "description": "LinkedIn page"},
        {"type": "people_also_ask", "title": "ignored", "url": "https://x.com", "description": ""}
      ]
    }]
  }]
}
```

`company-domain-lookup/src/test/resources/fixtures/dataforseo-organic-empty.json`:

```json
{"tasks": [{"result": [{"items": []}]}]}
```

- [ ] **Step 3: Write the IT**

WireMock servers are started in a `static` initializer so their ports are available when `@DynamicPropertySource` runs (Spring evaluates that hook before `@BeforeAll`). The output CSV path is fixed under `target/` so it's writable and predictable.

`company-domain-lookup/src/test/java/com/synapsedx/mailing/companydomain/CompanyDomainLookupJobIT.java`:

```java
package com.synapsedx.mailing.companydomain;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest
@SpringBatchTest
class CompanyDomainLookupJobIT {

  private static final Path OUTPUT_CSV = Path.of("target/it-enriched.csv");
  private static final WireMockServer DATAFORSEO_MOCK =
      new WireMockServer(WireMockConfiguration.options().dynamicPort());
  private static final WireMockServer LMSTUDIO_MOCK =
      new WireMockServer(WireMockConfiguration.options().dynamicPort());

  static {
    DATAFORSEO_MOCK.start();
    LMSTUDIO_MOCK.start();
  }

  @Autowired JobLauncherTestUtils jobLauncherTestUtils;

  @DynamicPropertySource
  static void props(DynamicPropertyRegistry r) {
    r.add("dataforseo.api.user", () -> "u");
    r.add("dataforseo.api.key", () -> "k");
    r.add(
        "dataforseo.serp-organic-endpoint",
        () ->
            "http://127.0.0.1:"
                + DATAFORSEO_MOCK.port()
                + "/v3/serp/google/organic/live/advanced");
    r.add("lmstudio.server", () -> "http://127.0.0.1:" + LMSTUDIO_MOCK.port());
    r.add("company-domain.input-csv", () -> "src/test/resources/fixtures/it-contacts.csv");
    r.add("company-domain.output-csv", () -> OUTPUT_CSV.toString());
    r.add("company-domain.serp-depth", () -> "10");
    r.add("company-domain.serp-top-n", () -> "5");
    r.add("spring.batch.job.enabled", () -> "false");
  }

  @AfterAll
  static void stopMocks() {
    DATAFORSEO_MOCK.stop();
    LMSTUDIO_MOCK.stop();
  }

  @BeforeEach
  void resetMocksAndOutput() throws Exception {
    DATAFORSEO_MOCK.resetAll();
    LMSTUDIO_MOCK.resetAll();
    Files.deleteIfExists(OUTPUT_CSV);
  }

  @Test
  void enrichesContactsWithDomain() throws Exception {
    var factoBody =
        new String(
            new ClassPathResource("fixtures/dataforseo-organic-factofrance.json")
                .getInputStream()
                .readAllBytes(),
            StandardCharsets.UTF_8);
    var emptyBody =
        new String(
            new ClassPathResource("fixtures/dataforseo-organic-empty.json")
                .getInputStream()
                .readAllBytes(),
            StandardCharsets.UTF_8);

    DATAFORSEO_MOCK.stubFor(
        post(urlEqualTo("/v3/serp/google/organic/live/advanced"))
            .withRequestBody(containing("Factofrance"))
            .willReturn(aResponse().withStatus(200).withBody(factoBody)));
    DATAFORSEO_MOCK.stubFor(
        post(urlEqualTo("/v3/serp/google/organic/live/advanced"))
            .withRequestBody(containing("ARTZAINAK"))
            .willReturn(aResponse().withStatus(200).withBody(emptyBody)));
    DATAFORSEO_MOCK.stubFor(
        post(urlEqualTo("/v3/serp/google/organic/live/advanced"))
            .withRequestBody(containing("Mutuel"))
            .willReturn(aResponse().withStatus(200).withBody(emptyBody)));

    LMSTUDIO_MOCK.stubFor(
        post(urlEqualTo("/v1/chat/completions"))
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody(
                        "{\"choices\":[{\"message\":{\"content\":\"{\\\"domain\\\":\\\"https://www.factofrance.com/\\\"}\"}}]}")));

    var execution = jobLauncherTestUtils.launchJob();
    assertThat(execution.getStatus().toString()).isEqualTo("COMPLETED");

    var lines = Files.readAllLines(OUTPUT_CSV);
    assertThat(lines).hasSize(6);
    assertThat(lines.get(0)).isEqualTo("first_name,last_name,company,article_id,domain");
    assertThat(lines.get(1)).isEqualTo("Philippe,Mutin,Factofrance,r1.md,factofrance.com");
    assertThat(lines.get(2)).isEqualTo("Marc,Tyan,Factofrance,r2.md,factofrance.com");
    assertThat(lines.get(3)).isEqualTo("Beñat,Cazanave,ARTZAINAK,r3.md,");
    assertThat(lines.get(4)).isEqualTo("Isabelle,Gautier,Crédit Mutuel,r4.md,");
    assertThat(lines.get(5)).isEqualTo("Jean,Test,,r5.md,");
  }
}
```

- [ ] **Step 4: Run the IT — expected to PASS**

Run: `./mvnw -pl company-domain-lookup test -Dtest=CompanyDomainLookupJobIT`
Expected: PASS. Output CSV exactly matches assertions.

If WireMock fails to start due to port issues, check that no other process is binding the random port (re-run usually resolves it).

- [ ] **Step 5: Run the full module test suite — expected to PASS**

Run: `./mvnw -pl company-domain-lookup test`
Expected: BUILD SUCCESS, all tests green.

- [ ] **Step 6: Commit**

```bash
git add company-domain-lookup/src/test/java/com/synapsedx/mailing/companydomain/CompanyDomainLookupJobIT.java \
        company-domain-lookup/src/test/resources/fixtures/
git commit -m "test(company-domain-lookup): end-to-end IT against WireMock for DataForSEO + LM Studio"
```

---

## Task 15: Taskfile

**Files:**
- Create: `company-domain-lookup/Taskfile.yml`

- [ ] **Step 1: Create the Taskfile**

`company-domain-lookup/Taskfile.yml`:

```yaml
version: "3"

vars:
  MVN_EXEC: "../mvnw"

tasks:
  default:
    desc: Run the company-domain-lookup pipeline
    cmd: '{{.MVN_EXEC}} spring-boot:run'

  run:
    desc: "Run with custom input/output; pass INPUT_CSV and OUTPUT_CSV"
    cmd: |
      {{.MVN_EXEC}} spring-boot:run \
        -Dspring-boot.run.arguments="--company-domain.input-csv={{.INPUT_CSV}} --company-domain.output-csv={{.OUTPUT_CSV}}"
    requires:
      vars: [INPUT_CSV, OUTPUT_CSV]

  build:
    desc: Build the jar
    cmd: "{{.MVN_EXEC}} package -DskipTests"

  test:
    desc: Run unit tests
    cmd: "{{.MVN_EXEC}} test"

  clean:
    desc: Remove build artifacts and output files
    cmds:
      - "{{.MVN_EXEC}} clean"
      - rm -rf output/
```

- [ ] **Step 2: Commit**

```bash
git add company-domain-lookup/Taskfile.yml
git commit -m "chore(company-domain-lookup): add Taskfile with default/run/build/test/clean"
```

---

## Task 16: License headers + full multi-module build

- [ ] **Step 1: Apply license headers**

Run: `task update-license`
Expected: SUCCESS. The license-management task touches the new module's Java files in-place.

- [ ] **Step 2: Full multi-module build**

Run: `./mvnw clean install`
Expected: BUILD SUCCESS across all five modules.

- [ ] **Step 3: Commit license-header changes (if any)**

```bash
git add company-domain-lookup/
git status
# If headers were added/modified:
git commit -m "chore(company-domain-lookup): apply license headers"
```

If `git status` shows no changes after `task update-license`, skip the commit.
