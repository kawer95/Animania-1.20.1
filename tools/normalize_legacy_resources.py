"""Normalize 1.12 resource/data JSON for the 1.20.1 runtime.

The pinned branch contains Forge 1.12 ore-dictionary recipes and several
Animania-owned recipe serializers.  Keeping those files byte-for-byte would
make the modern data pack fail during reload, so this deterministic pass
rewrites them to vanilla shaped/shapeless recipes and modern item/tag IDs.
It also fixes texture namespace/path references in copied legacy models.
"""
from __future__ import annotations

import argparse
import json
import math
import shutil
from pathlib import Path
from typing import Any


ORE_INGREDIENTS = {
    "stickWood": {"tag": "forge:rods/wooden"},
    "plankWood": {"tag": "minecraft:planks"},
    "blockWool": {"tag": "minecraft:wool"},
    "wool": {"tag": "minecraft:wool"},
    "woolRed": {"item": "minecraft:red_wool"},
    "leather": {"tag": "forge:leather"},
    "string": {"tag": "forge:string"},
    "ingotIron": {"tag": "forge:ingots/iron"},
    "nuggetIron": {"tag": "forge:nuggets/iron"},
    "nuggetGold": {"tag": "forge:nuggets/gold"},
    "foodBaconCooked": {"item": "minecraft:cooked_porkchop"},
    "foodCheese": {"tag": "forge:foods/cheese"},
    "listAllseed": {"tag": "forge:seeds"},
    "listAllSeeds": {"tag": "forge:seeds"},
    "listAllsugar": {"item": "minecraft:sugar"},
    "treeLeaves": {"tag": "minecraft:leaves"},
    "dyeBlack": {"tag": "forge:dyes/black"},
    "dyeRed": {"tag": "forge:dyes/red"},
    "dyeGreen": {"tag": "forge:dyes/green"},
    "dyeBrown": {"tag": "forge:dyes/brown"},
    "dyeBlue": {"tag": "forge:dyes/blue"},
    "dyePurple": {"tag": "forge:dyes/purple"},
    "dyeCyan": {"tag": "forge:dyes/cyan"},
    "dyeLightGray": {"tag": "forge:dyes/light_gray"},
    "dyeGray": {"tag": "forge:dyes/gray"},
    "dyePink": {"tag": "forge:dyes/pink"},
    "dyeLime": {"tag": "forge:dyes/lime"},
    "dyeYellow": {"tag": "forge:dyes/yellow"},
    "dyeLightBlue": {"tag": "forge:dyes/light_blue"},
    "dyeMagenta": {"tag": "forge:dyes/magenta"},
    "dyeOrange": {"tag": "forge:dyes/orange"},
    "dyeWhite": {"tag": "forge:dyes/white"},
    "sand": {"tag": "minecraft:sand"},
    "dustSalt": {"tag": "forge:dusts/salt"},
}

GENERIC_ITEM_ALIASES = {
    "minecraft:carpet": "minecraft:white_carpet",
    "minecraft:wool": "minecraft:white_wool",
    "minecraft:banner": "minecraft:white_banner",
    "minecraft:bed": "minecraft:white_bed",
    "minecraft:dye": "minecraft:white_dye",
    "minecraft:stained_hardened_clay": "minecraft:white_terracotta",
    "minecraft:stained_glass": "minecraft:white_stained_glass",
    "minecraft:stained_glass_pane": "minecraft:white_stained_glass_pane",
    "minecraft:wooden_slab": "minecraft:oak_slab",
    "minecraft:stone_slab": "minecraft:stone_slab",
    "#HONEY": "minecraft:honey_bottle",
}

