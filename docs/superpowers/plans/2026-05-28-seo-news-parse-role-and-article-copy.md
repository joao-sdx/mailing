# seo-news-parse: role field + article copy — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Extend `seo-news-parse` so the LLM-derived `role` value is persisted in the contacts CSV, and each source article whose LLM call returned at least one contact is copied next to the CSV.

**Architecture:** Add `role` to `PersonRow` and introduce an `ArticleContacts(sourceArticle, rows)` wrapper. `LmStudioExtractProcessor` returns `ArticleContacts`; `ContactsCsvWriter` consumes `ArticleContacts`, writes the new column, and copies the source `.md` next to the CSV when rows are non-empty. `SeoNewsParseJobConfig` chunk generics update mechanically.

**Tech Stack:** Java 21, Spring Boot 3.4.5, Spring Batch, Jackson, JUnit 5 + AssertJ, Maven.

**Spec:** `docs/superpowers/specs/2026-05-28-seo-news-parse-role-and-article-copy-design.md`

---

## File Structure

- **Modify:** `seo-news-parse/src/main/java/com/synapsedx/mailing/seonewsparse/model/PersonRow.java` — add `role` between `lastName` and `company`.
- **Create:** `seo-news-parse/src/main/java/com/synapsedx/mailing/seonewsparse/model/ArticleContacts.java` — wrapper record carrying source path + rows.
- **Modify:** `seo-news-parse/src/main/java/com/synapsedx/mailing/seonewsparse/batch/processor/LmStudioExtractProcessor.java` — return `ArticleContacts`, map `role`.
- **Modify:** `seo-news-parse/src/main/java/com/synapsedx/mailing/seonewsparse/batch/writer/ContactsCsvWriter.java` — accept `ArticleContacts`, new header, copy article when rows non-empty.
- **Modify:** `seo-news-parse/src/main/java/com/synapsedx/mailing/seonewsparse/batch/SeoNewsParseJobConfig.java` — update chunk generics.
- **Modify:** `seo-news-parse/src/test/java/com/synapsedx/mailing/seonewsparse/batch/processor/LmStudioExtractProcessorTest.java` — assert `role` + `sourceArticle`.
- **Modify:** `seo-news-parse/src/test/java/com/synapsedx/mailing/seonewsparse/batch/writer/ContactsCsvWriterTest.java` — new header, copy semantics.

---

## Task 1: Add `role` to `PersonRow` (red → green)

**Files:**
- Modify: `seo-news-parse/src/main/java/com/synapsedx/mailing/seonewsparse/model/PersonRow.java`

- [ ] **Step 1: Add `role` field to `PersonRow`**

Replace the entire file contents with:

```java
package com.synapsedx.mailing.seonewsparse.model;

public record PersonRow(
    String firstName, String lastName, String role, String company, String articleId) {}
```

- [ ] **Step 2: Verify compile failure surfaces all consumers**

Run from repo root: `./mvnw -pl seo-news-parse -am compile -q`

Expected: BUILD FAILURE. Compilation errors should originate in:
- `LmStudioExtractProcessor.java` (constructor invocation with 4 args)
- `ContactsCsvWriter.java` (the writer itself still compiles, but references will break in tests/job config later)
- `LmStudioExtractProcessorTest.java`, `ContactsCsvWriterTest.java` (constructor calls)
- `SeoNewsParseJobConfig.java` (only if the generic is read; will still compile but is now misleading)

This is the expected TDD red signal — do NOT fix in this task.

- [ ] **Step 3: Do not commit yet**

The codebase is intentionally broken; commit after Task 4 once green. Move to Task 2.

---

## Task 2: Add `ArticleContacts` wrapper record

**Files:**
- Create: `seo-news-parse/src/main/java/com/synapsedx/mailing/seonewsparse/model/ArticleContacts.java`

- [ ] **Step 1: Create `ArticleContacts`**

Create the file with this exact content:

```java
package com.synapsedx.mailing.seonewsparse.model;

import java.nio.file.Path;
import java.util.List;

public record ArticleContacts(Path sourceArticle, List<PersonRow> rows) {}
```

