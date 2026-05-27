# seo-news-search Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Create a standalone Maven module `seo-news-search` that reads DataForSEO queries from YAML, fetches news + article content via the DataForSEO API, and writes one `.md` file per result with YAML frontmatter.

**Architecture:** Single Spring Batch step: `YamlQueryReader → DataForSeoProcessor → MarkdownFileWriter`. Processor calls the DataForSEO news API for each query, then the content-parsing API per result URL. Writer creates `{filePrefix}-{index:02d}.md` in a configurable output directory (default `output/`).

**Tech Stack:** Java 21, Spring Boot 3.4, Spring Batch 5, H2 (in-memory job repository), Jackson YAML, Lombok, JUnit 5, Mockito, AssertJ.

---

## File Map

| File | Action | Responsibility |
|---|---|---|
| `pom.xml` | Modify | Add `seo-news-search` module |
| `seo-news-search/pom.xml` | Create | Maven module definition |
| `.../seonews/model/SearchQuery.java` | Create | YAML query record |
| `.../seonews/model/QueryList.java` | Create | YAML list wrapper |
| `.../seonews/model/RawNewsItem.java` | Create | Raw API result before content fetch |
| `.../seonews/model/NewsArticle.java` | Create | Final article with content + metadata |
| `.../seonews/config/DataForSeoProperties.java` | Create | API credentials config |
| `.../seonews/config/SeoNewsProperties.java` | Create | Output directory config |
| `.../seonews/client/DataForSeoClient.java` | Create | HTTP calls: news search + content parsing |
| `.../seonews/batch/reader/YamlQueryReader.java` | Create | Spring Batch ItemReader from classpath YAML |
| `.../seonews/batch/processor/DataForSeoProcessor.java` | Create | Calls API, assembles `List<NewsArticle>` |
| `.../seonews/batch/writer/MarkdownFileWriter.java` | Create | Writes `.md` files to output dir |
| `.../seonews/batch/SeoNewsJob.java` | Create | Spring Batch Job + Step wiring |
| `.../seonews/SeoNewsApplication.java` | Create | Spring Boot entry point |
| `seo-news-search/src/main/resources/application.yml` | Create | App config with defaults |
| `seo-news-search/src/main/resources/dataforseo-queries.yml` | Create | Search queries |
| `seo-news-search/Taskfile.yml` | Create | run/build/test shortcuts |
| `.../test/java/.../reader/YamlQueryReaderTest.java` | Create | Reader unit test |
| `.../test/java/.../processor/DataForSeoProcessorTest.java` | Create | Processor unit test |
| `.../test/java/.../writer/MarkdownFileWriterTest.java` | Create | Writer unit test |
| `seo-news-search/src/test/resources/dataforseo-queries.yml` | Create | Test YAML fixture (shadows main resource) |

All Java files live under `seo-news-search/src/main/java/com/synapsedx/mailing/seonews/` unless noted as test.

---

## Task 1: Maven setup

**Files:**
- Modify: `pom.xml` (root)
- Create: `seo-news-search/pom.xml`

- [ ] **Step 1: Add module to root pom**

In `pom.xml`, find `<modules>` and add the new entry:

```xml
<modules>
    <module>mailing-pipeline</module>
    <module>seo-news-search</module>
</modules>
```

- [ ] **Step 2: Create module pom**

