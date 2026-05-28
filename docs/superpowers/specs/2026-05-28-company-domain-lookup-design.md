# company-domain-lookup: enrich contacts CSV with company domain

## Goal

New standalone Maven module `company-domain-lookup` that reads the contacts
CSV produced by `seo-news-parse`, looks up the official web domain for each
unique company, and writes an enriched CSV with all original columns plus a
new `domain` column.

Chain:

```
seo-news-parse/output/<run>/contacts-<run>.csv
      → company-domain-lookup
      → contacts-<run>-with-domain.csv
```

## Motivation

Downstream mailing/outreach steps need a company web domain to compose sender
identity, validate the company exists, and build email patterns. The contacts
CSV today only carries the company name as it appeared in the source article,
which is not enough to address email or look up authoritative company data.

Deduplicating on company name before calling the search API is required so we
don't pay N DataForSEO + LLM calls for the same company when an article
yielded several contacts.

## Scope

### In scope

1. Read a contacts CSV produced by `seo-news-parse`. The reader is
   **header-driven**: it locates the `company` column by name, not index, so
   the spec is robust to the in-flight role-field addition (current header is
   `first_name,last_name,company,article_id`; post-role it will be
   `first_name,last_name,role,company,article_id`).
2. Deduplicate company names case-insensitively (trim + uppercase) before
   lookup.
3. For each unique company: query DataForSEO Google organic SERP, take top N
   results (default 5), ask the local LM Studio LLM to pick the official
   domain or return `null`.
4. Extract the bare host (strip scheme, strip leading `www.`, strip path)
   before writing.
5. Write an enriched CSV preserving the input columns (and their order) and
   appending a new trailing `domain` column. Empty `domain` when no result.

### Out of scope (YAGNI)

- Extracting a shared module for the DataForSEO / LM Studio clients. We copy
  the patterns locally (~200 added lines) to keep the change to a single new
  module.
- Persisting/caching domain lookups across runs.
- Validating that the chosen domain actually resolves or serves content.
- Skipping rows with empty `domain`; we keep them.
- Reformatting or normalising other columns.

## Design

### Module layout

New module `company-domain-lookup` declared in root `pom.xml` `<modules>`,
mirroring `seo-news-search` packaging (Spring Boot 3.4.5, Spring Batch, Java
21). Base package: `com.synapsedx.mailing.companydomain`.

```
company-domain-lookup/
  pom.xml
  Taskfile.yml
  src/main/java/com/synapsedx/mailing/companydomain/
    CompanyDomainLookupApplication.java
    config/
      CompanyDomainProperties.java         (@ConfigurationProperties("company-domain"))
      DataForSeoProperties.java            (copied pattern; prefix "dataforseo")
      LmStudioProperties.java              (copied pattern; prefix "lmstudio")
    client/
      DataForSeoSerpClient.java            (organic SERP only)
      LmStudioClient.java                  (chat-completions, JSON response)
    model/
      ContactRow.java                      (record: List<String> headers, List<String> values, String company)
      EnrichedContactRow.java              (record: ContactRow contact, String domain)
      SerpResult.java                      (record: title, url, snippet)
    batch/
      CompanyDomainLookupJobConfig.java
      reader/
        UniqueCompanyReader.java           (Step 1 reader)
        ContactsCsvReader.java             (Step 2 reader)
      processor/
        DomainLookupProcessor.java         (Step 1 processor)
        ContactEnrichProcessor.java        (Step 2 processor)
      writer/
        CompanyDomainMapWriter.java        (Step 1 writer — populates in-memory map)
        EnrichedContactsCsvWriter.java     (Step 2 writer)
      support/
        CompanyDomainMap.java              (Spring bean: ConcurrentHashMap<String,String>)
  src/main/resources/
    application.yml
  src/test/java/.../...
```

### Configuration

`CompanyDomainProperties` (record):

```java
@ConfigurationProperties("company-domain")
public record CompanyDomainProperties(
    String inputCsv,
    String outputCsv,
    int serpDepth,
    int serpTopN) {}
```

Defaults in `application.yml`:

```yaml
company-domain:
  input-csv: input/contacts.csv
  output-csv: output/contacts-with-domain.csv
  serp-depth: 10
  serp-top-n: 5
```

`DataForSeoProperties` (record, prefix `dataforseo`):

```java
@ConfigurationProperties("dataforseo")
public record DataForSeoProperties(Api api) {
  public record Api(String user, String key) {}
}
```

```yaml
dataforseo:
  api:
    user: ${dataforseo.api.user}
    key: ${dataforseo.api.key}
```

Throttle (2 req/s) is enforced at the processor level (see below).

`LmStudioProperties` (record, prefix `lmstudio`):

```java
@ConfigurationProperties("lmstudio")
public record LmStudioProperties(
    String server,
    String model,
    String key,
    int connectTimeoutSeconds,
    int requestTimeoutSeconds) {}
```

