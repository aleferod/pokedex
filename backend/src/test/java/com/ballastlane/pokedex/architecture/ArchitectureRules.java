package com.ballastlane.pokedex.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.GeneralCodingRules;
import java.util.List;

/**
 * The project's architecture rules, built for a base package so the same definitions run against
 * production code ({@link ArchitectureTest}) and against deliberately broken fixtures ({@link
 * ArchitectureRulesSelfTest}). Specification pattern: each method is one named, reusable rule.
 *
 * <p>Layers may still be empty while the project grows, so layer-scoped rules allow an empty
 * "should"; the self-test is what guarantees they still detect violations.
 */
final class ArchitectureRules {

  private static final String DOMAIN = "Domain";
  private static final String APPLICATION = "Application";
  private static final String ADAPTERS = "Adapters";
  private static final String INFRASTRUCTURE = "Infrastructure";

  private final String domain;
  private final String application;
  private final String adapter;
  private final String infrastructure;

  private ArchitectureRules(String basePackage) {
    this.domain = basePackage + ".domain..";
    this.application = basePackage + ".application..";
    this.adapter = basePackage + ".adapter..";
    this.infrastructure = basePackage + ".infrastructure..";
  }

  static ArchitectureRules forBasePackage(String basePackage) {
    return new ArchitectureRules(basePackage);
  }

  /** Dependency rule: adapter -> application -> domain; infrastructure wires and is never used. */
  ArchRule layerDependencies() {
    return layeredArchitecture()
        .consideringOnlyDependenciesInLayers()
        .withOptionalLayers(true)
        .layer(DOMAIN)
        .definedBy(domain)
        .layer(APPLICATION)
        .definedBy(application)
        .layer(ADAPTERS)
        .definedBy(adapter)
        .layer(INFRASTRUCTURE)
        .definedBy(infrastructure)
        .whereLayer(INFRASTRUCTURE)
        .mayNotBeAccessedByAnyLayer()
        .whereLayer(ADAPTERS)
        .mayOnlyBeAccessedByLayers(INFRASTRUCTURE)
        .whereLayer(APPLICATION)
        .mayOnlyBeAccessedByLayers(ADAPTERS, INFRASTRUCTURE)
        .whereLayer(DOMAIN)
        .mayOnlyBeAccessedByLayers(APPLICATION, ADAPTERS, INFRASTRUCTURE)
        .as("layers follow adapter -> application -> domain, infrastructure is never depended on");
  }

  /** The domain knows only itself and the JDK: no Spring, persistence or JSON mapping. */
  ArchRule domainIsFrameworkFree() {
    return classes()
        .that()
        .resideInAPackage(domain)
        .should()
        .onlyDependOnClassesThat()
        .resideInAnyPackage(domain, "java..")
        .allowEmptyShould(true)
        .as("domain depends only on the domain and the JDK");
  }

  /** Use cases know the domain, their ports and the JDK, so they test without a Spring context. */
  ArchRule applicationIsFrameworkFree() {
    return classes()
        .that()
        .resideInAPackage(application)
        .should()
        .onlyDependOnClassesThat()
        .resideInAnyPackage(application, domain, "java..")
        .allowEmptyShould(true)
        .as("application depends only on the application, the domain and the JDK");
  }

  /** Each adapter (adapter.in.web, adapter.out.pokeapi, ...) is independent of the others. */
  ArchRule adaptersAreIsolatedFromEachOther() {
    return slices()
        .matching(adapter.replace("..", "") + ".(*).(*)..")
        .namingSlices("adapter $1.$2")
        .should()
        .notDependOnEachOther()
        .allowEmptyShould(true)
        .as("adapters do not depend on each other");
  }

  ArchRule noFieldInjection() {
    return GeneralCodingRules.NO_CLASSES_SHOULD_USE_FIELD_INJECTION.allowEmptyShould(true);
  }

  ArchRule noStandardStreams() {
    return GeneralCodingRules.NO_CLASSES_SHOULD_ACCESS_STANDARD_STREAMS;
  }

  ArchRule noJavaUtilLogging() {
    return GeneralCodingRules.NO_CLASSES_SHOULD_USE_JAVA_UTIL_LOGGING;
  }

  List<ArchRule> all() {
    return List.of(
        layerDependencies(),
        domainIsFrameworkFree(),
        applicationIsFrameworkFree(),
        adaptersAreIsolatedFromEachOther(),
        noFieldInjection(),
        noStandardStreams(),
        noJavaUtilLogging());
  }
}