Create `seo-news-search/pom.xml`:

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd">
  <modelVersion>4.0.0</modelVersion>
  <artifactId>seo-news-search</artifactId>

  <parent>
    <groupId>com.synapsedx</groupId>
    <artifactId>mailing</artifactId>
    <version>1.0-SNAPSHOT</version>
  </parent>

  <dependencies>
    <dependency>
      <groupId>org.projectlombok</groupId>
      <artifactId>lombok</artifactId>
      <scope>provided</scope>
    </dependency>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-batch</artifactId>
    </dependency>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-jdbc</artifactId>
    </dependency>
    <dependency>
      <groupId>com.h2database</groupId>
      <artifactId>h2</artifactId>
      <scope>runtime</scope>
    </dependency>
    <dependency>
      <groupId>com.fasterxml.jackson.dataformat</groupId>
      <artifactId>jackson-dataformat-yaml</artifactId>
    </dependency>
    <dependency>
      <groupId>com.fasterxml.jackson.core</groupId>
      <artifactId>jackson-databind</artifactId>
    </dependency>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-test</artifactId>
      <scope>test</scope>
    </dependency>
  </dependencies>

  <build>
    <plugins>
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-compiler-plugin</artifactId>
        <version>${maven-compiler-plugin.version}</version>
        <configuration>
          <annotationProcessorPaths>
            <path>
              <groupId>org.projectlombok</groupId>
              <artifactId>lombok</artifactId>
              <version>${lombok.version}</version>
            </path>
          </annotationProcessorPaths>
        </configuration>
      </plugin>
      <plugin>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-maven-plugin</artifactId>
        <version>${spring-boot.version}</version>
        <executions>
          <execution>
            <goals>
              <goal>repackage</goal>
            </goals>
          </execution>
        </executions>
      </plugin>
      <plugin>
        <groupId>com.diffplug.spotless</groupId>
        <artifactId>spotless-maven-plugin</artifactId>
        <version>${spotless-maven-plugin.version}</version>
        <configuration>
          <java>
            <googleJavaFormat>
              <version>${google-java-format.version}</version>
              <style>GOOGLE</style>
              <reflowLongStrings>true</reflowLongStrings>
            </googleJavaFormat>
            <removeUnusedImports/>
            <importOrder/>
          </java>
        </configuration>
        <executions>
          <execution>
            <id>spotless-apply</id>
            <phase>process-sources</phase>
            <goals>
              <goal>apply</goal>
            </goals>
          </execution>
        </executions>
      </plugin>
    </plugins>
  </build>
</project>
```

- [ ] **Step 3: Create directory structure**

```bash
mkdir -p seo-news-search/src/main/java/com/synapsedx/mailing/seonews/{config,model,client,batch/{reader,processor,writer}}
mkdir -p seo-news-search/src/main/resources
mkdir -p seo-news-search/src/test/java/com/synapsedx/mailing/seonews/batch/{reader,processor,writer}
mkdir -p seo-news-search/src/test/resources
```

- [ ] **Step 4: Verify build**

```bash
./mvnw -pl seo-news-search validate -q
```

Expected: BUILD SUCCESS with no output.

- [ ] **Step 5: Commit**

```bash
git add pom.xml seo-news-search/pom.xml
git commit -m "feat: add seo-news-search Maven module"
```

---

## Task 2: Models

**Files:**
- Create: `seo-news-search/src/main/java/com/synapsedx/mailing/seonews/model/SearchQuery.java`
- Create: `seo-news-search/src/main/java/com/synapsedx/mailing/seonews/model/QueryList.java`
- Create: `seo-news-search/src/main/java/com/synapsedx/mailing/seonews/model/RawNewsItem.java`
- Create: `seo-news-search/src/main/java/com/synapsedx/mailing/seonews/model/NewsArticle.java`

- [ ] **Step 1: Create SearchQuery**

```java
package com.synapsedx.mailing.seonews.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record SearchQuery(
    String keyword,
    @JsonProperty("language_code") String languageCode,
    int depth,
    @JsonProperty("location_code") int locationCode,
    @JsonProperty("location_name") String locationName,
    @JsonProperty("file_prefix") String filePrefix) {}
```

- [ ] **Step 2: Create QueryList**

```java
package com.synapsedx.mailing.seonews.model;

import java.util.List;

public record QueryList(List<SearchQuery> queries) {}
```

- [ ] **Step 3: Create RawNewsItem**

```java
package com.synapsedx.mailing.seonews.model;

public record RawNewsItem(String title, String url, String domain, String published) {}
```

- [ ] **Step 4: Create NewsArticle**

```java
package com.synapsedx.mailing.seonews.model;

