# company-domain-lookup: add per-article summary column

## Goal

Extend the existing `company-domain-lookup` module so its enriched output CSV
carries one additional column, `summary`: a French summary (max 30 words) of
the source article each contact came from. The summary is produced by an
additional LM Studio LLM call and is computed **once per article**, then
copied onto every contact row that references that article.

The `role` column already added upstream by `seo-news-parse` requires **no
code change** — the module is header-driven and copies all input columns
through verbatim (verified: `ContactsCsvReader` reads every field into
`values`, `EnrichedContactsCsvWriter` writes them all back before appending
`domain`; `CompanyDomainLookupJobIT` already exercises both header shapes).

Chain (unchanged inputs, one extra output column):

```
seo-news-parse/output/<run>/contacts-<run>.csv  (+ result-*.md alongside)
      → company-domain-lookup
      → contacts-<run>-with-domain.csv  (… , domain, summary)
```

## Motivation

Downstream outreach wants a short, human-readable summary of the article that
surfaced each contact, so a sender can frame a relevant message without
re-reading the source. An article frequently yields several
persons/companies, so the summary must be deduplicated per `article_id` — we
do not pay one LLM call per contact row when many rows share an article.

## Scope

### In scope

1. A new batch step that, for each **unique** `article_id`, reads the source
   `.md`, strips YAML frontmatter, and asks LM Studio for a ≤30-word French
   summary.
2. An in-memory `article_id → summary` map carried between steps.
3. Appending a trailing `summary` column to the enriched output CSV.
4. Header-driven location of the `article_id` column (by name, fail fast if
   absent), mirroring the existing `company`-column handling.

### Out of scope (YAGNI)

- Post-processing/truncating the LLM summary. The prompt requests ≤30 words;
  we write whatever the model returns. No client-side word trimming.
- Caching summaries across runs.
- Summarizing frontmatter metadata or extracting article fields other than
  the body.
- Changing how `role` (or any other input column) is handled — it already
  passes through.
- Reordering or normalising existing columns.

## Design

### Three-step Spring Batch job

The job grows from two steps to three. The new step sits between the existing
two and is structurally a twin of Step 1, keyed on article instead of
company.

```
Job "company-domain-lookup-job"
  Step 1 "resolve-domains"     chunk 1
    UniqueCompanyReader → DomainLookupProcessor → CompanyDomainMapWriter
    (unchanged)

  Step 2 "resolve-summaries"   chunk 1                              (NEW)
    UniqueArticleReader      emits each unique article_id once
    ArticleSummaryProcessor  article_id → summary (or "")
    ArticleSummaryMapWriter  put(articleId, summary) into ArticleSummaryMap

  Step 3 "enrich-contacts"     chunk 100
    ContactsCsvReader        emits each row (now also carrying article_id)
    ContactEnrichProcessor   build EnrichedContactRow from both maps
    EnrichedContactsCsvWriter  writes original columns + domain + summary
```

`ArticleSummaryMap` is a new Spring bean wrapping a
`ConcurrentHashMap<String,String>`, keyed by the raw `article_id` value. It is
the seam between Step 2 and Step 3 and lives for the job execution, exactly
mirroring `CompanyDomainMap`.

### Configuration

`CompanyDomainProperties` gains one optional field, `articlesDir`. When blank
(default), articles are resolved against the **parent directory of
`inputCsv`** — which is where `seo-news-parse` copies the `result-*.md` files
next to the contacts CSV.

```java
@ConfigurationProperties("company-domain")
public record CompanyDomainProperties(
    String inputCsv,
    String outputCsv,
    String articlesDir,   // NEW; blank → inputCsv parent
    int serpDepth,
    int serpTopN) {}
```

```yaml
company-domain:
  input-csv: input/contacts.csv
  output-csv: output/contacts-with-domain.csv
  articles-dir: ""        # blank → directory containing input-csv
  serp-depth: 10
  serp-top-n: 5
```

CLI override: `--company-domain.articles-dir=/path/to/articles`.

### Reader: `UniqueArticleReader`

`ItemReader<String>`, modelled on `UniqueCompanyReader`:

1. Read the CSV header, locate the `article_id` column **by name**; fail fast
   with a clear error if absent.
2. Parse each data line with the existing `CsvLineParser`.
3. Extract the `article_id` value, `trim()` it.
4. Skip rows with an empty `article_id`.
5. Deduplicate, preserving first-seen order.
6. Return each distinct `article_id` once; `null` when exhausted.

### Processor: `ArticleSummaryProcessor`

`ItemProcessor<String, ArticleSummary>` where
`record ArticleSummary(String articleId, String summary)`.

Algorithm for an input `articleId`:

1. Resolve the article path:
   - if `articlesDir` is non-blank → `Path.of(articlesDir).resolve(articleId)`,
   - else → `Path.of(inputCsv).toAbsolutePath().getParent().resolve(articleId)`.
