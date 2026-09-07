"""Strict resource/data audit used by the release gate."""
from __future__ import annotations

import argparse
import json
import re
import struct
from pathlib import Path

MODULES = {"base": "animania", "farm": "animania_farm", "extra": "animania_extra", "catsdogs": "animania_catsdogs"}
ENTITY_SOURCES = {
    "farm": "farm/src/main/java/com/animania/farm/FarmLegacyIds.java",
    "extra": "extra/src/main/java/com/animania/extra/ExtraLegacyIds.java",
    "catsdogs": "catsdogs/src/main/java/com/animania/catsdogs/CatsDogsLegacyIds.java",
}
CONTENT_SOURCES = {
    "farm": "farm/src/main/java/com/animania/farm/FarmContent.java",
    "extra": "extra/src/main/java/com/animania/extra/ExtraContent.java",
    "catsdogs": "catsdogs/src/main/java/com/animania/catsdogs/CatsDogsContent.java",
}
RECIPE_TYPES = {
    "minecraft:crafting_shaped", "minecraft:crafting_shapeless", "minecraft:smelting",
    "minecraft:smoking", "minecraft:campfire_cooking", "minecraft:stonecutting",
    "minecraft:blasting", "minecraft:smithing_transform", "minecraft:smithing_trim",
    "forge:conditional", "animania:slop", "animania_farm:milk_conversion",
}
COOKING_RECIPE_TYPES = {
    "minecraft:smelting", "minecraft:smoking", "minecraft:campfire_cooking", "minecraft:blasting",
}
LEGACY_COOKING_PAIRS = {
    "farm": {
        "raw_prime_steak": "cooked_prime_steak", "raw_prime_beef": "cooked_prime_beef",
        "raw_horse": "cooked_horse", "raw_prime_pork": "cooked_prime_pork",
        "raw_prime_bacon": "cooked_prime_bacon", "raw_prime_chicken": "cooked_prime_chicken",
        "raw_chevon": "cooked_chevon", "raw_prime_chevon": "cooked_prime_chevon",
        "raw_prime_mutton": "cooked_prime_mutton",
    },
    "extra": {
        "raw_prime_rabbit": "cooked_prime_rabbit", "raw_frog_legs": "cooked_frog_legs",
        "raw_peacock": "cooked_peacock", "raw_prime_peacock": "cooked_prime_peacock",
    },
}
VANILLA_LOOT_FUNCTIONS = {
    "minecraft:set_count", "minecraft:looting_enchant", "minecraft:furnace_smelt", "minecraft:copy_state",
}
VANILLA_ADVANCEMENT_TRIGGERS = {
    "minecraft:impossible", "minecraft:tick", "minecraft:inventory_changed",
    "minecraft:recipe_crafted", "minecraft:player_killed_entity", "minecraft:entity_killed_player",
    "minecraft:player_hurt_entity", "minecraft:used_totem", "minecraft:consume_item",
    "minecraft:location", "minecraft:placed_block", "minecraft:enchanted_item",
}
SUPPORTED_ADVANCEMENT_TRIGGERS = VANILLA_ADVANCEMENT_TRIGGERS | {"animania:feed_animal"}
EXTERNAL_MODEL_PARENTS = {"forge:item/bucket"}


def _resolve_legacy_root(root: Path, requested: Path | None = None) -> Path | None:
    """Locate the read-only 1.12 checkout used for byte-level comparisons."""
    if requested is not None:
        candidate = requested if requested.is_absolute() else root / requested
        return candidate.resolve() if candidate.is_dir() else None
    for candidate in (root / "upstream" / "Animania-1.12", root.parent / "_legacy_animania"):
        if candidate.is_dir():
            return candidate.resolve()
    return None


def _is_external_model_parent(parent: str) -> bool:
    """Return whether a model parent is supplied by Minecraft or Forge itself."""
    return parent.startswith("minecraft:") or parent in EXTERNAL_MODEL_PARENTS


def _validate_ingredient(value: object, location: str, errors: list[str]) -> None:
    """Require modern item/tag ingredient objects (including shaped keys)."""
    if isinstance(value, list):
        for index, child in enumerate(value):
            _validate_ingredient(child, f"{location}[{index}]", errors)
        return
    if not isinstance(value, dict) or not (isinstance(value.get("item"), str) or isinstance(value.get("tag"), str)):
        errors.append(f"{location}: invalid ingredient {value!r}")