public record NewsArticle(
    String title,
    String url,
    String domain,
    String published,
    String keyword,
    String filePrefix,
    String content) {}
```

- [ ] **Step 5: Compile**

```bash
./mvnw -pl seo-news-search compile -q
```

Expected: BUILD SUCCESS.

- [ ] **Step 6: Commit**

```bash
git add seo-news-search/src/main/java/com/synapsedx/mailing/seonews/model/
git commit -m "feat: add seo-news-search model records"
```

---

## Task 3: Config properties

**Files:**
- Create: `seo-news-search/src/main/java/com/synapsedx/mailing/seonews/config/DataForSeoProperties.java`
- Create: `seo-news-search/src/main/java/com/synapsedx/mailing/seonews/config/SeoNewsProperties.java`

- [ ] **Step 1: Create DataForSeoProperties**

```java
package com.synapsedx.mailing.seonews.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("dataforseo")
public record DataForSeoProperties(Api api) {
  public record Api(String user, String key) {}
}
```

- [ ] **Step 2: Create SeoNewsProperties**

```java
package com.synapsedx.mailing.seonews.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("seo-news")
public record SeoNewsProperties(String outputDir) {}
```

- [ ] **Step 3: Compile**

```bash
./mvnw -pl seo-news-search compile -q
```

Expected: BUILD SUCCESS.

- [ ] **Step 4: Commit**

```bash
git add seo-news-search/src/main/java/com/synapsedx/mailing/seonews/config/
git commit -m "feat: add seo-news-search config properties"
```

---

## Task 4: DataForSeoClient

**Files:**
- Create: `seo-news-search/src/main/java/com/synapsedx/mailing/seonews/client/DataForSeoClient.java`

- [ ] **Step 1: Create client**

```java
package com.synapsedx.mailing.seonews.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.synapsedx.mailing.seonews.config.DataForSeoProperties;
import com.synapsedx.mailing.seonews.model.RawNewsItem;
import com.synapsedx.mailing.seonews.model.SearchQuery;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataForSeoClient {

  private static final String NEWS_ENDPOINT =
      "https://api.dataforseo.com/v3/serp/google/news/live/advanced";
  private static final String CONTENT_ENDPOINT =
      "https://api.dataforseo.com/v3/on_page/content_parsing/live";

  private final DataForSeoProperties properties;
  private final ObjectMapper objectMapper;
  private final HttpClient httpClient =
      HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build();

  public List<RawNewsItem> searchNews(SearchQuery query) throws Exception {
    var body =
        objectMapper.writeValueAsString(
            List.of(
                Map.of(
                    "keyword", query.keyword(),
                    "language_code", query.languageCode(),
                    "depth", query.depth(),
                    "location_code", query.locationCode(),
                    "location_name", query.locationName())));
    var raw = post(NEWS_ENDPOINT, body);
    var items =
        objectMapper
            .readTree(raw)
            .path("tasks")
            .path(0)
            .path("result")
            .path(0)
            .path("items");
    var result = new ArrayList<RawNewsItem>();
    for (var item : items) {
      var url = item.path("url").asText(null);
      if (url == null) {
        continue;
      }
      result.add(
          new RawNewsItem(
              item.path("title").asText(null),
              url,
              item.path("domain").asText(null),
              item.path("time_published").asText(null)));
    }
    log.info("dataforseo_news_found keyword={} count={}", query.keyword(), result.size());
    return result;
  }

  public String fetchContent(String url) {
    try {
      var body =
          objectMapper.writeValueAsString(
              List.of(objectMapper.createObjectNode().put("url", url)));
      var raw = post(CONTENT_ENDPOINT, body);
      var topics =
          objectMapper
              .readTree(raw)
              .path("tasks")
              .path(0)
              .path("result")
              .path(0)
              .path("items")
              .path(0)
              .path("page_content")
              .path("main_topic");
      var md = new StringBuilder();
      for (var topic : topics) {
        var title = topic.path("h_title").asText("").strip();
        if (!title.isBlank()) {
          md.append("## ").append(title).append("\n\n");
        }
        for (var content : topic.path("primary_content")) {
          var text = content.path("text").asText("").strip();
          if (!text.isBlank()) {
            md.append(text).append("\n\n");
          }
        }
      }
      var result = md.toString().strip();
      log.info("content_fetched url={} chars={}", url, result.length());
      return result;
    } catch (Exception e) {
      log.warn("content_fetch_failed url={} reason={}", url, e.getMessage());
      return "";
    }
  }

  private String post(String endpoint, String body) throws Exception {
    var credentials = properties.api().user() + ":" + properties.api().key();
    var auth =
        Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
    var request =
        HttpRequest.newBuilder()
            .uri(URI.create(endpoint))
            .header("Authorization", "Basic " + auth)
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build();
    var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    log.debug("dataforseo_post endpoint={} status={}", endpoint, response.statusCode());
    if (response.statusCode() != 200) {
      throw new IllegalStateException(
          "DataForSEO error status=" + response.statusCode());
    }
    return response.body();
  }
}
```

- [ ] **Step 2: Compile**

```bash
./mvnw -pl seo-news-search compile -q
```

Expected: BUILD SUCCESS.

- [ ] **Step 3: Commit**

```bash
git add seo-news-search/src/main/java/com/synapsedx/mailing/seonews/client/
git commit -m "feat: add DataForSeoClient for news search and content parsing"
```

---

## Task 5: YamlQueryReader + test

**Files:**
- Create: `seo-news-search/src/main/java/com/synapsedx/mailing/seonews/batch/reader/YamlQueryReader.java`
- Create: `seo-news-search/src/test/java/com/synapsedx/mailing/seonews/batch/reader/YamlQueryReaderTest.java`
- Create: `seo-news-search/src/test/resources/dataforseo-queries.yml`

- [ ] **Step 1: Write the failing test**

Create `seo-news-search/src/test/resources/dataforseo-queries.yml`:

```yaml
queries:
  - keyword: "test query one"
    language_code: fr
    depth: 2
    location_code: 2250
    location_name: France
    file_prefix: test-fr
  - keyword: "test query two"
    language_code: fr
    depth: 5
    location_code: 2250
    location_name: France
    file_prefix: test2-fr
