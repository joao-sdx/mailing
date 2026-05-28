# company-domain-lookup Per-Article Summary Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a trailing `summary` column to `company-domain-lookup`'s enriched CSV, produced by one LM Studio LLM call per unique source article (≤30-word French summary), while the already-handled `role` column continues to pass through unchanged.

**Architecture:** Insert a new middle Spring Batch step (`resolve-summaries`) that mirrors the existing `resolve-domains` step: a `UniqueArticleReader` emits each distinct `article_id`, an `ArticleSummaryProcessor` reads the `.md` body (frontmatter stripped) and calls `LmStudioClient.summarizeArticle`, and an `ArticleSummaryMapWriter` fills an in-memory `ArticleSummaryMap`. The final enrich step then looks up both `domain` and `summary`. Missing article file → hard fail; LLM error → empty summary (soft).

**Tech Stack:** Java 21, Spring Boot 3.4.5, Spring Batch, Lombok, Jackson, JUnit 5 + AssertJ + Mockito, WireMock (IT). Build via `../mvnw` from the module dir.

---

## Working directory

All paths below are relative to `company-domain-lookup/`. Run every Maven command from that directory:

```bash
cd /Users/joao.violante/IdeaProjects/mailing/company-domain-lookup
```

## File map

**Create:**
- `src/main/java/com/synapsedx/mailing/companydomain/model/ArticleSummary.java`
- `src/main/java/com/synapsedx/mailing/companydomain/batch/support/ArticleSummaryMap.java`
- `src/main/java/com/synapsedx/mailing/companydomain/batch/writer/ArticleSummaryMapWriter.java`
- `src/main/java/com/synapsedx/mailing/companydomain/batch/reader/UniqueArticleReader.java`
- `src/main/java/com/synapsedx/mailing/companydomain/batch/processor/ArticleSummaryProcessor.java`
- `src/main/resources/article-summary-prompt.md`
- `src/test/java/com/synapsedx/mailing/companydomain/batch/writer/ArticleSummaryMapWriterTest.java`
- `src/test/java/com/synapsedx/mailing/companydomain/batch/reader/UniqueArticleReaderTest.java`
- `src/test/java/com/synapsedx/mailing/companydomain/batch/processor/ArticleSummaryProcessorTest.java`
- `src/test/java/com/synapsedx/mailing/companydomain/CompanyDomainLookupMissingArticleIT.java`
- `src/test/resources/fixtures/no-article-id-header.csv`
- `src/test/resources/fixtures/r1.md` … `r5.md`
- `src/test/resources/fixtures/it-contacts-missing-article.csv`

**Modify:**
- `src/main/java/com/synapsedx/mailing/companydomain/config/CompanyDomainProperties.java` (add `articlesDir`)
- `src/main/resources/application.yml` (add `articles-dir`)
- `src/main/java/com/synapsedx/mailing/companydomain/client/LmStudioClient.java` (add `summarizeArticle`)
- `src/main/java/com/synapsedx/mailing/companydomain/model/ContactRow.java` (add `articleId`)
- `src/main/java/com/synapsedx/mailing/companydomain/model/EnrichedContactRow.java` (add `summary`)
- `src/main/java/com/synapsedx/mailing/companydomain/batch/reader/ContactsCsvReader.java` (locate `article_id`)
- `src/main/java/com/synapsedx/mailing/companydomain/batch/processor/ContactEnrichProcessor.java` (inject summary map)
- `src/main/java/com/synapsedx/mailing/companydomain/batch/writer/EnrichedContactsCsvWriter.java` (append `summary`)
- `src/main/java/com/synapsedx/mailing/companydomain/batch/CompanyDomainLookupJobConfig.java` (wire step 2)
- Tests: `PropertiesBindingTest`, `UniqueCompanyReaderTest`, `DomainLookupProcessorTest`, `ContactsCsvReaderTest`, `ContactEnrichProcessorTest`, `EnrichedContactsCsvWriterTest`, `CompanyDomainLookupJobIT`

> **Constructor-arity warning:** Tasks 1 and 6 change record constructors (`CompanyDomainProperties`, `ContactRow`, `EnrichedContactRow`). Every call site listed must be updated in the *same* task so the build stays green at each commit.

---

### Task 1: Add `articlesDir` to `CompanyDomainProperties`

Optional config field; blank → resolve articles against the input CSV's parent directory. No behavior change yet — this task only widens the record and fixes every call site.

**Files:**
- Modify: `src/main/java/com/synapsedx/mailing/companydomain/config/CompanyDomainProperties.java`
- Modify: `src/main/resources/application.yml`
- Modify: `src/test/java/com/synapsedx/mailing/companydomain/config/PropertiesBindingTest.java`
- Modify: `src/test/java/com/synapsedx/mailing/companydomain/batch/reader/UniqueCompanyReaderTest.java`
- Modify: `src/test/java/com/synapsedx/mailing/companydomain/batch/reader/ContactsCsvReaderTest.java`
- Modify: `src/test/java/com/synapsedx/mailing/companydomain/batch/processor/DomainLookupProcessorTest.java`
- Modify: `src/test/java/com/synapsedx/mailing/companydomain/batch/writer/EnrichedContactsCsvWriterTest.java`

- [ ] **Step 1: Update `PropertiesBindingTest` to expect the new field (failing test first)**

Add the property inside the `properties = { ... }` block (after the `company-domain.output-csv` line):

```java
      "company-domain.articles-dir=/tmp/articles",
```

Add this assertion inside `bindsAllProperties()` (after the `outputCsv` assertion):

```java
    assertThat(companyDomain.articlesDir()).isEqualTo("/tmp/articles");
```