def _validate_recipe(data: dict, location: str, errors: list[str]) -> None:
    recipe_type = data.get("type")
    if recipe_type not in RECIPE_TYPES:
        errors.append(f"unsupported recipe type {recipe_type} in {location}")
    if recipe_type == "forge:conditional":
        choices = data.get("recipes")
        if not isinstance(choices, list) or not choices:
            errors.append(f"{location}: conditional recipe needs choices")
            return
        for index, choice in enumerate(choices):
            if not isinstance(choice, dict) or not isinstance(choice.get("conditions"), list) or not isinstance(choice.get("recipe"), dict):
                errors.append(f"{location}: invalid conditional choice {index}")
                continue
            for condition in choice["conditions"]:
                if not isinstance(condition, dict) or condition.get("type") != "forge:mod_loaded" or not isinstance(condition.get("modid"), str):
                    errors.append(f"{location}: unsupported conditional recipe condition {condition!r}")
            _validate_recipe(choice["recipe"], f"{location}:recipes[{index}]", errors)
        return
    if recipe_type == "animania:slop":
        if location.replace("\\", "/").endswith("data/animania/recipes/slop.json"):
            return
        errors.append(f"{location}: animania:slop is reserved for the canonical slop recipe")
        return
    if recipe_type == "animania_farm:milk_conversion":
        if location.replace("\\", "/").endswith("data/animania_farm/recipes/milk_conversion.json"):
            return
        errors.append(f"{location}: milk conversion serializer is reserved for its canonical recipe")
        return
    if recipe_type in COOKING_RECIPE_TYPES:
        _validate_ingredient(data.get("ingredient"), f"{location}:ingredient", errors)
        if not isinstance(data.get("result"), str) or ":" not in data["result"]:
            errors.append(f"{location}: cooking result must be a namespaced item ID")
        if not isinstance(data.get("cookingtime"), int) or data["cookingtime"] <= 0:
            errors.append(f"{location}: cookingtime must be positive")
        return
    ingredients = data.get("ingredients")
    if ingredients is not None:
        _validate_ingredient(ingredients, f"{location}:ingredients", errors)
    key = data.get("key")
    if isinstance(key, dict):
        for symbol, ingredient in key.items():
            _validate_ingredient(ingredient, f"{location}:key.{symbol}", errors)
    result = data.get("result")
    if not isinstance(result, dict) or not isinstance(result.get("item"), str):
        errors.append(f"{location}: result must contain an item ID")


def _validate_loot(data: object, location: str, errors: list[str]) -> None:
    if isinstance(data, dict):
        function = data.get("function")
        if function is not None and function not in VANILLA_LOOT_FUNCTIONS:
            errors.append(f"{location}: unsupported loot function {function}")
        for key, value in data.items():
            _validate_loot(value, f"{location}.{key}", errors)
    elif isinstance(data, list):
        for index, value in enumerate(data):
            _validate_loot(value, f"{location}[{index}]", errors)


def _validate_advancement_node(data: object, location: str, errors: list[str]) -> None:
    if isinstance(data, dict):
        trigger = data.get("trigger")
        if trigger == "minecraft:tick":
            errors.append(f"{location}: unconditional tick criterion is forbidden")
        elif trigger == "animania:feed_animal":
            conditions = data.get("conditions")
            if not isinstance(conditions, dict) or not isinstance(conditions.get("entity"), str):
                errors.append(f"{location}: feed criterion must identify exactly one entity")
            itemstack = conditions.get("itemstack") if isinstance(conditions, dict) else None
            if itemstack is not None and (not isinstance(itemstack, dict) or not isinstance(itemstack.get("item"), str)):
                errors.append(f"{location}: feed criterion itemstack must contain a concrete item ID")
        elif trigger is not None and trigger not in SUPPORTED_ADVANCEMENT_TRIGGERS:
            errors.append(f"{location}: unsupported advancement trigger {trigger}")
        for key, value in data.items():
            _validate_advancement_node(value, f"{location}.{key}", errors)
    elif isinstance(data, list):
        for index, value in enumerate(data):
            _validate_advancement_node(value, f"{location}[{index}]", errors)


