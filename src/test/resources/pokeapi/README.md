# PokeAPI fixtures

Real responses from `https://pokeapi.co/api/v2`, captured on 2026-10-14 and **trimmed** to the
members this service reads (a full `/pokemon/{id}` response is ~300 KB because of `moves`).
Unknown members are ignored by the client DTOs, so trimming does not change behaviour.

| File | Source |
|---|---|
| `list-offset-0-limit-3.json` | `/pokemon?offset=0&limit=3` (count 1351) |
| `pokemon-{1,2,3,25}.json` | `/pokemon/{id}`: id, name, height, weight, sprites (front + official artwork), abilities, species, types, stats |
| `species-{1,2,3,25}.json` | `/pokemon-species/{id}`: id, name, genera (en, ja), a few flavor texts, evolution chain link |
