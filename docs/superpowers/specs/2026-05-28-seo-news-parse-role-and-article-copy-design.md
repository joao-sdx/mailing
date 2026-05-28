# seo-news-parse: add role field and copy articles with contacts

## Goal

Extend `seo-news-parse` to capture the `role` field already returned by the
LLM, and copy each source article into the CSV output directory whenever the
LLM identified at least one contact.

## Motivation

The LM Studio prompt (`seoit-prompt.md`) already requests a `role` field for
every identified person, but `LmStudioExtractProcessor` discards it when
building `PersonRow`. Keeping the role is required for downstream qualification
(decision-makers vs. operational staff).

Copying the source `.md` next to the CSV makes the CSV self-contained: a
reviewer can open the contact list and read the article that produced any row
without traversing back to the upstream `seo-news-search` output tree.

## Scope

### In scope

1. Add `role` column to `PersonRow` and the output CSV.
2. Wire the existing `role` value from the LLM JSON response into `PersonRow`.
3. Copy the source `.md` article into the CSV's parent directory when, and
   only when, the LLM returned at least one contact for that article.

### Out of scope (YAGNI)

- Prompt changes: `seoit-prompt.md` already asks for `role`.
- Configurable articles destination: articles land next to the CSV
  (`Path.of(outputCsv).getParent()`). No new property.
- Deduplication of copies across runs: `REPLACE_EXISTING` covers re-runs.
- Subdir structure for copied articles: flat layout in the CSV directory.

## Design

### Domain model

`PersonRow` gains a `role` field positioned between name and company:

```java
public record PersonRow(
    String firstName,
    String lastName,
    String role,
    String company,
    String articleId) {}
```

New wrapper record carries the source article alongside its extracted rows:

```java
public record ArticleContacts(Path sourceArticle, List<PersonRow> rows) {}
```

`ArticleContacts` lives in `com.synapsedx.mailing.seonewsparse.model`.

### Processor

`LmStudioExtractProcessor` changes its output type from
`List<PersonRow>` to `ArticleContacts`:

```java
public class LmStudioExtractProcessor
    implements ItemProcessor<Path, ArticleContacts> { ... }
```

When the LLM returns rows, the processor maps each JSON object and adds
`m.getOrDefault("role", "")` as the new `role` argument:

```java
new PersonRow(
    m.getOrDefault("prenom", ""),
    m.getOrDefault("nom", ""),
    m.getOrDefault("role", ""),
    m.getOrDefault("societe", ""),
    articleId)
```

The processor wraps the resulting list with the original `articlePath` and
returns `new ArticleContacts(articlePath, contacts)`.

Empty-result behavior is unchanged: when the LLM returns `[]` or fails, the
processor returns `null` so Spring Batch filters the item.

### Writer

`ContactsCsvWriter` changes its parameter to `ArticleContacts`:

```java
public class ContactsCsvWriter
    implements ItemWriter<ArticleContacts>, StepExecutionListener { ... }
```

CSV header becomes:

```
first_name,last_name,role,company,article_id
```

For each `ArticleContacts` in the chunk:

1. Append `rows` to the CSV (existing logic, with `role` interleaved).
2. If `rows` is non-empty, copy `sourceArticle` to
   `csvPath.getParent().resolve(sourceArticle.getFileName())` using
   `StandardCopyOption.REPLACE_EXISTING`.

If `csvPath.getParent()` is `null` (CSV written to working dir), the article
is copied into the working directory.

The existing aggregate-then-skip behavior is preserved: if every
`ArticleContacts` in the chunk has empty `rows`, the aggregate is empty and
the writer returns early without writing CSV or copying anything (per-item
copy is gated on `rows.isEmpty() == false`).

### Job configuration

`SeoNewsParseJobConfig` updates the chunk generics from
`<Path, List<PersonRow>>` to `<Path, ArticleContacts>` to match the new
processor/writer signatures. No step-flow or chunk-size changes.

## Tests

- **`LmStudioExtractProcessorTest`**: extend the mocked LM Studio response to
  include `role`. Assert the returned `ArticleContacts` has the expected
  `sourceArticle` path and that each row carries the role value.
- **`ContactsCsvWriterTest`**: two new cases on top of existing coverage:
  - When an `ArticleContacts` has non-empty rows, the source `.md` is copied
    next to the CSV.
  - When an `ArticleContacts` has empty rows, no file is copied (even if the
    chunk also contains items with rows).
  - CSV header and a sample row include the `role` column in the right
    position.
- **`MarkdownDirectoryReaderTest`**: unchanged.

## Risks

- **Signature ripple**: changing the processor/writer generics touches
  `SeoNewsParseJobConfig`. Mechanical; caught at compile time.
- **Article filename collisions**: two articles with the same filename from
  different upstream subdirs would overwrite each other in the flat copy
  layout. Acceptable for now — the upstream `seo-news-search` writer prefixes
  filenames with the `file-prefix` value, which is now sequence-unique after
  the previous commit.
- **CSV column order**: downstream consumers reading by index break. None
  exist yet inside this repo; consumers read by header.