- [ ] **Step 2: Run license header task**

From repo root: `task update-license`

Expected: command succeeds (header added if the project uses one for this module; no-op otherwise). Per `CLAUDE.md`, never paste headers manually.

- [ ] **Step 3: Compile-check the new file in isolation**

Run: `./mvnw -pl seo-news-parse -am compile -q`

Expected: still failing because of Task 1 changes — that is fine. Confirm there are no new errors mentioning `ArticleContacts.java` itself.

---

## Task 3: Update `LmStudioExtractProcessor` to return `ArticleContacts` with `role`

**Files:**
- Modify: `seo-news-parse/src/main/java/com/synapsedx/mailing/seonewsparse/batch/processor/LmStudioExtractProcessor.java`
- Modify: `seo-news-parse/src/test/java/com/synapsedx/mailing/seonewsparse/batch/processor/LmStudioExtractProcessorTest.java`

- [ ] **Step 1: Rewrite the processor tests for the new return type**

Replace the contents of `LmStudioExtractProcessorTest.java` with:

```java
package com.synapsedx.mailing.seonewsparse.batch.processor;

import static org.assertj.core.api.Assertions.assertThat;

import com.synapsedx.mailing.seonewsparse.config.LmStudioProperties;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LmStudioExtractProcessorTest {

  @TempDir Path tempDir;

  private LmStudioExtractProcessor processor;
  private String mockResponse;

  @BeforeEach
  void setup() {
    var props = new LmStudioProperties("http://localhost:1234", "test-model", "test-key", 5, 30);
    processor =
        new LmStudioExtractProcessor(props) {
          @Override
          String post(String body) {
            return mockResponse;
          }
        };
    processor.systemPrompt = "system prompt";
    processor.userPromptTemplate = "User: {article_content}";
  }

  @Test
  void happyPath_returnsArticleContactsWithRole() throws Exception {
    var mdFile = tempDir.resolve("article.md");
    Files.writeString(
        mdFile,
        """
        ---
        title: "Mon Article"
        url: https://example.com
        ---

        Body content here.
        """);

    mockResponse =
        """
        {"choices":[{"message":{"content":"[{\\"prenom\\":\\"Jean\\",\\"nom\\":\\"Dupont\\",\\"societe\\":\\"BNP Paribas\\",\\"role\\":\\"Directeur\\",\\"email\\":\\"\\"}]"}}]}
        """;

    var result = processor.process(mdFile);

    assertThat(result).isNotNull();
    assertThat(result.sourceArticle()).isEqualTo(mdFile);
    assertThat(result.rows()).hasSize(1);
    var person = result.rows().getFirst();
    assertThat(person.firstName()).isEqualTo("Jean");
    assertThat(person.lastName()).isEqualTo("Dupont");
    assertThat(person.role()).isEqualTo("Directeur");
    assertThat(person.company()).isEqualTo("BNP Paribas");
    assertThat(person.articleId()).isEqualTo("article.md");
  }

  @Test
  void missingRoleDefaultsToEmptyString() throws Exception {
    var mdFile = tempDir.resolve("no-role.md");
    Files.writeString(mdFile, "Body without frontmatter.");

    mockResponse =
        """
        {"choices":[{"message":{"content":"[{\\"prenom\\":\\"Alice\\",\\"nom\\":\\"Martin\\",\\"societe\\":\\"Acme\\"}]"}}]}
        """;

    var result = processor.process(mdFile);

    assertThat(result).isNotNull();
    assertThat(result.rows()).hasSize(1);
    assertThat(result.rows().getFirst().role()).isEqualTo("");
  }

  @Test
  void emptyArrayReturnsNull() throws Exception {
    var mdFile = tempDir.resolve("empty.md");
    Files.writeString(
        mdFile,
        """
        ---
        title: "Empty Article"
        ---

        Some body.
        """);

    mockResponse =
        """
        {"choices":[{"message":{"content":"[]"}}]}
        """;

    var result = processor.process(mdFile);

    assertThat(result).isNull();
  }

  @Test
  void httpErrorReturnsNull() throws Exception {
    var mdFile = tempDir.resolve("error.md");
    Files.writeString(
        mdFile,
        """
        ---
        title: "Error Article"
        ---

        Some body.
        """);

    processor =
        new LmStudioExtractProcessor(
            new LmStudioProperties("http://localhost:1234", "test-model", "test-key", 5, 30)) {
          @Override
          String post(String body) {
            throw new IllegalStateException("LM Studio error status=500");
          }
        };
    processor.systemPrompt = "system prompt";
    processor.userPromptTemplate = "User: {article_content}";

    var result = processor.process(mdFile);

    assertThat(result).isNull();
  }

  @Test
  void noFrontmatterUsesFilenameAsArticleId() throws Exception {
    var mdFile = Files.createTempFile(tempDir, "plain", ".md");
    Files.writeString(mdFile, "Just some plain text body with no frontmatter delimiters at all.");

    mockResponse =
        """
        {"choices":[{"message":{"content":"[{\\"prenom\\":\\"Marie\\",\\"nom\\":\\"Martin\\",\\"societe\\":\\"AXA\\",\\"role\\":\\"DG\\",\\"email\\":\\"\\"}]"}}]}
        """;

    var result = processor.process(mdFile);

    assertThat(result).isNotNull();
    assertThat(result.rows()).hasSize(1);
    assertThat(result.rows().getFirst().articleId()).isEqualTo(mdFile.getFileName().toString());
    assertThat(result.rows().getFirst().role()).isEqualTo("DG");
  }
}
```

