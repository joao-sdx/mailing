package com.synapsedx.mailing.seonewsparse.batch.processor;

import static org.assertj.core.api.Assertions.assertThat;

import com.synapsedx.mailing.seonewsparse.config.LmStudioProperties;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LmStudioExtractProcessorTest {

  @TempDir Path tempDir;

  private LmStudioExtractProcessor processor;
  private String mockResponse;

  @BeforeEach
  void setup() {
    var props = new LmStudioProperties("http://localhost:1234", "test-model", "test-key", 5, 30);
    processor =
        new LmStudioExtractProcessor(props) {
          @Override
          String post(String body) {
            return mockResponse;
          }
        };
    processor.systemPrompt = "system prompt";
    processor.userPromptTemplate = "User: {article_content}";
  }

  @Test
  void happyPath_returnsArticleContactsWithRole() throws Exception {
    var mdFile = tempDir.resolve("article.md");
    Files.writeString(
        mdFile,
        """
        ---
        title: "Mon Article"
        url: https://example.com
        ---

        Body content here.
        """);

    mockResponse =
        """
        {"choices":[{"message":{"content":"[{\\"prenom\\":\\"Jean\\",\\"nom\\":\\"Dupont\\",\\"societe\\":\\"BNP Paribas\\",\\"role\\":\\"Directeur\\",\\"email\\":\\"\\"}]"}}]}
        """;

    var result = processor.process(mdFile);

    assertThat(result).isNotNull();
    assertThat(result.sourceArticle()).isEqualTo(mdFile);
    assertThat(result.rows()).hasSize(1);
    var person = result.rows().getFirst();
    assertThat(person.firstName()).isEqualTo("Jean");
    assertThat(person.lastName()).isEqualTo("Dupont");
    assertThat(person.role()).isEqualTo("Directeur");
    assertThat(person.company()).isEqualTo("BNP Paribas");
    assertThat(person.articleId()).isEqualTo("article.md");
  }

  @Test
  void missingRoleDefaultsToEmptyString() throws Exception {
    var mdFile = tempDir.resolve("no-role.md");
    Files.writeString(mdFile, "Body without frontmatter.");

    mockResponse =
        """
        {"choices":[{"message":{"content":"[{\\"prenom\\":\\"Alice\\",\\"nom\\":\\"Martin\\",\\"societe\\":\\"Acme\\"}]"}}]}
        """;

    var result = processor.process(mdFile);

    assertThat(result).isNotNull();
    assertThat(result.rows()).hasSize(1);
    assertThat(result.rows().getFirst().role()).isEqualTo("");
  }

  @Test
  void emptyArrayReturnsNull() throws Exception {
    var mdFile = tempDir.resolve("empty.md");
    Files.writeString(
        mdFile,
        """
        ---
        title: "Empty Article"
        ---

        Some body.
        """);

    mockResponse =
        """
        {"choices":[{"message":{"content":"[]"}}]}
        """;

    var result = processor.process(mdFile);

    assertThat(result).isNull();
  }

  @Test
  void httpErrorReturnsNull() throws Exception {
    var mdFile = tempDir.resolve("error.md");
    Files.writeString(
        mdFile,
        """
        ---
        title: "Error Article"
        ---

        Some body.
        """);

    processor =
        new LmStudioExtractProcessor(
            new LmStudioProperties("http://localhost:1234", "test-model", "test-key", 5, 30)) {
          @Override
          String post(String body) {
            throw new IllegalStateException("LM Studio error status=500");
          }
        };
    processor.systemPrompt = "system prompt";
    processor.userPromptTemplate = "User: {article_content}";

    var result = processor.process(mdFile);

    assertThat(result).isNull();
  }

  @Test
  void noFrontmatterUsesFilenameAsArticleId() throws Exception {
    var mdFile = Files.createTempFile(tempDir, "plain", ".md");
    Files.writeString(mdFile, "Just some plain text body with no frontmatter delimiters at all.");

    mockResponse =
        """
        {"choices":[{"message":{"content":"[{\\"prenom\\":\\"Marie\\",\\"nom\\":\\"Martin\\",\\"societe\\":\\"AXA\\",\\"role\\":\\"DG\\",\\"email\\":\\"\\"}]"}}]}
        """;

    var result = processor.process(mdFile);

    assertThat(result).isNotNull();
    assertThat(result.rows()).hasSize(1);
    assertThat(result.rows().getFirst().articleId()).isEqualTo(mdFile.getFileName().toString());
    assertThat(result.rows().getFirst().role()).isEqualTo("DG");
  }
}
