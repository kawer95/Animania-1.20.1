# Farm module guidance

This file supplements the repository-root `AGENTS.md` for `farm`.

## Scope

Farm owns livestock, horses and carts, breeding/products, troughs, nests, salt
licks, hives, food, and the related models, textures, recipes, and handbook
entries. Shared mechanics belong in Base only when other addons consume them.

## Important constraints

- Mod ID and live data namespace: `animania_farm`.
- Runtime dependency direction is Farm -> Base.
- For model, UV, animation, riding, sleeping, or child-rendering bugs, compare
  both the legacy Java model hierarchy and its texture dimensions before
  adjusting 1.20.1 transforms.
- Trough and hive rendering must be checked with normal and shader/OptiFine-like
  render paths; avoid coplanar surfaces, unstable random transforms, and
  accidental face culling.
- Recipes migrated from OreDictionary must use real 1.20.1 tags such as
  `forge:ingots/iron`, never synthetic `minecraft:iron_ingots`-style tags.

## Focused verification

- `.\gradlew.bat :farm:test`
- `.\gradlew.bat :farm:runGameTestServer`
- `.\gradlew.bat resourceAudit modelConversionAudit` for resource/model work
- Inspect `farm/run/gameTestServer/logs/latest.log` for recipe/tag load errors
  when recipes or data packs change.
