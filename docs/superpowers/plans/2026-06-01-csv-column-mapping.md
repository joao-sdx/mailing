# CSV Column Mapping Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace hardcoded column-name lookups in `apollo-people-search` and `company-domain-lookup` CSV readers with a configurable `column-mapping` in `application.yml`, so input files with different column headers work without code changes.

**Architecture:** A new `CsvColumnMapper` utility class (one per module, in the existing `csv/` package alongside `CsvLineParser`) resolves a `Map<String, String>` config (internal name → CSV header name) against actual CSV headers at reader init time, returning `Map<String, Integer>` (internal name → column index). Each module's `@ConfigurationProperties` record gains a `columnMapping` field; `application.yml` defaults match the current hardcoded column names so existing pipelines need no config changes.

**Tech Stack:** Java 21, Spring Boot 3.4.5, Spring Batch, JUnit 5, AssertJ

---

## File Map

### apollo-people-search

| Action | Path |
|---|---|
| Create | `apollo-people-search/src/main/java/com/synapsedx/mailing/apollo/csv/CsvColumnMapper.java` |
| Create | `apollo-people-search/src/test/java/com/synapsedx/mailing/apollo/csv/CsvColumnMapperTest.java` |
| Modify | `apollo-people-search/src/main/java/com/synapsedx/mailing/apollo/config/ApolloProperties.java` |
| Modify | `apollo-people-search/src/main/resources/application.yml` |
| Modify | `apollo-people-search/src/main/java/com/synapsedx/mailing/apollo/batch/reader/UniqueDomainReader.java` |
| Modify | `apollo-people-search/src/test/java/com/synapsedx/mailing/apollo/batch/reader/UniqueDomainReaderTest.java` |
| Modify | `apollo-people-search/src/test/java/com/synapsedx/mailing/apollo/batch/writer/ApolloPeopleCsvWriterTest.java` |
| Modify | `apollo-people-search/src/test/java/com/synapsedx/mailing/apollo/client/ApolloClientTest.java` |

### company-domain-lookup

| Action | Path |
|---|---|
| Create | `company-domain-lookup/src/main/java/com/synapsedx/mailing/companydomain/csv/CsvColumnMapper.java` |
| Create | `company-domain-lookup/src/test/java/com/synapsedx/mailing/companydomain/csv/CsvColumnMapperTest.java` |
| Modify | `company-domain-lookup/src/main/java/com/synapsedx/mailing/companydomain/config/CompanyDomainProperties.java` |
| Modify | `company-domain-lookup/src/main/resources/application.yml` |
| Modify | `company-domain-lookup/src/main/java/com/synapsedx/mailing/companydomain/batch/reader/ContactsCsvReader.java` |
| Modify | `company-domain-lookup/src/main/java/com/synapsedx/mailing/companydomain/batch/reader/UniqueCompanyReader.java` |
| Modify | `company-domain-lookup/src/main/java/com/synapsedx/mailing/companydomain/batch/reader/UniqueArticleReader.java` |
| Modify (constructor only) | `company-domain-lookup/src/test/java/com/synapsedx/mailing/companydomain/batch/reader/ContactsCsvReaderTest.java` |
| Modify (constructor only) | `company-domain-lookup/src/test/java/com/synapsedx/mailing/companydomain/batch/reader/UniqueCompanyReaderTest.java` |
| Modify (constructor only) | `company-domain-lookup/src/test/java/com/synapsedx/mailing/companydomain/batch/reader/UniqueArticleReaderTest.java` |
| Modify (constructor only) | `company-domain-lookup/src/test/java/com/synapsedx/mailing/companydomain/batch/processor/ArticleSummaryProcessorTest.java` |
| Modify (constructor only) | `company-domain-lookup/src/test/java/com/synapsedx/mailing/companydomain/batch/processor/DomainLookupProcessorTest.java` |
| Modify (constructor only) | `company-domain-lookup/src/test/java/com/synapsedx/mailing/companydomain/batch/writer/EnrichedContactsCsvWriterTest.java` |

---

### Task 1: CsvColumnMapper in apollo-people-search

**Files:**
- Create: `apollo-people-search/src/test/java/com/synapsedx/mailing/apollo/csv/CsvColumnMapperTest.java`
- Create: `apollo-people-search/src/main/java/com/synapsedx/mailing/apollo/csv/CsvColumnMapper.java`

