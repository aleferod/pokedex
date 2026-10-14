# Spec Delta: api-errors

## ADDED Requirements

### Requirement: Field-level input errors
When a request is rejected with 400 because of its input, `errors[]` SHALL contain one entry per offending field, each with that field's name and a message. This applies to:
- constraint violations
- values of the wrong JSON type
- members the endpoint doesn't accept

A body that isn't valid JSON SHALL answer 400 with a `detail` saying the body is malformed. Its `errors[]` stays empty, because no field can be blamed.

#### Scenario: Missing required field
- **WHEN** a client posts a JSON body without the required `username`
- **THEN** the response status is 400 and `errors` contains an entry with `field` `username` and a non-empty `message`

#### Scenario: Unknown member
- **WHEN** a client posts a JSON body with a member `role` that the endpoint does not accept
- **THEN** the response status is 400 and `errors` contains an entry with `field` `role`

#### Scenario: Wrong JSON type
- **WHEN** a client sends `"username": {"nested": true}` where a string is expected
- **THEN** the response status is 400 and `errors` contains an entry with `field` `username`

#### Scenario: Malformed JSON
- **WHEN** a client posts `{"username": ` (truncated JSON)
- **THEN** the response status is 400, `detail` states that the request body is malformed JSON, and `errors` is empty

#### Scenario: Several invalid fields
- **WHEN** a client posts a body where both `username` and `email` are invalid
- **THEN** `errors` contains one entry for `username` and one for `email`