```

Create `YamlQueryReaderTest.java`:

```java
package com.synapsedx.mailing.seonews.batch.reader;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class YamlQueryReaderTest {

  @Test
  void readsAllQueriesFromYaml() throws Exception {
    var reader = new YamlQueryReader();

    var q1 = reader.read();
    var q2 = reader.read();
    var q3 = reader.read();

    assertThat(q1).isNotNull();
    assertThat(q1.keyword()).isEqualTo("test query one");
    assertThat(q1.filePrefix()).isEqualTo("test-fr");
    assertThat(q1.depth()).isEqualTo(2);

    assertThat(q2).isNotNull();
    assertThat(q2.keyword()).isEqualTo("test query two");
    assertThat(q2.filePrefix()).isEqualTo("test2-fr");

    assertThat(q3).isNull();
  }
}
```

- [ ] **Step 2: Run test to confirm it fails**

```bash
./mvnw -pl seo-news-search test -Dtest=YamlQueryReaderTest -q 2>&1 | tail -5
```

Expected: FAIL — `YamlQueryReader` does not exist yet.

- [ ] **Step 3: Create YamlQueryReader**

```java
package com.synapsedx.mailing.seonews.batch.reader;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.synapsedx.mailing.seonews.model.QueryList;
import com.synapsedx.mailing.seonews.model.SearchQuery;
import java.util.Iterator;
import org.springframework.batch.item.ItemReader;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

@Component
public class YamlQueryReader implements ItemReader<SearchQuery> {

  private final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
  private Iterator<SearchQuery> iterator;