2. **If the file does not exist → throw** (`IllegalStateException` with the
   resolved path). This aborts the job: a missing source article is a
   data-integrity error, not a soft miss.
3. Read the file, strip a leading YAML frontmatter block: if the content
   starts with a line that is exactly `---`, drop everything up to and
   including the next `---` line; otherwise use the content as-is.
4. Call `LmStudioClient.summarizeArticle(body)` → `Optional<String>`.
5. Empty result (LLM error, blank response) → `summary = ""` (soft).
6. Return `new ArticleSummary(articleId, summary)`.

Logging (project convention):

```
log.info("article_summary_start article={}", articleId);
log.info("article_summary_done article={} words={}", articleId, wordCount);
log.warn("article_summary_failed article={} reason={}", articleId, reason);
```

### Writer: `ArticleSummaryMapWriter`

For each `ArticleSummary` in the chunk: `map.put(articleId, summary)`.
Trivial, mirroring `CompanyDomainMapWriter`.

### Client: `LmStudioClient.summarizeArticle`

New method on the existing client:

```java
public Optional<String> summarizeArticle(String articleBody)
```

- Loads a new prompt template `article-summary-prompt.md` from the classpath
  (alongside `domain-pick-prompt.md`), rendered by replacing `{{article}}`.
- Reuses the existing `post(...)` helper, `temperature: 0`. `max_tokens`
  sized for a short summary (e.g. 120).
- Extracts `choices[0].message.content`, strips any ``` fences (same defensive
  handling already used for the domain call), `strip()`s it.
- Blank content → `Optional.empty()`. Any exception → log `WARN`, return
  `Optional.empty()`. **No word truncation.**

Prompt (`article-summary-prompt.md`):

```
Résume l'article suivant en 30 mots maximum, en français.
Renvoie uniquement le résumé, sans préambule ni guillemets.

Article:
{{article}}
```

### Step 3: enrichment changes

- **`ContactRow`** gains `articleId`:
  `record ContactRow(List<String> headers, List<String> values, String company, String articleId)`.
  `ContactsCsvReader` locates the `article_id` column by name (fail fast if
  absent) in addition to `company`, and populates it.
- **`EnrichedContactRow`** gains `summary`:
  `record EnrichedContactRow(ContactRow contact, String domain, String summary)`.
- **`ContactEnrichProcessor`** looks up
  `domain = companyDomainMap.getOrDefault(companyKey, "")` and
  `summary = articleSummaryMap.getOrDefault(contact.articleId(), "")`.
- **`EnrichedContactsCsvWriter`**: header line becomes
  `String.join(",", headers) + ",domain,summary\n"`; each row appends
  `escapeCsv(domain)` then `escapeCsv(summary)`.

## Tests (TDD)

- **`UniqueArticleReaderTest`** — dedup of `article_id` (preserving first-seen
  order, skipping empties); fail-fast when the `article_id` column is absent.
- **`ArticleSummaryProcessorTest`** — frontmatter stripped before the LLM
  sees the body; **missing file throws**; LLM returns empty → empty summary;
  LLM success → that summary returned verbatim (no truncation even for >30
  words).
- **`EnrichedContactsCsvWriterTest`** — updated for the trailing `summary`
  column: header `…,domain,summary` written once; summaries containing commas
  / quotes are escaped; empty summary serialised as an empty field.
- **`ContactsCsvReaderTest`** — `article_id` value populated on `ContactRow`;
  fail-fast when the column is absent.
- **`CompanyDomainLookupJobIT`** — extend the fixture: place `result-*.md`
  files next to the input CSV; WireMock stubs both the domain pick and the
  summary calls. Assert: LM Studio summarize called exactly once per unique
  article; each output row carries the summary mapped from its `article_id`;
  output header is `…,domain,summary`. Add a case where an `article_id`
  references a non-existent file and assert the job fails.

## Risks

- **Hard fail on missing article**: chosen deliberately — a contacts CSV whose
  `article_id` points at a missing `.md` indicates a broken upstream chain,
  and we want that surfaced loudly rather than producing rows with silently
  empty summaries. The trade-off is that one missing file aborts the whole
  run.
- **LLM word-count adherence**: the prompt requests ≤30 words but the model
  may overshoot; per decision we do **not** truncate. Downstream consumers
  treat `summary` as best-effort prose.
- **Frontmatter detection**: the strip logic keys on a leading `---` line. An
  article that legitimately starts its body with `---` after no frontmatter
  is an unlikely edge; the simple rule is accepted for the MVP.
- **Sequential LLM cost**: Step 2 adds one LM Studio call per unique article
  (chunk 1, sequential). At current volumes (tens of articles) this is
  seconds; no throttle needed since LM Studio is local.