```yaml
lmstudio:
  server: http://127.0.0.1:1234
  model: nvidia/nemotron-3-nano-4b
  key: ${openai.key:lm-studio}
  connect-timeout-seconds: 10
  request-timeout-seconds: 60
```

The prompt is loaded from a fixed classpath resource
(`src/main/resources/domain-pick-prompt.md`); it is not a configurable
property. `temperature` and `max_tokens` are hard-coded in the client at
sensible defaults for a JSON classification task (`temperature=0`,
`max_tokens=200`).

CLI overrides:

```
--company-domain.input-csv=...  --company-domain.output-csv=...
--dataforseo.api.user=...        --dataforseo.api.key=...
--lmstudio.server=...            --lmstudio.model=...
```

### Two-step Spring Batch job

```
Job "company-domain-lookup-job"
  Step 1 "resolve-domains"   chunk size 1
    Reader     UniqueCompanyReader     emits each unique company key once
    Processor  DomainLookupProcessor   company → domain (or empty string)
    Writer     CompanyDomainMapWriter  put(companyKey, domain) into CompanyDomainMap

  Step 2 "enrich-contacts"   chunk size 100
    Reader     ContactsCsvReader       emits each row from the original CSV
    Processor  ContactEnrichProcessor  build EnrichedContactRow from map
    Writer     EnrichedContactsCsvWriter
```

Two steps keep dedup explicit and let Spring Batch report progress separately
for the expensive (API-bound) and cheap (file-bound) phases. The
`CompanyDomainMap` bean is the seam between steps; it lives for the
duration of the job execution.

### Reader: `UniqueCompanyReader`

Implements `ItemReader<String>`. In `open` (or lazily on first `read`):

1. Read the CSV header line, locate the index of the `company` column by name
   (fail fast with a clear error if absent).
2. Read all remaining lines, parsing each with a small CSV parser that
   inverts the project's `escapeCsv` rules (handles quoted fields containing
   commas, doubled quotes, embedded newlines).
3. Extract the value at the `company` index from each row.
4. Compute `companyKey = company.trim().toUpperCase(Locale.ROOT)`.
5. Skip rows where `companyKey` is empty.
6. Deduplicate `companyKey` while preserving first-seen order.
7. Iterate that list, returning the **original-case** company name on each
   `read()` call (we need the original for the LLM prompt; the key is only
   used as map key).

Returns `null` when exhausted. Not restartable across JVMs (state is
in-memory); acceptable for an MVP.

### Processor: `DomainLookupProcessor`

```java
public class DomainLookupProcessor implements ItemProcessor<String, CompanyDomain> {
  public CompanyDomain process(String company) { ... }
}
```

Where `CompanyDomain` is `record CompanyDomain(String companyKey, String domain)`.

Algorithm:

1. `companyKey = company.trim().toUpperCase(Locale.ROOT)`.
2. Call `DataForSeoSerpClient.searchOrganic(company, serpDepth)` → list of
   `SerpResult`. Apply a per-request 500 ms sleep (2 req/s, matching
   `seo-news-search`).
3. If empty list → return `new CompanyDomain(companyKey, "")`.
4. Take top `serpTopN` results.
5. Call `LmStudioClient.pickOfficialDomain(company, topResults)` → returns
   `Optional<String>` (URL or domain string).
6. If empty → return `new CompanyDomain(companyKey, "")`.
7. Normalise the LLM-returned string with `extractHost`:
   - parse via `URI.create(...)` after prefixing `https://` if missing,
   - take `URI.getHost()`,
   - strip leading `www.`,
   - lowercase.
   - If parsing fails, return `""`.
8. Return `new CompanyDomain(companyKey, host)`.

On exceptions from either client: log at `WARN`, return
`new CompanyDomain(companyKey, "")`. Never abort the job for one company.

Logging (project convention `{operation}_{resource} key=value`):

```
log.info("domain_lookup_start company={}", company);
log.info("domain_lookup_done company={} domain={}", company, domain);
log.warn("domain_lookup_failed company={} reason={}", company, e.getMessage());
```

### Writer: `CompanyDomainMapWriter`

Trivial — for each `CompanyDomain` in the chunk, `map.put(companyKey, domain)`.

### Client: `DataForSeoSerpClient`

New class, ~80 lines, copy of the pattern in `DataForSeoClient` but pointing
at the organic SERP endpoint:

```
https://api.dataforseo.com/v3/serp/google/organic/live/advanced
```

Request body (single task in array):

```json
[{
  "keyword": "<company>",
  "language_code": "fr",
  "depth": <serpDepth>,
  "location_code": 2250,
  "location_name": "France"
}]
```

Response parsing: walk `tasks[0].result[0].items`, keep only items where
`type == "organic"`, project to `SerpResult(title, url, snippet)`. Snippet
field name: `description`.

Auth: Basic, identical to existing client (`api.user:api.key`, base64).

### Client: `LmStudioClient`