  @Override
  public SearchQuery read() throws Exception {
    if (iterator == null) {
      try (var in = new ClassPathResource("dataforseo-queries.yml").getInputStream()) {
        iterator = mapper.readValue(in, QueryList.class).queries().iterator();
      }
    }
    return iterator.hasNext() ? iterator.next() : null;
  }
}
```

- [ ] **Step 4: Run test to confirm it passes**

```bash
./mvnw -pl seo-news-search test -Dtest=YamlQueryReaderTest -q 2>&1 | tail -5
```

Expected: `BUILD SUCCESS`, 1 test passed.

- [ ] **Step 5: Commit**

```bash
git add seo-news-search/src/main/java/com/synapsedx/mailing/seonews/batch/reader/ \
        seo-news-search/src/test/java/com/synapsedx/mailing/seonews/batch/reader/ \
        seo-news-search/src/test/resources/dataforseo-queries.yml
git commit -m "feat: add YamlQueryReader with unit test"
```

---

## Task 6: DataForSeoProcessor + test

**Files:**
- Create: `seo-news-search/src/main/java/com/synapsedx/mailing/seonews/batch/processor/DataForSeoProcessor.java`
- Create: `seo-news-search/src/test/java/com/synapsedx/mailing/seonews/batch/processor/DataForSeoProcessorTest.java`

- [ ] **Step 1: Write the failing test**

```java
package com.synapsedx.mailing.seonews.batch.processor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.synapsedx.mailing.seonews.client.DataForSeoClient;
import com.synapsedx.mailing.seonews.model.RawNewsItem;
import com.synapsedx.mailing.seonews.model.SearchQuery;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DataForSeoProcessorTest {

  @Mock
  DataForSeoClient client;

  @InjectMocks
  DataForSeoProcessor processor;

  private final SearchQuery query =
      new SearchQuery("banque digitale", "fr", 2, 2250, "France", "banque-fr");

  @Test
  void buildsArticlesWithKeywordAndFilePrefix() throws Exception {
    var rawItems =
        List.of(
            new RawNewsItem("Article 1", "https://ex.com/1", "ex.com", "2026-05-01T00:00:00Z"),
            new RawNewsItem("Article 2", "https://ex.com/2", "ex.com", "2026-05-02T00:00:00Z"));
    when(client.searchNews(query)).thenReturn(rawItems);
    when(client.fetchContent("https://ex.com/1")).thenReturn("## Heading\n\nContent.");
    when(client.fetchContent("https://ex.com/2")).thenReturn("");

    var result = processor.process(query);

    assertThat(result).hasSize(2);
    assertThat(result.get(0).title()).isEqualTo("Article 1");
    assertThat(result.get(0).keyword()).isEqualTo("banque digitale");
    assertThat(result.get(0).filePrefix()).isEqualTo("banque-fr");
    assertThat(result.get(0).content()).isEqualTo("## Heading\n\nContent.");
    assertThat(result.get(1).content()).isEmpty();
  }

  @Test
  void returnsNullWhenNewsApiFails() throws Exception {
    when(client.searchNews(query)).thenThrow(new RuntimeException("API error"));

    var result = processor.process(query);

    assertThat(result).isNull();
  }
}
```

- [ ] **Step 2: Run test to confirm it fails**

```bash
./mvnw -pl seo-news-search test -Dtest=DataForSeoProcessorTest -q 2>&1 | tail -5
```

Expected: FAIL — `DataForSeoProcessor` does not exist yet.

- [ ] **Step 3: Create DataForSeoProcessor**

```java
package com.synapsedx.mailing.seonews.batch.processor;

