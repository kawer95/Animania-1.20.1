package com.animania.common.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraft.tags.ItemTags;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Common settings retain the old gameplay knobs while using Forge TOML. */
public final class AnimaniaConfig {
    private static final Pattern FOOD_OVERRIDE_PATTERN = Pattern.compile(
            "^\\s*([a-z0-9_.-]+:[a-z0-9_./-]+)\\s*\\(\\s*([+-]?\\d+)\\s*,\\s*([+-]?(?:\\d+(?:\\.\\d*)?|\\.\\d+))\\s*\\)\\s*$",
            Pattern.CASE_INSENSITIVE);
    public static final ForgeConfigSpec COMMON_SPEC;
    public static final ForgeConfigSpec.IntValue HUNGER_INTERVAL;
    public static final ForgeConfigSpec.IntValue THIRST_INTERVAL;
    public static final ForgeConfigSpec.IntValue GESTATION_TICKS;
    public static final ForgeConfigSpec.IntValue CHILD_GROWTH_TICK;
    public static final ForgeConfigSpec.IntValue FEED_TIMER;
    public static final ForgeConfigSpec.IntValue WATER_TIMER;
    public static final ForgeConfigSpec.IntValue PLAY_TIMER;
    public static final ForgeConfigSpec.IntValue LAID_TIMER;
    public static final ForgeConfigSpec.IntValue FEATHER_TIMER;
    public static final ForgeConfigSpec.IntValue WOOL_REGROWTH_TIMER;
    public static final ForgeConfigSpec.IntValue STARVATION_TIMER;
    public static final ForgeConfigSpec.IntValue EGG_HATCH_CHANCE;
    public static final ForgeConfigSpec.IntValue SALT_LICK_TICK;
    public static final ForgeConfigSpec.IntValue SALT_LICK_MAX_USES;
    public static final ForgeConfigSpec.IntValue ENTITY_BREEDING_LIMIT;
    public static final ForgeConfigSpec.DoubleValue BIRTH_MULTIPLE_CHANCE;
    public static final ForgeConfigSpec.DoubleValue ANIMAL_LOSS_CHANCE;
    public static final ForgeConfigSpec.BooleanValue FOODS_GIVE_BONUS_EFFECTS;
    public static final ForgeConfigSpec.BooleanValue SHOW_MOD_UPDATE_NOTIFICATION;
    public static final ForgeConfigSpec.BooleanValue SHOW_PARTS;
    public static final ForgeConfigSpec.BooleanValue SHOW_UNHAPPY_PARTICLES;
    public static final ForgeConfigSpec.BooleanValue ALLOW_SEED_DISPENSER_PLACEMENT;
    public static final ForgeConfigSpec.BooleanValue SHIFT_SEED_PLACEMENT;
    public static final ForgeConfigSpec.BooleanValue ANIMALS_STARVE;
    public static final ForgeConfigSpec.BooleanValue ALLOW_MOB_RIDING;
    public static final ForgeConfigSpec.BooleanValue ALLOW_TROUGH_AUTOMATION;
    public static final ForgeConfigSpec.IntValue TROUGH_SOLID_CAPACITY;
    public static final ForgeConfigSpec.IntValue TROUGH_FLUID_CAPACITY;
    public static final ForgeConfigSpec.DoubleValue FALL_DAMAGE_REDUCE_MULTIPLIER;
    public static final ForgeConfigSpec.BooleanValue WATER_REMOVED_AFTER_DRINKING;
    public static final ForgeConfigSpec.BooleanValue PLANTS_REMOVED_AFTER_EATING;
    public static final ForgeConfigSpec.BooleanValue AMBIANCE_MODE;
    public static final ForgeConfigSpec.BooleanValue ANIMALS_SLEEP;
    public static final ForgeConfigSpec.BooleanValue ANIMALS_CAN_ATTACK;
    public static final ForgeConfigSpec.IntValue AI_TICKS_BETWEEN_FIRINGS;
    public static final ForgeConfigSpec.BooleanValue TAMED_ANIMALS_TELEPORT;
    public static final ForgeConfigSpec.BooleanValue FANCY_EGGS;
    public static final ForgeConfigSpec.BooleanValue FANCY_EGGS_ROTATE;
    public static final ForgeConfigSpec.BooleanValue EAT_FOOD_ANYTIME;
    public static final ForgeConfigSpec.BooleanValue BIRDS_DROP_FEATHERS;
    public static final ForgeConfigSpec.IntValue AI_BLOCK_SEARCH_RANGE;
    public static final ForgeConfigSpec.IntValue ANIMAL_CAP_SEARCH_RANGE;
    public static final ForgeConfigSpec.BooleanValue REQUIRE_ANIMAL_INTERACTION_FOR_AI;
    public static final ForgeConfigSpec.BooleanValue SPAWN_FRESH_WATER_SQUIDS;
    public static final ForgeConfigSpec.BooleanValue FEED_TO_BREED;
    public static final ForgeConfigSpec.BooleanValue MALES_MATE_MULTIPLE_FEMALES;
    public static final ForgeConfigSpec.BooleanValue ENABLE_NATURAL_SPAWNS;
    public static final ForgeConfigSpec.BooleanValue ENABLE_VEHICLES;
    /** Legacy trough allow-list, retained as a modern registry-ID list. */
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> TROUGH_FOOD;
    /** Legacy slop ingredient list, retained for datapack/addon queries. */
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> SLOP_INGREDIENTS;
    /** Optional per-item food overrides in `id(hunger,saturation)` form. */
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> FOOD_VALUE_OVERRIDES;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.comment("Animania shared gameplay settings").push("gameplay");
        HUNGER_INTERVAL = builder.comment("Ticks between hunger updates").defineInRange("hungerInterval", 2400, 20, 120000);
        THIRST_INTERVAL = builder.comment("Ticks between thirst updates").defineInRange("thirstInterval", 1800, 20, 120000);
        GESTATION_TICKS = builder.comment("Default pregnancy duration; matches the 1.12 careAndFeeding.gestationTimer").defineInRange("gestationTicks", 20000, 200, 24000);
        CHILD_GROWTH_TICK = builder.comment("Ticks per one-percent child growth step; the 1.12 lifecycle has 85 steps")
                .defineInRange("childGrowthTick", 200, 20, 120000);
        FEED_TIMER = builder.defineInRange("feedTimer", 12000, 20, 240000);
        WATER_TIMER = builder.defineInRange("waterTimer", 12000, 20, 240000);
        PLAY_TIMER = builder.defineInRange("playTimer", 12000, 20, 240000);
        LAID_TIMER = builder.defineInRange("laidTimer", 2000, 20, 240000);
        FEATHER_TIMER = builder.defineInRange("featherTimer", 12000, 20, 240000);
        WOOL_REGROWTH_TIMER = builder.defineInRange("woolRegrowthTimer", 8000, 20, 240000);
        STARVATION_TIMER = builder.defineInRange("starvationTimer", 400, 20, 240000);
        EGG_HATCH_CHANCE = builder.defineInRange("eggHatchChance", 2, 1, 1000);
        SALT_LICK_TICK = builder.defineInRange("saltLickTick", 8000, 20, 240000);
        SALT_LICK_MAX_USES = builder.defineInRange("saltLickMaxUses", 200, 1, 100000);
        ENTITY_BREEDING_LIMIT = builder.defineInRange("entityBreedingLimit", 15, 1, 1000);
        BIRTH_MULTIPLE_CHANCE = builder.defineInRange("birthMultipleChance", 0.1D, 0.0D, 1.0D);
        ANIMAL_LOSS_CHANCE = builder.defineInRange("animalLossChance", 0.0D, 0.0D, 1.0D);
        FOODS_GIVE_BONUS_EFFECTS = builder.define("foodsGiveBonusEffects", true);
        SHOW_MOD_UPDATE_NOTIFICATION = builder.define("showModUpdateNotification", true);
        SHOW_PARTS = builder.define("showParts", false);
        SHOW_UNHAPPY_PARTICLES = builder.define("showUnhappyParticles", true);
        ALLOW_SEED_DISPENSER_PLACEMENT = builder.define("allowSeedDispenserPlacement", true);
        SHIFT_SEED_PLACEMENT = builder.define("shiftSeedPlacement", false);
        ANIMALS_STARVE = builder.define("animalsStarve", false);
        ALLOW_MOB_RIDING = builder.define("allowMobRiding", true);
        ALLOW_TROUGH_AUTOMATION = builder.define("allowTroughAutomation", true);
        TROUGH_SOLID_CAPACITY = builder.comment("Maximum solid food portions held by one trough")
                .defineInRange("troughSolidCapacity", 64, 3, 64);
        TROUGH_FLUID_CAPACITY = builder.comment("Maximum liquid held by one trough, in millibuckets")
                .defineInRange("troughFluidCapacity", 3000, 1000, 3000);
        FALL_DAMAGE_REDUCE_MULTIPLIER = builder.defineInRange("fallDamageReduceMultiplier", 0.45D, 0.0D, 1.0D);
        WATER_REMOVED_AFTER_DRINKING = builder.define("waterRemovedAfterDrinking", true);
        PLANTS_REMOVED_AFTER_EATING = builder.define("plantsRemovedAfterEating", true);
        AMBIANCE_MODE = builder.define("ambianceMode", false);
        ANIMALS_SLEEP = builder.define("animalsSleep", true);
        ANIMALS_CAN_ATTACK = builder.define("animalsCanAttackOthers", true);
        AI_TICKS_BETWEEN_FIRINGS = builder.defineInRange("ticksBetweenAIFirings", 100, 1, 120000);
        TAMED_ANIMALS_TELEPORT = builder.define("tamedAnimalsTeleport", true);
        FANCY_EGGS = builder.define("fancyEggs", false);
        FANCY_EGGS_ROTATE = builder.define("fancyEggsRotate", false);
        EAT_FOOD_ANYTIME = builder.define("eatFoodAnytime", true);
        BIRDS_DROP_FEATHERS = builder.define("birdsDropFeathers", true);
        AI_BLOCK_SEARCH_RANGE = builder.defineInRange("aiBlockSearchRange", 16, 1, 256);
        ANIMAL_CAP_SEARCH_RANGE = builder.defineInRange("animalCapSearchRange", 80, 1, 512);
        REQUIRE_ANIMAL_INTERACTION_FOR_AI = builder.define("requireAnimalInteractionForAI", true);
        SPAWN_FRESH_WATER_SQUIDS = builder.define("spawnFreshWaterSquids", true);
        FEED_TO_BREED = builder.define("feedToBreed", true);
        MALES_MATE_MULTIPLE_FEMALES = builder.define("malesMateMultipleFemales", false);
        ENABLE_NATURAL_SPAWNS = builder.define("enableNaturalSpawns", true);
        ENABLE_VEHICLES = builder.define("enableVehicles", true);
        TROUGH_FOOD = builder.defineList("troughFood", List.of(
                "minecraft:wheat", "simplecorn:corncob", "harvestcraft:barleyitem", "harvestcraft:oatsitem",
                "harvestcraft:ryeitem", "harvestcraft:cornitem", "minecraft:apple", "minecraft:carrot", "minecraft:beetroot",
                "minecraft:potato", "minecraft:poisonous_potato", "minecraft:wheat_seeds",
                "minecraft:melon_seeds", "minecraft:beetroot_seeds", "minecraft:pumpkin_seeds",
                "biomesoplenty:turnip_seeds", "minecraft:egg", "animania_farm:brown_egg", "listAllbeefraw", "minecraft:fish"),
                value -> value instanceof String);
        SLOP_INGREDIENTS = builder.defineList("slopIngredients", List.of(
                "minecraft:carrot", "minecraft:beetroot", "minecraft:potato",
                "minecraft:poisonous_potato", "minecraft:bread"), value -> value instanceof String);
        FOOD_VALUE_OVERRIDES = builder.defineList("foodValueOverrides", List.of(), value -> value instanceof String);
        builder.pop();
        COMMON_SPEC = builder.build();
    }

    private AnimaniaConfig() {
    }

    /** Match the modern registry IDs and the two common 1.12 OreDictionary aliases. */
    public static boolean matchesTroughFood(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        List<? extends String> configured;
        try {
            configured = TROUGH_FOOD.get();
        } catch (IllegalStateException ignored) {
            return false;
        }
        var id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        String registryId = id == null ? "" : id.toString();
        for (String raw : configured) {
            if (raw == null) continue;
            String value = raw.trim().toLowerCase(java.util.Locale.ROOT);
            if (value.equals(registryId)) return true;
            if (value.equals("minecraft:fish") && stack.is(ItemTags.FISHES)) return true;
            if ((value.equals("listallbeefraw") || value.equals("listallbeef"))
                    && (stack.is(net.minecraft.world.item.Items.BEEF)
                    || stack.is(net.minecraft.world.item.Items.COOKED_BEEF))) return true;
        }
        return false;
    }

    /** Match the live slop allow-list instead of freezing its defaults into recipe JSON. */
    public static boolean matchesSlopIngredient(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        List<? extends String> configured;
        try {
            configured = SLOP_INGREDIENTS.get();
        } catch (IllegalStateException ignored) {
            configured = List.of("minecraft:carrot", "minecraft:beetroot", "minecraft:potato",
                    "minecraft:poisonous_potato", "minecraft:bread");
        }
        var id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (id == null) return false;
        String registryId = id.toString();
        return configured.stream().filter(java.util.Objects::nonNull)
                .map(value -> value.trim().toLowerCase(java.util.Locale.ROOT))
                .anyMatch(registryId::equals);
    }

    /** Parsed form of the legacy {@code namespace:item(nutrition,saturation)} syntax. */
    public record FoodValueOverride(ResourceLocation itemId, int nutrition, float saturationModifier) { }

    public static Optional<FoodValueOverride> parseFoodValueOverride(String raw) {
        if (raw == null) return Optional.empty();
        Matcher match = FOOD_OVERRIDE_PATTERN.matcher(raw);
        if (!match.matches()) return Optional.empty();
        ResourceLocation id = ResourceLocation.tryParse(match.group(1).toLowerCase(java.util.Locale.ROOT));
        if (id == null) return Optional.empty();
        try {
            int nutrition = Integer.parseInt(match.group(2));
            float saturation = Float.parseFloat(match.group(3));
            if (nutrition < 0 || saturation < 0.0F || !Float.isFinite(saturation)) return Optional.empty();
            return Optional.of(new FoodValueOverride(id, nutrition, saturation));
        } catch (NumberFormatException ignored) {
            return Optional.empty();
        }
    }

    /** Resolve on every consumption so a reloaded common config takes effect without rebuilding registries. */
    public static Optional<FoodValueOverride> foodValueOverride(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !stack.getItem().isEdible()) return Optional.empty();
        ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (itemId == null) return Optional.empty();
        List<? extends String> configured;
        try { configured = FOOD_VALUE_OVERRIDES.get(); }
        catch (RuntimeException ignored) { return Optional.empty(); }
        return configured.stream().map(AnimaniaConfig::parseFoodValueOverride)
                .flatMap(Optional::stream).filter(value -> value.itemId().equals(itemId)).findFirst();
    }

    public static boolean foodsGiveBonusEffects() {
        try { return FOODS_GIVE_BONUS_EFFECTS.get(); }
        catch (RuntimeException ignored) { return true; }
    }

    public static boolean eatFoodAnytime() {
        try { return EAT_FOOD_ANYTIME.get(); }
        catch (RuntimeException ignored) { return true; }
    }

    public static int troughSolidCapacity() {
        try { return TROUGH_SOLID_CAPACITY.get(); }
        catch (RuntimeException ignored) { return 64; }
    }

    public static int troughFluidCapacity() {
        try { return TROUGH_FLUID_CAPACITY.get(); }
        catch (RuntimeException ignored) { return 3000; }
    }
}