def _validate_advancement(data: object, location: str, errors: list[str]) -> None:
    """Validate both the advancement graph payload and every criterion node."""
    if not isinstance(data, dict):
        errors.append(f"{location}: advancement root must be an object")
        return
    criteria = data.get("criteria")
    if not isinstance(criteria, dict) or not criteria:
        errors.append(f"{location}: advancement must contain at least one criterion")
        return
    for name, criterion in criteria.items():
        if not isinstance(name, str) or not name:
            errors.append(f"{location}: criterion names must be non-empty strings")
        if not isinstance(criterion, dict) or not isinstance(criterion.get("trigger"), str):
            errors.append(f"{location}: criterion {name!r} must contain a trigger")

    requirements = data.get("requirements")
    if requirements is not None:
        if not isinstance(requirements, list) or not requirements:
            errors.append(f"{location}: requirements must be a non-empty list")
        else:
            flattened: list[str] = []
            for index, group in enumerate(requirements):
                if not isinstance(group, list) or not group or not all(isinstance(item, str) for item in group):
                    errors.append(f"{location}: requirements[{index}] must be a non-empty string list")
                    continue
                flattened.extend(group)
            unknown = sorted(set(flattened) - set(criteria))
            omitted = sorted(set(criteria) - set(flattened))
            duplicated = sorted({name for name in flattened if flattened.count(name) > 1})
            if unknown:
                errors.append(f"{location}: requirements reference unknown criteria: {', '.join(unknown)}")
            if omitted:
                errors.append(f"{location}: requirements omit criteria: {', '.join(omitted)}")
            if duplicated:
                errors.append(f"{location}: requirements duplicate criteria: {', '.join(duplicated)}")
    _validate_advancement_node(data, location, errors)


def _validate_advancement_graph(root: Path, namespace: str, module: str, errors: list[str]) -> None:
    advancement_root = root / "data" / namespace / "advancements"
    if not advancement_root.exists():
        return
    records: dict[str, tuple[Path, dict]] = {}
    for path in advancement_root.rglob("*.json"):
        try:
            data = json.loads(path.read_text(encoding="utf-8"))
        except (OSError, json.JSONDecodeError):
            continue
        advancement_id = f"{namespace}:{path.relative_to(advancement_root).with_suffix('').as_posix()}"
        records[advancement_id] = (path, data)

    roots = []
    for advancement_id, (path, data) in records.items():
        parent = data.get("parent")
        if parent is None:
            roots.append(advancement_id)
            # A visible root must never use a criterion that fires merely by
            # joining or ticking. Root completion is intentionally driven by
            # actual Animania gameplay descendants, matching the 1.12 tree.
            triggers = {criterion.get("trigger") for criterion in data.get("criteria", {}).values()
                        if isinstance(criterion, dict)}
            if triggers != {"minecraft:impossible"}:
                errors.append(f"{module}: advancement root {advancement_id} must use only minecraft:impossible")
            display = data.get("display", {})
            background = display.get("background") if isinstance(display, dict) else None
            if isinstance(background, str) and ":" in background:
                texture_namespace, texture_path = background.split(":", 1)
                texture = root / "assets" / texture_namespace / texture_path
                if not texture.is_file():
                    errors.append(
                        f"{module}: advancement root {advancement_id} has missing background texture {background}"
                    )
        elif isinstance(parent, str) and parent.split(":", 1)[0] in MODULES.values() and parent not in records:
            errors.append(f"{module}: advancement {advancement_id} has missing Animania parent {parent}")
    if len(roots) != 1:
        errors.append(f"{module}: expected exactly one advancement root, found {len(roots)}")


def _legacy_entity_ids(root: Path, module: str) -> set[str]:
    source = root / ENTITY_SOURCES[module]
    if not source.exists():
        return set()
    text = source.read_text(encoding="utf-8")
    body = text.split("List.of(", 1)[1].split(");", 1)[0]
    return set(re.findall(r'"([a-z0-9_]+)"', body))


def _java_list(root: Path, source: str, field: str) -> set[str]:
    text = (root / source).read_text(encoding="utf-8")
    match = re.search(field + r"\s*=\s*List\.of\((.*?)\);", text, re.S)
    return set(re.findall(r'"([a-z0-9_]+)"', match.group(1))) if match else set()