import com.synapsedx.mailing.seonews.client.DataForSeoClient;
import com.synapsedx.mailing.seonews.model.NewsArticle;
import com.synapsedx.mailing.seonews.model.SearchQuery;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataForSeoProcessor implements ItemProcessor<SearchQuery, List<NewsArticle>> {

  private final DataForSeoClient client;

  @Override
  public List<NewsArticle> process(SearchQuery query) {
    try {
      var rawItems = client.searchNews(query);
      var articles = new ArrayList<NewsArticle>();
      for (var item : rawItems) {
        var content = client.fetchContent(item.url());
        articles.add(
            new NewsArticle(
                item.title(),
                item.url(),
                item.domain(),
                item.published(),
                query.keyword(),
                query.filePrefix(),
                content));
      }
      return articles;
    } catch (Exception e) {
      log.error("dataforseo_processor_failed keyword={}", query.keyword(), e);
      return null;
    }
  }
}
```

- [ ] **Step 4: Run test to confirm it passes**

```bash
./mvnw -pl seo-news-search test -Dtest=DataForSeoProcessorTest -q 2>&1 | tail -5
```

Expected: `BUILD SUCCESS`, 2 tests passed.

- [ ] **Step 5: Commit**

```bash
git add seo-news-search/src/main/java/com/synapsedx/mailing/seonews/batch/processor/ \
        seo-news-search/src/test/java/com/synapsedx/mailing/seonews/batch/processor/
git commit -m "feat: add DataForSeoProcessor with unit test"
```

---

## Task 7: MarkdownFileWriter + test

**Files:**
- Create: `seo-news-search/src/main/java/com/synapsedx/mailing/seonews/batch/writer/MarkdownFileWriter.java`
- Create: `seo-news-search/src/test/java/com/synapsedx/mailing/seonews/batch/writer/MarkdownFileWriterTest.java`

- [ ] **Step 1: Write the failing test**

```java
package com.synapsedx.mailing.seonews.batch.writer;

import static org.assertj.core.api.Assertions.assertThat;

import com.synapsedx.mailing.seonews.config.SeoNewsProperties;
import com.synapsedx.mailing.seonews.model.NewsArticle;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.batch.item.Chunk;

class MarkdownFileWriterTest {

  @TempDir
  Path tempDir;

  MarkdownFileWriter writer;

  @BeforeEach
  void setUp() throws Exception {
    writer = new MarkdownFileWriter(new SeoNewsProperties(tempDir.toString()));
    writer.init();
  }

  @Test
  void writesFileWithFrontmatterAndContent() throws Exception {
    var article =
        new NewsArticle(
            "Test Title",
            "https://ex.com/article",
            "ex.com",
            "2026-05-01T00:00:00Z",
            "banque digitale",
            "banque-fr",
            "## Section\n\nContent paragraph.");
    var chunk = new Chunk<>(List.of(List.of(article)));

    writer.write(chunk);

    var file = tempDir.resolve("banque-fr-00.md");
    assertThat(file).exists();
    var content = Files.readString(file);
    assertThat(content).startsWith("---\n");
    assertThat(content).contains("title: \"Test Title\"");
    assertThat(content).contains("url: https://ex.com/article");
    assertThat(content).contains("domain: ex.com");
    assertThat(content).contains("published: 2026-05-01T00:00:00Z");
    assertThat(content).contains("keyword: banque digitale");
    assertThat(content).contains("---\n");
    assertThat(content).contains("## Section\n\nContent paragraph.");
  }

  @Test
  void writesMultipleFilesWithZeroPaddedIndex() throws Exception {
    var articles =
        List.of(
            new NewsArticle("A", "https://a.com/1", "a.com", null, "kw", "prefix", ""),
            new NewsArticle("B", "https://a.com/2", "a.com", null, "kw", "prefix", ""));
    var chunk = new Chunk<>(List.of(articles));

    writer.write(chunk);

    assertThat(tempDir.resolve("prefix-00.md")).exists();
    assertThat(tempDir.resolve("prefix-01.md")).exists();
  }

  @Test
  void writesFileWithEmptyContentWhenArticleBodyIsBlank() throws Exception {
    var article =
        new NewsArticle("Title", "https://ex.com", "ex.com", null, "kw", "test", "");
    var chunk = new Chunk<>(List.of(List.of(article)));

    writer.write(chunk);

    var file = tempDir.resolve("test-00.md");
    assertThat(file).exists();
    var content = Files.readString(file);
    assertThat(content).contains("title: \"Title\"");
    assertThat(content).doesNotContain("null");
  }
}
```

- [ ] **Step 2: Run test to confirm it fails**

```bash
./mvnw -pl seo-news-search test -Dtest=MarkdownFileWriterTest -q 2>&1 | tail -5
```

Expected: FAIL — `MarkdownFileWriter` does not exist yet.

- [ ] **Step 3: Create MarkdownFileWriter**

```java
package com.synapsedx.mailing.seonews.batch.writer;