- [ ] **Step 2: Run the binding test to verify it fails**

Run: `../mvnw -Dtest=PropertiesBindingTest test`
Expected: COMPILATION FAILURE — `cannot find symbol: method articlesDir()`.

- [ ] **Step 3: Add the field to the record**

Replace the whole record body in `CompanyDomainProperties.java`:

```java
@ConfigurationProperties("company-domain")
public record CompanyDomainProperties(
    String inputCsv, String outputCsv, String articlesDir, int serpDepth, int serpTopN) {}
```

- [ ] **Step 4: Add the default to `application.yml`**

Under the `company-domain:` block, add `articles-dir` (blank means "use the input CSV's directory"):

```yaml
company-domain:
  input-csv: input/contacts.csv
  output-csv: output/contacts-with-domain.csv
  articles-dir: ""
  serp-depth: 10
  serp-top-n: 5
```

- [ ] **Step 5: Fix every other constructor call site (3-arg → insert `""` after output)**

`UniqueCompanyReaderTest.java` — both occurrences become:

```java
        new CompanyDomainProperties(
            "src/test/resources/fixtures/contacts-sample.csv", "out.csv", "", 10, 5);
```
```java
        new CompanyDomainProperties(
            "src/test/resources/fixtures/no-company-header.csv", "out.csv", "", 10, 5);
```

`ContactsCsvReaderTest.java`:

```java
        new CompanyDomainProperties(
            "src/test/resources/fixtures/contacts-sample.csv", "out.csv", "", 10, 5);
```

`DomainLookupProcessorTest.java` — both occurrences:

```java
      new CompanyDomainProperties("in.csv", "out.csv", "", 10, 5);
```
```java
    var smallProps = new CompanyDomainProperties("in.csv", "out.csv", "", 10, 3);
```

`EnrichedContactsCsvWriterTest.java`:

```java
    var props = new CompanyDomainProperties("ignored", out.toString(), "", 10, 5);
```

- [ ] **Step 6: Run the full module test suite to verify green**

Run: `../mvnw test`
Expected: BUILD SUCCESS, all existing tests pass (binding test now asserts `articlesDir`).

- [ ] **Step 7: Commit**

```bash
git add src/main/java/com/synapsedx/mailing/companydomain/config/CompanyDomainProperties.java \
        src/main/resources/application.yml src/test
git commit -m "feat(company-domain-lookup): add optional articles-dir property"
```

---

### Task 2: `ArticleSummary` model, `ArticleSummaryMap` bean, and writer

Standalone new classes mirroring `CompanyDomain` / `CompanyDomainMap` / `CompanyDomainMapWriter`. Nothing references them yet.

**Files:**
- Create: `src/main/java/com/synapsedx/mailing/companydomain/model/ArticleSummary.java`
- Create: `src/main/java/com/synapsedx/mailing/companydomain/batch/support/ArticleSummaryMap.java`
- Create: `src/main/java/com/synapsedx/mailing/companydomain/batch/writer/ArticleSummaryMapWriter.java`
- Test: `src/test/java/com/synapsedx/mailing/companydomain/batch/writer/ArticleSummaryMapWriterTest.java`

- [ ] **Step 1: Write the failing writer test**

`ArticleSummaryMapWriterTest.java`:

```java
package com.synapsedx.mailing.companydomain.batch.writer;

import static org.assertj.core.api.Assertions.assertThat;

import com.synapsedx.mailing.companydomain.batch.support.ArticleSummaryMap;
import com.synapsedx.mailing.companydomain.model.ArticleSummary;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.batch.item.Chunk;

class ArticleSummaryMapWriterTest {

  @Test
  void putsEachSummaryIntoMap() throws Exception {
    var map = new ArticleSummaryMap();
    var writer = new ArticleSummaryMapWriter(map);

    writer.write(
        new Chunk<>(
            List.of(
                new ArticleSummary("r1.md", "Résumé un"),
                new ArticleSummary("r2.md", ""))));

    assertThat(map.get("r1.md")).isEqualTo("Résumé un");
    assertThat(map.get("r2.md")).isEqualTo("");
    assertThat(map.get("absent.md")).isEqualTo("");
  }
}
```

- [ ] **Step 2: Run it to verify it fails**

Run: `../mvnw -Dtest=ArticleSummaryMapWriterTest test`
Expected: COMPILATION FAILURE — `ArticleSummary`, `ArticleSummaryMap`, `ArticleSummaryMapWriter` do not exist.

- [ ] **Step 3: Create the model**

`ArticleSummary.java`:

```java
package com.synapsedx.mailing.companydomain.model;

public record ArticleSummary(String articleId, String summary) {}
```

- [ ] **Step 4: Create the map bean**

`ArticleSummaryMap.java`:

```java
package com.synapsedx.mailing.companydomain.batch.support;

import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class ArticleSummaryMap {

  private final ConcurrentHashMap<String, String> entries = new ConcurrentHashMap<>();

  public void put(String key, String summary) {
    entries.put(key, summary == null ? "" : summary);
  }

  public String get(String key) {
    return entries.getOrDefault(key, "");
  }
}
```

- [ ] **Step 5: Create the writer**

`ArticleSummaryMapWriter.java`:

```java
package com.synapsedx.mailing.companydomain.batch.writer;

import com.synapsedx.mailing.companydomain.batch.support.ArticleSummaryMap;
import com.synapsedx.mailing.companydomain.model.ArticleSummary;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ArticleSummaryMapWriter implements ItemWriter<ArticleSummary> {

  private final ArticleSummaryMap map;

  @Override
  public void write(Chunk<? extends ArticleSummary> chunk) {
    for (var item : chunk.getItems()) {
      map.put(item.articleId(), item.summary());
    }
  }
}
```

