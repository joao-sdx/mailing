# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Test

Uses the bundled Maven wrapper (`./mvnw`, Maven 3.9.5+, Java 21).

```bash
./mvnw clean install                                 # full multi-module build
./mvnw -pl <module> test                             # test one module
./mvnw -pl <module> -Dtest=<ClassName> test          # single test class
./mvnw -pl <module> -Dtest=<ClassName>#<method> test # single test method
./mvnw -pl <module> spotless:apply                   # format (google-java-format 1.27)
```

Every module also ships a `Taskfile.yml` with a consistent shape — `build`, `test`, `clean`, and a `default` / `run` task that launches the module's Spring Batch job. Inspect the module's Taskfile before running it manually; the parameterized `run` tasks (e.g. `task run INPUT_YML=... OUTPUT_DIR=...` in `seo-news-search`) are the supported way to chain modules.

## Multi-Module Layout

Root `pom.xml` aggregates: `mailing-pipeline`, `seo-news-search`, `seo-news-parse`, `unitelegal2dataforseo`, `company-domain-lookup`, `sedia-funding-search`. The on-disk `pipeline/` directory contains a child pom but **is not** in the root `<modules>` list — it builds standalone via its own Taskfile and is not part of `./mvnw install`.

## Architecture: File-Chained Batch Pipeline

This is a **French B2B contact-intelligence batch pipeline**. Each module is a self-contained Spring Boot 3.4.5 / Spring Batch app whose output feeds the next module's input via files on disk. There is no in-process orchestration across modules — chaining happens at the shell level (see Taskfile `run` tasks).

Typical flow:

```
unitelegal2dataforseo → seo-news-search → seo-news-parse
   (INSEE CSV)            (DataForSEO)       (LM Studio LLM)
   query YAML            markdown articles    contacts CSV
```

- **`unitelegal2dataforseo`** — reads INSEE `StockUniteLegale` CSV, dedups company-name keywords (substring-aware), writes a DataForSEO query YAML. Defaults from `query-defaults.yml`; each emitted query gets a sequence-suffixed `file_prefix` so downstream filenames are unique.
- **`seo-news-search`** — reads the query YAML (path overridable via `--seo-news.input-yml`), calls DataForSEO (throttled 2 req/s), writes one `*.md` per article with YAML frontmatter + body to `--seo-news.output-dir`.
- **`seo-news-parse`** — reads a directory of `*.md` articles, sends each to a local LM Studio LLM, extracts contacts to a CSV. Paths overridable via `--seo-news-parse.input-dir` and `--seo-news-parse.output-csv`.
- **`company-domain-lookup`** — reads a contacts CSV (header-driven), resolves each company's official domain via DataForSEO + LM Studio, then enriches every row with `domain`, `summary`, and `relevant` columns. Two-step job: deduplicate companies first, then enrich all rows. Output path via `--company-domain.output-csv`.
- **`sedia-funding-search`** — fetches open EU funding calls/topics from the SEDIA Funding & Tenders Portal (Horizon Europe, EIC Accelerator, Digital Europe), scores each call for SME/digitalization relevance with LM Studio, writes a CSV of calls. Fully public API (`apiKey=SEDIA`, no registration). Output path via `--sedia.output-csv`. See `sedia-funding-search/README.md` for API details and rate-limit notes.
- **`mailing-pipeline`** — separate Supabase-backed track. Multiple jobs live in one Spring Boot app; **select the job at runtime with `--spring.batch.job.name=<name>`** (`seo`, `seo-contact`, `enrich-companies`, `seo-identify-target`). The Taskfile shows one task per job.
- **`pipeline/`** (orphan module) — INSEE master pipeline orchestrating four sub-jobs over `workdir/01-siren` → `02-siren-line` → `03-company` / `04-contact` / `05-relation` → `10-company-search-news` / `11-person-search-news` → `12-company-news`. Workdir stage numbering is sparse, not contiguous.

## Conventions

- **Java 21, records-first** for DTOs and value objects; Lombok for `@Slf4j`, `@RequiredArgsConstructor`. `@Data`/`@Value` are avoided.
- **Logging**: SLF4J only (never `System.out`). Project convention is structured `{operation}_{resource} key=value` (e.g. `log.info("llm_contacts_found article={} count={}", ...)`)
- **License headers** are managed by `task update-license` — never paste headers manually.
- **Configuration**: each module exposes a `@ConfigurationProperties` record (e.g. `SeoNewsProperties`, `SeoNewsParseProperties`); override via `--<prefix>.<key>=...` on the command line. New input/output paths should follow the existing `inputXxx`/`outputXxx` pattern so the Taskfile `run` task can plumb them.
- **Tests**: JUnit 5 + AssertJ; integration tests use `@SpringBootTest` + `JobLauncherTestUtils` against H2. Naming: `*Test` for unit, `*IT` for end-to-end job tests.

## Design Docs

Specs for in-flight work live in `docs/superpowers/specs/YYYY-MM-DD-<topic>-design.md`. Read the relevant spec before touching a module that has one — it captures non-obvious design decisions (e.g. why `seo-news-parse` wraps `List<PersonRow>` in an `ArticleContacts` record).
