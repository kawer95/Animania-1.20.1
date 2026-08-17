# Farm gameplay audit remediation — 2026-08-12

This report records the implementation and executable verification for the 14
blocking findings in `farm-gameplay-audit-2026-08-12.md`. It does not mark the
whole 3.0.0 release complete and does not substitute server tests for the
separate client-visual and multiplayer release gates.

## Remediation status

| Finding | Implemented correction | Executable evidence |
| --- | --- | --- |
| F-GAME-001 | Per-ID health, movement speed and attack damage profiles for all 99 Farm animals | `allFarmAnimalProfilesApplyAtRuntime`; `FarmRegistryTest` |
| F-GAME-002 | Per-ID entity and API collision dimensions, including breed-specific goat and vehicle dimensions | `allFarmAnimalProfilesApplyAtRuntime`; `FarmRegistryTest` |
| F-GAME-003 | Farm hurt-by, melee, bull knockback/fighting, rooster combat and chicken amphibian targeting | Farm goal behavior audit: 10 results / 5 source rows, zero skipped/errors |
| F-GAME-004 | Farm chickens use the side-biased watch goal | Farm goal behavior audit and Farm dedicated-server suite |
| F-GAME-005 | Adult hog/sow saddling, carrot-on-a-stick mounting/control/boost and NBT persistence | `farmPigRidingAndCarvingKnifeRestoreLegacyInteractions` |
| F-GAME-006 | Carving knife sterilizes eligible adult Farm males and consumes one durability | `farmPigRidingAndCarvingKnifeRestoreLegacyInteractions` |
| F-GAME-007 | Fed/watered lactation gates, watered-state consumption, no mare milk, Mooshroom stew and Purp lava product | `farmProductionConversionsAndCrowStateFollowLegacyRules` |
| F-GAME-008 | Transactional adult Mooshroom-to-Friesian shearing with state, drops and tool damage | `farmProductionConversionsAndCrowStateFollowLegacyRules` |
| F-GAME-009 | Transactional Farm pig-to-zombified-piglin lightning conversion preserving child/name/NoAI | `farmProductionConversionsAndCrowStateFollowLegacyRules` |
| F-GAME-010 | Natural-spawn marker, named/non-natural exclusion, biome-compatible breed selection and cancel-after-add transaction | `vanillaCowReplacementRetainsWorldBoundarySemantics` |
| F-GAME-011 | Hive-biome production gate, 25 mB yield, configured delay plus 0..99 jitter, independent wild-hive sting schedule and biome-gated placement | `farmHiveAndHenSchedulersHonorBiomeAndCareGates`; facility behavior audit |
| F-GAME-012 | Day/awake/fed/watered egg gates, half-delay initialization and full-delay subsequent reset | `farmHiveAndHenSchedulersHonorBiomeAndCareGates`; `farmLactationAndEggLayStatePersists` |
| F-GAME-013 | Synced and persisted 50-tick rooster crow state driving the native model pose | `farmProductionConversionsAndCrowStateFollowLegacyRules` |
| F-GAME-014 | Removed milk-bottle/wedge shortcut and generic menu; restored direct progress/output interaction and fluid-only milk/water processing | `farmFluidsAndCheeseMoldProcess`; `cheeseMoldAcceptsModernMilkFluid` |

## Commands and results

- `gradlew :farm:test --rerun-tasks --no-daemon`: passed.
- `gradlew :farm:runGameTestServer --no-daemon`: 64/64 required tests passed.
- Six Farm behavior auditors: 76 results over 37 owned source rows, zero skipped/errors.
- `gradlew build --no-daemon`: all four modules and config migrator passed.
- `gradlew :base:runGameTestServer :extra:runGameTestServer :catsdogs:runGameTestServer --no-daemon`:
  Base 124/124, Extra 30/30, Cats&Dogs 13/13 passed.

GameTest log SHA-256 values:

- Farm: `1B23107CD961EF69A851A4C10BB32592FC4155CAA17FEC7E374995CF7ACAD378`
- Base: `D9A7818121EA22A4C40DBE9D770CC32267DE80E8AF06EE9D96EE2364366F4028`
- Extra: `60E3407EBAD37A4E9FD877F242697FA3DCE4EC04071176BFE4CC0BD5C8CBC6AD`
- Cats&Dogs: `2496ADBEA4AA8B5EAD3B744BCC92438BE08217C0D43AF2882F71F5F05DB9028E`

## Test build artifacts

The following `build/release` production artifacts are the only JARs suitable
for a normal Forge installation. `build/libs` contains mapped development
bytecode and must not be deployed.

| JAR | Bytes | SHA-256 |
| --- | ---: | --- |
| `animania-base-1.20.1-3.0.0.jar` | 2,828,945 | `7E01F2025DD3CBF2F785E499039DC508AA56109BF11875092EC115162D5AEC83` |
| `animania-farm-1.20.1-3.0.0.jar` | 6,324,893 | `7B177F26208787D47DE2CB7AABDD4CC6B9948199DE8F11DD9880755B49EF2417` |
| `animania-extra-1.20.1-3.0.0.jar` | 1,885,786 | `DE984252A63FBD1D39B7DB85BEA3B2C439A6CBF03511ED0CC78041E3DA74A6B1` |
| `animania-catsdogs-1.20.1-3.0.0.jar` | 816,177 | `846344BB772A644F8AB4C537EE3EB40E63F9E172C2B3459137D854F78A1F7D96` |

Implementation checkpoints: `27e50bc` and `50bc702` on
`codex/port-checkpoints`.
