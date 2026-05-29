# procurement-search

Monitor EU and French public procurement tenders from a single command.

## 1. What it does

`procurement-search` queries **TED** (Tenders Electronic Daily — the EU-wide official journal covering all member states) and **BOAMP** (Bulletin Officiel des Annonces de Marchés Publics — France's national procurement bulletin) for active and open tenders that match your filters. Results from both sources are merged into a single CSV file you can open in Excel or feed to the next pipeline step. Both APIs are public — no registration or API key is needed for standard use. BASE/Portugal is not yet supported (see [Limitations](#7-limitations)).

---

## 2. Prerequisites

- **Java 21** — must be installed and on your `PATH`
- **Run from the root of the `mailing` repo** — the Maven wrapper `./mvnw` lives at the repo root; the commands below use `../mvnw` because you `cd` into `procurement-search` first
- **`go-task`** — optional, but makes running much easier; install from https://taskfile.dev

No API keys are required. Both TED and BOAMP are freely accessible public APIs.

---

## 3. Quick start

**With go-task (recommended):**
```bash
cd procurement-search
task run INPUT_YML=src/main/resources/procurement-queries.yml OUTPUT_CSV=output/tenders.csv
```

**Without go-task:**
```bash
cd procurement-search
../mvnw spring-boot:run \
  -Dspring-boot.run.arguments="--procurement.input-yml=src/main/resources/procurement-queries.yml --procurement.output-csv=output/tenders.csv"
```

`src/main/resources/procurement-queries.yml` is the built-in sample. **Copy it to a working location and edit it** — don't modify the file in `src/main/resources` directly, as it will be overwritten on the next build.

```bash
cp src/main/resources/procurement-queries.yml my-queries.yml
# edit my-queries.yml
task run INPUT_YML=my-queries.yml OUTPUT_CSV=output/tenders.csv
```

---

## 4. Configuring queries

This is where you control what tenders get fetched. Everything lives in your YAML file.

### 4.1 Structure

The file is a YAML list of queries. Each query targets one source (`TED` or `BOAMP`) and specifies either a high-level `filter` or a `rawQuery` sent verbatim to the source's own query engine.

```yaml
queries:
  - source: TED
    filter:
      keywords: ["logiciel", "digitalisation"]
      countries: [FRA, LUX]
      publicationDateFrom: "2026-01-01"
      activeOnly: true

  - source: BOAMP
    filter:
      keywords: ["logiciel"]
      departments: ["75", "92"]
      publicationDateFrom: "2026-01-01"
```

You can have as many queries as you like — TED and BOAMP entries can be freely mixed. All results land in the same output CSV.

### 4.2 Filter fields

| Field | Type | TED | BOAMP | Notes |
|---|---|---|---|---|
| `keywords` | list of strings | ✅ full-text (`FT ~ term`) | ✅ full-text (`search(objet,"term")`) | AND-joined; each keyword is a separate clause |
| `publicationDateFrom` | date (`YYYY-MM-DD`) | ✅ | ✅ | inclusive start date for publication |
| `countries` | list of ISO-3166 alpha-3 | ✅ | ❌ ignored | e.g. `[FRA, DEU, LUX]`; BOAMP is always France |
| `departments` | list of strings | ❌ ignored | ✅ | BOAMP `code_departement`, e.g. `["75", "92", "13"]` |
| `activeOnly` | boolean | ✅ (`scope=ACTIVE`) | ❌ no equivalent | TED: filters to active/open notices only |

> **BOAMP and countries:** BOAMP only covers France, so the `countries` field has no effect on BOAMP queries. Use `departments` to narrow by geography within France.

### 4.3 Worked examples

**1. TED — IT services in France, from a date**
```yaml
- source: TED
  filter:
    keywords: ["logiciel", "informatique"]
    countries: [FRA]
    publicationDateFrom: "2026-01-01"
    activeOnly: true
```
Fetches active TED notices mentioning both "logiciel" and "informatique", published in France from 1 January 2026 onward.

---

**2. TED — Multiple countries**
```yaml
- source: TED
  filter:
    keywords: ["digitalisation"]
    countries: [FRA, BEL, LUX]
    publicationDateFrom: "2026-03-01"
    activeOnly: true
```
Same keyword across the France/Belgium/Luxembourg corridor.

---

**3. BOAMP — By department (Paris region)**
```yaml
- source: BOAMP
  filter:
    keywords: ["logiciel", "numérique"]
    departments: ["75", "92", "93", "94"]
    publicationDateFrom: "2026-01-01"
```
BOAMP notices in the Paris inner ring mentioning "logiciel" or "numérique".

---

**4. TED with `rawQuery` — Filter by CPV code**
```yaml
- source: TED
  rawQuery: "buyer-country=FRA AND classification-cpv=72000000 AND publication-date>=20260101"
```
Use `rawQuery` when you need something the high-level `filter` can't express — here, filtering by CPV code 72000000 (IT services). When `rawQuery` is present, the `filter` block is ignored entirely.

> Note: TED date format inside `rawQuery` is `YYYYMMDD` (no separators).

---

**5. BOAMP with `rawQuery`**
```yaml
- source: BOAMP
  rawQuery: "search(objet,\"logiciel\") AND dateparution>=\"2026-01-01\""
```
Full ODSQL expression sent directly to BOAMP's API. Useful for queries that combine text search with date ranges in ways the `filter` block doesn't expose.

### 4.4 `rawQuery` reference

These quick-reference tables cover the most common patterns. For the full query language, see the source's own documentation.

**TED Expert-Search quick reference**

| What | Syntax | Example |
|---|---|---|
| Full-text search | `FT ~ term` | `FT ~ logiciel` |
| Buyer country | `buyer-country=XXX` | `buyer-country=FRA` |
| Multiple countries | `buyer-country IN (A B C)` | `buyer-country IN (FRA LUX)` |
| CPV code | `classification-cpv=XXXXXXXX` | `classification-cpv=72000000` |
| Date from | `publication-date>=YYYYMMDD` | `publication-date>=20260101` |
| Date range | `publication-date=(YYYYMMDD <> YYYYMMDD)` | `publication-date=(20260101 <> 20260601)` |
| Combine | `AND`, `OR`, `NOT`, parentheses | `FT ~ logiciel AND buyer-country=FRA` |
| Validate interactively | https://ted.europa.eu/en/expert-search | |

> TED date format: `YYYYMMDD` with no separators (e.g. `20260101`, not `2026-01-01`).

**BOAMP ODSQL quick reference**

| What | Syntax | Example |
|---|---|---|
| Full-text on title | `search(objet,"term")` | `search(objet,"logiciel")` |
| Date from | `dateparution>="YYYY-MM-DD"` | `dateparution>="2026-01-01"` |
| By department | `code_departement=75` | `code_departement=75` |
| Combine | `AND`, `OR` | `search(objet,"logiciel") AND code_departement=75` |

> BOAMP date format: `"YYYY-MM-DD"` (ISO, quoted). Full fields reference: https://boamp-datadila.opendatasoft.com/explore/dataset/boamp/api/

---

## 5. Output format

The output is a single CSV at the path you specify with `--procurement.output-csv` (or `OUTPUT_CSV=` with go-task). All sources are combined in one file.

| Column | TED | BOAMP |
|---|---|---|
| `source` | `TED` | `BOAMP` |
| `id` | Publication number (e.g. `123456-2026`) | `idweb` (e.g. `26-0001`) |
| `title` | Notice title (French preferred) | `objet` |
| `buyer` | Buyer name (French preferred) | `nomacheteur` |
| `country` | Buyer country (e.g. `FRA`) | Always `FRA` |
| `classification` | CPV codes joined (e.g. `72000000, 48000000`) | Descriptor labels joined |
| `value` | Total procurement value (may be empty) | Always empty |
| `publication_date` | Publication date | `dateparution` |
| `deadline` | Deadline to submit tender | `datelimitereponse` (date part) |
| `url` | Link to notice on TED | Link to BOAMP notice |

---

## 6. Tuning & limits

**Throttling:** by default, 500 ms between queries to avoid hammering either API. Override with:
```
--procurement.throttle-millis=200
```

**TED pagination:** cursor-based; will fetch all matching notices regardless of how many pages there are. No manual page size tuning needed.

**BOAMP pagination:** capped at **10,000 results per query** (Opendatasoft platform limit). If your query hits the cap, you'll see a `boamp_results_truncated` warning in the logs. To work around it, split your query into shorter date ranges or add more specific keywords.

**BOAMP rate limits:** the BOAMP API is public but has a daily request quota. For most use cases this is not an issue. If you're running high-volume queries, you can raise your quota by passing an API key:
```
--boamp.api-key=YOUR_KEY
```
Getting a key is free — register at https://boamp-datadila.opendatasoft.com.

**Timeouts:** defaults are 10 s connect / 30 s per request. Override with:
```
--ted.connect-timeout-seconds=10
--ted.request-timeout-seconds=30
--boamp.connect-timeout-seconds=10
--boamp.request-timeout-seconds=30
```

---

## 7. Limitations

- **BASE/Portugal not yet supported.** There is no stable, unauthenticated public API for all-sector Portuguese open tenders. BASE integration is planned once the IMPIC OCDS API token is obtained.

- **No cross-source deduplication.** EU above-threshold notices are published on both TED and BOAMP. If you query both sources with overlapping criteria, the same tender may appear twice in the output CSV. Check the `url` column to identify duplicates.

- **CPV codes do not filter BOAMP.** TED organises notices by CPV code; BOAMP uses its own French descriptor taxonomy. There is no automatic mapping between the two. For sector-specific BOAMP searches, use `rawQuery` with `search(objet,"your term")` instead.

- **BOAMP cap at 10,000 results.** Queries returning more than 10,000 notices are silently truncated by the Opendatasoft platform. A `boamp_results_truncated` warning is logged, but the CSV will not contain the excess rows. Narrow your query if you see this warning.
