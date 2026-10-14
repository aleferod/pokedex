package com.ballastlane.pokedex.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/** Fails the build when production code breaks a Clean Architecture or coding rule. */
class ArchitectureTest {

  private static final String BASE_PACKAGE = "com.ballastlane.pokedex";

  private static JavaClasses productionClasses;

  @BeforeAll
  static void importProductionClasses() {
    productionClasses =
        new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages(BASE_PACKAGE);
  }

  static Stream<ArchRule> rules() {
    return ArchitectureRules.forBasePackage(BASE_PACKAGE).all().stream();
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("rules")
  void should_passOnProductionCode_when_ruleIsChecked(ArchRule rule) {
    rule.check(productionClasses);
  }
}
