# Spec Delta: service-configuration

## ADDED Requirements

### Requirement: Token-signing secret
Outside the `test` profile, the service SHALL read its token-signing secret from `JWT_SECRET`. If the secret is missing, startup SHALL fail with a message naming `JWT_SECRET`. If it is shorter than 32 bytes, startup SHALL fail with a message stating the minimum length. The repository SHALL contain no real secret, only a placeholder in `.env.example`.

#### Scenario: Secret missing
- **WHEN** the service starts with the `docker` profile, `DB_PASSWORD` set and `JWT_SECRET` unset
- **THEN** startup fails, and the error message names `JWT_SECRET`

#### Scenario: Secret too short
- **WHEN** the service starts with `JWT_SECRET` set to a 16-character value
- **THEN** startup fails, and the error message states that at least 32 bytes are required

#### Scenario: Token lifetime configurable
- **WHEN** the service starts with `JWT_TTL` set to `PT15M`
- **THEN** issued tokens expire 15 minutes after issue
