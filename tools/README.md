# Migration tooling

`build_migration_matrix.py` reads the pinned 1.12 checkout and emits a JSON
inventory with source hashes, module ownership, candidate legacy IDs and
explicit implementation/verification fields.  It never changes the upstream
checkout.  A release is prohibited while any entry is `unstarted`, `open`, or
unverified.

Example:

```text
python tools/build_migration_matrix.py \
  --source upstream/Animania-1.12 \
  --output docs/migration-matrix.json
```

Additional release checks:

* `normalize_legacy_resources.py` rewrites 1.12 recipe serializers and model
  namespaces into 1.20.1-compatible JSON.
* `ensure_texture_aliases.py` records deterministic aliases for unresolved
  legacy texture names in `build/texture-aliases.json`.
* `ensure_locales.py` emits the 25-locale JSON surface for every module.
* `audit_resources.py` is the strict resource/data gate used by Gradle. It
  accepts `--legacy-root` for an explicit read-only 1.12 checkout and otherwise
  checks `upstream/Animania-1.12` followed by the sibling `_legacy_animania`.
* `audit_texture_resolver.py` expands every registered entity variant and
  checks the native nested texture path plus the renderer's flat fallback.
  It is a client/resource prerequisite, not a visual-regression pass.
* `audit_client_smoke.py` hashes a real Forge client log/debug log and checks
  resource reload, block-atlas initialization, all four mod IDs, and model
  construction errors. It does not close per-model client requirements.
* `audit_api_contract.py` binds the Java 17 public API state surface to the
  source fingerprints and the actual Base JUnit selectors.
* `audit_api_data.py` closes only the three public data contracts that have
  dedicated selectors; it is registered with the central evidence protocol.
* `audit_api_legacy_interfaces.py` binds only the four legacy interfaces with
  dedicated state/call selectors; data-manager facades without equivalent
  Java 17 behavior remain open.
* `audit_resource_semantics.py` proves the namespace/shape semantics of the
  explicitly modernized advancement, sound and mold resources; it does not
  close client geometry requirements.
* `audit_breed_behavior.py` binds each registered Farm, Extra and Cats&Dogs
  child ID to its own Forge GameTest marker and per-entry evidence file;
  source classes without a runtime child marker remain open.
* `audit_base_block_behavior.py` binds the Base storage/block contracts to
  multi-selector Forge GameTests and per-entry evidence files; it covers
  trough, nest, salt-lick, mud, straw, seed-pile and invisible-proxy behavior.
* `audit_base_registry_behavior.py` binds the complete Base sound registry and
  legacy OreDictionary-to-tag categories to live Forge selectors.
* `audit_farm_facility_behavior.py` binds Farm wool, cheese, hive and fluid/
  item-handler contracts to per-entry Forge GameTest evidence.
* `audit_farm_goal_behavior.py` binds Farm horse-goal and species-food
  temptation contracts to exact live Forge GameTest selectors.
* `audit_farm_special_item_behavior.py` binds Farm egg, food, fluid-container,
  cheese-wheel and riding-crop behavior to a live selector.
* `audit_farm_vehicle_behavior.py` binds Farm cart/tiller/wagon entities,
  inventories and placement items to exact live selectors.
* `audit_farm_child_growth_behavior.py` binds each Farm child base family to
  the live care-gated adult-transition selector.
* `audit_farm_fluid_behavior.py` binds Farm milk and honey fluid blocks to the
  live source/flowing registration and cheese-mold process selector.
* `audit_extra_hamster_behavior.py` binds the Extra hamster wheel, coloured
  ball/carry, and death-drop state to live server selectors.
* `audit_extra_spawn_behavior.py` binds Extra natural family caps, spawn-egg
  bypass, and Forge join-event vanilla-rabbit replacement to live selectors.
* `audit_sound_handler_behavior.py` binds Farm's 96 and Extra's 52 legacy
  sound IDs to independent Forge registry GameTests and mod-bus integration
  evidence; implementation ownership remains with `strict-java`.
* `audit_animation_conversion.py` regenerates all eight pinned CraftStudio
  clips in an isolated archive and compares the exact native
  `AnimationDefinition` source, alongside the Farm/Extra bake-and-bone JUnit
  selector. It does not close client visual requirements.
* `audit_config_defaults.py` compares every pinned Base/addon configuration
  default with ForgeConfigSpec and ConfigMigrator, and binds each module to
  its real registry/config-migrator JUnit selectors.
* `audit_java_model_implementation.py` regenerates each eligible pinned Java
  model in an isolated tree and compares its complete generated
  `LayerDefinition` method to the checked-in target. It supplies implementation
  evidence only, never a client/visual pass.
* `audit_public_api_facade_implementation.py` gives each historical public API
  facade a distinct target and JUnit selector while explicitly leaving a
  behavior requirement open when no one-to-one modern runtime mapping exists.
* `audit_generic_ai_behavior.py` maps each explicitly listed 1.12 generic AI
  to one native goal plus a unique marker from a freshly executed Forge
  GameTest. It refuses an absent marker or a non-passing module test run.
* `apply_verified_closure.py` classifies every row as `closed_verified`,
  `client_verification_pending`, `nonclient_verification_pending`, or
  `implementation_or_mapping_required`; this classification is central and
  never lets an auditor set `release_allowed`.
* `audit_config_converter_runtime.py` executes the packaged converter, checks
  read-only input and reports the real overwrite refusal on a second run.
* `audit_model_visual_regression.py` refuses weak “model exists” evidence;
  it accepts only a graphics-client capture manifest with a screenshot and
  geometry/pose digest for every one of the 130 converted model entries.
* `verify_release_gates.py --write` computes stage-7 global gates. Missing
  optional-compat, startup, multiplayer, or endurance artifacts are failures;
  this command cannot set `release_allowed`.
* Legacy/specialized auditors may emit local reports, but they are forbidden
  from setting a release bit. Only `apply_verified_closure.py` combines
  per-requirement evidence with the global gate report.
* `build_id_mapping.py` writes `docs/id-mapping.json` for registry migration.