import com.synapsedx.mailing.seonews.config.SeoNewsProperties;
import com.synapsedx.mailing.seonews.model.NewsArticle;
import jakarta.annotation.PostConstruct;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MarkdownFileWriter implements ItemWriter<List<NewsArticle>> {

  private final SeoNewsProperties properties;

  @PostConstruct
  void init() throws Exception {
    var dir = Path.of(properties.outputDir());
    if (!Files.exists(dir)) {
      Files.createDirectories(dir);
      log.info("output_dir_created path={}", dir.toAbsolutePath());
    }
    if (!Files.isWritable(dir)) {
      throw new IllegalStateException(
          "Output directory is not writable: " + dir.toAbsolutePath());
    }
  }

  @Override
  public void write(Chunk<? extends List<NewsArticle>> chunk) throws Exception {
    for (var articles : chunk.getItems()) {
      for (int i = 0; i < articles.size(); i++) {
        writeArticle(articles.get(i), i);
      }
    }
  }

  private void writeArticle(NewsArticle article, int index) throws Exception {
    var filename = String.format("%s-%02d.md", article.filePrefix(), index);
    var file = Path.of(properties.outputDir(), filename);
    var sb = new StringBuilder();
    sb.append("---\n");
    sb.append("title: \"").append(escape(article.title())).append("\"\n");
    sb.append("url: ").append(nullToEmpty(article.url())).append("\n");
    sb.append("domain: ").append(nullToEmpty(article.domain())).append("\n");
    sb.append("published: ").append(nullToEmpty(article.published())).append("\n");
    sb.append("keyword: ").append(nullToEmpty(article.keyword())).append("\n");
    sb.append("---\n");
    if (article.content() != null && !article.content().isBlank()) {
      sb.append("\n").append(article.content());
    }
    Files.writeString(file, sb.toString());
    log.info("article_written file={}", filename);
  }

  private String escape(String value) {
    return value == null ? "" : value.replace("\"", "\\\"");
  }

  private String nullToEmpty(String value) {
    return value == null ? "" : value;
  }
}
```

- [ ] **Step 4: Run test to confirm it passes**

```bash
./mvnw -pl seo-news-search test -Dtest=MarkdownFileWriterTest -q 2>&1 | tail -5
```

Expected: `BUILD SUCCESS`, 3 tests passed.

- [ ] **Step 5: Commit**

```bash
git add seo-news-search/src/main/java/com/synapsedx/mailing/seonews/batch/writer/ \
        seo-news-search/src/test/java/com/synapsedx/mailing/seonews/batch/writer/
git commit -m "feat: add MarkdownFileWriter with unit test"
```

---

## Task 8: SeoNewsJob + SeoNewsApplication

**Files:**
- Create: `seo-news-search/src/main/java/com/synapsedx/mailing/seonews/batch/SeoNewsJob.java`
- Create: `seo-news-search/src/main/java/com/synapsedx/mailing/seonews/SeoNewsApplication.java`

- [ ] **Step 1: Create SeoNewsJob**

```java
package com.synapsedx.mailing.seonews.batch;

import com.synapsedx.mailing.seonews.batch.processor.DataForSeoProcessor;
import com.synapsedx.mailing.seonews.batch.reader.YamlQueryReader;
import com.synapsedx.mailing.seonews.batch.writer.MarkdownFileWriter;
import com.synapsedx.mailing.seonews.model.NewsArticle;
import com.synapsedx.mailing.seonews.model.SearchQuery;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@RequiredArgsConstructor
public class SeoNewsJob {

  private final JobRepository jobRepository;
  private final PlatformTransactionManager transactionManager;
  private final YamlQueryReader yamlQueryReader;
  private final DataForSeoProcessor dataForSeoProcessor;
  private final MarkdownFileWriter markdownFileWriter;

