# Extra module guidance

This file supplements the repository-root `AGENTS.md` for `extra`.

## Scope

Extra owns rabbits, rodents, peafowl, amphibians, hamster equipment, and their
models, textures, recipes, AI, breeding, and handbook content.

## Important constraints

- Mod ID and live resource namespace: `animania_extra`.
- Runtime dependency direction is Extra -> Base.
- Stable entity variants must not flicker between frames. Rendering must derive
  deterministic texture/model choices from synchronized entity state.
- For peafowl tail/crest, rabbit pose, frog texture, and child model fixes,
  preserve the legacy parent-child transform hierarchy rather than compensating
  only in the handbook preview.

## Focused verification

- `.\gradlew.bat :extra:test`
- `.\gradlew.bat :extra:runGameTestServer`
- `.\gradlew.bat resourceAudit modelConversionAudit` for resource/model work