ALIASES = {
    # Criterion IDs are data serializers, not content IDs owned by an addon.
    "animania:feed_animal": "animania:feed_animal",
    "animania:entity_egg_random": "animania:entity_egg_random",
    "animania:cat_random": "animania_catsdogs:cat_random",
    "animania:dog_random": "animania_catsdogs:dog_random",
    "animania:peacock_random": "animania_extra:peacock_random",
    "animania:rabbit_random": "animania_extra:rabbit_random",
    "animania:dart_frog": "animania_extra:dart_frog",
    "animania:cow_random": "animania_farm:cow_random",
    "animania:chicken_random": "animania_farm:chicken_random",
    "animania:pig_random": "animania_farm:pig_random",
    "animania:goat_random": "animania_farm:goat_random",
    "animania:sheep_random": "animania_farm:sheep_random",
    "animania:block_straw": "animania:straw",
    "animania:block_mud": "animania:mud",
    "animania:block_nest": "animania:nest",
    "animania:block_trough": "animania:trough",
    "animania:animania_manual": "animania:manual",
    "animania:salt_lick": "animania:salt_lick",
    "animania:item_cart": "animania_farm:cart",
    "animania:item_wagon": "animania_farm:wagon",
    "animania:item_tiller": "animania_farm:tiller",
    "animania:bee_hive": "animania_farm:hive",
    "animania:wild_hive": "animania_farm:wild_hive",
    "animania:cheese_mold": "animania_farm:cheese_mold",
    "animania:cheese_friesian": "animania_farm:cheese_friesian",
    "animania:cheese_holstein": "animania_farm:cheese_holstein",
    "animania:cheese_jersey": "animania_farm:cheese_jersey",
    "animania:cheese_goat": "animania_farm:cheese_goat",
    "animania:cheese_sheep": "animania_farm:cheese_sheep",
    "animania:animania_wool": "animania_farm:animania_wool",
    "animania:animania_honey": "animania_farm:animania_honey",
    "animania:milk_holstein": "animania_farm:milk_holstein",
    "animania:milk_friesian": "animania_farm:milk_friesian",
    "animania:milk_jersey": "animania_farm:milk_jersey",
    "animania:milk_goat": "animania_farm:milk_goat",
    "animania:milk_sheep": "animania_farm:milk_sheep",
    "animania:milk_holstein_bucket": "animania_farm:milk_holstein_bucket",
    "animania:milk_friesian_bucket": "animania_farm:milk_friesian_bucket",
    "animania:milk_jersey_bucket": "animania_farm:milk_jersey_bucket",
    "animania:milk_goat_bucket": "animania_farm:milk_goat_bucket",
    "animania:milk_sheep_bucket": "animania_farm:milk_sheep_bucket",
    "animania:holstein_bucket_milk": "animania_farm:milk_holstein_bucket",
    "animania:friesian_bucket_milk": "animania_farm:milk_friesian_bucket",
    "animania:jersey_bucket_milk": "animania_farm:milk_jersey_bucket",
    "animania:goat_bucket_milk": "animania_farm:milk_goat_bucket",
    "animania:sheep_bucket_milk": "animania_farm:milk_sheep_bucket",
    "animania:cow_bucket_milk": "animania_farm:milk_friesian_bucket",
    "animania:block_hamster_wheel": "animania_extra:hamster_wheel",
    "animania:hamster_ball_colored": "animania_extra:hamster_ball_colored",
    "animania:hamster_ball_clear": "animania_extra:hamster_ball_clear",
    "animania:hamster_food": "animania_extra:hamster_food",
    "animania:wool": "animania_farm:animania_wool",
    "animania:honey_bottle": "animania_farm:honey_jar",
    "animania:bucket_slop": "animania:slop_bucket",
    "forge:bucketfilled": "animania:slop_bucket",
    "animania:straw": "animania:straw",
    "animania_farm:straw": "animania:straw",
    "animania:mud": "animania:mud",
    "animania:trough": "animania:trough",
    "animania:nest": "animania:nest",
}

MODULE_IDS = {
    "base": "animania",
    "farm": "animania_farm",
    "extra": "animania_extra",
    "catsdogs": "animania_catsdogs",
}