- [ ] **Step 2: Run the test class — verify it fails to compile**

Run: `./mvnw -pl seo-news-parse -am test-compile -q`

Expected: BUILD FAILURE referencing `LmStudioExtractProcessorTest.java` because `process` still returns `List<PersonRow>` and `PersonRow` has no `role` accessor. This is the TDD red phase.

- [ ] **Step 3: Update `LmStudioExtractProcessor` implementation**

Edit `LmStudioExtractProcessor.java`:

(a) Change the import for `PersonRow` — add a second import:

```java
import com.synapsedx.mailing.seonewsparse.model.ArticleContacts;
import com.synapsedx.mailing.seonewsparse.model.PersonRow;
```

(b) Change the `implements` clause:

```java
public class LmStudioExtractProcessor implements ItemProcessor<Path, ArticleContacts> {
```

(c) Change the `process` method signature and the two return statements. The full method body becomes:

```java
  @Override
  public ArticleContacts process(Path articlePath) {
    try {
      var rawContent = Files.readString(articlePath);

      var parts = FRONTMATTER_SPLIT.split(rawContent, -1);
      String body;
      if (parts.length >= 3) {
        body = parts[2];
      } else {
        body = rawContent;
      }

      var userPrompt = userPromptTemplate.replace("{article_content}", body);
      var messages = mapper.createArrayNode();
      messages.add(mapper.createObjectNode().put("role", "system").put("content", systemPrompt));
      messages.add(mapper.createObjectNode().put("role", "user").put("content", userPrompt));
      var requestNode = mapper.createObjectNode();
      requestNode.put("model", properties.model());
      requestNode.set("messages", messages);
      requestNode.put("temperature", 0.1);

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

      if (content.isEmpty() || content.matches("\\[\\s*\\]")) {
        log.warn("llm_empty_response article={}", articlePath.getFileName());
        return null;
      }

      var articleId = articlePath.getFileName().toString();
      var raw = mapper.readValue(content, new TypeReference<List<Map<String, String>>>() {});
      var contacts =
          raw.stream()
              .map(
                  m ->
                      new PersonRow(
                          m.getOrDefault("prenom", ""),
                          m.getOrDefault("nom", ""),
                          m.getOrDefault("role", ""),
                          m.getOrDefault("societe", ""),
                          articleId))
              .toList();

      log.info(
          "llm_contacts_found article={} count={}", articlePath.getFileName(), contacts.size());
      return new ArticleContacts(articlePath, contacts);

    } catch (Exception e) {
      log.warn(
          "llm_extract_failed article={} reason={}", articlePath.getFileName(), e.getMessage());
      return null;
    }
  }
```

