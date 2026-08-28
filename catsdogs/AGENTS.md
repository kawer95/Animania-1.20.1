# Cats & Dogs module guidance

This file supplements the repository-root `AGENTS.md` for `catsdogs`.

## Scope

Cats & Dogs owns cat, dog, wolf, puppy, and kitten entities; pet AI and
interactions; pet bowls and litter/tower blocks; and their resources.

## Important constraints

- Mod ID and live resource namespace: `animania_catsdogs`.
- Runtime dependency direction is Cats & Dogs -> Base.
- Many breeds share geometry but not necessarily UV assumptions. Scan every
  model using a changed shared part and validate left/right leg mirroring,
  missing faces, and texture dimensions across breeds.
- Pet inventory/food/water state must synchronize and persist. Avoid solving a
  renderer symptom by changing authoritative gameplay data on the client.

## Focused verification

- `.\gradlew.bat :catsdogs:test`
- `.\gradlew.bat :catsdogs:runGameTestServer`
- `.\gradlew.bat resourceAudit modelConversionAudit` for resource/model work
