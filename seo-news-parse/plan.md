# seo-news-parse Implementation Plan

## Context

The `seo-news-search` module writes `.md` files (YAML frontmatter + article body) to `seo-news-search/output/`. This new module reads those articles, runs each through a local LM Studio model to extract named people and their companies, and produces a CSV for downstream use.

The `mailing-pipeline` module already has all the LM Studio plumbing — `SeoidLlmProcessor` (HTTP client, OpenAI-compatible JSON parsing, prompt loading), `seoit-prompt.md` (returns `prenom/nom/societe/role/email` from French B2B articles), and `SeoidCsvWriter` (manual CSV escaping). This module reuses those patterns rather than introducing new abstractions.

Output: `output/contacts.csv` per run with columns `first_name,last_name,company,article_name`. Fixed filename, truncated and rewritten on each run.

## Architecture

Standalone Maven module `seo-news-parse`, sibling to `seo-news-search` and `pipeline`. Single-step Spring Batch job, chunk size 1:

```
MarkdownDirectoryReader → LmStudioExtractProcessor → ContactsCsvWriter
        Path                  List<PersonRow>
```

- **Reader** scans the configured input dir for `*.md` files upfront (queue iterator pattern, same as `InseeEnrichReader`), emits one `Path` at a time.
- **Processor** reads the file, parses the YAML frontmatter to pull out `title`, sends body to LM Studio with the existing prompts, parses the JSON array response, returns `List<PersonRow>` mapped from `prenom/nom/societe` + article title. Empty result → returns `null` (Spring Batch filter, skips article).
- **Writer** appends rows to `output/contacts.csv`. Header written once on first non-empty chunk; file truncated on first write of the run.

## Critical Files

### Parent pom update

- **Modify** `/Users/joao.violante/IdeaProjects/mailing/pom.xml`: add `<module>seo-news-parse</module>` to `<modules>` (sibling to `seo-news-search`, `pipeline`).

### New module — mirror `seo-news-search` exactly

Package root: `com.synapsedx.mailing.seonewsparse`

- **Create** `seo-news-parse/pom.xml` — copy `seo-news-search/pom.xml` verbatim, change `<artifactId>` to `seo-news-parse`. Same dependencies (no extra deps needed; reuses `java.net.http.HttpClient` + Jackson).
- **Create** `seo-news-parse/Taskfile.yml` — copy from `seo-news-search/Taskfile.yml`, update `desc` strings (writes `contacts.csv`).
- **Create** `seo-news-parse/src/main/resources/application.yml`:
  ```yaml
  spring:
    config:
      import: optional:file:.env[.properties]
    batch:
      job:
        enabled: true
      jdbc:
        initialize-schema: always

  lmstudio:
    server: http://127.0.0.1:1234
    model: nvidia/nemotron-3-nano-4b
    key: ${openai.key:lm-studio}
    connect-timeout-seconds: 10
    request-timeout-seconds: 60

  seo-news-parse:
    input-dir: ../seo-news-search/output
    output-csv: output/contacts.csv
  ```
- **Copy verbatim from `mailing-pipeline/src/main/resources/`** into `seo-news-parse/src/main/resources/`:
  - `seoit-prompt.md`
  - `seoit-system-prompt.md`

### Java sources (`seo-news-parse/src/main/java/com/synapsedx/mailing/seonewsparse/`)

- **`SeoNewsParseApplication.java`** — `@SpringBootApplication` + `@EnableConfigurationProperties({LmStudioProperties.class, SeoNewsParseProperties.class})`. Mirrors `SeoNewsApplication`.

- **`config/LmStudioProperties.java`** — record:
  ```java
  @ConfigurationProperties("lmstudio")
  public record LmStudioProperties(
      String server, String model, String key,
      int connectTimeoutSeconds, int requestTimeoutSeconds) {}
  ```
  (Flat shape — `mailing-pipeline` nests under `openai`, but flattening is simpler when this module only talks to LM Studio.)

- **`config/SeoNewsParseProperties.java`** — record:
  ```java
  @ConfigurationProperties("seo-news-parse")
  public record SeoNewsParseProperties(String inputDir, String outputCsv) {}
  ```

- **`model/PersonRow.java`** — record `(String firstName, String lastName, String company, String articleName)`.

- **`model/Article.java`** — record `(Path path, String title, String body)`. (Internal use; processor parses frontmatter into this.)

- **`batch/reader/MarkdownDirectoryReader.java`** — implements `ItemReader<Path>`. On first `read()`, runs `Files.find(inputDir, 1, isMarkdownFile)`, collects all `.md` paths into a queue. Subsequent `read()` calls pop from queue; returns `null` when empty. Pattern modeled on `InseeEnrichReader`. Throws on missing input dir at startup (`@PostConstruct` validation).

