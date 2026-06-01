# CSV Column Mapping — Design Spec

**Date:** 2026-06-01  
**Scope:** `apollo-people-search`, `company-domain-lookup`  
**Status:** Approved

## Problem

Both `apollo-people-search` and `company-domain-lookup` read CSV files with hardcoded column names (`company`, `domain`, `article_id`). When a user's input CSV uses different column headers, the pipeline silently reads wrong data or fails with an unhelpful index error.

## Goal

Allow users to configure, per module, which CSV column header maps to each internal name the code uses. Missing or misconfigured mappings must fail fast with a clear error.

## Out of Scope

- `unitelegal2dataforseo` (reads a fixed-format INSEE government CSV — column names are not user-controlled)
- A shared `csv-common` Maven module (over-engineering for two consumers)

---

## Config Shape

Each module's `@ConfigurationProperties` record gains a `Map<String, String> columnMapping` field. Key = internal name used by the code; value = CSV column header in the input file.

### `apollo-people-search/src/main/resources/application.yml`

```yaml
apollo:
  column-mapping:
    company: company
    domain: domain
```

### `company-domain-lookup/src/main/resources/application.yml`

```yaml
company-domain:
  column-mapping:
    company: company
    article_id: article_id
```

The defaults match the current hardcoded names, so existing pipelines require no config changes.

To remap columns in a custom input file:

```yaml
apollo:
  column-mapping:
    company: organizationName
    domain: websiteDomain
```

If `column-mapping` is absent or empty, `CsvColumnMapper` throws `IllegalStateException` immediately — consistent with fail-fast behavior.

---

## `CsvColumnMapper` Utility

Added to each module's `csv/` package alongside the existing `CsvLineParser`. One static method:

```java
public class CsvColumnMapper {

    public static Map<String, Integer> resolve(List<String> headers, Map<String, String> columnMapping) {
        if (columnMapping == null || columnMapping.isEmpty()) {
            throw new IllegalStateException("column-mapping must be configured");
        }
        var result = new LinkedHashMap<String, Integer>();
        for (var entry : columnMapping.entrySet()) {
            int idx = headers.indexOf(entry.getValue());
            if (idx < 0) {
                throw new IllegalArgumentException(
                    "CSV column '%s' (mapped to '%s') not found in headers: %s"
                        .formatted(entry.getValue(), entry.getKey(), headers));
            }
            result.put(entry.getKey(), idx);
        }
        return result;
    }
}
```

- Called **once** per reader when the CSV header line is parsed.
- Returns `Map<String, Integer>` (internal name → zero-based column index).
- Extra columns in the CSV are silently ignored.
- The class is intentionally duplicated in both modules, following the same pattern as `CsvLineParser`.

---

## Reader Changes

### `apollo-people-search` — `UniqueDomainReader`

**Before:**
```java
companyIdx = header.indexOf("company");
domainIdx  = header.indexOf("domain");
```

**After:**
```java
var indices = CsvColumnMapper.resolve(header, props.columnMapping());
// then per row:
fields.get(indices.get("company"))
fields.get(indices.get("domain"))
```

The `companyIdx` and `domainIdx` fields are removed.

### `company-domain-lookup` — `ContactsCsvReader`, `UniqueCompanyReader`, `UniqueArticleReader`

Same pattern. Both use the internal names `company` and `article_id`. Each reader calls `CsvColumnMapper.resolve()` once at header-parse time and uses `indices.get(...)` per data row.

---

## Properties Record Changes

### `ApolloProperties`

Add field:
```java
Map<String, String> columnMapping
```

### `CompanyDomainProperties`

Add field:
```java
Map<String, String> columnMapping
```

Spring Boot binds `Map<String, String>` from YAML naturally with `@ConfigurationProperties`.

---

## Error Handling

| Situation | Error Type | Message |
|---|---|---|
| `column-mapping` is null or empty | `IllegalStateException` | `"column-mapping must be configured"` |
| Configured column name not in CSV headers | `IllegalArgumentException` | `"CSV column '<value>' (mapped to '<key>') not found in headers: [...]"` |

Both errors surface at reader initialization (header-parse time), before any data rows are processed.

---

## Testing

### New: `CsvColumnMapperTest` (in each module's `csv/` test package)

| Test | Assertion |
|---|---|
| Happy path — mapping matches headers, extra columns ignored | Returns correct `Map<String, Integer>` |
| Missing column — configured value not in headers | Throws `IllegalArgumentException` with column name and internal name in message |
| Null mapping | Throws `IllegalStateException` |
| Empty mapping | Throws `IllegalStateException` |

### Existing tests

- Reader unit tests (`UniqueDomainReaderTest`, etc.) need no structural changes — their fixture CSVs already use the default column names.
- Integration tests (`ApolloPeopleSearchJobIT`, etc.) exercise the full reader path and will catch broken mappings end-to-end.