- [ ] **Step 1: Write the failing test**

Create `apollo-people-search/src/test/java/com/synapsedx/mailing/apollo/csv/CsvColumnMapperTest.java`:

```java
package com.synapsedx.mailing.apollo.csv;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class CsvColumnMapperTest {

  @Test
  void resolvesConfiguredColumns() {
    var mapping = Map.of("company", "organizationName", "domain", "websiteDomain");
    var headers = List.of("id", "organizationName", "websiteDomain", "extra");

    var indices = CsvColumnMapper.resolve(headers, mapping);

    assertThat(indices.get("company")).isEqualTo(1);
    assertThat(indices.get("domain")).isEqualTo(2);
  }

  @Test
  void ignoresExtraColumnsInCsv() {
    var mapping = Map.of("company", "company");
    var headers = List.of("company", "extra1", "extra2");

    var indices = CsvColumnMapper.resolve(headers, mapping);

    assertThat(indices).hasSize(1);
    assertThat(indices.get("company")).isEqualTo(0);
  }

  @Test
  void throwsWhenConfiguredColumnMissingFromHeaders() {
    var mapping = Map.of("company", "organizationName");
    var headers = List.of("id", "domain");

    assertThatThrownBy(() -> CsvColumnMapper.resolve(headers, mapping))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("organizationName")
        .hasMessageContaining("company");
  }

  @Test
  void throwsWhenMappingIsNull() {
    assertThatThrownBy(() -> CsvColumnMapper.resolve(List.of("company"), null))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("column-mapping");
  }

  @Test
  void throwsWhenMappingIsEmpty() {
    assertThatThrownBy(() -> CsvColumnMapper.resolve(List.of("company"), Map.of()))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("column-mapping");
  }
}
```

- [ ] **Step 2: Run test to verify it fails**

```bash
./mvnw -pl apollo-people-search -Dtest=CsvColumnMapperTest test
```

Expected: BUILD FAILURE — `CsvColumnMapper` does not exist yet.

- [ ] **Step 3: Implement CsvColumnMapper**

Create `apollo-people-search/src/main/java/com/synapsedx/mailing/apollo/csv/CsvColumnMapper.java`:

```java
package com.synapsedx.mailing.apollo.csv;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CsvColumnMapper {

  public static Map<String, Integer> resolve(List<String> headers, Map<String, String> columnMapping) {
    if (columnMapping == null || columnMapping.isEmpty()) {
      throw new IllegalStateException("column-mapping must be configured");
    }
    var result = new LinkedHashMap<String, Integer>();
    for (var entry : columnMapping.entrySet()) {
      var idx = headers.indexOf(entry.getValue());
      if (idx < 0) {
        throw new IllegalArgumentException(
            "CSV column '%s' (mapped to '%s') not found in headers: %s"
                .formatted(entry.getValue(), entry.getKey(), headers));
      }
      result.put(entry.getKey(), idx);
    }
    return result;
  }
}
```

- [ ] **Step 4: Add license header**

```bash
cd apollo-people-search && task update-license && cd ..
```

- [ ] **Step 5: Run tests to verify they pass**

```bash
./mvnw -pl apollo-people-search -Dtest=CsvColumnMapperTest test
```

Expected: BUILD SUCCESS — 5 tests passing.

- [ ] **Step 6: Commit**

```bash
git add apollo-people-search/src/main/java/com/synapsedx/mailing/apollo/csv/CsvColumnMapper.java \
        apollo-people-search/src/test/java/com/synapsedx/mailing/apollo/csv/CsvColumnMapperTest.java
git commit -m "feat(apollo-people-search): add CsvColumnMapper utility"
```

---

### Task 2: Wire columnMapping into ApolloProperties and fix test constructors

**Files:**
- Modify: `apollo-people-search/src/main/java/com/synapsedx/mailing/apollo/config/ApolloProperties.java`
- Modify: `apollo-people-search/src/main/resources/application.yml`
- Modify: `apollo-people-search/src/test/java/com/synapsedx/mailing/apollo/batch/reader/UniqueDomainReaderTest.java`
- Modify: `apollo-people-search/src/test/java/com/synapsedx/mailing/apollo/batch/writer/ApolloPeopleCsvWriterTest.java`
- Modify: `apollo-people-search/src/test/java/com/synapsedx/mailing/apollo/client/ApolloClientTest.java`

