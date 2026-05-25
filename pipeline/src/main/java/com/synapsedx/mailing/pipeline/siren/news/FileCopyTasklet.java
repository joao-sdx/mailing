package com.synapsedx.mailing.pipeline.siren.news;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;

/**
 * Copies *.json files from {@code inputDir} to {@code outputDir}, then moves sources to {@code
 * doneDir}.
 */
@Slf4j
@RequiredArgsConstructor
class FileCopyTasklet implements Tasklet {

  private final Path inputDir;
  private final Path outputDir;
  private final Path doneDir;

  @Override
  public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext)
      throws Exception {
    if (!Files.exists(inputDir)) {
      log.debug("file_copy_tasklet_skip inputDir={} (does not exist)", inputDir);
      return RepeatStatus.FINISHED;
    }
    Files.createDirectories(outputDir);
    Files.createDirectories(doneDir);

    try (var stream = Files.list(inputDir)) {
      stream.filter(p -> p.toString().endsWith(".json")).sorted().forEach(this::copyAndMove);
    }
    return RepeatStatus.FINISHED;
  }

  private void copyAndMove(Path source) {
    try {
      var fileName = source.getFileName();
      Files.copy(source, outputDir.resolve(fileName), StandardCopyOption.REPLACE_EXISTING);
      Files.move(source, doneDir.resolve(fileName), StandardCopyOption.REPLACE_EXISTING);
      log.debug("file_distributed src={} dst={}", source, outputDir.resolve(fileName));
    } catch (IOException e) {
      throw new RuntimeException("Failed to distribute file: " + source, e);
    }
  }
}