  @Bean
  public Job seoNewsJob() {
    return new JobBuilder("seo-news", jobRepository)
        .start(searchAndWriteStep())
        .build();
  }

  @Bean
  public Step searchAndWriteStep() {
    return new StepBuilder("searchAndWriteStep", jobRepository)
        .<SearchQuery, List<NewsArticle>>chunk(1, transactionManager)
        .reader(yamlQueryReader)
        .processor(dataForSeoProcessor)
        .writer(markdownFileWriter)
        .build();
  }
}
```

- [ ] **Step 2: Create SeoNewsApplication**

```java
package com.synapsedx.mailing.seonews;

import com.synapsedx.mailing.seonews.config.DataForSeoProperties;
import com.synapsedx.mailing.seonews.config.SeoNewsProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({DataForSeoProperties.class, SeoNewsProperties.class})
public class SeoNewsApplication {

  public static void main(String[] args) {
    SpringApplication.run(SeoNewsApplication.class, args);
  }
}
```

- [ ] **Step 3: Compile**

```bash
./mvnw -pl seo-news-search compile -q
```

Expected: BUILD SUCCESS.

- [ ] **Step 4: Commit**

```bash
git add seo-news-search/src/main/java/com/synapsedx/mailing/seonews/batch/SeoNewsJob.java \
        seo-news-search/src/main/java/com/synapsedx/mailing/seonews/SeoNewsApplication.java
git commit -m "feat: add SeoNewsJob and SeoNewsApplication"
```

---

## Task 9: Resources and Taskfile

**Files:**
- Create: `seo-news-search/src/main/resources/application.yml`
- Create: `seo-news-search/src/main/resources/dataforseo-queries.yml`
- Create: `seo-news-search/Taskfile.yml`

- [ ] **Step 1: Create application.yml**

```yaml
spring:
  batch:
    job:
      enabled: true
    jdbc:
      initialize-schema: always

dataforseo:
  api:
    user: ${DATAFORSEO_USER}
    key: ${DATAFORSEO_KEY}

seo-news:
  output-dir: output
```

- [ ] **Step 2: Create dataforseo-queries.yml**

Copy content from `mailing-pipeline/src/main/resources/dataforseo-queries.yml`:

```yaml
queries:
  - keyword: "banque transformation digitale 2026"
    language_code: fr
    depth: 2
    location_code: 2250
    location_name: France
    file_prefix: banque-fr
  - keyword: "assurance dématérialisation conformité 2026"
    language_code: fr
    depth: 2
    location_code: 2250
    location_name: France
    file_prefix: assurance-fr
```

- [ ] **Step 3: Create Taskfile.yml**

```yaml
version: "3"

vars:
  MVN_EXEC: "../mvnw"

tasks:
  run:
    desc: Run the seo-news-search pipeline (writes .md files to output/)
    cmd: '{{.MVN_EXEC}} -pl seo-news-search spring-boot:run'

  build:
    desc: Build the jar
    cmd: "{{.MVN_EXEC}} -pl seo-news-search package -DskipTests"

  test:
    desc: Run unit tests
    cmd: "{{.MVN_EXEC}} -pl seo-news-search test"

  clean:
    desc: Remove build artifacts and output files
    cmds:
      - "{{.MVN_EXEC}} -pl seo-news-search clean"
      - rm -rf output/
```

- [ ] **Step 4: Run all tests**

```bash
./mvnw -pl seo-news-search test -q 2>&1 | tail -10
```

Expected: `BUILD SUCCESS`, all tests pass.

- [ ] **Step 5: Build the jar**

```bash
./mvnw -pl seo-news-search package -DskipTests -q
```

Expected: `BUILD SUCCESS`, jar created at `seo-news-search/target/seo-news-search-1.0-SNAPSHOT.jar`.

- [ ] **Step 6: Commit**

```bash
git add seo-news-search/src/main/resources/ seo-news-search/Taskfile.yml
git commit -m "feat: add seo-news-search resources and Taskfile"
```