New class, ~120 lines, copy of the chat-completions pattern from
`LmStudioExtractProcessor` in `seo-news-parse` but extracted into a dedicated
client because we want a JSON-only response with a single field.

Method:

```java
public Optional<String> pickOfficialDomain(String company, List<SerpResult> results)
```

Prompt template (loaded from `src/main/resources/domain-pick-prompt.md`):

```
Tu reçois le nom d'une compagnie et une liste de résultats Google.
Renvoie UNIQUEMENT un JSON {"domain": "<domaine>"} où <domaine> est le
domaine racine (ex: "factofrance.com") du site officiel de cette compagnie
parmi les résultats. Si aucun résultat ne correspond à un site officiel
(annuaire, presse, agrégateur, réseau social), renvoie {"domain": null}.

Compagnie: {{company}}

Résultats:
{{#each results}}
- titre: {{title}}
  url: {{url}}
  description: {{snippet}}
{{/each}}
```

(Handlebars-style placeholders rendered manually with `String.replace`; we
don't pull in a template engine.)

Behavior:

- POST to `{server}/v1/chat/completions` with body containing `model`
  (from `LmStudioProperties.model()`), `temperature: 0`, `max_tokens: 200`,
  `response_format: {"type":"json_object"}`, and a single user message with
  the rendered prompt.
- `Authorization: Bearer <LmStudioProperties.key()>` header (LM Studio
  accepts the literal `lm-studio` placeholder; this also makes the client
  drop-in for real OpenAI-compatible endpoints).
- HTTP client built with
  `HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(connectTimeoutSeconds))`,
  per-request timeout
  `HttpRequest.newBuilder().timeout(Duration.ofSeconds(requestTimeoutSeconds))`.
- Parse the response, extract `choices[0].message.content`, parse as JSON,
  read `domain` field.
- If `domain` is JSON null, return `Optional.empty()`.
- If the response is unparseable, log `WARN` and return `Optional.empty()`.

### Step 2: enrichment

`ContactsCsvReader` reads the same CSV again, header-driven (same parser as
`UniqueCompanyReader`). Each emitted `ContactRow` carries the header list,
the row's values (in input order), and the resolved `company` value for
lookup.

`ContactEnrichProcessor` looks up `company.trim().toUpperCase(Locale.ROOT)`
in `CompanyDomainMap` and builds
`new EnrichedContactRow(contact, map.getOrDefault(key, ""))`.

`EnrichedContactsCsvWriter` writes the output header **once**: the original
input headers joined with `,` plus `,domain`. Each row writes the original
values in original order, then the resolved `domain` field. Same
`StepExecutionListener`-driven first-write pattern and the same `escapeCsv`
rules as `ContactsCsvWriter` in `seo-news-parse`.

### Taskfile

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

## Tests

- **`UniqueCompanyReaderTest`** — given a CSV with duplicates (mixed case,
  whitespace, empty company rows), the reader emits exactly the expected
  set of original-case companies in first-seen order.
- **`DomainLookupProcessorTest`** — mocks `DataForSeoSerpClient` and
  `LmStudioClient`. Cases: empty SERP → empty domain; LLM returns null →
  empty domain; LLM returns `https://www.factofrance.com/contact` →
  `factofrance.com`; LLM returns malformed string → empty domain; SERP throws
  → empty domain and `WARN` logged.
- **`EnrichedContactsCsvWriterTest`** — header is written once across two
  chunks; rows with commas/quotes in company name are escaped; empty domain
  serialised as empty field.
- **`CompanyDomainLookupJobIT`** — `@SpringBootTest` + `JobLauncherTestUtils`.
  WireMock servers stub DataForSEO and LM Studio. Input fixture CSV with 5
  rows / 3 unique companies. Asserts: DataForSEO called exactly 3 times,
  LM Studio called 3 times, output CSV has 5 rows with correct domains
  remapped per company. Run the IT with both header shapes (4-column current
  and 5-column post-role) to lock in header-driven behavior.

## Risks

- **Domain copy-paste drift**: `DataForSeoSerpClient` and `LmStudioClient`
  copy patterns from other modules. If the upstream clients evolve (e.g. new
  auth, new error handling), this module won't pick it up automatically.
  Accepted — extracting a shared module is out of scope; the copies are
  short.
- **In-memory map size**: `CompanyDomainMap` holds all unique companies. At
  current contact volumes (hundreds), this is negligible. If we ever process
  100k+ unique companies, we'd switch to a small SQLite/H2 table.
- **LLM hallucination**: even with SERP grounding, the LLM may return a
  plausible-but-wrong domain. We accept this for now; downstream consumers
  treat `domain` as best-effort. A future iteration could HEAD the domain to
  confirm it resolves.
- **DataForSEO throttle**: 500 ms sleep per call is conservative. For a CSV
  with 200 unique companies that means ~100 s for step 1 — acceptable for
  a batch job.