Note: the unused local `frontmatter` from the original is dropped since `body` is the only branch consumed.

- [ ] **Step 4: Run the processor tests — green**

Run: `./mvnw -pl seo-news-parse -Dtest=LmStudioExtractProcessorTest test -q`

Expected: BUILD SUCCESS, 5 tests pass.

Note: `SeoNewsParseJobConfig.java` will still fail to compile because the chunk generic is `<Path, List<PersonRow>>`. Compile-only invocation may surface this — `-Dtest=...` runs the whole module compile. If `test-compile` fails on the job config, defer the run until Task 4 completes. Specifically: if you see `incompatible types: LmStudioExtractProcessor cannot be converted to ItemProcessor<Path,List<PersonRow>>` from `SeoNewsParseJobConfig.java`, proceed to Task 4 first, then come back and re-run this step.

- [ ] **Step 5: Do not commit yet — job config still broken**

Move to Task 4.

---

## Task 4: Update `SeoNewsParseJobConfig` chunk generics

**Files:**
- Modify: `seo-news-parse/src/main/java/com/synapsedx/mailing/seonewsparse/batch/SeoNewsParseJobConfig.java`

- [ ] **Step 1: Update imports and chunk type**

Open `SeoNewsParseJobConfig.java`. Replace these two imports:

```java
import com.synapsedx.mailing.seonewsparse.model.PersonRow;
...
import java.util.List;
```

with:

```java
import com.synapsedx.mailing.seonewsparse.model.ArticleContacts;
```

(Drop the `java.util.List` import — no longer used.)

Then change the chunk line in `extractStep()`:

```java
        .<Path, ArticleContacts>chunk(1, transactionManager)
```

- [ ] **Step 2: Compile the module**

Run: `./mvnw -pl seo-news-parse -am compile -q`

Expected: BUILD SUCCESS. Production code is now type-consistent end to end.

- [ ] **Step 3: Run processor tests again to confirm green**

Run: `./mvnw -pl seo-news-parse -Dtest=LmStudioExtractProcessorTest test -q`

Expected: BUILD SUCCESS, 5 tests pass. (CSV writer tests will still fail — addressed in Task 5.)

- [ ] **Step 4: Do not commit yet — writer tests broken**

Move to Task 5.

---

## Task 5: Update `ContactsCsvWriter` for new header + article copy

**Files:**
- Modify: `seo-news-parse/src/main/java/com/synapsedx/mailing/seonewsparse/batch/writer/ContactsCsvWriter.java`
- Modify: `seo-news-parse/src/test/java/com/synapsedx/mailing/seonewsparse/batch/writer/ContactsCsvWriterTest.java`

- [ ] **Step 1: Rewrite the writer tests**

Replace `ContactsCsvWriterTest.java` with:

```java
package com.synapsedx.mailing.seonewsparse.batch.writer;

import static org.assertj.core.api.Assertions.assertThat;

import com.synapsedx.mailing.seonewsparse.config.SeoNewsParseProperties;
import com.synapsedx.mailing.seonewsparse.model.ArticleContacts;
import com.synapsedx.mailing.seonewsparse.model.PersonRow;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.batch.item.Chunk;

class ContactsCsvWriterTest {

  @TempDir Path tempDir;

  private Path writeArticle(String filename, String body) throws Exception {
    var src = tempDir.resolve(filename);
    Files.writeString(src, body);
    return src;
  }

  @Test
  void writesHeaderOnceAndRoleColumn() throws Exception {
    var csvPath = tempDir.resolve("out").resolve("contacts.csv").toString();
    var writer = new ContactsCsvWriter(new SeoNewsParseProperties("input", csvPath));

    var article1 = writeArticle("a1.md", "body 1");
    var article2 = writeArticle("a2.md", "body 2");
    var article3 = writeArticle("a3.md", "body 3");

    var firstChunk =
        new Chunk<>(
            List.of(
                new ArticleContacts(
                    article1,
                    List.of(
                        new PersonRow("Jean", "Dupont", "Directeur", "BNP", "a1.md"),
                        new PersonRow("Marie", "Martin", "DG", "AXA", "a1.md"))),
                new ArticleContacts(
                    article2, List.of(new PersonRow("Pierre", "Bernard", "DSI", "LVMH", "a2.md")))));
    writer.write(firstChunk);

    var secondChunk =
        new Chunk<>(
            List.of(
                new ArticleContacts(
                    article3,
                    List.of(new PersonRow("Alice", "Durand", "CFO", "Total", "a3.md")))));
    writer.write(secondChunk);

    var content = Files.readString(Path.of(csvPath));
    var lines = content.lines().toList();

    assertThat(lines.getFirst()).isEqualTo("first_name,last_name,role,company,article_id");
    assertThat(
            lines.stream()
                .filter(l -> l.equals("first_name,last_name,role,company,article_id"))
                .count())
        .isEqualTo(1);
    assertThat(content).contains("Jean,Dupont,Directeur,BNP,a1.md");
    assertThat(content).contains("Marie,Martin,DG,AXA,a1.md");
    assertThat(content).contains("Pierre,Bernard,DSI,LVMH,a2.md");
    assertThat(content).contains("Alice,Durand,CFO,Total,a3.md");
  }

  @Test
  void escapesCommaInFields() throws Exception {
    var csvPath = tempDir.resolve("contacts.csv").toString();
    var writer = new ContactsCsvWriter(new SeoNewsParseProperties("input", csvPath));
    var article = writeArticle("a.md", "body");

    var chunk =
        new Chunk<>(
            List.of(
                new ArticleContacts(
                    article,
                    List.of(new PersonRow("Jean", "Dupont", "Dir, Adj.", "BNP, Paribas", "a.md")))));
    writer.write(chunk);

    var content = Files.readString(Path.of(csvPath));
    assertThat(content).contains("\"Dir, Adj.\"");
    assertThat(content).contains("\"BNP, Paribas\"");
  }

  @Test
  void copiesArticleNextToCsvWhenRowsPresent() throws Exception {
    var outDir = tempDir.resolve("out");
    var csvPath = outDir.resolve("contacts.csv").toString();
    var writer = new ContactsCsvWriter(new SeoNewsParseProperties("input", csvPath));
    var article = writeArticle("kept.md", "article body kept");

    var chunk =
        new Chunk<>(
            List.of(
                new ArticleContacts(
                    article, List.of(new PersonRow("X", "Y", "Role", "Co", "kept.md")))));
    writer.write(chunk);

    var copied = outDir.resolve("kept.md");
    assertThat(copied).exists();
    assertThat(Files.readString(copied)).isEqualTo("article body kept");
  }

  @Test
  void doesNotCopyArticleWhenRowsEmpty() throws Exception {
    var outDir = tempDir.resolve("out");
    var csvPath = outDir.resolve("contacts.csv").toString();
    var writer = new ContactsCsvWriter(new SeoNewsParseProperties("input", csvPath));
    var skipped = writeArticle("skipped.md", "should not be copied");
    var kept = writeArticle("kept.md", "should be copied");

    var chunk =
        new Chunk<>(
            List.of(
                new ArticleContacts(skipped, List.of()),
                new ArticleContacts(
                    kept, List.of(new PersonRow("X", "Y", "Role", "Co", "kept.md")))));
    writer.write(chunk);

    assertThat(outDir.resolve("skipped.md")).doesNotExist();
    assertThat(outDir.resolve("kept.md")).exists();
  }

  @Test
  void emptyChunkIsSkipped() throws Exception {
    var csvPath = tempDir.resolve("out").resolve("contacts.csv").toString();
    var writer = new ContactsCsvWriter(new SeoNewsParseProperties("input", csvPath));
    var article = writeArticle("a.md", "body");

    var chunk = new Chunk<>(List.of(new ArticleContacts(article, List.<PersonRow>of())));
    writer.write(chunk);

    assertThat(Path.of(csvPath)).doesNotExist();
    assertThat(tempDir.resolve("out").resolve("a.md")).doesNotExist();
  }

  @Test
  void overwritesArticleOnReRun() throws Exception {
    var outDir = tempDir.resolve("out");
    var csvPath = outDir.resolve("contacts.csv").toString();
    Files.createDirectories(outDir);
    Files.writeString(outDir.resolve("a.md"), "stale content");

    var writer = new ContactsCsvWriter(new SeoNewsParseProperties("input", csvPath));
    var article = writeArticle("a.md", "fresh content");

    var chunk =
        new Chunk<>(
            List.of(
                new ArticleContacts(
                    article, List.of(new PersonRow("X", "Y", "Role", "Co", "a.md")))));
    writer.write(chunk);

    assertThat(Files.readString(outDir.resolve("a.md"))).isEqualTo("fresh content");
  }
}
```