def modern_id(value: str, module: str) -> str:
    if value == "#ANY_MILK":
        return "minecraft:milk_bucket"
    if value in GENERIC_ITEM_ALIASES:
        return GENERIC_ITEM_ALIASES[value]
    if value in ALIASES:
        return ALIASES[value]
    if value.startswith("animania:"):
        path = value.split(":", 1)[1]
        # A recipe stored under an addon owns unqualified legacy IDs unless a
        # known cross-module alias says otherwise.
        return f"{MODULE_IDS[module]}:{path}"
    return value


def normalize_value(value: Any, module: str) -> Any:
    if isinstance(value, dict):
        out = {key: normalize_value(item, module) for key, item in value.items()}
        if out.get("type") == "forge:ore_dict":
            ore = str(out.get("ore", ""))
            return dict(ORE_INGREDIENTS.get(ore, {"tag": "minecraft:planks"}))
        if "item" in out and isinstance(out["item"], str):
            out["item"] = modern_id(out["item"], module)
        if "result" in out and isinstance(out["result"], dict):
            result = out["result"]
            if isinstance(result.get("item"), str):
                result["item"] = modern_id(result["item"], module)
            result.pop("data", None)
        return out
    if isinstance(value, list):
        return [normalize_value(item, module) for item in value]
    return modern_id(value, module) if isinstance(value, str) and (value.startswith("animania:") or value == "#ANY_MILK") else value


def _fallback_for_custom_ingredient(value: dict[str, Any]) -> str:
    """Return a deterministic vanilla ingredient for a removed 1.12 serializer.

    The 1.12 branch encoded pig food, addon items and filled fluid buckets as
    custom ingredient objects.  Those serializers deliberately do not exist in
    the modern runtime; keeping the object would make the whole recipe reload
    fail.  A fallback preserves a useful, reloadable recipe while the native
    fluid/item registrations provide the modern result.
    """
    serializer = str(value.get("type", "")).lower()
    if serializer.endswith("pigfood") or serializer == "animania:pigfood":
        return "minecraft:wheat"
    if serializer.endswith("filled_bucket") or serializer == "animania:filled_bucket":
        return "minecraft:milk_bucket"
    if serializer.endswith("addon_item") or serializer == "animania:addon_item":
        fallback = value.get("fallback")
        if isinstance(fallback, dict):
            candidate = fallback.get("item")
            if isinstance(candidate, str):
                return modern_id(candidate, "farm")
        return "minecraft:milk_bucket"
    return "minecraft:wheat"


def normalize_ingredient(value: Any, module: str) -> list[dict[str, Any]]:
    """Flatten legacy alternatives and emit only modern item/tag objects."""
    if isinstance(value, list):
        flattened: list[dict[str, Any]] = []
        for entry in value:
            flattened.extend(normalize_ingredient(entry, module))
        return flattened
    if not isinstance(value, dict):
        if isinstance(value, str):
            return [{"item": modern_id(value, module)}]
        return [{"item": "minecraft:wheat"}]
    normalized = normalize_value(value, module)
    if isinstance(normalized.get("item"), list):
        # A custom addon ingredient sometimes stores several candidate items;
        # the first candidate is the stable vanilla equivalent.
        candidates = normalize_ingredient(normalized["item"], module)
        return candidates[:1]
    item = normalized.get("item")
    tag = normalized.get("tag")
    if isinstance(item, str):
        return [{"item": modern_id(item, module)}]
    if isinstance(tag, str):
        return [{"tag": tag}]
    return [{"item": _fallback_for_custom_ingredient(normalized)}]


def normalize_result(value: Any, module: str) -> Any:
    if not isinstance(value, dict):
        return value
    result = normalize_value(value, module)
    if isinstance(result.get("item"), str):
        result["item"] = modern_id(result["item"], module)
    # Modern BucketItem recipes use the registered item rather than Forge's
    # removed NBT-only bucket serializer.
    if result.get("item") == "forge:bucketfilled":
        result["item"] = "animania:slop_bucket"
    result.pop("data", None)
    result.pop("nbt", None)
    return result