- [ ] **Step 6: Run the test to verify it passes**

Run: `../mvnw -Dtest=ArticleSummaryMapWriterTest test`
Expected: PASS.

- [ ] **Step 7: Commit**

```bash
git add src/main/java/com/synapsedx/mailing/companydomain/model/ArticleSummary.java \
        src/main/java/com/synapsedx/mailing/companydomain/batch/support/ArticleSummaryMap.java \
        src/main/java/com/synapsedx/mailing/companydomain/batch/writer/ArticleSummaryMapWriter.java \
        src/test/java/com/synapsedx/mailing/companydomain/batch/writer/ArticleSummaryMapWriterTest.java
git commit -m "feat(company-domain-lookup): add ArticleSummary model, map and writer"
```

---

### Task 3: `UniqueArticleReader`

Emits each distinct `article_id` once (first-seen order, empties skipped), fail-fast if the column is absent. Mirrors `UniqueCompanyReader` but keys on the raw trimmed value (no uppercasing).

**Files:**
- Create: `src/main/java/com/synapsedx/mailing/companydomain/batch/reader/UniqueArticleReader.java`
- Test: `src/test/java/com/synapsedx/mailing/companydomain/batch/reader/UniqueArticleReaderTest.java`
- Create fixture: `src/test/resources/fixtures/no-article-id-header.csv`

- [ ] **Step 1: Create the fixture without an `article_id` column**

`no-article-id-header.csv`:

```csv
first_name,last_name,company
Jean,Test,Factofrance
```

- [ ] **Step 2: Write the failing reader test**

`UniqueArticleReaderTest.java` (reuses the existing `contacts-sample.csv`, whose `article_id` values are `result-10-01.md` twice then `-02..-05.md`):

```java
package com.synapsedx.mailing.companydomain.batch.reader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.synapsedx.mailing.companydomain.config.CompanyDomainProperties;
import java.util.ArrayList;
import org.junit.jupiter.api.Test;

class UniqueArticleReaderTest {

  @Test
  void emitsUniqueArticleIdsPreservingFirstSeenOrder() throws Exception {
    var props =
        new CompanyDomainProperties(
            "src/test/resources/fixtures/contacts-sample.csv", "out.csv", "", 10, 5);
    var reader = new UniqueArticleReader(props);

    var seen = new ArrayList<String>();
    String next;
    while ((next = reader.read()) != null) {
      seen.add(next);
    }

    assertThat(seen)
        .containsExactly(
            "result-10-01.md",
            "result-10-02.md",
            "result-10-03.md",
            "result-10-04.md",
            "result-10-05.md");
  }

  @Test
  void failsFastWhenArticleIdColumnMissing() {
    var props =
        new CompanyDomainProperties(
            "src/test/resources/fixtures/no-article-id-header.csv", "out.csv", "", 10, 5);
    var reader = new UniqueArticleReader(props);
    assertThatThrownBy(reader::read)
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("article_id");
  }
}
```

- [ ] **Step 3: Run it to verify it fails**

Run: `../mvnw -Dtest=UniqueArticleReaderTest test`
Expected: COMPILATION FAILURE — `UniqueArticleReader` does not exist.

- [ ] **Step 4: Create the reader**

`UniqueArticleReader.java`:

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
    var articleIdx = headers.indexOf("article_id");
    if (articleIdx < 0) {
      throw new IllegalStateException(
          "input CSV missing 'article_id' column; headers=" + headers + " file=" + path);
    }

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

- [ ] **Step 5: Run the test to verify it passes**

Run: `../mvnw -Dtest=UniqueArticleReaderTest test`
Expected: PASS (both cases).

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/synapsedx/mailing/companydomain/batch/reader/UniqueArticleReader.java \
        src/test/java/com/synapsedx/mailing/companydomain/batch/reader/UniqueArticleReaderTest.java \
        src/test/resources/fixtures/no-article-id-header.csv
git commit -m "feat(company-domain-lookup): add UniqueArticleReader"
```

---

### Task 4: `LmStudioClient.summarizeArticle`

New method on the existing client plus its prompt resource. Reuses the existing private `post(...)` helper. No truncation — returns the model's text verbatim (fences stripped). Covered by the processor test (Task 5) and the IT (Task 7); no dedicated unit test, matching the existing `pickOfficialDomain` pattern.

**Files:**
- Create: `src/main/resources/article-summary-prompt.md`
- Modify: `src/main/java/com/synapsedx/mailing/companydomain/client/LmStudioClient.java`

- [ ] **Step 1: Create the prompt resource**

`article-summary-prompt.md`:

```markdown
Résume l'article suivant en 30 mots maximum, en français.
Renvoie uniquement le résumé, sans préambule ni guillemets.

Article:
{{article}}
```

- [ ] **Step 2: Load the new template in `init()`**

In `LmStudioClient.java`, add a field next to `promptTemplate`:

```java
  private String promptTemplate;
  private String summaryPromptTemplate;
```

In `init()`, after the existing `promptTemplate = ...` assignment, add:

```java
    summaryPromptTemplate =
        new ClassPathResource("article-summary-prompt.md").getContentAsString(StandardCharsets.UTF_8);
