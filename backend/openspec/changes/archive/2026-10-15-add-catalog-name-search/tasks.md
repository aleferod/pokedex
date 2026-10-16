# Tasks: add-catalog-name-search

TDD: each `test:` commit is red on assertions against stubs, and the `feat:` commit makes it green. Tests follow `should_<expected>_when_<condition>`.

## 1. Domain

- [x] 1.1 Write `PokemonNameQueryTest` (D1). Verify: red against a stub.
- [x] 1.2 Implement `PokemonNameQuery` and `CatalogName`. Verify: 1.1 green.

## 2. Use case

- [x] 2.1 Write the search tests in `ListPokemonUseCaseTest` (D2). Add the two port methods and `ListPokemon.search` as stubs. Verify: red.
- [x] 2.2 Implement `ListPokemonUseCase.search`. Verify: 2.1 green.

## 3. Adapters

- [x] 3.1 Capture the full name list as a fixture. Write the adapter tests (`findAllNames`, `findSummaries`) and the caching tests (D3, D4). Verify: red.
- [x] 3.2 Implement both in `PokeApiCatalogAdapter` and `CachingPokemonCatalog`, with the `pokemon-names` cache in `CatalogCacheConfiguration`. Verify: 3.1 green.

## 4. Endpoint

- [x] 4.1 Write the controller slice tests and `PokemonCatalogIT` search tests (D5, D6). Verify: red.
- [x] 4.2 Accept `name` in `PokemonController`, with the OpenAPI documentation. Verify: 4.1 green; `./scripts/verify-in-docker.sh` green (coverage, ArchUnit, Spotless, `-Werror`).

## 5. Verification

- [x] 5.1 Against `docker compose up --build` and the real PokeAPI: `?name=pika`, `?name=mr mime`, `?name=chu&size=5&page=4`, `?name=pi`, and the name list requested once across two searches. Update the README (Catalog section, endpoints table). Run `openspec validate add-catalog-name-search --strict` and gitleaks. Verify: as specified; valid; no leaks.

## Workflow follow-up

- Push, open the PR and wait for CI to go green.
- Archive as the PR's last commit, with `openspec validate --all --strict` run before committing. Wait for CI to be green again, then squash-merge.