- [ ] **Step 2: Confirm tests fail to compile**

Run: `./mvnw -pl seo-news-parse -Dtest=ContactsCsvWriterTest test -q`

Expected: BUILD FAILURE — the writer still implements `ItemWriter<List<PersonRow>>` and the header constant is missing `role`. TDD red.

- [ ] **Step 3: Update `ContactsCsvWriter` to new contract**

Replace the contents of `ContactsCsvWriter.java` with:

```java
package com.synapsedx.mailing.seonewsparse.batch.writer;

import com.synapsedx.mailing.seonewsparse.config.SeoNewsParseProperties;
import com.synapsedx.mailing.seonewsparse.model.ArticleContacts;
import com.synapsedx.mailing.seonewsparse.model.PersonRow;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
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
public class ContactsCsvWriter
    implements ItemWriter<ArticleContacts>, StepExecutionListener {

  private static final String HEADER = "first_name,last_name,role,company,article_id";

  private final SeoNewsParseProperties properties;
  private boolean headerWritten = false;

  @Override
  public void beforeStep(StepExecution stepExecution) {
    headerWritten = false;
  }

  @Override
  public void write(Chunk<? extends ArticleContacts> chunk) throws Exception {
    var rows = new ArrayList<PersonRow>();
    for (var item : chunk.getItems()) {
      rows.addAll(item.rows());
    }
    if (rows.isEmpty()) {
      return;
    }

    var csvPath = Path.of(properties.outputCsv());
    var parent = csvPath.getParent();
    if (parent != null) {
      Files.createDirectories(parent);
    }

    if (!headerWritten) {
      Files.writeString(
          csvPath, HEADER + "\n", StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
      headerWritten = true;
    }

    var sb = new StringBuilder();
    for (var row : rows) {
      sb.append(escapeCsv(row.firstName()))
          .append(",")
          .append(escapeCsv(row.lastName()))
          .append(",")
          .append(escapeCsv(row.role()))
          .append(",")
          .append(escapeCsv(row.company()))
          .append(",")
          .append(escapeCsv(row.articleId()))
          .append("\n");
    }
    Files.writeString(csvPath, sb.toString(), StandardOpenOption.APPEND);
    log.info("csv_written file={} rows={}", csvPath.getFileName(), rows.size());

    for (var item : chunk.getItems()) {
      if (item.rows().isEmpty()) {
        continue;
      }
      var destDir = parent != null ? parent : Path.of("");
      var dest = destDir.resolve(item.sourceArticle().getFileName().toString());
      Files.copy(item.sourceArticle(), dest, StandardCopyOption.REPLACE_EXISTING);
      log.info("article_copied src={} dest={}", item.sourceArticle().getFileName(), dest);
    }
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

- [ ] **Step 4: Run writer tests — green**

Run: `./mvnw -pl seo-news-parse -Dtest=ContactsCsvWriterTest test -q`

Expected: BUILD SUCCESS, 6 tests pass.

- [ ] **Step 5: Run the full module test suite**

Run: `./mvnw -pl seo-news-parse test -q`

Expected: BUILD SUCCESS, all tests pass (processor + writer + reader).

- [ ] **Step 6: Apply formatting**

Run: `./mvnw -pl seo-news-parse spotless:apply -q`

Expected: success. Re-run tests if spotless modified anything.

- [ ] **Step 7: Apply license headers**

Run from repo root: `task update-license`

Expected: success.

- [ ] **Step 8: Final module build**

Run: `./mvnw -pl seo-news-parse -am verify -q`

Expected: BUILD SUCCESS.

- [ ] **Step 9: Commit**

```bash
git add seo-news-parse/src/main/java/com/synapsedx/mailing/seonewsparse/model/PersonRow.java \
        seo-news-parse/src/main/java/com/synapsedx/mailing/seonewsparse/model/ArticleContacts.java \
        seo-news-parse/src/main/java/com/synapsedx/mailing/seonewsparse/batch/processor/LmStudioExtractProcessor.java \
        seo-news-parse/src/main/java/com/synapsedx/mailing/seonewsparse/batch/writer/ContactsCsvWriter.java \
        seo-news-parse/src/main/java/com/synapsedx/mailing/seonewsparse/batch/SeoNewsParseJobConfig.java \
        seo-news-parse/src/test/java/com/synapsedx/mailing/seonewsparse/batch/processor/LmStudioExtractProcessorTest.java \
        seo-news-parse/src/test/java/com/synapsedx/mailing/seonewsparse/batch/writer/ContactsCsvWriterTest.java

