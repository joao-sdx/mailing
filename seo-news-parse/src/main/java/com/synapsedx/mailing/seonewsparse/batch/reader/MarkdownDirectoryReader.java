package com.synapsedx.mailing.seonewsparse.batch.reader;

import com.synapsedx.mailing.seonewsparse.config.SeoNewsParseProperties;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.Queue;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemReader;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MarkdownDirectoryReader implements ItemReader<Path> {

  private final SeoNewsParseProperties properties;
  private Queue<Path> queue;

  @PostConstruct
  void init() {
    var dir = Path.of(properties.inputDir());
    if (!Files.isDirectory(dir)) {
      throw new IllegalStateException("Input dir does not exist or is not a directory: " + dir);
    }
  }

  @Override
  public Path read() throws Exception {
    if (queue == null) {
      loadQueue();
    }
    return queue.poll();
  }

  private void loadQueue() throws IOException {
    var dir = Path.of(properties.inputDir());
    queue = new LinkedList<>();
    try (var stream =
        Files.find(dir, 1, (p, attr) -> attr.isRegularFile() && p.toString().endsWith(".md"))) {
      queue = stream.sorted().collect(Collectors.toCollection(LinkedList::new));
    }
    log.info("markdown_reader_init input_dir={} files={}", dir, queue.size());
  }
}
