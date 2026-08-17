package com.animania.catsdogs;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class CatsDogsConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue ENABLE_SPAWNS;
    public static final ForgeConfigSpec.IntValue SPAWN_LIMIT_CATS;
    public static final ForgeConfigSpec.IntValue SPAWN_LIMIT_DOGS;
    public static final ForgeConfigSpec.IntValue SPAWN_PROBABILITY_CATS;
    public static final ForgeConfigSpec.IntValue SPAWN_PROBABILITY_DOGS;
    public static final ForgeConfigSpec.BooleanValue REPLACE_VANILLA_WOLVES;
    public static final ForgeConfigSpec.BooleanValue REPLACE_VANILLA_OCELOTS;
    public static final ForgeConfigSpec.IntValue NUMBER_DOG_FAMILIES;
    public static final ForgeConfigSpec.IntValue NUMBER_CAT_FAMILIES;
    public static final ForgeConfigSpec.ConfigValue<String> CAT_BED;
    public static final ForgeConfigSpec.ConfigValue<String> CAT_BED2;
    public static final ForgeConfigSpec.ConfigValue<String> DOG_BED;
    public static final ForgeConfigSpec.ConfigValue<String> DOG_BED2;
    public static final Map<String, ForgeConfigSpec.ConfigValue<List<? extends String>>> BIOME_TYPES;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> CAT_FOOD;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> DOG_FOOD;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> PET_BOWL_FOOD;
    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.push("catsdogs");
        ENABLE_SPAWNS = builder.define("enableNaturalSpawns", true);
        SPAWN_LIMIT_CATS = builder.defineInRange("spawnLimitCats", 20, 1, 256);
        SPAWN_LIMIT_DOGS = builder.defineInRange("spawnLimitDogs", 20, 1, 256);
        SPAWN_PROBABILITY_CATS = builder.defineInRange("spawnProbabilityCats", 4, 1, 100);
        SPAWN_PROBABILITY_DOGS = builder.defineInRange("spawnProbabilityDogs", 5, 1, 100);
        REPLACE_VANILLA_WOLVES = builder.define("replaceVanillaWolves", true);
        REPLACE_VANILLA_OCELOTS = builder.define("replaceVanillaOcelots", true);
        NUMBER_DOG_FAMILIES = builder.defineInRange("numberDogFamilies", 2, 1, 32);
        NUMBER_CAT_FAMILIES = builder.defineInRange("numberCatFamilies", 2, 1, 32);
        CAT_BED = builder.define("catBed", "animania_catsdogs:cat_bed_1");
        CAT_BED2 = builder.define("catBed2", "animania_catsdogs:cat_bed_2");
        DOG_BED = builder.define("dogBed", "animania_catsdogs:dog_pillow");
        DOG_BED2 = builder.define("dogBed2", "animania:straw");
        CAT_FOOD = builder.defineList("catFood", List.of("minecraft:fish"), value -> value instanceof String);
        DOG_FOOD = builder.defineList("dogFood", List.of("listAllbeefraw"), value -> value instanceof String);
        PET_BOWL_FOOD = builder.defineList("petBowlFood", List.of("minecraft:fish", "listAllbeefraw", "animania_extra:hamster_food"), value -> value instanceof String);
        Map<String, ForgeConfigSpec.ConfigValue<List<? extends String>>> biomes = new LinkedHashMap<>();
        defineBiome(biomes, builder, "wolfBiomeTypes", List.of("MOUNTAIN", "FOREST", "SNOWY", "COLD"));
        defineBiome(biomes, builder, "foxBiomeTypes", List.of("FOREST", "SNOWY", "COLD"));
        defineBiome(biomes, builder, "ocelotBiomeTypes", List.of("HOT", "JUNGLE", "SAVANNA"));
        BIOME_TYPES = Map.copyOf(biomes);
        builder.pop();
        SPEC = builder.build();
    }
    private CatsDogsConfig() { }

    private static void defineBiome(Map<String, ForgeConfigSpec.ConfigValue<List<? extends String>>> values,
                                    ForgeConfigSpec.Builder builder, String key, List<String> defaults) {
        values.put(key, builder.defineList(key, defaults, value -> value instanceof String));
    }

    /** Match modern registry IDs and the two legacy ore-dictionary food names. */
    public static boolean matchesConfiguredFood(List<? extends String> configured, ItemStack stack) {
        if (stack == null || stack.isEmpty() || configured == null) return false;
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        String registryId = id == null ? "" : id.toString();
        for (String raw : configured) {
            if (raw == null) continue;
            String value = raw.trim().toLowerCase(java.util.Locale.ROOT);
            if (value.equals(registryId)) return true;
            // 1.12 used OreDictionary aliases that have no direct 1.20.1
            // equivalent. Preserve the intent with vanilla/modern tags.
            if (value.equals("minecraft:fish") && (stack.is(net.minecraft.tags.ItemTags.FISHES)
                    || stack.is(net.minecraft.world.item.Items.COD) || stack.is(net.minecraft.world.item.Items.SALMON))) return true;
            if ((value.equals("listallbeefraw") || value.equals("listallbeef"))
                    && (stack.is(net.minecraft.world.item.Items.BEEF) || stack.is(net.minecraft.world.item.Items.COOKED_BEEF))) return true;
        }
        return false;
    }

    public static boolean matchesCatFood(ItemStack stack) {
        try { return matchesConfiguredFood(CAT_FOOD.get(), stack); }
        catch (IllegalStateException ignored) { return false; }
    }

    public static boolean matchesDogFood(ItemStack stack) {
        try { return matchesConfiguredFood(DOG_FOOD.get(), stack); }
        catch (IllegalStateException ignored) { return false; }
    }

    public static boolean matchesPetBowlFood(ItemStack stack) {
        try { return matchesConfiguredFood(PET_BOWL_FOOD.get(), stack); }
        catch (IllegalStateException ignored) { return false; }
    }
}
