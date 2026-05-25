package com.synapsedx.mailing.pipeline.siren.news;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.synapsedx.mailing.pipeline.siren.enrich.model.enrich.CompanyRecord;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemStream;
import org.springframework.stereotype.Component;

/**
 * Reads {@code *.json} files from {@code 10-company-search-news/} and emits one {@link
 * CompanySearchQuery} per non-null denomination field.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CompanyNewsSearchReader implements ItemReader<CompanySearchQuery>, ItemStream {

  private final CompanyNewsSearchProperties properties;
  private final ObjectMapper objectMapper;

  private Queue<Path> pendingFiles;
  private Queue<CompanySearchQuery> expansionQueue;
  private final List<Path> processedFiles = new ArrayList<>();

  @Override
  public void open(ExecutionContext ctx) {
    var inputDir = Path.of(properties.getInputDir());
    pendingFiles = new LinkedList<>();
    expansionQueue = new LinkedList<>();
    try (var stream =
        Files.find(
            inputDir, 1, (p, attr) -> attr.isRegularFile() && p.toString().endsWith(".json"))) {
      stream.sorted().forEach(pendingFiles::add);
    } catch (IOException e) {
      throw new IllegalStateException("Cannot scan input dir: " + inputDir, e);
    }
    log.info("company_news_reader_open input_dir={} files={}", inputDir, pendingFiles.size());
  }

  @Override
  public CompanySearchQuery read() throws Exception {
    while (expansionQueue.isEmpty()) {
      if (pendingFiles == null || pendingFiles.isEmpty()) {
        return null;
      }
      var file = pendingFiles.poll();
      var company = objectMapper.readValue(file.toFile(), CompanyRecord.class);
      processedFiles.add(file);
      expand(company).forEach(expansionQueue::add);
      log.debug("company_news_expanded rcs={} queries={}", company.rcs(), expansionQueue.size());
    }
    return expansionQueue.poll();
  }

  @Override
  public void update(ExecutionContext ctx) {}

  @Override
  public void close() {}

  public List<Path> getProcessedFiles() {
    return List.copyOf(processedFiles);
  }

  private static List<CompanySearchQuery> expand(CompanyRecord company) {
    var denoms =
        Stream.of(
                company.denominationUniteLegale(),
                company.denominationUsuelle1UniteLegale(),
                company.denominationUsuelle2UniteLegale(),
                company.denominationUsuelle3UniteLegale())
            .filter(d -> d != null && !d.isBlank())
            .toList();

    var queries = new ArrayList<CompanySearchQuery>(denoms.size());
    for (int i = 0; i < denoms.size(); i++) {
      queries.add(new CompanySearchQuery(company.rcs(), denoms.get(i), i));
    }
    return queries;
  }
}