def _validate_biome_modifier(data: object, location: str, namespace: str, errors: list[str]) -> None:
    if not isinstance(data, dict):
        errors.append(f"{location}: biome modifier must be an object")
        return
    modifier_type = data.get("type")
    if modifier_type == f"{namespace}:configured_spawns":
        if set(data) != {"type"}:
            errors.append(f"{location}: config-backed biome modifier contains unsupported fields")
        return
    if modifier_type != "forge:add_spawns":
        errors.append(f"{location}: unsupported biome modifier type {modifier_type!r}")
        return
    biomes = data.get("biomes")
    if not isinstance(biomes, (str, list)):
        errors.append(f"{location}: biome modifier missing biomes tag/list")
    spawners = data.get("spawners")
    if not isinstance(spawners, list) or not spawners:
        errors.append(f"{location}: biome modifier must contain spawners")
        return
    for index, spawner in enumerate(spawners):
        if not isinstance(spawner, dict):
            errors.append(f"{location}:spawners[{index}] is not an object")
            continue
        entity_type = spawner.get("type")
        if not isinstance(entity_type, str) or ":" not in entity_type:
            errors.append(f"{location}:spawners[{index}] missing namespaced type")
        for key in ("weight", "minCount", "maxCount"):
                if not isinstance(spawner.get(key), int) or spawner[key] <= 0:
                    errors.append(f"{location}:spawners[{index}] invalid {key}")