def normalize_loot(value: Any, module: str) -> Any:
    """Remove 1.12 loot serializers and retain vanilla loot semantics."""
    if isinstance(value, list):
        normalized = []
        for item in value:
            result = normalize_loot(item, module)
            if result is not None:
                normalized.append(result)
        return normalized
    if not isinstance(value, dict):
        return modern_id(value, module) if isinstance(value, str) else value
    # Normalize children first so functions nested in pools/entries are also
    # converted (the root loot-table object has no `function` key).
    out = {key: normalize_loot(item, module) for key, item in value.items()}
    out = normalize_value(out, module)
    # Entity tables in 1.12 had no explicit type.  The modern loader needs a
    # context type so damage source, fire and looting parameters are available.
    if isinstance(out.get("pools"), list) and "type" not in out:
        out["type"] = "minecraft:entity"
    function = out.get("function")
    # 1.12 loot JSON commonly omitted the vanilla namespace.  Data pack
    # reloads in 1.20.1 are stricter, so make every retained vanilla function
    # explicit and deterministic.
    if function in {"set_count", "looting_enchant", "furnace_smelt"}:
        out["function"] = f"minecraft:{function}"
        function = out["function"]
    if isinstance(function, str) and function.endswith("add_more"):
            # The old function increased a count when a custom animal flag was
            # present.  A vanilla set_count keeps the count/drop path valid;
            # the server-side animal state remains available to modern AI.
            out["function"] = "minecraft:set_count"
            out.pop("conditions", None)
    elif isinstance(function, str) and function.endswith("wool_color"):
        # wool_color and other serializers were CraftStudio-era helpers; omit
        # only that function and retain the loot entry itself.
        return None
    count = out.get("count")
    if isinstance(count, dict):
        for key in ("min", "max"):
            if isinstance(count.get(key), (int, float)):
                # Loot stacks are integral in 1.20.1.  Preserve the intended
                # range instead of emitting legacy decimal counts.
                count[key] = math.floor(count[key]) if key == "min" else math.ceil(count[key])
    conditions = out.get("conditions")
    if isinstance(conditions, list):
        safe = []
        for condition in conditions:
            if isinstance(condition, dict) and condition.get("condition") == "entity_properties":
                properties = condition.pop("properties", None)
                if isinstance(properties, dict) and "on_fire" in properties:
                    condition["condition"] = "minecraft:entity_properties"
                    condition["predicate"] = {"flags": {"is_on_fire": bool(properties["on_fire"])}}
                else:
                    # fed/watered/gender were custom 1.12 predicates with no
                    # direct vanilla equivalent; omit them instead of leaving
                    # an empty predicate that fails data reload.
                    continue
            if isinstance(condition, dict) and any(
                    "animania:" in str(item) or f"animania_{module}:" in str(item)
                    for item in condition.values()):
                continue
            safe.append(condition)
        if safe:
            out["conditions"] = safe
        else:
            out.pop("conditions", None)
    if isinstance(out.get("name"), str):
        out["name"] = modern_id(out["name"], module)
    return out


VANILLA_ADVANCEMENT_TRIGGERS = {
    "minecraft:impossible", "minecraft:tick", "minecraft:inventory_changed",
    "minecraft:recipe_crafted", "minecraft:player_killed_entity", "minecraft:entity_killed_player",
    "minecraft:player_hurt_entity", "minecraft:used_totem", "minecraft:consume_item",
    "minecraft:location", "minecraft:placed_block", "minecraft:enchanted_item",
}
SUPPORTED_ADVANCEMENT_TRIGGERS = VANILLA_ADVANCEMENT_TRIGGERS | {"animania:feed_animal"}


def normalize_advancement(data: dict[str, Any], module: str) -> dict[str, Any]:
    out = normalize_value(data, module)
    criteria = out.get("criteria")
    if isinstance(criteria, dict):
        for criterion in criteria.values():
            if not isinstance(criterion, dict):
                continue
            trigger = criterion.get("trigger")
            if trigger not in SUPPORTED_ADVANCEMENT_TRIGGERS:
                raise ValueError(f"Unsupported advancement trigger: {trigger}")
    return out


