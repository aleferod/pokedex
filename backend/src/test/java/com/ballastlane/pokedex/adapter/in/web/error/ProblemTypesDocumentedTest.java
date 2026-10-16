package com.ballastlane.pokedex.adapter.in.web.error;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ClassPathResource;

/** Every problem {@code type} URI the API can return must land on a documented section. */
class ProblemTypesDocumentedTest {

  private static final Path PROBLEMS_DOC = Path.of("docs", "problems.md");

  @Test
  void should_haveDocumentedSection_when_problemTypeExists() throws IOException {
    // Given
    Set<String> anchors = githubAnchorsOf(PROBLEMS_DOC);

    // When
    Set<String> slugs =
        Arrays.stream(ProblemType.values()).map(ProblemType::slug).collect(Collectors.toSet());

    // Then
    assertThat(anchors).containsAll(slugs);
  }

  @Test
  void should_pointTypeUrisAtProblemsDoc_when_defaultConfigurationIsUsed() {
    // Given
    YamlPropertiesFactoryBean yaml = new YamlPropertiesFactoryBean();
    yaml.setResources(new ClassPathResource("application.yml"));

    // When
    Properties properties = yaml.getObject();

    // Then
    assertThat(properties).isNotNull();
    assertThat(properties.getProperty("app.problems.base-uri"))
        .isEqualTo("https://github.com/aleferod/pokedex/blob/main/backend/docs/problems.md#");
  }

  /** Anchors as GitHub renders them for markdown headings. */
  private static Set<String> githubAnchorsOf(Path markdown) throws IOException {
    return Files.readAllLines(markdown).stream()
        .filter(line -> line.startsWith("## ") || line.startsWith("### "))
        .map(line -> line.replaceFirst("^#+\\s+", ""))
        .map(
            heading ->
                heading
                    .toLowerCase(Locale.ROOT)
                    .replaceAll("[^a-z0-9 _-]", "")
                    .trim()
                    .replace(' ', '-'))
        .collect(Collectors.toSet());
  }
}