git commit -m "feat(seo-news-parse): capture LLM role and copy source article next to CSV

- PersonRow gains role field between lastName and company
- New ArticleContacts(sourceArticle, rows) wrapper carries source path
- LmStudioExtractProcessor maps role from LLM JSON and returns ArticleContacts
- ContactsCsvWriter writes role column and copies the source .md next to
  the CSV when at least one contact was extracted
- SeoNewsParseJobConfig chunk generics updated to <Path, ArticleContacts>"
```

---

## Self-Review

**Spec coverage:**
- Add `role` to `PersonRow` between name and company → Task 1.
- New `ArticleContacts` wrapper in `…seonewsparse.model` → Task 2.
- Processor returns `ArticleContacts`, maps `m.getOrDefault("role", "")` → Task 3.
- Empty/HTTP-error processor branches still return `null` → Task 3 (`emptyArrayReturnsNull`, `httpErrorReturnsNull`).
- Writer accepts `ArticleContacts`, header includes `role` between `last_name` and `company` → Task 5.
- Copy `sourceArticle` next to CSV with `REPLACE_EXISTING` only when `rows` non-empty → Task 5 (`copiesArticleNextToCsvWhenRowsPresent`, `doesNotCopyArticleWhenRowsEmpty`, `overwritesArticleOnReRun`).
- Aggregate-then-skip preserved (empty chunk writes nothing) → Task 5 (`emptyChunkIsSkipped`).
- `csvPath.getParent()` null falls back to working dir → handled in writer code (`destDir = parent != null ? parent : Path.of("")`). Not asserted by a test; risk is low, working directory is implicit.
- `SeoNewsParseJobConfig` chunk generics updated → Task 4.

**Placeholder scan:** No TBDs, no "add error handling," every code step ships complete code.

**Type consistency:** `PersonRow(firstName, lastName, role, company, articleId)` — 5 args — used consistently in processor mapping, both test files, and writer column order. `ArticleContacts(sourceArticle, rows)` — same accessor names everywhere. Writer references `item.rows()` and `item.sourceArticle()` matching the record. Chunk generic `<Path, ArticleContacts>` matches processor return type and writer input type.

---

## Execution Handoff

**Plan complete and saved to `docs/superpowers/plans/2026-05-28-seo-news-parse-role-and-article-copy.md`. Two execution options:**

1. **Subagent-Driven (recommended)** — I dispatch a fresh subagent per task, review between tasks, fast iteration.
2. **Inline Execution** — Execute tasks in this session using executing-plans, batch execution with checkpoints.

**Which approach?**