```

- [ ] **Step 3: Add the `summarizeArticle` method**

Add this method to `LmStudioClient` (e.g. directly after `pickOfficialDomain`):

```java
  public Optional<String> summarizeArticle(String articleBody) {
    try {
      var rendered = summaryPromptTemplate.replace("{{article}}", articleBody);

      var messages = mapper.createArrayNode();
      messages.add(mapper.createObjectNode().put("role", "user").put("content", rendered));

      var requestNode = mapper.createObjectNode();
      requestNode.put("model", properties.model());
      requestNode.set("messages", messages);
      requestNode.put("temperature", 0);
      requestNode.put("max_tokens", 120);

      var rawResponse = post(mapper.writeValueAsString(requestNode));
      var content =
          mapper
              .readTree(rawResponse)
              .path("choices")
              .path(0)
              .path("message")
              .path("content")
              .asText("");
      content = content.replaceAll("(?s)```json\\s*", "").replaceAll("(?s)```\\s*", "").strip();
      return content.isBlank() ? Optional.empty() : Optional.of(content);
    } catch (Exception e) {
      log.warn("article_summary_failed reason={}", e.getMessage());
      return Optional.empty();
    }
  }
```

- [ ] **Step 4: Compile to verify green**

Run: `../mvnw test-compile`
Expected: BUILD SUCCESS.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/synapsedx/mailing/companydomain/client/LmStudioClient.java \
        src/main/resources/article-summary-prompt.md
git commit -m "feat(company-domain-lookup): add LmStudioClient.summarizeArticle"
```

---

### Task 5: `ArticleSummaryProcessor`

