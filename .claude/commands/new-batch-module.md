---
description: Scaffold and build a new Spring Batch module mirroring existing siblings — reads an input file, calls an external API and/or LM Studio, writes CSV/markdown/YAML
allowed-tools: Bash, Read, Write, Edit, Grep, Glob
---

## Context

This repo is a file-chained French B2B contact-intelligence batch pipeline. Each module is a self-contained Spring Boot
3.4.5 / Spring Batch app whose file output feeds the next module. Reference modules to mirror precisely:
`seo-news-search` (API client), `seo-news-parse` (LLM), `company-domain-lookup` (two-step dedup→enrich + LM Studio).
Read `CLAUDE.md` for conventions first.

## Your Task

Create a new Spring Batch module that reads an input file, calls an external API and/or a local LM Studio LLM, and
writes an output file — replicating the exact patterns of the sibling modules. This is non-trivial work: brainstorm the
design first, then build via TDD.

### Steps

1. **Brainstorm** the module's shape (input format, API/LLM calls, output columns) — invoke `superpowers:brainstorming`
   before designing. Save the spec to `docs/superpowers/specs/YYYY-MM-DD-<topic>-design.md`.
2. **Explore reference modules**: read `seo-news-search`/`company-domain-lookup` pom.xml, the `@ConfigurationProperties`
   record, batch job config, client(s), reader/processor/writer, application.yml, Taskfile.yml. Match their exact style.
3. **Bootstrap**: pom.xml (child of root parent, mirroring `seo-news-search/pom.xml`); add the module to the root
   `<modules>` list; `@SpringBootApplication` main class; application.yml; Taskfile.yml with `default`/`run`/`build`/
   `test`/`clean` and `MVN_EXEC: "../mvnw"`.
4. **Config record(s)**: `@ConfigurationProperties` record using `inputXxx`/`outputXxx` naming, overridable via
   `--<prefix>.<key>=`. Make endpoint URLs configurable so ITs can redirect to WireMock.
5. **Domain model**: Java 21 records, no behavior.
6. **Reader**: header-driven CSV parser (index lookup, not positional assumptions).
7. **Client(s)**: mirror `DataForSeoClient` (RestClient, auth + POST, throttle ~2 req/s / 500ms) and/or
   `LmStudioClient`.
8. **Processor + Writer**: writer mirrors `ContactsCsvWriter` escaping; preserves ALL input columns and only appends new
   ones.
9. **Batch job config**: chunk-oriented steps; often two-step (dedup → enrich).
10. **End-to-end `*IT`** against WireMock fixtures.
11. `task update-license`, then full `./mvnw clean install`.

### Guardrails

- Output paths are **filesystem files, NOT classpath resources**.
- CSV writers **preserve all input columns**; append new columns only.
- Dedup is **case-insensitive** (trim + uppercase) but emit the original case; emit an **empty value** (not a skipped
  row) when an API/LLM returns nothing.
- LM Studio `response_format.type` must be `json_schema`, `json_object`, or `text` (status 400 otherwise). Config block:
  `lmstudio.{server,model,key,connect-timeout-seconds,request-timeout-seconds}`.
- **Never paste license headers** — use `task update-license`. If `task` is unavailable, skip and note it. Don't touch
  license headers in files outside the new module.
- Use Java 21 sequenced collections: `getFirst()` not `get(0)`.
- **No `Co-Authored-By:` trailers** in commits. Commit/push only when asked.