- [ ] **Step 1: Add columnMapping to ApolloProperties**

Replace the full contents of `apollo-people-search/src/main/java/com/synapsedx/mailing/apollo/config/ApolloProperties.java`:

```java
package com.synapsedx.mailing.apollo.config;

import java.util.List;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("apollo")
public record ApolloProperties(
    Api api,
    String peopleSearchEndpoint,
    String inputCsv,
    String outputCsv,
    int perPage,
    int topN,
    long throttleMillis,
    List<String> seniorities,
    List<String> titles,
    Map<String, String> columnMapping) {

  public ApolloProperties {
    if (peopleSearchEndpoint == null || peopleSearchEndpoint.isBlank()) {
      peopleSearchEndpoint = "https://api.apollo.io/api/v1/mixed_people/api_search";
    }
    if (titles == null) {
      titles = java.util.List.of();
    }
  }

  public record Api(String key) {}
}
```

- [ ] **Step 2: Add column-mapping defaults to application.yml**

In `apollo-people-search/src/main/resources/application.yml`, append these two lines at the end of the file (under the `apollo:` block, at the same indentation as `titles:`):

```yaml
  column-mapping:
    company: company
    domain: domain
```

The end of the file should look like:

```yaml
  titles:
    - CTO
    - Chief Technology Officer
    - Chief Digital Officer
    - Chief Information Officer
    - VP Engineering
    - VP Technology
  column-mapping:
    company: company
    domain: domain
```

- [ ] **Step 3: Update ApolloProperties constructor in UniqueDomainReaderTest**

`UniqueDomainReaderTest` has two constructor calls — one in the helper method `readerFor()` (line 17) and one inline in `failsFastOnMissingDomainHeader` (line 80). Add `Map.of("company", "company", "domain", "domain")` as the 10th argument to both, and add `import java.util.Map;`.

The helper method becomes:

```java
private UniqueDomainReader readerFor(String classpathCsv) throws Exception {
  var path = new ClassPathResource(classpathCsv).getFile().getAbsolutePath();
  var props =
      new ApolloProperties(
          new ApolloProperties.Api("key"),
          null,
          path,
          "output/people.csv",
          25,
          10,
          0,
          List.of("c_suite"),
          List.of(),
          Map.of("company", "company", "domain", "domain"));
  return new UniqueDomainReader(props);
}
```

The inline constructor in `failsFastOnMissingDomainHeader` becomes:

```java
var props =
    new ApolloProperties(
        new ApolloProperties.Api("key"),
        null,
        tmpFile.toString(),
        "output/people.csv",
        25,
        10,
        0,
        List.of("c_suite"),
        List.of(),
        Map.of("company", "company", "domain", "domain"));
```

- [ ] **Step 4: Update ApolloProperties constructor in ApolloPeopleCsvWriterTest**

In `ApolloPeopleCsvWriterTest`, the constructor in `writerFor()` (line 22) becomes:

```java
var props =
    new ApolloProperties(
        new ApolloProperties.Api("key"),
        null,
        "input/contacts.csv",
        outputPath.toString(),
        25,
        10,
        0,
        List.of("c_suite"),
        List.of(),
        Map.of("company", "company", "domain", "domain"));
```

Add `import java.util.Map;` if not already present.

- [ ] **Step 5: Update ApolloProperties constructor in ApolloClientTest**

In `ApolloClientTest`, the constructor in `buildClient()` (line 42) becomes:

```java
var props =
    new ApolloProperties(
        new ApolloProperties.Api("test-key"),
        "http://127.0.0.1:" + MOCK.port() + "/api/v1/mixed_people/api_search",
        "input/contacts.csv",
        "output/people.csv",
        25,
        10,
        0,
        List.of("owner", "founder", "c_suite", "vp", "head", "director"),
        List.of(),
        Map.of("company", "company", "domain", "domain"));
```

Add `import java.util.Map;` if not already present.

- [ ] **Step 6: Run all apollo-people-search tests to verify compilation and passing**

```bash
./mvnw -pl apollo-people-search test
```

Expected: BUILD SUCCESS.

- [ ] **Step 7: Commit**