def _validate_png(path: Path, location: str, errors: list[str]) -> None:
    """Validate the PNG signature/header without requiring an image library."""
    try:
        header = path.read_bytes()[:24]
    except OSError as exc:
        errors.append(f"{location}: unreadable texture: {exc}")
        return
    if len(header) < 24 or header[:8] != b"\x89PNG\r\n\x1a\n" or header[12:16] != b"IHDR":
        errors.append(f"{location}: invalid PNG header")
        return
    width, height = struct.unpack(">II", header[16:24])
    if width <= 0 or height <= 0 or width > 2048 or height > 2048:
        errors.append(f"{location}: invalid dimensions {width}x{height}")


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--root", type=Path, required=True)
    parser.add_argument("--legacy-root", type=Path,
                        help="read-only Animania 1.12 checkout; defaults to the pinned or sibling checkout")
    args = parser.parse_args()
    errors: list[str] = []
    report: dict[str, object] = {"modules": {}, "errors": errors}
    legacy_root = _resolve_legacy_root(args.root, args.legacy_root)
    if legacy_root is None:
        errors.append(
            "missing legacy source tree; pass --legacy-root or provide "
            "upstream/Animania-1.12 or ../_legacy_animania"
        )
    for module, namespace in MODULES.items():
        resource_root = args.root / module / "src" / "main" / "resources"
        module_report = {
            "json": 0,
            "models": 0,
            "recipes": 0,
            "loot_tables": 0,
            "locales": 0,
            "entity_textures": 0,
            "manual_pages": 0,
            "sounds": 0,
        }
        for path in resource_root.rglob("*"):
            if not path.is_file():
                continue
            relative = path.relative_to(resource_root).as_posix().lower()
            # The source checkout retains the pinned 1.12 namespace for
            # byte-level migration comparison, but that tree is excluded
            # from processResources.  Audit the canonical 1.20.1 namespace
            # only so duplicated legacy manual/sound/data files do not make
            # parity counts appear doubled.
            if module != "base" and relative.startswith(f"assets/{module}/animania/"):
                continue
            if module == "base" and any(relative.startswith(f"assets/animania/{kind}/")
                                        for kind in ("advancements", "recipes", "loot_tables", "tags")):
                continue
            if path.suffix.lower() == ".json" and "manual" in path.relative_to(resource_root).parts:
                module_report["manual_pages"] += 1
            if re.search(r"craftstudio|\.csjs(model|anim)$", relative):
                errors.append(f"{module}: CraftStudio resource {relative}")
            if path.suffix.lower() == ".json":
                module_report["json"] += 1
                try:
                    data = json.loads(path.read_text(encoding="utf-8"))
                except (OSError, json.JSONDecodeError) as exc:
                    errors.append(f"{module}: invalid JSON {relative}: {exc}")
                    continue
                if path.name.lower() == "sounds.json" and isinstance(data, dict):
                    module_report["sounds"] += len(data)
                    for event_id, event in data.items():
                        if event_id != event_id.lower() or not re.fullmatch(r"[a-z0-9/._-]+", event_id):
                            errors.append(f"{module}: invalid modern sound event ID {event_id!r} in {relative}")
                        if not isinstance(event, dict) or not isinstance(event.get("sounds"), list):
                            errors.append(f"{module}: malformed sound event {event_id!r} in {relative}")
                            continue
                        for sample in event["sounds"]:
                            name = sample.get("name") if isinstance(sample, dict) else sample
                            if not isinstance(name, str):
                                errors.append(f"{module}: malformed sound sample in {event_id!r}")
                                continue
                            if name != name.lower() or not re.fullmatch(r"(?:[a-z0-9._-]+:)?[a-z0-9/._-]+", name):
                                errors.append(f"{module}: invalid modern sound sample ID {name!r} in {event_id!r}")
                                continue
                            sample_namespace, separator, sample_path = name.partition(":")
                            if not separator:
                                sample_namespace, sample_path = namespace, sample_namespace
                            owner = next((candidate for candidate, candidate_namespace in MODULES.items()
                                          if candidate_namespace == sample_namespace), None)
                            sample_file = (args.root / owner / "src/main/resources/assets" / sample_namespace /
                                           "sounds" / f"{sample_path}.ogg") if owner else None
                            if sample_file is None or not sample_file.exists():
                                errors.append(f"{module}: missing sound sample {name} referenced by {event_id!r}")
                if "/models/" in f"/{relative}" and isinstance(data, dict):
                    module_report["models"] += 1
                    textures = data.get("textures", {})
                    if isinstance(textures, dict):
                        for ref in textures.values():
                            if not isinstance(ref, str) or ref.startswith("#"):
                                continue
                            # An unqualified model texture resolves in the
                            # current namespace.  Treat it exactly like an
                            # explicit reference so old 1.12 plural paths
                            # cannot silently render purple/black.
                            if ":" in ref:
                                ref_namespace, texture_path = ref.split(":", 1)
                            else:
                                ref_namespace, texture_path = namespace, ref
                            if texture_path.startswith(("items/", "blocks/")):
                                errors.append(f"{module}: legacy plural texture path {ref} referenced by {relative}")
                            if ref_namespace == "minecraft":
                                continue
                            # Models in an addon may deliberately reference a
                            # Base texture because Base is a mandatory runtime
                            # dependency (the legacy egg tint layers do this).
                            texture_candidates = [
                                args.root / owner / "src/main/resources/assets" / ref_namespace / "textures" / f"{texture_path}.png"
                                for owner in MODULES
                            ]
                            if not any(texture.exists() for texture in texture_candidates):
                                errors.append(f"{module}: missing texture {ref} referenced by {relative}")
                if "/recipes/" in f"/{relative}" and "/data/" in f"/{relative}" and isinstance(data, dict):
                    module_report["recipes"] += 1
                    _validate_recipe(data, f"{module}:{relative}", errors)
                if "/loot_tables/" in f"/{relative}" and "/data/" in f"/{relative}":
                    module_report["loot_tables"] += 1
                    if "/entities/" in f"/{relative}":
                        if data.get("type") != "minecraft:entity" or not isinstance(data.get("pools"), list):
                            errors.append(f"{module}:{relative}: entity loot table requires minecraft:entity type and pools")
                    _validate_loot(data, f"{module}:{relative}", errors)
                if "/advancements/" in f"/{relative}" and "/data/" in f"/{relative}":
                    _validate_advancement(data, f"{module}:{relative}", errors)
                if "/forge/biome_modifier/" in f"/{relative}" and "/data/" in f"/{relative}":
                    _validate_biome_modifier(data, f"{module}:{relative}", namespace, errors)
        if module != "base":
            _validate_advancement_graph(resource_root, namespace, module, errors)
        if module in ENTITY_SOURCES:
            entity_dir = resource_root / "data" / namespace / "loot_tables" / "entities"
            actual = {item.stem for item in entity_dir.glob("*.json")} if entity_dir.exists() else set()
            expected = _legacy_entity_ids(args.root, module)
            missing = sorted(expected - actual)
            extra = sorted(actual - expected)
            if missing:
                errors.append(f"{module}: missing entity loot tables: {', '.join(missing)}")
            if extra:
                errors.append(f"{module}: unregistered entity loot tables: {', '.join(extra)}")

            # Entity renderers resolve the stable legacy registry path to a
            # same-named texture at runtime.  Validate that path directly in
            # addition to the texture references found in block/item models;
            # this catches a visually invisible entity even when the shared
            # native ModelPart renderer still compiles and registers.
            texture_dir = resource_root / "assets" / namespace / "textures" / "entity"
            textures = {item.stem for item in texture_dir.glob("*.png")} if texture_dir.exists() else set()
            module_report["entity_textures"] = len(textures)
            for texture in sorted(texture_dir.glob("*.png")):
                _validate_png(texture, f"{module}:entity texture {texture.name}", errors)
            missing_textures = sorted(expected - textures)
            if missing_textures:
                errors.append(f"{module}: missing entity textures: {', '.join(missing_textures)}")

            # The native renderer resolves coat/sex/state variants directly
            # from the preserved 1.12 texture tree. Verify every source PNG
            # byte-for-byte so a same-named generic alias cannot hide a lost
            # horse, hamster, sheep, frog, rabbit, cat or dog variant.
            pinned_entity_dir = ((legacy_root / "src" / "main" / "resources" / "assets" / module /
                                  "animania" / "textures" / "entity") if legacy_root is not None else None)
            preserved_entity_dir = (resource_root / "assets" / module / "animania" /
                                    "textures" / "entity")
            pinned_pngs = {item.relative_to(pinned_entity_dir).as_posix(): item
                           for item in pinned_entity_dir.rglob("*.png")} if pinned_entity_dir is not None and pinned_entity_dir.exists() else {}
            preserved_pngs = {item.relative_to(preserved_entity_dir).as_posix(): item
                              for item in preserved_entity_dir.rglob("*.png")} if preserved_entity_dir.exists() else {}
            module_report["legacy_entity_textures"] = len(preserved_pngs)
            if pinned_entity_dir is not None:
                if not pinned_entity_dir.exists():
                    errors.append(f"{module}: missing legacy entity texture source tree {pinned_entity_dir}")
                else:
                    for relative, source in sorted(pinned_pngs.items()):
                        target = preserved_pngs.get(relative)
                        if target is None:
                            errors.append(f"{module}: missing preserved legacy entity texture {relative}")
                        elif source.read_bytes() != target.read_bytes():
                            errors.append(f"{module}: modified preserved legacy entity texture {relative}")
                    unexpected_preserved = sorted(set(preserved_pngs) - set(pinned_pngs))
                    if unexpected_preserved:
                        errors.append(f"{module}: unexpected files in preserved legacy entity texture tree: {', '.join(unexpected_preserved)}")

            content_items = _java_list(args.root, CONTENT_SOURCES[module], "ITEM_IDS")
            content_blocks = _java_list(args.root, CONTENT_SOURCES[module], "BLOCK_IDS")
            egg_items = {"entity_egg_" + entity for entity in expected
                         if entity not in {"cart", "wagon", "tiller"}}
            expected_item_models = content_items | content_blocks | egg_items
            model_dir = resource_root / "assets" / namespace / "models" / "item"
            actual_item_models = {item.stem for item in model_dir.glob("*.json")}
            missing_models = sorted(expected_item_models - actual_item_models)
            if missing_models:
                errors.append(f"{module}: missing registered item models: {', '.join(missing_models)}")

            # Concrete animal eggs use the original two tint layers and sex
            # overlay. Random-family eggs are the only eggs allowed to use a
            # family-specific single-layer icon. This catches the placeholder
            # regression that made every breed look like the same random egg.
            male_prefixes = ("bull_", "rooster_", "hog_", "buck_", "ram_", "stallion_", "peacock_", "male_", "tom_")
            female_prefixes = ("cow_", "hen_", "sow_", "doe_", "ewe_", "mare_", "peahen_", "female_", "queen_")
            for entity_id in sorted(expected - {"cart", "wagon", "tiller"}):
                egg_model_path = model_dir / f"entity_egg_{entity_id}.json"
                if not egg_model_path.exists():
                    continue
                try:
                    egg_data = json.loads(egg_model_path.read_text(encoding="utf-8"))
                except (OSError, json.JSONDecodeError):
                    continue
                egg_textures = egg_data.get("textures", {})
                if egg_textures.get("layer0") != "animania:item/egg_layer_1":
                    errors.append(f"{module}: concrete egg {entity_id} does not use legacy primary tint layer")
                if egg_textures.get("layer1") != "animania:item/egg_layer_2":
                    errors.append(f"{module}: concrete egg {entity_id} does not use legacy secondary tint layer")
                expected_overlay = "animania:item/egg_layer_male" if entity_id.startswith(male_prefixes) else (
                    "animania:item/egg_layer_female" if entity_id.startswith(female_prefixes) else None)
                if egg_textures.get("layer2") != expected_overlay:
                    errors.append(f"{module}: concrete egg {entity_id} has wrong legacy sex overlay")

            special_dart = model_dir / "entity_egg_dart_frog.json"
            if special_dart.exists():
                special_data = json.loads(special_dart.read_text(encoding="utf-8"))
                dart_textures = special_data.get("textures", {})
                if (dart_textures.get("layer0") != "animania:item/egg_layer_1"
                        or dart_textures.get("layer1") != "animania:item/egg_layer_2"):
                    errors.append(f"{module}: dart-frog egg does not use the modern tinted egg layers")

            generated_name = ("CatsDogs" if module == "catsdogs" else module.title()) + "LegacyModelLayers.java"
            generated = args.root / module / "src/main/java/com/animania" / module / "client/model" / generated_name
            generated_text = generated.read_text(encoding="utf-8") if generated.exists() else ""
            layer_ids = set(re.findall(r'LAYERS\.put\("([a-z0-9_]+)"', generated_text))
            missing_layers = sorted(expected - {"cart", "wagon", "tiller"} - layer_ids)
            if missing_layers:
                errors.append(f"{module}: missing breed-specific native model layers: {', '.join(missing_layers)}")

        locale_dir = resource_root / "assets" / namespace / "lang"
        locales = sorted(item.stem for item in locale_dir.glob("*.json")) if locale_dir.exists() else []
        module_report["locales"] = len(locales)
        if len(locales) != 25:
            errors.append(f"{module}: expected 25 locale JSON files, found {len(locales)}")
        if module != "base":
            mods_toml = resource_root / "META-INF" / "mods.toml"
            text = mods_toml.read_text(encoding="utf-8") if mods_toml.exists() else ""
            processed = resource_root.parent.parent.parent / "build" / "resources" / "main" / "META-INF" / "mods.toml"
            processed_text = processed.read_text(encoding="utf-8") if processed.exists() else text
            if "${base_dependency}" not in text and not re.search(r'modId="animania"\s*\nmandatory=true', processed_text):
                errors.append(f"{module}: missing mandatory Base dependency")
        report["modules"][module] = module_report

        for raw, cooked in LEGACY_COOKING_PAIRS.get(module, {}).items():
            recipe = resource_root / "data" / namespace / "recipes" / f"{raw}_smelting.json"
            if not recipe.exists():
                errors.append(f"{module}: missing legacy cooking recipe {raw} -> {cooked}")
                continue
            try:
                payload = json.loads(recipe.read_text(encoding="utf-8"))
            except (OSError, json.JSONDecodeError):
                continue
            if payload.get("ingredient") != {"item": f"{namespace}:{raw}"} or payload.get("result") != f"{namespace}:{cooked}":
                errors.append(f"{module}: incorrect legacy cooking recipe {raw} -> {cooked}")
            if payload.get("experience") != 0.3 or payload.get("cookingtime") != 200:
                errors.append(f"{module}: legacy cooking values changed for {raw} -> {cooked}")

    # The manual is resource-driven and deliberately replaces Patchouli.  A
    # page-count parity check catches an addon whose book JSONs were omitted
    # from the modern source set even when the base screen itself compiles.
    legacy_manual_root = args.root / "upstream" / "Animania-1.12" / "src" / "main" / "resources"
    if legacy_manual_root.exists():
        legacy_manual = sum(1 for path in legacy_manual_root.rglob("*.json")
                            if "manual" in path.relative_to(legacy_manual_root).parts)
        modern_manual = sum(report["modules"][module]["manual_pages"] for module in MODULES)
        if modern_manual != legacy_manual:
            errors.append(f"manual page count mismatch: legacy {legacy_manual}, modern {modern_manual}")
        legacy_sounds = 0
        for path in legacy_manual_root.rglob("sounds.json"):
            try:
                data = json.loads(path.read_text(encoding="utf-8"))
            except (OSError, json.JSONDecodeError):
                continue
            if isinstance(data, dict):
                legacy_sounds += len(data)
        modern_sounds = sum(report["modules"][module]["sounds"] for module in MODULES)
        if modern_sounds != legacy_sounds:
            errors.append(f"sound event count mismatch: legacy {legacy_sounds}, modern {modern_sounds}")

    # Registry-driven item/block asset coverage, including Base and dynamic
    # registrations.  The older Content-list-only check missed creative-tab
    # icons and constructor-generated objects, which can render purple/black
    # despite otherwise valid JSON.
    mapping_path = args.root / "docs" / "id-mapping.json"
    if mapping_path.exists():
        mapping = json.loads(mapping_path.read_text(encoding="utf-8"))
        namespace_module = {namespace: module for module, namespace in MODULES.items()}
        checked: set[tuple[str, str]] = set()
        solid_blocks: set[str] = {"animania_farm:cheese_mold"}
        for entry in mapping.get("entries", []):
            kind = entry.get("kind")
            if kind not in {"item", "block"}: continue
            namespace, content_id = entry["modern_id"].split(":", 1)
            key = (kind, entry["modern_id"])
            if key in checked: continue
            checked.add(key)
            module = namespace_module.get(namespace)
            if module is None:
                errors.append(f"mapping references unknown target namespace {namespace}")
                continue
            asset_root = args.root / module / "src/main/resources/assets" / namespace
            target = asset_root / ("models/item" if kind == "item" else "blockstates") / f"{content_id}.json"
            if not target.exists():
                errors.append(f"{module}: missing mapped {kind} asset {target.relative_to(args.root).as_posix()}")
            if kind == "block" and entry["modern_id"] != "animania:slop":
                solid_blocks.add(entry["modern_id"])
        for modern_id in sorted(solid_blocks):
            namespace, content_id = modern_id.split(":", 1)
            module = namespace_module.get(namespace)
            if module is None: continue
            loot = args.root / module / "src/main/resources/data" / namespace / "loot_tables/blocks" / f"{content_id}.json"
            if not loot.exists():
                errors.append(f"{module}: solid block {modern_id} has no block loot table")

    # Addon-owned gameplay blocks must not be silently duplicated by Base.
    # This keeps Base-only startup honest and prevents two unrelated registry
    # objects from masquerading as optional-addon compatibility.
    base_blocks_source = args.root / "base/src/main/java/com/animania/common/AnimaniaBlocks.java"
    if base_blocks_source.exists():
        base_text = base_blocks_source.read_text(encoding="utf-8")
        for addon_id in ("cheese_mold", "pet_bowl", "hamster_wheel"):
            if re.search(rf'(?:simple|container|BLOCKS\.register)\("{addon_id}"', base_text):
                errors.append(f"base: addon-owned block {addon_id} is registered in Base")

    # Resolve every non-vanilla JSON model parent as well as its texture
    # references. A missing custom parent is another direct missing-model
    # texture even when the child JSON itself parses.
    asset_roots = {namespace: args.root / module / "src/main/resources/assets" / namespace
                   for module, namespace in MODULES.items()}
    for namespace, asset_root in asset_roots.items():
        for model in (asset_root / "models").rglob("*.json") if (asset_root / "models").exists() else []:
            try:
                data = json.loads(model.read_text(encoding="utf-8"))
            except (OSError, json.JSONDecodeError):
                continue
            parent = data.get("parent")
            if not isinstance(parent, str) or ":" not in parent: continue
            if _is_external_model_parent(parent): continue
            parent_namespace, parent_path = parent.split(":", 1)
            parent_root = asset_roots.get(parent_namespace)
            parent_model = parent_root / "models" / f"{parent_path}.json" if parent_root else None
            if parent_model is None or not parent_model.exists():
                errors.append(f"{namespace}: missing model parent {parent} referenced by {model.relative_to(args.root).as_posix()}")

    # CraftStudio clips are intentionally replaced by native 1.20.1
    # ModelPart/LayerDefinition/AnimationDefinition code.  Keep this check
    # independent of the resource walk so a stale source file cannot quietly
    # reintroduce the old runtime dependency.
    model_source = args.root / "base" / "src" / "main" / "java" / "com" / "animania" / "client" / "model"
    animal_model = model_source / "AnimaniaAnimalModel.java"
    animation_source = model_source / "AnimaniaAnimations.java"
    for path in (animal_model, animation_source):
        if not path.exists():
            errors.append(f"base: missing native model source {path.relative_to(args.root).as_posix()}")
    if animal_model.exists():
        text = animal_model.read_text(encoding="utf-8")
        for token in ("ModelPart", "LayerDefinition", "createBodyLayer"):
            if token not in text:
                errors.append(f"base: native animal model missing {token}")
    if animation_source.exists():
        text = animation_source.read_text(encoding="utf-8")
        for token in ("AnimationDefinition", "WALK", "RUN", "SLEEP", "EAT", "DRINK", "PLAY", "BREED", "GRAZE"):
            if token not in text:
                errors.append(f"base: native animation source missing {token}")
    output = args.root / "build" / "resource-audit.json"
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(json.dumps(report, ensure_ascii=False, indent=2))
    if errors:
        raise SystemExit(1)


if __name__ == "__main__":
    main()