Resolves the article path (configured `articlesDir`, else the input CSV's parent), **throws if the file is missing**, strips leading YAML frontmatter, and asks the LLM for a summary (empty on LLM failure).

**Files:**
- Create: `src/main/java/com/synapsedx/mailing/companydomain/batch/processor/ArticleSummaryProcessor.java`
- Test: `src/test/java/com/synapsedx/mailing/companydomain/batch/processor/ArticleSummaryProcessorTest.java`

- [ ] **Step 1: Write the failing processor test**

`ArticleSummaryProcessorTest.java`:

```java
package com.synapsedx.mailing.companydomain.batch.processor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.synapsedx.mailing.companydomain.client.LmStudioClient;
import com.synapsedx.mailing.companydomain.config.CompanyDomainProperties;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

class ArticleSummaryProcessorTest {

  private CompanyDomainProperties propsForDir(Path dir) {
    return new CompanyDomainProperties("ignored.csv", "out.csv", dir.toString(), 10, 5);
  }

  @Test
  void stripsFrontmatterBeforeSendingBodyToLlm(@TempDir Path dir) throws Exception {
    Files.writeString(
        dir.resolve("a.md"),
        "---\ntitle: T\nurl: http://x\n---\nCorps réel de l'article.\n");
    var llm = mock(LmStudioClient.class);
    when(llm.summarizeArticle(anyString())).thenReturn(Optional.of("Un résumé."));
    var processor = new ArticleSummaryProcessor(propsForDir(dir), llm);

    var result = processor.process("a.md");

    assertThat(result.articleId()).isEqualTo("a.md");
    assertThat(result.summary()).isEqualTo("Un résumé.");
    var captor = ArgumentCaptor.forClass(String.class);
    Mockito.verify(llm).summarizeArticle(captor.capture());
    assertThat(captor.getValue()).isEqualTo("Corps réel de l'article.");
  }

  @Test
  void usesWholeContentWhenNoFrontmatter(@TempDir Path dir) throws Exception {
    Files.writeString(dir.resolve("b.md"), "Pas de frontmatter ici.");
    var llm = mock(LmStudioClient.class);
    when(llm.summarizeArticle(anyString())).thenReturn(Optional.of("ok"));
    var processor = new ArticleSummaryProcessor(propsForDir(dir), llm);

    processor.process("b.md");

    var captor = ArgumentCaptor.forClass(String.class);
    Mockito.verify(llm).summarizeArticle(captor.capture());
    assertThat(captor.getValue()).isEqualTo("Pas de frontmatter ici.");
  }

  @Test
  void throwsWhenArticleFileMissing(@TempDir Path dir) {
    var llm = mock(LmStudioClient.class);
    var processor = new ArticleSummaryProcessor(propsForDir(dir), llm);

    assertThatThrownBy(() -> processor.process("missing.md"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("missing.md");
  }

  @Test
  void emptySummaryWhenLlmReturnsEmpty(@TempDir Path dir) throws Exception {
    Files.writeString(dir.resolve("c.md"), "Contenu.");
    var llm = mock(LmStudioClient.class);
    when(llm.summarizeArticle(anyString())).thenReturn(Optional.empty());
    var processor = new ArticleSummaryProcessor(propsForDir(dir), llm);

    assertThat(processor.process("c.md").summary()).isEqualTo("");
  }

  @Test
  void returnsSummaryVerbatimWithoutTruncatingLongOutput(@TempDir Path dir) throws Exception {
    Files.writeString(dir.resolve("d.md"), "Contenu.");
    var longSummary = "mot ".repeat(50).trim(); // 50 words, well over 30
    var llm = mock(LmStudioClient.class);
    when(llm.summarizeArticle(anyString())).thenReturn(Optional.of(longSummary));
    var processor = new ArticleSummaryProcessor(propsForDir(dir), llm);

    assertThat(processor.process("d.md").summary()).isEqualTo(longSummary);
  }
}
```

- [ ] **Step 2: Run it to verify it fails**

Run: `../mvnw -Dtest=ArticleSummaryProcessorTest test`
Expected: COMPILATION FAILURE — `ArticleSummaryProcessor` does not exist.

- [ ] **Step 3: Create the processor**

`ArticleSummaryProcessor.java`:

```java
package com.synapsedx.mailing.companydomain.batch.processor;

import com.synapsedx.mailing.companydomain.client.LmStudioClient;
import com.synapsedx.mailing.companydomain.config.CompanyDomainProperties;
import com.synapsedx.mailing.companydomain.model.ArticleSummary;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ArticleSummaryProcessor implements ItemProcessor<String, ArticleSummary> {

  private final CompanyDomainProperties properties;
  private final LmStudioClient lmStudioClient;

  @Override
  public ArticleSummary process(String articleId) {
    var path = resolveArticlePath(articleId);
    if (!Files.exists(path)) {
      throw new IllegalStateException("article file not found: " + path);
    }
    log.info("article_summary_start article={}", articleId);
    var body = stripFrontmatter(readFile(path));
    var summary = lmStudioClient.summarizeArticle(body).orElse("");
    log.info("article_summary_done article={} words={}", articleId, wordCount(summary));
    return new ArticleSummary(articleId, summary);
  }

  private Path resolveArticlePath(String articleId) {
    var dir = properties.articlesDir();
    if (dir != null && !dir.isBlank()) {
      return Path.of(dir).resolve(articleId);
    }
    return Path.of(properties.inputCsv()).toAbsolutePath().getParent().resolve(articleId);
  }

  private String readFile(Path path) {
    try {
      return Files.readString(path);
    } catch (Exception e) {
      throw new IllegalStateException("failed to read article file: " + path, e);
    }
  }

  private String stripFrontmatter(String content) {
    var lines = content.split("\n", -1);
    if (lines.length > 0 && lines[0].strip().equals("---")) {
      for (var i = 1; i < lines.length; i++) {
        if (lines[i].strip().equals("---")) {
          return String.join("\n", Arrays.asList(lines).subList(i + 1, lines.length)).strip();
        }
      }
    }
    return content.strip();
  }

  private int wordCount(String summary) {
    return summary.isBlank() ? 0 : summary.trim().split("\\s+").length;
  }
}
```

- [ ] **Step 4: Run the test to verify it passes**

Run: `../mvnw -Dtest=ArticleSummaryProcessorTest test`
Expected: PASS (all five cases).

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/synapsedx/mailing/companydomain/batch/processor/ArticleSummaryProcessor.java \
        src/test/java/com/synapsedx/mailing/companydomain/batch/processor/ArticleSummaryProcessorTest.java
git commit -m "feat(company-domain-lookup): add ArticleSummaryProcessor"
```

---

### Task 6: Thread `articleId` and `summary` through models and enrichment (output unchanged)

Widen `ContactRow` with `articleId` and `EnrichedContactRow` with `summary`, populate them in the reader/processor, and inject the summary map into `ContactEnrichProcessor`. The writer still emits only `domain` in this task, so the IT stays green; the writer's output change happens in Task 7.

**Files:**
- Modify: `src/main/java/com/synapsedx/mailing/companydomain/model/ContactRow.java`
- Modify: `src/main/java/com/synapsedx/mailing/companydomain/model/EnrichedContactRow.java`
- Modify: `src/main/java/com/synapsedx/mailing/companydomain/batch/reader/ContactsCsvReader.java`
- Modify: `src/main/java/com/synapsedx/mailing/companydomain/batch/processor/ContactEnrichProcessor.java`
- Modify: `src/test/java/com/synapsedx/mailing/companydomain/batch/reader/ContactsCsvReaderTest.java`
- Modify: `src/test/java/com/synapsedx/mailing/companydomain/batch/processor/ContactEnrichProcessorTest.java`
- Modify: `src/test/java/com/synapsedx/mailing/companydomain/batch/writer/EnrichedContactsCsvWriterTest.java` (constructor arity only)

- [ ] **Step 1: Update `ContactsCsvReaderTest` to expect `articleId` (failing test first)**

In `emitsEveryRowWithHeadersAndCompany()`, after the `first.company()` assertion, add:

```java
    assertThat(first.articleId()).isEqualTo("result-10-01.md");
```

Add a new test method for the fail-fast on a missing `article_id` column:

```java
  @Test
  void failsFastWhenArticleIdColumnMissing() {
    var props =
        new CompanyDomainProperties(
            "src/test/resources/fixtures/no-article-id-header.csv", "out.csv", "", 10, 5);
    var reader = new ContactsCsvReader(props);
    org.assertj.core.api.Assertions.assertThatThrownBy(reader::read)
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("article_id");
  }
```

- [ ] **Step 2: Update `ContactEnrichProcessorTest` for the new constructor and `summary`**

Replace the test body with the two-map constructor and `articleId`/`summary` expectations:

```java
package com.synapsedx.mailing.companydomain.batch.processor;

import static org.assertj.core.api.Assertions.assertThat;

import com.synapsedx.mailing.companydomain.batch.support.ArticleSummaryMap;
import com.synapsedx.mailing.companydomain.batch.support.CompanyDomainMap;
import com.synapsedx.mailing.companydomain.model.ContactRow;
import java.util.List;
import org.junit.jupiter.api.Test;

class ContactEnrichProcessorTest {

  @Test
  void enrichesWithMappedDomainAndSummary() {
    var domainMap = new CompanyDomainMap();
    domainMap.put("FACTOFRANCE", "factofrance.com");
    var summaryMap = new ArticleSummaryMap();
    summaryMap.put("r.md", "Résumé de l'article.");
    var processor = new ContactEnrichProcessor(domainMap, summaryMap);

    var row =
        new ContactRow(
            List.of("first_name", "last_name", "company", "article_id"),
            List.of("Philippe", "Mutin", " factofrance ", "r.md"),
            " factofrance ",
            "r.md");

    var enriched = processor.process(row);

    assertThat(enriched.contact()).isSameAs(row);
    assertThat(enriched.domain()).isEqualTo("factofrance.com");
    assertThat(enriched.summary()).isEqualTo("Résumé de l'article.");
  }

  @Test
  void emptyDomainAndSummaryWhenNotInMaps() {
    var processor = new ContactEnrichProcessor(new CompanyDomainMap(), new ArticleSummaryMap());
    var row =
        new ContactRow(
            List.of("first_name", "last_name", "company", "article_id"),
            List.of("a", "b", "Unknown", "x.md"),
            "Unknown",
            "x.md");

    var enriched = processor.process(row);
    assertThat(enriched.domain()).isEqualTo("");
    assertThat(enriched.summary()).isEqualTo("");
  }
}
```

- [ ] **Step 3: Fix `ContactRow` constructor calls in `EnrichedContactsCsvWriterTest` (arity only — keep asserting `domain` output)**

Add the 4th `ContactRow` arg (the `article_id` value already present in `values`) and keep `EnrichedContactRow` 2-arg for now. The three `EnrichedContactRow(new ContactRow(...), domain)` blocks become:

```java
                new EnrichedContactRow(
                    new ContactRow(
                        headers, List.of("A", "B", "Factofrance", "r1.md"), "Factofrance", "r1.md"),
                    "factofrance.com"),
                new EnrichedContactRow(
                    new ContactRow(
                        headers, List.of("C", "D", "Unknown", "r2.md"), "Unknown", "r2.md"),
                    ""))));
