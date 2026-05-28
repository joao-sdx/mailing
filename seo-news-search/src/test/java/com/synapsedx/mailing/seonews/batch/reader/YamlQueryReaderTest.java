package com.synapsedx.mailing.seonews.batch.reader;

import static org.assertj.core.api.Assertions.assertThat;

import com.synapsedx.mailing.seonews.config.SeoNewsProperties;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;

class YamlQueryReaderTest {

  @Test
  void readsAllQueriesFromYaml() throws Exception {
    var properties = new SeoNewsProperties("classpath:dataforseo-queries.yml", "output");
    var reader = new YamlQueryReader(properties, new DefaultResourceLoader());

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
