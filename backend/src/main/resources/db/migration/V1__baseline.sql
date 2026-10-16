-- Baseline of the pokedex schema.
--
-- Intentionally empty: every table arrives with the change that introduces its feature
-- (users with add-user-auth, local_pokemon with add-local-sync). This first version proves
-- the migration pipeline end to end: history table, ordering and idempotent restarts.
SELECT 1;