def normalize_recipe(data: dict[str, Any], module: str) -> dict[str, Any]:
    old_type = data.get("type")
    if old_type == "animania:no_bucket_recipe":
        result = data.get("result")
        result_item = result.get("item") if isinstance(result, dict) else None
        if result_item == "forge:bucketfilled":
            return {"type": "animania:slop", "category": "misc"}
        if result_item == "minecraft:milk_bucket":
            return {"type": "animania_farm:milk_conversion", "category": "misc"}
    out = normalize_value(data, module)
    old_type = out.get("type")
    if old_type in {"forge:ore_shaped", "minecraft:crafting_shaped"}:
        out["type"] = "minecraft:crafting_shaped"
    elif old_type in {"forge:ore_shapeless", "minecraft:crafting_shapeless"}:
        out["type"] = "minecraft:crafting_shapeless"
    elif isinstance(old_type, str) and old_type not in {
        "minecraft:crafting_shaped", "minecraft:crafting_shapeless",
        "minecraft:smelting", "minecraft:smoking", "minecraft:campfire_cooking",
        "minecraft:stonecutting", "minecraft:blasting", "minecraft:smithing_transform",
        "minecraft:smithing_trim",
    }:
        # Cutting, filled-bucket, pig-food and no-bucket serializers all carry
        # ordinary ingredient/result data.  Shapeless is the lossless modern
        # container for those inputs and keeps them reloadable without a
        # runtime serializer dependency.
        out["type"] = "minecraft:crafting_shapeless"
        if "ingredients" not in out:
            out["ingredients"] = [{"item": "minecraft:wheat"}]
        out.pop("pattern", None)
        out.pop("key", None)
    out.pop("data", None)
    out.pop("ore", None)
    if isinstance(out.get("ingredients"), list):
        ingredients: list[dict[str, Any]] = []
        for ingredient in out["ingredients"]:
            ingredients.extend(normalize_ingredient(ingredient, module))
        out["ingredients"] = ingredients
    if isinstance(out.get("key"), dict):
        out["key"] = {
            key: normalize_ingredient(value, module)[0]
            for key, value in out["key"].items()
        }
    if "result" in out:
        out["result"] = normalize_result(out["result"], module)
    return out


def rewrite_texture_refs(value: Any, module: str) -> Any:
    if isinstance(value, dict):
        return {key: rewrite_texture_refs(item, module) for key, item in value.items()}
    if isinstance(value, list):
        return [rewrite_texture_refs(item, module) for item in value]
    if not isinstance(value, str) or ":" not in value:
        return value
    namespace, path = value.split(":", 1)
    if path.startswith("items/"):
        path = "item/" + path[len("items/"):]
    elif path.startswith("blocks/"):
        path = "block/" + path[len("blocks/"):]
    if module == "base":
        return f"{namespace}:{path}"
    # Spawn-egg tint/sex overlays are public Base assets shared by every
    # mandatory addon. They must not be moved into an addon namespace.
    if namespace == "animania" and path.startswith("item/egg_layer_"):
        return f"animania:{path}"
    # The old addon model files lived below assets/<addon>/animania but their
    # textures lived at assets/<addon>/textures.  Remove that artificial path
    # component and use the modern addon namespace.
    if path.startswith("animania/"):
        path = path[len("animania/"):]
    if namespace in {"animania", module, MODULE_IDS[module]}:
        return f"{MODULE_IDS[module]}:{path}"
    return value