```
```java
                new EnrichedContactRow(
                    new ContactRow(
                        headers,
                        List.of("E,F", "G\"H", "Crédit Mutuel", "r3.md"),
                        "Crédit Mutuel",
                        "r3.md"),
                    "creditmutuel.fr"))));
```

> Do **not** change the assertions in this test yet — Task 7 updates them when the writer starts emitting `summary`.

- [ ] **Step 4: Run the affected tests to verify they fail to compile**

Run: `../mvnw -Dtest=ContactsCsvReaderTest,ContactEnrichProcessorTest,EnrichedContactsCsvWriterTest test`
Expected: COMPILATION FAILURE — `ContactRow.articleId()`, `EnrichedContactRow.summary()`, and the two-arg `ContactEnrichProcessor` constructor don't exist yet.

- [ ] **Step 5: Add `articleId` to `ContactRow`**

`ContactRow.java`:

```java
package com.synapsedx.mailing.companydomain.model;

import java.util.List;

public record ContactRow(
    List<String> headers, List<String> values, String company, String articleId) {}
```

- [ ] **Step 6: Add `summary` to `EnrichedContactRow`**

`EnrichedContactRow.java`:

```java
package com.synapsedx.mailing.companydomain.model;

public record EnrichedContactRow(ContactRow contact, String domain, String summary) {}
```

- [ ] **Step 7: Populate `articleId` in `ContactsCsvReader` (locate column, fail-fast)**

In `ContactsCsvReader.java` add a field beside `companyIdx`:

```java
  private int companyIdx = -1;
  private int articleIdIdx = -1;
```

In `read()`, replace the return block:

```java
    var company =
        companyIdx >= 0 && companyIdx < fields.size() ? fields.get(companyIdx).trim() : "";
    var articleId =
        articleIdIdx >= 0 && articleIdIdx < fields.size() ? fields.get(articleIdIdx).trim() : "";
    return new ContactRow(headers, fields, company, articleId);
```

In `init()`, after the `companyIdx` lookup/guard block, add:

```java
    articleIdIdx = headers.indexOf("article_id");
    if (articleIdIdx < 0) {
      throw new IllegalStateException(
          "input CSV missing 'article_id' column; headers=" + headers + " file=" + path);
    }
```

- [ ] **Step 8: Inject the summary map into `ContactEnrichProcessor`**

Replace `ContactEnrichProcessor.java`:

```java
package com.synapsedx.mailing.companydomain.batch.processor;

