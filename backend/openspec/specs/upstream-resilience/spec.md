# upstream-resilience Specification

## Purpose
Absorbs short PokeAPI failures by retrying them, and stops calling PokeAPI while it is failing (circuit breaker), so clients get a quick, honest 503 instead of waiting on a broken dependency.

## Requirements

### Requirement: Retry transient upstream failures
Every PokeAPI request SHALL be retried when it fails transiently: a connection failure, a 5xx answer or a 429 answer. It SHALL make at most 3 attempts in total, waiting an exponentially growing, randomized interval (200 ms initially) between them. A 404, any other 4xx answer, a timeout or an unreadable answer SHALL NOT be retried. The attempts and initial wait SHALL be configurable.

#### Scenario: A blip is absorbed
- **WHEN** PokeAPI answers a detail request with 503 once and then succeeds
- **THEN** `GET /api/v1/pokemon/1` answers 200, and PokeAPI received two requests for that Pokémon

#### Scenario: A timeout is not repeated
- **WHEN** PokeAPI doesn't answer within the read timeout
- **THEN** the response is 503, and PokeAPI received the request only once

#### Scenario: An unknown Pokémon is not retried
- **WHEN** PokeAPI answers 404 for a Pokémon
- **THEN** the response is 404, and PokeAPI received the request only once

### Requirement: Circuit breaker on PokeAPI
The service SHALL stop calling PokeAPI while it is failing. When at least half of the last calls fail (counted over a window of 20 calls, once at least 10 were made), the circuit SHALL open for 30 seconds. While it is open, requests that need PokeAPI SHALL answer 503 with `Retry-After` immediately, without calling PokeAPI. After that, 3 trial calls SHALL decide whether it closes or opens again. A 404 SHALL count as a success. All four numbers SHALL be configurable.

#### Scenario: Failing fast
- **WHEN** PokeAPI keeps failing until the circuit opens, and a client then requests `GET /api/v1/pokemon/1`
- **THEN** the response is 503 with `Retry-After`, and PokeAPI received no request for it

#### Scenario: Closing again
- **WHEN** the circuit is open, the open period has passed, and PokeAPI has recovered
- **THEN** the next requests are answered with data from PokeAPI

### Requirement: Observable resilience events
Every retry and every change of circuit state SHALL be logged with the request's correlation id, naming the upstream request and the reason, without the upstream response body.

#### Scenario: Circuit opens
- **WHEN** the circuit opens
- **THEN** a warning is logged that the PokeAPI circuit changed from `CLOSED` to `OPEN`
