# PokeAPI fixtures

Real responses from `https://pokeapi.co/api/v2`, captured on 2026-10-14 and **trimmed** to the
members this service reads (a full `/pokemon/{id}` response is ~300 KB because of `moves`).
Unknown members are ignored by the client DTOs, so trimming does not change behaviour.

| File | Source |
|---|---|
| `list-offset-0-limit-3.json` | `/pokemon?offset=0&limit=3` (count 1351) |
| `list-offset-0-limit-100000.json` | `/pokemon?offset=0&limit=100000`: every name and URL (1351), captured untrimmed on 2026-10-15 for the name search |
| `pokemon-{1,2,3,25,132,133}.json` | `/pokemon/{id}`: id, name, height, weight, sprites (front + official artwork), abilities, species, types, stats |
| `species-{1,2,3,25,132,133}.json` | `/pokemon-species/{id}`: id, name, genera (en, ja), flavor texts (first non-English, first and newest English), evolution-chain link |
| `evolution-chain-{1,10,66,67}.json` | `/evolution-chain/{id}`: Bulbasaur line, Pichu line, Ditto (no evolutions), Eevee (8 branches); evolution details reduced to the condition fields the service describes |