import com.synapsedx.mailing.companydomain.batch.support.ArticleSummaryMap;
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

  private final CompanyDomainMap domainMap;
  private final ArticleSummaryMap summaryMap;

  @Override
  public EnrichedContactRow process(ContactRow row) {
    var key = row.company().trim().toUpperCase(Locale.ROOT);
    var domain = domainMap.get(key);
    var summary = summaryMap.get(row.articleId());
    return new EnrichedContactRow(row, domain, summary);
  }
}
```

> `EnrichedContactsCsvWriter` still compiles: it only reads `row.contact()` and `row.domain()`; the added `summary` component is ignored until Task 7.

- [ ] **Step 9: Run the full suite to verify green**

Run: `../mvnw test`
Expected: BUILD SUCCESS. The IT still passes — output remains `…,article_id,domain` because the writer is unchanged and step 2 is not yet wired.

- [ ] **Step 10: Commit**

```bash
git add src/main src/test
git commit -m "feat(company-domain-lookup): thread articleId and summary through enrichment models"
```

---

### Task 7: Wire the summary step, emit the `summary` column, and update integration tests

Activate the feature end-to-end: add the `resolve-summaries` step to the job, make the writer emit the trailing `summary` column, and update/extend the integration tests. The writer change and IT updates ship together so the build stays green.

**Files:**
- Modify: `src/main/java/com/synapsedx/mailing/companydomain/batch/CompanyDomainLookupJobConfig.java`
- Modify: `src/main/java/com/synapsedx/mailing/companydomain/batch/writer/EnrichedContactsCsvWriter.java`
- Modify: `src/test/java/com/synapsedx/mailing/companydomain/batch/writer/EnrichedContactsCsvWriterTest.java`
- Modify: `src/test/java/com/synapsedx/mailing/companydomain/CompanyDomainLookupJobIT.java`
- Create fixtures: `src/test/resources/fixtures/r1.md` … `r5.md`, `it-contacts-missing-article.csv`
- Create: `src/test/java/com/synapsedx/mailing/companydomain/CompanyDomainLookupMissingArticleIT.java`

- [ ] **Step 1: Update `EnrichedContactsCsvWriterTest` to expect the `summary` column**

Wrap each `EnrichedContactRow` with a third (summary) arg and update the assertions. Replace the two `write(...)` blocks' rows and the assertion block:

```java
    writer.write(
        new Chunk<>(
            List.of(
                new EnrichedContactRow(
                    new ContactRow(
                        headers, List.of("A", "B", "Factofrance", "r1.md"), "Factofrance", "r1.md"),
                    "factofrance.com",
                    "Résumé un"),
                new EnrichedContactRow(
                    new ContactRow(
                        headers, List.of("C", "D", "Unknown", "r2.md"), "Unknown", "r2.md"),
                    "",
                    ""))));
    writer.write(
        new Chunk<>(
            List.of(
                new EnrichedContactRow(
                    new ContactRow(
                        headers,
                        List.of("E,F", "G\"H", "Crédit Mutuel", "r3.md"),
                        "Crédit Mutuel",
                        "r3.md"),
                    "creditmutuel.fr",
                    "Résumé, avec virgule"))));

    var lines = Files.readAllLines(out);
    assertThat(lines).hasSize(4);
    assertThat(lines.get(0)).isEqualTo("first_name,last_name,company,article_id,domain,summary");
    assertThat(lines.get(1)).isEqualTo("A,B,Factofrance,r1.md,factofrance.com,Résumé un");
    assertThat(lines.get(2)).isEqualTo("C,D,Unknown,r2.md,,");
    assertThat(lines.get(3))
        .isEqualTo("\"E,F\",\"G\"\"H\",Crédit Mutuel,r3.md,creditmutuel.fr,\"Résumé, avec virgule\"");
```

- [ ] **Step 2: Run the writer test to verify it fails**

Run: `../mvnw -Dtest=EnrichedContactsCsvWriterTest test`
Expected: FAIL — header is `…,domain` (missing `,summary`) and rows lack the summary field.

- [ ] **Step 3: Make the writer emit `summary`**

In `EnrichedContactsCsvWriter.java`, change the header line:

```java
      var headerLine = String.join(",", headers) + ",domain,summary\n";
```

And in the row loop, replace the domain-append line with domain + summary:

```java
      sb.append(",").append(escapeCsv(row.domain()));
      sb.append(",").append(escapeCsv(row.summary())).append("\n");
```

- [ ] **Step 4: Run the writer test to verify it passes**

Run: `../mvnw -Dtest=EnrichedContactsCsvWriterTest test`
Expected: PASS.

- [ ] **Step 5: Wire the `resolve-summaries` step into the job**

In `CompanyDomainLookupJobConfig.java`, add imports:

```java
import com.synapsedx.mailing.companydomain.batch.processor.ArticleSummaryProcessor;
import com.synapsedx.mailing.companydomain.batch.reader.UniqueArticleReader;
import com.synapsedx.mailing.companydomain.batch.writer.ArticleSummaryMapWriter;
import com.synapsedx.mailing.companydomain.model.ArticleSummary;
```

Add the injected dependencies (alongside the existing `final` fields):

```java
  private final UniqueArticleReader uniqueArticleReader;
  private final ArticleSummaryProcessor articleSummaryProcessor;
  private final ArticleSummaryMapWriter articleSummaryMapWriter;
```

Insert the new step into the job flow between the two existing steps:

```java
  @Bean
  public Job companyDomainLookupJob() {
    return new JobBuilder("company-domain-lookup-job", jobRepository)
        .start(resolveDomainsStep())
        .next(resolveSummariesStep())
        .next(enrichContactsStep())
        .build();
  }

  @Bean
  public Step resolveSummariesStep() {
    return new StepBuilder("resolveSummariesStep", jobRepository)
        .<String, ArticleSummary>chunk(1, transactionManager)
        .reader(uniqueArticleReader)
        .processor(articleSummaryProcessor)
        .writer(articleSummaryMapWriter)
        .build();
  }
```

- [ ] **Step 6: Create the article fixtures next to the IT input CSV**

The IT input is `src/test/resources/fixtures/it-contacts.csv`, so articles resolve to that same directory. Create `r1.md` through `r5.md`, each with frontmatter + body, e.g. `r1.md`:

```markdown
---
title: Article 1
url: http://example.com/1
---
Corps de l'article numéro un pour le test d'intégration.
```

Create `r2.md`, `r3.md`, `r4.md`, `r5.md` the same way (vary the number word). Content is irrelevant to assertions — the LM Studio mock returns a fixed summary — but the files **must exist** or the job hard-fails.

- [ ] **Step 7: Update the happy-path IT (`CompanyDomainLookupJobIT`)**

Add imports:

```java
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
```

Replace the single LM Studio stub with two content-differentiated stubs (domain prompt contains `Compagnie`; summary prompt contains the ASCII substring `30 mots`):

```java
    LMSTUDIO_MOCK.stubFor(
        post(urlEqualTo("/v1/chat/completions"))
            .withRequestBody(containing("Compagnie"))
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody(
                        "{\"choices\":[{\"message\":{\"content\":\"{\\\"domain\\\":\\\"https://www.factofrance.com/\\\"}\"}}]}")));
    LMSTUDIO_MOCK.stubFor(
        post(urlEqualTo("/v1/chat/completions"))
            .withRequestBody(containing("30 mots"))
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody(
                        "{\"choices\":[{\"message\":{\"content\":\"Résumé de l'article test\"}}]}")));
