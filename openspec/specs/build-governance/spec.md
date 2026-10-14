# build-governance Specification

## Purpose
Turns the project's architecture and quality rules into automated build gates, so a violation fails the build and CI instead of relying on code review to catch it.

## Requirements

### Requirement: Layer dependency rules
The build SHALL fail when production code breaks the dependency rule `adapter → application → domain`:
- `domain` depends on anything other than the Java standard library and `domain` itself
- `application` depends on `adapter`, `infrastructure` or any framework
- an inbound adapter depends on an outbound adapter, or the other way round
- an adapter depends on `infrastructure`

#### Scenario: Domain imports a framework
- **WHEN** a class in the `domain` package imports a Spring, persistence or JSON-mapping type
- **THEN** the build fails, and the report names the offending class and dependency

#### Scenario: Application imports an adapter
- **WHEN** a class in the `application` package references a class in an `adapter` package
- **THEN** the build fails, and the report names the offending class and dependency

#### Scenario: Adapters coupled to each other
- **WHEN** a class in `adapter.in.web` references a class in `adapter.out.persistence`
- **THEN** the build fails

#### Scenario: Rules proven against known violations
- **WHEN** the architecture rules run against test fixtures that deliberately break each rule
- **THEN** every rule reports its violation, so a rule that silently stopped matching anything is caught

### Requirement: Coding rules
The build SHALL fail when production code:
- injects dependencies into fields instead of through constructors
- writes to standard output or standard error
- uses `java.util.logging`

#### Scenario: Field injection
- **WHEN** a production class has a field annotated for dependency injection
- **THEN** the build fails

### Requirement: Coverage gate
The build SHALL fail when automated tests cover less than 85 % of the lines in the `domain` and `application` packages combined.

#### Scenario: Coverage below threshold
- **WHEN** the tests cover less than 85 % of the lines in `domain` and `application`
- **THEN** the build fails and states the measured and required coverage

#### Scenario: Coverage report produced
- **WHEN** the build runs the test suite
- **THEN** it produces a browsable HTML coverage report

### Requirement: Warning-free, consistently formatted code
The build SHALL fail on any compiler warning and on any source file that breaks the project's formatting standard.

#### Scenario: Compiler warning
- **WHEN** production or test code triggers a compiler warning, such as an unchecked conversion or deprecated API usage
- **THEN** the build fails

#### Scenario: Formatting violation
- **WHEN** a source file is not formatted to the project standard
- **THEN** the build fails and names the file
- **AND** a single documented command reformats the code base

### Requirement: Banned dependencies
The build SHALL fail when the dependency tree contains an in-memory substitute for the production database, or JUnit 4. The build SHALL also fail when it runs on a Java version older than 21.

#### Scenario: In-memory database added
- **WHEN** a developer adds the H2 database as a dependency
- **THEN** the build fails and names the banned dependency

### Requirement: Continuous integration
Every push to `main` and every pull request SHALL trigger a pipeline that:
- builds the service and runs the full test suite, including tests backed by real database containers
- validates the OpenSpec specifications in strict mode
- builds the container image
- scans the repository for committed secrets
- publishes the coverage report as a downloadable artifact

#### Scenario: Pull request opened
- **WHEN** a pull request is opened or updated
- **THEN** the pipeline runs every check above, and the pull request shows a pass or fail status for each

#### Scenario: Status badge
- **WHEN** someone views the repository README
- **THEN** it shows a badge with the pipeline's latest status on `main`