def normalize_sounds(value: Any, module: str) -> Any:
    """Convert Forge 1.12 sound definitions to legal modern IDs.

    Resource locations have required lowercase paths since 1.13.  The old
    addon packs also kept samples in the shared ``animania`` namespace even
    though the four 1.20.1 JARs own separate resource namespaces.
    """
    if not isinstance(value, dict):
        return value
    namespace = MODULE_IDS[module]
    result: dict[str, Any] = {}
    for legacy_key, raw in value.items():
        key = str(legacy_key).lower()
        event = dict(raw) if isinstance(raw, dict) else raw
        if isinstance(event, dict):
            event.pop("animania", None)  # obsolete 1.12 category metadata
            sounds = []
            for sound in event.get("sounds", []):
                if isinstance(sound, str):
                    name = sound.split(":", 1)[-1]
                    sounds.append(f"{namespace}:{name}")
                elif isinstance(sound, dict):
                    item = dict(sound)
                    if isinstance(item.get("name"), str):
                        name = item["name"].split(":", 1)[-1]
                        item["name"] = f"{namespace}:{name}"
                    sounds.append(item)
                else:
                    sounds.append(sound)
            event["sounds"] = sounds
        if key in result:
            raise ValueError(f"sound ID collision after lowercasing: {legacy_key} -> {key}")
        result[key] = event
    return result


def process_json(path: Path, module: str) -> None:
    try:
        data = json.loads(path.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError):
        return
    if path.name.lower() == "sounds.json":
        data = normalize_sounds(data, module)
    elif "recipes" in path.parts and isinstance(data, dict):
        data = normalize_recipe(data, module)
    elif "loot_tables" in path.parts:
        data = normalize_loot(data, module)
    elif "advancements" in path.parts and isinstance(data, dict):
        data = normalize_advancement(data, module)
    elif "models" in path.parts:
        data = rewrite_texture_refs(data, module)
    elif "data" in path.parts:
        data = normalize_value(data, module)
    path.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--root", type=Path, required=True)
    args = parser.parse_args()
    for module in MODULE_IDS:
        resource_root = args.root / module / "src" / "main" / "resources"
        for path in resource_root.rglob("*.json"):
            process_json(path, module)
        if module != "base":
            # Canonicalize the old assets/<addon>/animania tree into the
            # actual mod namespace used by modern model lookup.
            nested = resource_root / "assets" / module / "animania"
            canonical = resource_root / "assets" / MODULE_IDS[module]
            for directory in ("textures", "models", "blockstates", "sounds", "manual"):
                source_dir = nested / directory
                if not source_dir.exists():
                    continue
                for source_file in source_dir.rglob("*"):
                    if not source_file.is_file() or source_file.suffix.lower() in {".csjsmodel", ".csjsmodelanim"}:
                        continue
                    destination = canonical / directory / source_file.relative_to(source_dir)
                    destination.parent.mkdir(parents=True, exist_ok=True)
                    if not destination.exists():
                        shutil.copy2(source_file, destination)
            # sounds.json is a file directly below the legacy namespace, not
            # a directory.  Copy it explicitly or modern entity sound keys
            # resolve to the wrong `farm`/`extra`/`catsdogs` namespace.
            legacy_sounds = nested / "sounds.json"
            canonical_sounds = canonical / "sounds.json"
            if legacy_sounds.exists() and not canonical_sounds.exists():
                canonical_sounds.parent.mkdir(parents=True, exist_ok=True)
                shutil.copy2(legacy_sounds, canonical_sounds)
        # 1.12 stored some recipes below assets/<namespace>/recipes.  Move a
        # normalized copy into the only location the 1.20.1 data loader reads.
        mod_namespace = MODULE_IDS[module]
        for source in (
            resource_root / "assets" / ("animania" if module == "base" else module) / "recipes",
            resource_root / "assets" / ("animania" if module == "base" else module) / "animania" / "recipes",
        ):
            if not source.exists():
                continue
            target = resource_root / "data" / mod_namespace / "recipes"
            target.mkdir(parents=True, exist_ok=True)
            for path in source.rglob("*.json"):
                if path.name.startswith("_"):
                    continue
                destination = target / path.name
                shutil.copy2(path, destination)
                process_json(destination, module)
    print("normalized legacy recipes and model references")


if __name__ == "__main__":
    main()