```bash
git add apollo-people-search/src/main/java/com/synapsedx/mailing/apollo/config/ApolloProperties.java \
        apollo-people-search/src/main/resources/application.yml \
        apollo-people-search/src/test/java/com/synapsedx/mailing/apollo/batch/reader/UniqueDomainReaderTest.java \
        apollo-people-search/src/test/java/com/synapsedx/mailing/apollo/batch/writer/ApolloPeopleCsvWriterTest.java \
        apollo-people-search/src/test/java/com/synapsedx/mailing/apollo/client/ApolloClientTest.java
git commit -m "feat(apollo-people-search): add columnMapping to ApolloProperties with yml defaults"
```

---

### Task 3: Update UniqueDomainReader to use CsvColumnMapper

**Files:**
- Modify: `apollo-people-search/src/main/java/com/synapsedx/mailing/apollo/batch/reader/UniqueDomainReader.java`
- Modify: `apollo-people-search/src/test/java/com/synapsedx/mailing/apollo/batch/reader/UniqueDomainReaderTest.java`

- [ ] **Step 1: Replace loadEntries() in UniqueDomainReader**

Add `import com.synapsedx.mailing.apollo.csv.CsvColumnMapper;` to the imports.

Remove `import java.util.List;` (no longer used as a standalone reference in loadEntries — keep it if it appears elsewhere).

Replace the `loadEntries()` method with:

```java
private List<CompanyRef> loadEntries() throws Exception {
  var path = Path.of(properties.inputCsv());
  var lines = Files.readAllLines(path);
  if (lines.isEmpty()) {
    log.warn("apollo_reader_empty_file file={}", path);
    return List.of();
  }

  var header = CsvLineParser.parse(lines.getFirst());
  var indices = CsvColumnMapper.resolve(header, properties.columnMapping());

  var seen = new HashSet<String>();
  var result = new ArrayList<CompanyRef>();

  for (var i = 1; i < lines.size(); i++) {
    var fields = CsvLineParser.parse(lines.get(i));
    var companyIdx = indices.get("company");
    var domainIdx = indices.get("domain");
    if (fields.size() <= Math.max(companyIdx, domainIdx)) {
      continue;
    }
    var domain = fields.get(domainIdx).trim();
    if (domain.isBlank()) {
      continue;
    }
    var key = domain.toLowerCase(Locale.ROOT);
    if (seen.add(key)) {
      result.add(new CompanyRef(fields.get(companyIdx).trim(), domain));
    }
  }

  log.info("apollo_reader_loaded unique_domains={} file={}", result.size(), path.getFileName());
  return result;
}
```

The old manual `companyIdx`/`domainIdx` validation blocks (the two `if (companyIdx < 0)` / `if (domainIdx < 0)` checks) are removed — `CsvColumnMapper.resolve()` handles this.

- [ ] **Step 2: Update the error-type assertion in failsFastOnMissingDomainHeader**

The column-missing error is now `IllegalArgumentException` (thrown by `CsvColumnMapper`) rather than `IllegalStateException`. Update the assertion:

```java
assertThatThrownBy(reader::read)
    .isInstanceOf(IllegalArgumentException.class)
    .hasMessageContaining("domain");
```

- [ ] **Step 3: Run all apollo-people-search tests**

```bash
./mvnw -pl apollo-people-search test
```

Expected: BUILD SUCCESS — all tests passing.

- [ ] **Step 4: Commit**

```bash
git add apollo-people-search/src/main/java/com/synapsedx/mailing/apollo/batch/reader/UniqueDomainReader.java \
        apollo-people-search/src/test/java/com/synapsedx/mailing/apollo/batch/reader/UniqueDomainReaderTest.java
git commit -m "feat(apollo-people-search): use CsvColumnMapper in UniqueDomainReader"
```

---

### Task 4: CsvColumnMapper in company-domain-lookup

**Files:**
- Create: `company-domain-lookup/src/test/java/com/synapsedx/mailing/companydomain/csv/CsvColumnMapperTest.java`
- Create: `company-domain-lookup/src/main/java/com/synapsedx/mailing/companydomain/csv/CsvColumnMapper.java`

- [ ] **Step 1: Write the failing test**

Create `company-domain-lookup/src/test/java/com/synapsedx/mailing/companydomain/csv/CsvColumnMapperTest.java`:

