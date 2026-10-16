package com.ballastlane.pokedex.architecture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.EvaluationResult;
import java.util.function.Function;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Proves the architecture rules are not vacuous: each rule must flag a fixture that deliberately
 * breaks it, and no rule may flag the clean fixture. Without this, a rule whose package pattern
 * matches nothing would pass forever.
 */
class ArchitectureRulesSelfTest {

  private static final String FIXTURES = "com.ballastlane.archfixtures";

  static Stream<Arguments> violatingFixtures() {
    return Stream.of(
        violation("domainspring", ArchitectureRules::domainIsFrameworkFree, "DomainUsingSpring"),
        violation("domainjpa", ArchitectureRules::domainIsFrameworkFree, "DomainUsingJpa"),
        violation("domainjackson", ArchitectureRules::domainIsFrameworkFree, "DomainUsingJackson"),
        violation("domainapp", ArchitectureRules::layerDependencies, "DomainUsingApplication"),
        violation("appadapter", ArchitectureRules::layerDependencies, "UseCaseUsingAdapter"),
        violation("appspring", ArchitectureRules::applicationIsFrameworkFree, "UseCaseUsingSpring"),
        violation(
            "adaptercoupling",
            ArchitectureRules::adaptersAreIsolatedFromEachOther,
            "ControllerUsingPersistence"),
        violation(
            "adapterinfra", ArchitectureRules::layerDependencies, "ControllerUsingInfrastructure"),
        violation("fieldinjection", ArchitectureRules::noFieldInjection, "FieldInjected"),
        violation("stdstreams", ArchitectureRules::noStandardStreams, "PrintsToConsole"),
        violation("jul", ArchitectureRules::noJavaUtilLogging, "UsesJavaUtilLogging"));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("violatingFixtures")
  void should_reportViolation_when_fixtureBreaksTheRule(
      String fixture, Function<ArchitectureRules, ArchRule> ruleOf, String offendingClass) {
    // Given
    String basePackage = FIXTURES + "." + fixture;
    JavaClasses classes = new ClassFileImporter().importPackages(basePackage);
    ArchRule rule = ruleOf.apply(ArchitectureRules.forBasePackage(basePackage));

    // When
    EvaluationResult result = rule.evaluate(classes);

    // Then
    assertThat(result.hasViolation())
        .as("rule '%s' must flag fixture '%s'", rule.getDescription(), fixture)
        .isTrue();
    assertThat(result.getFailureReport().getDetails())
        .as("violation report names the offending class")
        .anySatisfy(detail -> assertThat(detail).contains(offendingClass));
  }

  @Test
  void should_reportNoViolation_when_codeFollowsEveryRule() {
    // Given
    String basePackage = FIXTURES + ".clean";
    JavaClasses classes = new ClassFileImporter().importPackages(basePackage);

    // When / Then
    ArchitectureRules.forBasePackage(basePackage)
        .all()
        .forEach(
            rule ->
                assertThat(rule.evaluate(classes).hasViolation())
                    .as("rule '%s' must accept the clean fixture", rule.getDescription())
                    .isFalse());
  }

  private static Arguments violation(
      String fixture, Function<ArchitectureRules, ArchRule> ruleOf, String offendingClass) {
    return arguments(fixture, ruleOf, offendingClass);
  }
}
