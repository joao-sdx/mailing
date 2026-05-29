# sedia-funding-search

Spring Batch module that searches open EU funding calls from the [SEDIA Funding & Tenders
Portal](https://ec.europa.eu/info/funding-tenders/opportunities/portal/screen/home) and
scores each call for SME / digital-transformation relevance using a local LM Studio LLM.

## What it does

1. **Fetches** all open topics from the SEDIA search API across three EU programmes:
    - **Horizon Europe** — including EIC Accelerator (EIC calls live under Horizon Europe)
    - **Digital Europe (DIGITAL)**
2. **Scores** each call for relevance to digitalization / SME innovation via a local LM
   Studio LLM (French prompt, `true`/`false` answer).
3. **Writes** a CSV file with one row per call:

   ```
   identifier,call_identifier,title,programme,status,deadline,start_date,budget,url,relevant
   HORIZON-EIC-2026-ACCELERATOR-01-01,HORIZON-EIC-2026-ACCELERATOR-01,"EIC Accelerator Open 2026",...,true
   DIGITAL-2024-CLOUD-AI-01-01,DIGITAL-2024-CLOUD-AI-01,"AI and Data Cloud for SMEs",...,true
   ```

## How to run

LM Studio must be running on `http://127.0.0.1:1234` with a chat-completion model loaded.

```bash
# Default output: output/sedia-calls.csv
task default

# Custom output path
task run OUTPUT_CSV=output/sedia-2026-05.csv
```

To narrow the result set (reduces LLM calls), pass a keyword via `sedia.text`:

```bash
../mvnw spring-boot:run \
  -Dspring-boot.run.arguments="--sedia.text=digital --sedia.output-csv=output/sedia-digital.csv"
```

## Configuration

All properties in `application.yml` under the `sedia:` prefix:

| Property               | Default                                                     | Description                                     |
|------------------------|-------------------------------------------------------------|-------------------------------------------------|
| `search-endpoint`      | `https://api.tech.ec.europa.eu/search-api/prod/rest/search` | SEDIA search API URL                            |
| `api-key`              | `SEDIA`                                                     | Shared public API key (no registration needed)  |
| `text`                 | `***`                                                       | Full-text filter; `***` = match-all             |
| `framework-programmes` | `["43108390", "43152860"]`                                  | Horizon Europe + Digital Europe programme codes |
| `statuses`             | `["31094502", "31094502"]`                                  | Open calls and Forthcoming                      |
| `types`                | `["1"]`                                                     | Topic records (carry identifiers + portal URLs) |
| `page-size`            | `100`                                                       | Results per page (max 100)                      |
| `output-csv`           | `output/sedia-calls.csv`                                    | Output CSV path                                 |

LM Studio is configured under `lmstudio:` (server, model, key, timeouts, relevance-max-tokens).

### `framework-programmes` — known codes (live-verified 2026-05-29)

| Code       | Programme                                                                                                          |
|------------|--------------------------------------------------------------------------------------------------------------------|
| `43108390` | **Horizon Europe** — includes EIC Accelerator, EIC Pathfinder, EIC Transition, MSCA, ERC, and all other HE pillars |
| `43152860` | **Digital Europe Programme (DIGITAL)**                                                                             |

EIC Accelerator-only calls are a subset of Horizon Europe. They share `frameworkProgramme=43108390` and can be isolated
by additionally filtering `programmeDivision=43121682`, but that requires a separate query (the API does not support OR
across different fields in one request).

### `statuses` — known codes

| Code       | Meaning                                                          |
|------------|------------------------------------------------------------------|
| `31094501` | **Forthcoming** — call published but not yet open for submission |
| `31094502` | **Open** — currently accepting applications                      |
| `31094503` | **Closed** — deadline passed                                     |

### `types` — known codes

| Code | Meaning                                                                                                                                                                   |
|------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `1`  | **Grant topic** — canonical topic records; carry a topic identifier (e.g. `HORIZON-EIC-2026-ACCELERATOR-01-01`), full metadata, and a portal URL. **Use this for calls.** |
| `2`  | Grant (financial support entries, less structured)                                                                                                                        |
| `8`  | Grant / financial support (e.g. EIC Booster-style entries)                                                                                                                |
| `0`  | **Tender** — procurement notices, not grant calls                                                                                                                         |

## SEDIA API & rate limit

The SEDIA Funding & Tenders Portal search API is **fully public** — no registration,
no API key generation, no OAuth. The `apiKey=SEDIA` value is a shared, static key used
by the portal's own front-end.

**The EU Commission does not publish a documented rate limit, quota, or 429/Retry-After
policy** for this endpoint. The live API returns no `X-RateLimit-*` or `Retry-After`
headers, and no numeric ceiling appears in any publicly readable documentation.

> **Official reference:** EU Funding & Tenders Portal — "APIs" support page:
> `https://ec.europa.eu/info/funding-tenders/opportunities/portal/screen/support/apis`
> (the Commission's designated entry point for these APIs; the page is a JS single-page
> application, which is why no rate-limit text can be quoted from it).
>
> Live endpoint: `https://api.tech.ec.europa.eu/search-api/prod/rest/search?apiKey=SEDIA`

Because the key is shared by all public users of the portal, aggressive use risks
gateway-level throttling or IP blocking. This module self-imposes a conservative
**2 requests/second, sequential** ceiling via a 500 ms sleep between page fetches —
consistent with the rest of this pipeline.

### API request format (implementation note)

The SEDIA endpoint is a **multipart/form-data POST**: `query`, `sort`, and `languages`
are sent as named form parts each with `Content-Type: application/json`; the `apiKey`,
`text`, `pageSize`, `pageNumber`, and `language` parameters go on the query string.
Sending `query` as a plain JSON body or a regular form field causes a 415 error.

Example of a working bare curl:

```bash
curl -X POST \
  'https://api.tech.ec.europa.eu/search-api/prod/rest/search?apiKey=SEDIA&text=***&pageSize=2&pageNumber=1&language=en' \
  -F 'query={"bool":{"must":[{"terms":{"type":["1"]}},{"terms":{"status":["31094502"]}},{"terms":{"frameworkProgramme":["43108390","43152860"]}}]}};type=application/json' \
  -F 'sort={"field":"deadlineDate","order":"ASC"};type=application/json' \
  -F 'languages=["en"];type=application/json'
```