```

Replace the output assertions (new trailing `summary` column; every row gets the stubbed summary):

```java
    var lines = Files.readAllLines(OUTPUT_CSV);
    assertThat(lines).hasSize(6);
    assertThat(lines.get(0))
        .isEqualTo("first_name,last_name,company,article_id,domain,summary");
    assertThat(lines.get(1))
        .isEqualTo("Philippe,Mutin,Factofrance,r1.md,factofrance.com,Résumé de l'article test");
    assertThat(lines.get(2))
        .isEqualTo("Marc,Tyan,Factofrance,r2.md,factofrance.com,Résumé de l'article test");
    assertThat(lines.get(3))
        .isEqualTo("Beñat,Cazanave,ARTZAINAK,r3.md,,Résumé de l'article test");
    assertThat(lines.get(4))
        .isEqualTo("Isabelle,Gautier,Crédit Mutuel,r4.md,,Résumé de l'article test");
    assertThat(lines.get(5)).isEqualTo("Jean,Test,,r5.md,,Résumé de l'article test");

    LMSTUDIO_MOCK.verify(
        5, postRequestedFor(urlEqualTo("/v1/chat/completions")).withRequestBody(containing("30 mots")));
```

- [ ] **Step 8: Create the missing-article IT**

Create `it-contacts-missing-article.csv` (references an article file that does not exist):

```csv
first_name,last_name,company,article_id
Jean,Test,Factofrance,does-not-exist.md
```

Create `CompanyDomainLookupMissingArticleIT.java`. It points `input-csv` at the missing-article fixture, stubs DataForSEO to return an empty SERP (so step 1 needs no LLM call), and asserts the job ends `FAILED` because step 2 hard-fails on the absent file:

```java
package com.synapsedx.mailing.companydomain;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import java.nio.charset.StandardCharsets;
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
class CompanyDomainLookupMissingArticleIT {

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
            "http://127.0.0.1:" + DATAFORSEO_MOCK.port() + "/v3/serp/google/organic/live/advanced");
    r.add("lmstudio.server", () -> "http://127.0.0.1:" + LMSTUDIO_MOCK.port());
    r.add(
        "company-domain.input-csv",
        () -> "src/test/resources/fixtures/it-contacts-missing-article.csv");
    r.add("company-domain.output-csv", () -> "target/it-missing-article.csv");
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
  void resetMocks() throws Exception {
    DATAFORSEO_MOCK.resetAll();
    LMSTUDIO_MOCK.resetAll();
    var emptyBody =
        new String(
            new ClassPathResource("fixtures/dataforseo-organic-empty.json")
                .getInputStream()
                .readAllBytes(),
            StandardCharsets.UTF_8);
    DATAFORSEO_MOCK.stubFor(
        post(urlEqualTo("/v3/serp/google/organic/live/advanced"))
            .willReturn(aResponse().withStatus(200).withBody(emptyBody)));
  }

  @Test
  void jobFailsWhenArticleFileMissing() throws Exception {
    var execution = jobLauncherTestUtils.launchJob();
    assertThat(execution.getStatus().toString()).isEqualTo("FAILED");
  }
}
```

- [ ] **Step 9: Run both ITs**

Run: `../mvnw -Dtest=CompanyDomainLookupJobIT,CompanyDomainLookupMissingArticleIT test`
Expected: PASS — happy path produces the 6-line CSV with summaries and verifies 5 summary calls; missing-article job ends FAILED.

- [ ] **Step 10: Commit**

```bash
git add src/main src/test
git commit -m "feat(company-domain-lookup): wire per-article summary step and emit summary column"
```

---

### Task 8: Full build, format, and final verification

**Files:** none (verification only).

- [ ] **Step 1: Format**

Run: `../mvnw spotless:apply`
Expected: BUILD SUCCESS (reformats any drift introduced by the edits above).

- [ ] **Step 2: Full module build with tests**

Run: `../mvnw clean install`
Expected: BUILD SUCCESS, 0 failures/errors. (If spotless reformatted anything, the build verifies it stayed consistent.)

- [ ] **Step 3: Commit any formatting changes (if spotless modified files)**

```bash
git add -A
git commit -m "style(company-domain-lookup): apply spotless formatting" || echo "nothing to format"
```

---

## Verification against the spec

- **`role` passes through** — unchanged behavior; covered by the existing header-driven reader/writer and the happy-path IT header assertion (`first_name,last_name,company,article_id,domain,summary`).
- **Summary once per article** — `UniqueArticleReader` dedups `article_id`; the IT verifies exactly 5 summary calls for 5 distinct articles.
- **≤30 words, no truncation** — prompt requests 30 words; `ArticleSummaryProcessorTest.returnsSummaryVerbatimWithoutTruncatingLongOutput` proves no client-side trimming.
- **Body only (frontmatter stripped)** — `ArticleSummaryProcessorTest.stripsFrontmatterBeforeSendingBodyToLlm`.
- **Articles dir = CSV parent, overridable** — `articlesDir` resolution in `ArticleSummaryProcessor`; binding covered by `PropertiesBindingTest`.
- **Missing file → hard fail** — `ArticleSummaryProcessorTest.throwsWhenArticleFileMissing` (unit) and `CompanyDomainLookupMissingArticleIT` (job FAILED).
- **LLM error → empty summary (soft)** — `ArticleSummaryProcessorTest.emptySummaryWhenLlmReturnsEmpty`.
- **Trailing `summary` column** — `EnrichedContactsCsvWriterTest` + happy-path IT.