```java
package com.synapsedx.mailing.companydomain.csv;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class CsvColumnMapperTest {

  @Test
  void resolvesConfiguredColumns() {
    var mapping = Map.of("company", "organizationName", "article_id", "ref");
    var headers = List.of("id", "organizationName", "ref", "extra");

    var indices = CsvColumnMapper.resolve(headers, mapping);

    assertThat(indices.get("company")).isEqualTo(1);
    assertThat(indices.get("article_id")).isEqualTo(2);
  }

  @Test
  void ignoresExtraColumnsInCsv() {
    var mapping = Map.of("company", "company");
    var headers = List.of("company", "extra1", "extra2");

    var indices = CsvColumnMapper.resolve(headers, mapping);

    assertThat(indices).hasSize(1);
    assertThat(indices.get("company")).isEqualTo(0);
  }

  @Test
  void throwsWhenConfiguredColumnMissingFromHeaders() {
    var mapping = Map.of("company", "organizationName");
    var headers = List.of("id", "article_id");

    assertThatThrownBy(() -> CsvColumnMapper.resolve(headers, mapping))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("organizationName")
        .hasMessageContaining("company");
  }

  @Test
  void throwsWhenMappingIsNull() {
    assertThatThrownBy(() -> CsvColumnMapper.resolve(List.of("company"), null))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("column-mapping");
  }

  @Test
  void throwsWhenMappingIsEmpty() {
    assertThatThrownBy(() -> CsvColumnMapper.resolve(List.of("company"), Map.of()))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("column-mapping");
  }
}
```

- [ ] **Step 2: Run test to verify it fails**

```bash
./mvnw -pl company-domain-lookup -Dtest=CsvColumnMapperTest test
```

Expected: BUILD FAILURE — `CsvColumnMapper` does not exist yet.

- [ ] **Step 3: Implement CsvColumnMapper**

Create `company-domain-lookup/src/main/java/com/synapsedx/mailing/companydomain/csv/CsvColumnMapper.java`:

```java
package com.synapsedx.mailing.companydomain.csv;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CsvColumnMapper {

  public static Map<String, Integer> resolve(List<String> headers, Map<String, String> columnMapping) {
    if (columnMapping == null || columnMapping.isEmpty()) {
      throw new IllegalStateException("column-mapping must be configured");
    }
    var result = new LinkedHashMap<String, Integer>();
    for (var entry : columnMapping.entrySet()) {
      var idx = headers.indexOf(entry.getValue());
      if (idx < 0) {
        throw new IllegalArgumentException(
            "CSV column '%s' (mapped to '%s') not found in headers: %s"
                .formatted(entry.getValue(), entry.getKey(), headers));
      }
      result.put(entry.getKey(), idx);
    }
    return result;
  }
}
```

- [ ] **Step 4: Add license header**

```bash
cd company-domain-lookup && task update-license && cd ..
```

- [ ] **Step 5: Run tests to verify they pass**

```bash
./mvnw -pl company-domain-lookup -Dtest=CsvColumnMapperTest test
```

Expected: BUILD SUCCESS — 5 tests passing.

- [ ] **Step 6: Commit**

```bash
git add company-domain-lookup/src/main/java/com/synapsedx/mailing/companydomain/csv/CsvColumnMapper.java \
        company-domain-lookup/src/test/java/com/synapsedx/mailing/companydomain/csv/CsvColumnMapperTest.java
git commit -m "feat(company-domain-lookup): add CsvColumnMapper utility"
```

---

### Task 5: Wire columnMapping into CompanyDomainProperties and fix test constructors

**Files:**
- Modify: `company-domain-lookup/src/main/java/com/synapsedx/mailing/companydomain/config/CompanyDomainProperties.java`
- Modify: `company-domain-lookup/src/main/resources/application.yml`
- Modify (constructor only): all test files listed in the file map above

- [ ] **Step 1: Add columnMapping to CompanyDomainProperties**

Replace the full contents of `company-domain-lookup/src/main/java/com/synapsedx/mailing/companydomain/config/CompanyDomainProperties.java`:

```java
package com.synapsedx.mailing.companydomain.config;

import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("company-domain")
public record CompanyDomainProperties(
    String inputCsv,
    String outputCsv,
    String articlesDir,
    int serpDepth,
    int serpTopN,
    Map<String, String> columnMapping) {}
```

- [ ] **Step 2: Add column-mapping defaults to application.yml**