- **`batch/processor/LmStudioExtractProcessor.java`** — implements `ItemProcessor<Path, List<PersonRow>>`. Adapted from `SeoidLlmProcessor`:
  - `@PostConstruct init()`: builds `HttpClient`, loads `seoit-system-prompt.md` and `seoit-prompt.md` from classpath.
  - `process(Path)`:
    1. Read file, split frontmatter from body on `---` delimiters (article body starts after the second `---`).
    2. Extract `title` line from frontmatter (simple `title: "..."` regex; matches the format `MarkdownFileWriter` writes).
    3. Build chat-completion request (system + user with `{article_content}` replaced by body), POST to `${server}/v1/chat/completions` with `Bearer` auth.
    4. Parse `choices[0].message.content`, strip markdown code fences (same regex as `SeoidLlmProcessor`), parse JSON array.
    5. Map each entry → `PersonRow(prenom, nom, societe, articleTitle)`. Drop `role`/`email` (not in output).
    6. Return `null` if empty/`[]` — Spring Batch filter skips the article.
  - On HTTP failure or JSON parse error: log warning, return `null` (don't fail the whole job).

- **`batch/writer/ContactsCsvWriter.java`** — implements `ItemWriter<List<PersonRow>>`. Adapted from `SeoidCsvWriter`:
  - Header: `first_name,last_name,company,article_name`.
  - First `write()` call: `Files.createDirectories(parent)`, write header with `CREATE + TRUNCATE_EXISTING` (overwrites previous run).
  - Subsequent calls: append rows.
  - Same `escapeCsv()` helper (quotes-if-contains-comma-quote-newline, double-quotes for inner quotes).
  - Iterates each `List<PersonRow>` in the chunk, flattens rows.

- **`batch/SeoNewsParseJobConfig.java`** — `@Configuration`. Mirrors `SeoNewsJobConfig`:
  ```java
  @Bean Job seoNewsParseJob() { ...JobBuilder("seo-news-parse", ...)...start(extractStep()).build(); }
  @Bean Step extractStep() {
    return new StepBuilder("extractStep", jobRepository)
        .<Path, List<PersonRow>>chunk(1, transactionManager)
        .reader(markdownDirectoryReader)
        .processor(lmStudioExtractProcessor)
        .writer(contactsCsvWriter)
        .build();
  }
  ```

### Tests (`seo-news-parse/src/test/java/...`)

- **`MarkdownDirectoryReaderTest`** — uses `@TempDir`, writes 2 fake `.md` files, asserts reader returns both then `null`. Asserts missing dir → exception at init.
- **`LmStudioExtractProcessorTest`** — uses `@TempDir` to write a `.md` file with known frontmatter + body. Mocks `HttpClient.send()` to return a canned chat-completion JSON. Asserts:
  - Happy path: 2-element JSON array → 2 `PersonRow` with correct article title.
  - Empty array `[]` → returns `null`.
  - HTTP non-200 → returns `null` (no exception).
  - Frontmatter parsing: extracts `title: "Some title"` correctly.
- **`ContactsCsvWriterTest`** — uses `@TempDir`, writes one chunk of 2 rows + a second chunk, asserts:
  - File contains header once.
  - All rows present in order.
  - Comma in company name → field quoted.

## Patterns to Reuse (do not reinvent)

| What | Where to copy from | File |
|---|---|---|
| LM Studio HTTP client + chat-completion request/parse | `mailing-pipeline` | `seo/batch/processor/SeoidLlmProcessor.java` |
| YAML frontmatter format produced by upstream | `seo-news-search` | `batch/writer/MarkdownFileWriter.java` (lines 43–68) |
| CSV writer with manual escaping | `mailing-pipeline` | `seo/batch/writer/SeoidCsvWriter.java` |
| Directory-scanning `ItemReader` (queue-of-paths pattern) | `pipeline` | `siren/enrich/InseeEnrichReader.java` |
| Module shell (pom, Taskfile, app class, job config) | `seo-news-search` | entire module |
| Extraction prompts (system + user) | `mailing-pipeline` | `resources/seoit-system-prompt.md`, `resources/seoit-prompt.md` |

## Verification

1. **Build:** from repo root, `./mvnw -pl seo-news-parse -am package -DskipTests` succeeds.
2. **Unit tests:** `cd seo-news-parse && task test` → all 3 test classes green.
3. **End-to-end** with LM Studio running on `127.0.0.1:1234` and `seo-news-search/output/` populated:
   - `cd seo-news-parse && task` (alias for `task default` → `../mvnw spring-boot:run`).
   - Job logs show `llm_call_start article=...` per markdown file, then `csv_written` lines.
   - `output/contacts.csv` exists, has header row, has one row per extracted person.
   - Spot-check: open one source `.md`, confirm a named person from the body appears in the CSV with the article's `title` from the frontmatter as `article_name`.
4. **Empty-result handling:** point `input-dir` at an empty directory; rerun. Job completes with no rows; CSV not created (or contains header only — both acceptable).
5. **LM Studio down:** stop LM Studio, rerun against populated input. Job completes without throwing; logs show HTTP errors per article; no rows in CSV.