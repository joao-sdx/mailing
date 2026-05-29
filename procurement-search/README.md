# procurement-search — User Manual

Query EU and French public procurement sources, get a single CSV of open tenders you can bid on.

---

## What you get

Run one command, point it at a YAML file with your search criteria, and get `tenders.csv` with every matching open tender from:

- **TED** (Tenders Electronic Daily) — the official EU procurement journal. Covers all 27 member states, all sectors, all languages. Free, no account needed.
- **BOAMP** (Bulletin Officiel des Annonces de Marchés Publics) — France's national procurement bulletin. Best for tenders that never make it to TED (below EU threshold). Free, no account needed.

Both sources are queried in one run. Results land in one file.

> **Note:** BASE/Portugal (below-threshold Portuguese tenders) is not yet supported — see [Known limitations](#known-limitations).

---

## Before you start

You need:

- **Java 21** on your `PATH`
- The **`mailing` repo** checked out (the Maven wrapper `../mvnw` is at the repo root)
- **`go-task`** — optional but recommended; install from https://taskfile.dev

No API keys, no registration, no accounts.

---

## Your first search

The repo ships with a sample query file you can run immediately:

```bash
cd procurement-search
task run INPUT_YML=src/main/resources/procurement-queries.yml OUTPUT_CSV=output/tenders.csv
```

Without go-task:

```bash
cd procurement-search
../mvnw spring-boot:run \
  -Dspring-boot.run.arguments="--procurement.input-yml=src/main/resources/procurement-queries.yml --procurement.output-csv=output/tenders.csv"
```

Open `output/tenders.csv` when it finishes. Each row is one tender.

---

## Customising your search

Copy the sample file and edit it — don't edit the one inside `src/main/resources/` directly.

```bash
cp src/main/resources/procurement-queries.yml my-queries.yml
# edit my-queries.yml, then:
task run INPUT_YML=my-queries.yml OUTPUT_CSV=output/tenders.csv
```

### The query file format

A YAML list of queries. Each query targets one source and tells it what to look for.

```yaml
queries:
  - source: TED
    filter:
      keywords: ["logiciel", "digitalisation"]
      countries: [FRA, PRT, ESP]
      publicationDateFrom: "2026-01-01"
      activeOnly: true

  - source: BOAMP
    filter:
      keywords: ["logiciel"]
      departments: ["75", "92"]
      publicationDateFrom: "2026-01-01"
```

You can have as many queries as you want, mixing TED and BOAMP freely. All results land in the same CSV.

---

## Filter options

| Field | What it does | TED | BOAMP |
|---|---|---|---|
| `keywords` | Search words, AND-joined | ✅ | ✅ |
| `publicationDateFrom` | Only notices published on/after this date (`YYYY-MM-DD`) | ✅ | ✅ |
| `countries` | Buyer country codes (ISO-3166 alpha-3) | ✅ | ❌ BOAMP is always France |
| `departments` | French department numbers (e.g. `"75"`, `"92"`) | ❌ | ✅ |
| `activeOnly` | Skip awarded/cancelled notices, show only open tenders | ✅ | ❌ no equivalent |

**Country codes for TED:** `FRA` France · `PRT` Portugal · `ESP` Spain · `DEU` Germany · `BEL` Belgium · `LUX` Luxembourg · `ITA` Italy · `NLD` Netherlands · and all other EU member states.

---

## Example queries

### IT services across France, Portugal, and Spain

```yaml
- source: TED
  filter:
    keywords: ["logiciel", "informatique"]
    countries: [FRA, PRT, ESP]
    publicationDateFrom: "2026-01-01"
    activeOnly: true
```

### French tenders in the Paris region

```yaml
- source: BOAMP
  filter:
    keywords: ["logiciel", "numérique"]
    departments: ["75", "92", "93", "94"]
    publicationDateFrom: "2026-01-01"
```

### TED — filter by CPV code (IT services, code 72000000)

The high-level `filter` doesn't cover CPV codes. Use `rawQuery` for that:

```yaml
- source: TED
  rawQuery: "buyer-country IN (FRA PRT ESP) AND classification-cpv=72000000 AND publication-date>=20260101"
```

When `rawQuery` is set, the `filter` block is ignored entirely. The query is sent verbatim to TED's Expert-Search engine.

> TED date format inside `rawQuery` is `YYYYMMDD` with no separators (e.g. `20260101`, not `2026-01-01`).

### BOAMP — advanced ODSQL query

```yaml
- source: BOAMP
  rawQuery: "search(objet,\"logiciel\") AND dateparution>=\"2026-01-01\""
```

---

## Reading the output CSV

| Column | What's in it |
|---|---|
| `source` | `TED` or `BOAMP` |
| `id` | TED publication number (e.g. `123456-2026`) or BOAMP `idweb` (e.g. `26-0001`) |
| `title` | Notice title (TED: French preferred; BOAMP: `objet`) |
| `buyer` | Contracting authority name |
| `country` | Buyer's country (BOAMP is always `FRA`) |
| `classification` | CPV codes joined (TED) or French descriptor labels joined (BOAMP) |
| `value` | Estimated contract value — often empty, especially on BOAMP |
| `publication_date` | Date the notice was published |
| `deadline` | Deadline to submit a tender — prioritise rows where this is not empty |
| `url` | Direct link to the notice |

---

## rawQuery reference

Use these when `filter` isn't enough.

### TED Expert-Search

| Want | Write |
|---|---|
| Keyword in notice text | `FT ~ logiciel` |
| Single country | `buyer-country=FRA` |
| Multiple countries | `buyer-country IN (FRA PRT ESP)` |
| CPV sector | `classification-cpv=72000000` |
| Published from a date | `publication-date>=20260101` |
| Date range | `publication-date=(20260101 <> 20260601)` |
| Combine | `FT ~ logiciel AND buyer-country=FRA AND publication-date>=20260101` |

Validate your query interactively: https://ted.europa.eu/en/expert-search

### BOAMP ODSQL

| Want | Write |
|---|---|
| Keyword in tender object | `search(objet,"logiciel")` |
| Published from a date | `dateparution>="2026-01-01"` |
| Combine | `search(objet,"logiciel") AND dateparution>="2026-01-01"` |

Dataset fields reference: https://boamp-datadila.opendatasoft.com/explore/dataset/boamp/api/

---

## Tuning

**Slow it down** (if you're seeing rate-limit errors):
```
--procurement.throttle-millis=1000
```

**Speed it up** (default is 500 ms between queries):
```
--procurement.throttle-millis=100
```

**BOAMP quota** — the public API has a daily request limit. For heavy use, get a free key at https://boamp-datadila.opendatasoft.com and pass it with:
```
--boamp.api-key=YOUR_KEY
```

**Timeouts** (increase if you have a slow connection):
```
--ted.request-timeout-seconds=60
--boamp.request-timeout-seconds=60
```

---

## Known limitations

**BOAMP 10,000-result cap.** The Opendatasoft platform returns at most 10,000 rows per query. If your query matches more, you'll see a `boamp_results_truncated` warning in the logs. Fix: split the date range into shorter windows, or add more specific keywords.

**No deduplication across sources.** High-value French tenders above the EU threshold appear on both TED and BOAMP. If you query both with overlapping criteria, the same tender may appear twice. Use the `url` column to spot duplicates.

**CPV codes don't work on BOAMP.** TED uses CPV codes; BOAMP uses French descriptors. They don't map automatically — use `rawQuery` with `search(objet,"your term")` for BOAMP sector searches.

**BASE/Portugal not yet supported.** There is no stable unauthenticated public API for all-sector Portuguese open tenders. Below-threshold PT tenders aren't covered by TED either. BASE integration is planned once access to the IMPIC OCDS API is obtained.