In `company-domain-lookup/src/main/resources/application.yml`, the `company-domain:` block should become:

```yaml
company-domain:
  input-csv: input/contacts.csv
  output-csv: output/contacts-with-domain.csv
  articles-dir: ""
  serp-depth: 10
  serp-top-n: 5
  column-mapping:
    company: company
    article_id: article_id
```

- [ ] **Step 3: Update all CompanyDomainProperties constructor calls in tests**

There are 8 constructor call sites across 5 test files. Add `Map.of("company", "company", "article_id", "article_id")` as the last argument to every one of them, and add `import java.util.Map;` to each file.

**ContactsCsvReaderTest** (2 calls — find all `new CompanyDomainProperties(` and add the map arg):
```java
// both calls gain the same last argument:
Map.of("company", "company", "article_id", "article_id")
```

**UniqueCompanyReaderTest** (2 calls — same pattern):
```java
Map.of("company", "company", "article_id", "article_id")
```

**UniqueArticleReaderTest** (2 calls — same pattern):
```java
Map.of("company", "company", "article_id", "article_id")
```

**ArticleSummaryProcessorTest** (1 call — line 22):
```java
return new CompanyDomainProperties("ignored.csv", "out.csv", dir.toString(), 10, 5,
    Map.of("company", "company", "article_id", "article_id"));
```

**DomainLookupProcessorTest** (2 calls):
```java
// line 22:
new CompanyDomainProperties("in.csv", "out.csv", "", 10, 5,
    Map.of("company", "company", "article_id", "article_id"));
// line 93:
var smallProps = new CompanyDomainProperties("in.csv", "out.csv", "", 10, 3,
    Map.of("company", "company", "article_id", "article_id"));
```

**EnrichedContactsCsvWriterTest** (1 call — line 21):
```java
var props = new CompanyDomainProperties("ignored", out.toString(), "", 10, 5,
    Map.of("company", "company", "article_id", "article_id"));
```

- [ ] **Step 4: Run all company-domain-lookup tests to verify compilation and passing**

```bash
./mvnw -pl company-domain-lookup test
```

Expected: BUILD SUCCESS.

- [ ] **Step 5: Commit**

```bash
git add company-domain-lookup/src/main/java/com/synapsedx/mailing/companydomain/config/CompanyDomainProperties.java \
        company-domain-lookup/src/main/resources/application.yml \
        company-domain-lookup/src/test/java/com/synapsedx/mailing/companydomain/batch/reader/ContactsCsvReaderTest.java \
        company-domain-lookup/src/test/java/com/synapsedx/mailing/companydomain/batch/reader/UniqueCompanyReaderTest.java \
        company-domain-lookup/src/test/java/com/synapsedx/mailing/companydomain/batch/reader/UniqueArticleReaderTest.java \
        company-domain-lookup/src/test/java/com/synapsedx/mailing/companydomain/batch/processor/ArticleSummaryProcessorTest.java \
        company-domain-lookup/src/test/java/com/synapsedx/mailing/companydomain/batch/processor/DomainLookupProcessorTest.java \
        company-domain-lookup/src/test/java/com/synapsedx/mailing/companydomain/batch/writer/EnrichedContactsCsvWriterTest.java
git commit -m "feat(company-domain-lookup): add columnMapping to CompanyDomainProperties with yml defaults"
```

---

### Task 6: Update company-domain-lookup readers to use CsvColumnMapper

**Files:**
- Modify: `company-domain-lookup/src/main/java/com/synapsedx/mailing/companydomain/batch/reader/ContactsCsvReader.java`
- Modify: `company-domain-lookup/src/main/java/com/synapsedx/mailing/companydomain/batch/reader/UniqueCompanyReader.java`
- Modify: `company-domain-lookup/src/main/java/com/synapsedx/mailing/companydomain/batch/reader/UniqueArticleReader.java`

- [ ] **Step 1: Replace ContactsCsvReader**

Replace the full contents of `company-domain-lookup/src/main/java/com/synapsedx/mailing/companydomain/batch/reader/ContactsCsvReader.java`:

```java
package com.synapsedx.mailing.companydomain.batch.reader;

import com.synapsedx.mailing.companydomain.config.CompanyDomainProperties;
import com.synapsedx.mailing.companydomain.csv.CsvColumnMapper;
import com.synapsedx.mailing.companydomain.csv.CsvLineParser;
import com.synapsedx.mailing.companydomain.model.ContactRow;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
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
  private Map<String, Integer> columnIndices;
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
    var companyIdx = columnIndices.get("company");
    var articleIdIdx = columnIndices.get("article_id");
    var company = companyIdx < fields.size() ? fields.get(companyIdx).trim() : "";
    var articleId = articleIdIdx < fields.size() ? fields.get(articleIdIdx).trim() : "";
    return new ContactRow(headers, fields, company, articleId);
  }

  private void init() throws Exception {
    var path = Path.of(properties.inputCsv());
    var lines = Files.readAllLines(path);
    if (lines.isEmpty()) {
      throw new IllegalStateException("input CSV is empty: " + path);
    }
    headers = CsvLineParser.parse(lines.getFirst());
    columnIndices = CsvColumnMapper.resolve(headers, properties.columnMapping());
    dataLines = lines.subList(1, lines.size()).iterator();
    log.info(
        "contacts_csv_loaded file={} headers={} rows={}",
        path.getFileName(),
        headers,
        lines.size() - 1);
  }
}
```

- [ ] **Step 2: Replace UniqueCompanyReader**

Replace the full contents of `company-domain-lookup/src/main/java/com/synapsedx/mailing/companydomain/batch/reader/UniqueCompanyReader.java`:

```java
package com.synapsedx.mailing.companydomain.batch.reader;

import com.synapsedx.mailing.companydomain.config.CompanyDomainProperties;
import com.synapsedx.mailing.companydomain.csv.CsvColumnMapper;
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
    var headers = CsvLineParser.parse(lines.getFirst());
    var companyIdx = CsvColumnMapper.resolve(headers, properties.columnMapping()).get("company");

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

- [ ] **Step 3: Replace UniqueArticleReader**

Replace the full contents of `company-domain-lookup/src/main/java/com/synapsedx/mailing/companydomain/batch/reader/UniqueArticleReader.java`:

```java
package com.synapsedx.mailing.companydomain.batch.reader;

import com.synapsedx.mailing.companydomain.config.CompanyDomainProperties;
import com.synapsedx.mailing.companydomain.csv.CsvColumnMapper;
import com.synapsedx.mailing.companydomain.csv.CsvLineParser;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemReader;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class UniqueArticleReader implements ItemReader<String> {

  private final CompanyDomainProperties properties;
  private Iterator<String> iterator;

  @Override
  public String read() throws Exception {
    if (iterator == null) {
      iterator = loadUniqueArticleIds().iterator();
    }
    return iterator.hasNext() ? iterator.next() : null;
  }

  private List<String> loadUniqueArticleIds() throws Exception {
    var path = Path.of(properties.inputCsv());
    var lines = Files.readAllLines(path);
    if (lines.isEmpty()) {
      throw new IllegalStateException("input CSV is empty: " + path);
    }
    var headers = CsvLineParser.parse(lines.getFirst());
    var articleIdx = CsvColumnMapper.resolve(headers, properties.columnMapping()).get("article_id");

    var unique = new ArrayList<String>();
    var seen = new HashSet<String>();
    for (var i = 1; i < lines.size(); i++) {
      var fields = CsvLineParser.parse(lines.get(i));
      if (articleIdx >= fields.size()) {
        continue;
      }
      var value = fields.get(articleIdx).trim();
      if (value.isEmpty()) {
        continue;
      }
      if (seen.add(value)) {
        unique.add(value);
      }
    }
    log.info("unique_articles_loaded file={} count={}", path.getFileName(), unique.size());
    return unique;
  }
}
```

- [ ] **Step 4: Run all company-domain-lookup tests**

```bash
./mvnw -pl company-domain-lookup test
```

Expected: BUILD SUCCESS — all tests passing.

- [ ] **Step 5: Commit**

```bash
git add company-domain-lookup/src/main/java/com/synapsedx/mailing/companydomain/batch/reader/ContactsCsvReader.java \
        company-domain-lookup/src/main/java/com/synapsedx/mailing/companydomain/batch/reader/UniqueCompanyReader.java \
        company-domain-lookup/src/main/java/com/synapsedx/mailing/companydomain/batch/reader/UniqueArticleReader.java
git commit -m "feat(company-domain-lookup): use CsvColumnMapper in all CSV readers"
```
