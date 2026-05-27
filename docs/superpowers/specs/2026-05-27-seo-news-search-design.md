# seo-news-search — Design Spec

**Date:** 2026-05-27
**Status:** Approved

## Overview

A new standalone Maven module `seo-news-search` that reads `dataforseo-queries.yml`, calls the DataForSEO news search API for each query, fetches article content for each result, and writes one Markdown file per article to a configurable output directory.

## Module Structure

New module `seo-news-search` added as a child of the root `pom.xml`. Same stack as `mailing-pipeline`: Spring Boot 3.4, Spring Batch, H2 (in-memory job repository), Jackson YAML, Lombok.

```
seo-news-search/
  pom.xml
  src/main/java/com/synapsedx/mailing/seonews/
    SeoNewsApplication.java
    config/
      DataForSeoProperties.java   (api.user, api.key)
      SeoNewsProperties.java      (output-dir, default "output")
    model/
      SearchQuery.java            (record — keyword, languageCode, depth, locationCode, locationName, filePrefix)
      QueryList.java              (YAML wrapper: List<SearchQuery> queries)
      NewsArticle.java            (record — title, url, domain, published, keyword, content)
    batch/
      SeoNewsJob.java             (Job + Step bean definitions)
      reader/
        YamlQueryReader.java      (ItemReader<SearchQuery>, reads dataforseo-queries.yml from classpath)
      processor/
        DataForSeoProcessor.java  (ItemProcessor<SearchQuery, List<NewsArticle>>)
      writer/
        MarkdownFileWriter.java   (ItemWriter<List<NewsArticle>>)
    client/
      DataForSeoClient.java       (HTTP wrapper for news + content-parsing endpoints)
  src/main/resources/
    application.yml
    dataforseo-queries.yml
```

## Batch Job

Single Spring Batch step, chunk size 1:

```
YamlQueryReader → DataForSeoProcessor → MarkdownFileWriter
```

**YamlQueryReader** — `ItemReader<SearchQuery>`. Reads `dataforseo-queries.yml` from the classpath on first call and iterates. Identical pattern to `mailing-pipeline`'s `YamlQueryReader`.

**DataForSeoProcessor** — `ItemProcessor<SearchQuery, List<NewsArticle>>`:
1. POST to `/v3/serp/google/news/live/advanced` with the query parameters
2. Parse the news items from the response
3. For each item, POST to `/v3/on_page/content_parsing/live` to retrieve article body as Markdown (topics → headings + paragraphs)
4. Return a `List<NewsArticle>` — one entry per news result

**MarkdownFileWriter** — `ItemWriter<List<NewsArticle>>`:
- Iterates the list from each chunk item
- Writes one `.md` file per article to the configured output directory
- File name: `{file_prefix}-{index:02d}.md`, index resets per query (e.g. `banque-fr-00.md`, `banque-fr-01.md`)
- Output directory is created at application startup if it does not exist

## Output File Format

```markdown
---
title: "Article Title"
url: https://example.com/article
domain: example.com
published: 2026-05-20T10:00:00Z
keyword: banque transformation digitale 2026
---

## Section Heading

Article paragraph text...
```

Metadata block uses YAML frontmatter (`---` delimiters). Article content follows, reconstructed from the content-parsing response (`h_title` → `##` heading, `primary_content[].text` → paragraph). Existing files with the same name are overwritten.

## Configuration

`application.yml`:
```yaml
dataforseo:
  api:
    user: ${DATAFORSEO_USER}
    key: ${DATAFORSEO_KEY}

seo-news:
  output-dir: output
```

Credentials are supplied via environment variables. Output directory is relative to the working directory when the jar is run.

## Error Handling

| Scenario | Behavior |
|---|---|
| YAML parse failure | Fail fast — job aborts before any API calls |
| Output directory not writable | Fail fast at startup |
| DataForSEO news call fails for a query | Log error, skip query, continue |
| Content parsing fails for a single URL | Log warning, write `.md` with empty body (frontmatter still written), continue |

No retry logic. No Spring Batch restart across runs (H2 is in-memory).

## What This Module Does NOT Do

- No Supabase/NocoBase writes
- No LLM summarization
- No in-memory query store (queries flow directly through the single step)
