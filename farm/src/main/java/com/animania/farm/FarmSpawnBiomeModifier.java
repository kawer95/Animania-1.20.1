package com.animania.farm;

import com.animania.common.world.LegacyBiomeMatcher;
import com.mojang.serialization.Codec;
import net.minecraft.core.Holder;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.world.BiomeModifier;
import net.minecraftforge.common.world.ModifiableBiomeInfo;

import java.util.List;
import java.util.Locale;

/** Config-backed replacement for the old BiomeDictionary spawn registrations. */
public final class FarmSpawnBiomeModifier implements BiomeModifier {
    @Override
    public void modify(Holder<Biome> biome, Phase phase, ModifiableBiomeInfo.BiomeInfo.Builder builder) {
        if (phase != Phase.ADD || !biome.is(BiomeTags.IS_OVERWORLD) || !bool(FarmConfig.ENABLE_SPAWNS, true)) return;
        for (var entry : AnimaniaFarm.ENTITIES.entrySet()) {
            String id = entry.getKey();
            String family = naturalFamily(id);
            if (family == null || !familyEnabled(family)) continue;
            var biomes = FarmConfig.BIOME_TYPES.get(biomeKey(id));
            if (biomes == null || !LegacyBiomeMatcher.matches(biome, list(biomes))) continue;
            int weight = weight(family, id);
            int minimum = family.equals("horse") ? 1 : 2;
            int maximum = Math.max(minimum, familySize(family));
            builder.getMobSpawnSettings().addSpawn(MobCategory.CREATURE,
                    new MobSpawnSettings.SpawnerData(entry.getValue().get(), weight, minimum, maximum));
        }
    }

    @Override
    public Codec<? extends BiomeModifier> codec() {
        return FarmWorldgen.CONFIGURED_SPAWNS.get();
    }

    private static String naturalFamily(String id) {
        if (id.startsWith("cow_")) return "cow";
        if (id.startsWith("sow_")) return "pig";
        if (id.startsWith("hen_")) return "chicken";
        if (id.startsWith("mare_")) return "horse";
        if (id.startsWith("doe_")) return "goat";
        if (id.startsWith("ewe_")) return "sheep";
        return null;
    }

    private static boolean familyEnabled(String family) {
        return switch (family) {
            case "cow" -> bool(FarmConfig.SPAWN_ANIMANIA_COWS, true);
            case "pig" -> bool(FarmConfig.SPAWN_ANIMANIA_PIGS, true);
            case "chicken" -> bool(FarmConfig.SPAWN_ANIMANIA_CHICKENS, true);
            case "horse" -> bool(FarmConfig.SPAWN_ANIMANIA_HORSES, true);
            case "goat" -> bool(FarmConfig.SPAWN_ANIMANIA_GOATS, true);
            case "sheep" -> bool(FarmConfig.SPAWN_ANIMANIA_SHEEP, true);
            default -> false;
        };
    }

    private static int weight(String family, String id) {
        int value = switch (family) {
            case "cow" -> integer(FarmConfig.SPAWN_PROBABILITY_COWS, 9);
            case "pig" -> integer(FarmConfig.SPAWN_PROBABILITY_PIGS, 9);
            case "chicken" -> integer(FarmConfig.SPAWN_PROBABILITY_CHICKENS, 9);
            case "horse" -> integer(FarmConfig.SPAWN_PROBABILITY_HORSES, 8);
            case "goat" -> integer(FarmConfig.SPAWN_PROBABILITY_GOATS, 8);
            case "sheep" -> integer(FarmConfig.SPAWN_PROBABILITY_SHEEP, 8);
            default -> 1;
        };
        if (id.equals("sow_yorkshire") || id.equals("doe_angora") || id.equals("doe_fainting")) value /= 2;
        return Math.max(1, value);
    }

    private static int familySize(String family) {
        return switch (family) {
            case "cow" -> integer(FarmConfig.NUMBER_COW_FAMILIES, 2);
            case "pig" -> integer(FarmConfig.NUMBER_PIG_FAMILIES, 2);
            case "chicken" -> integer(FarmConfig.NUMBER_CHICKEN_FAMILIES, 2);
            case "horse" -> integer(FarmConfig.NUMBER_HORSE_FAMILIES, 2);
            case "goat" -> integer(FarmConfig.NUMBER_GOAT_FAMILIES, 1);
            case "sheep" -> integer(FarmConfig.NUMBER_SHEEP_FAMILIES, 3);
            default -> 1;
        };
    }

    private static String biomeKey(String id) {
        String family = naturalFamily(id);
        String breed = id.substring(id.indexOf('_') + 1);
        String pascal = pascal(breed);
        return switch (family) {
            case "cow" -> "cow" + pascal + "BiomeTypes";
            case "pig" -> "pig" + pascal + "BiomeTypes";
            case "chicken" -> "chicken" + pascal + "BiomeTypes";
            case "horse" -> "draftHorseBiomeTypes";
            case "goat" -> "goat" + pascal + "BiomeTypes";
            case "sheep" -> "sheep" + pascal + "BiomeTypes";
            default -> "";
        };
    }

    static boolean matchesConfiguredBiome(String id, Holder<Biome> biome) {
        var configured = FarmConfig.BIOME_TYPES.get(id.equals("hive") ? "hiveValidBiomeTypes" : biomeKey(id));
        return configured != null && LegacyBiomeMatcher.matches(biome, list(configured));
    }

    private static String pascal(String value) {
        StringBuilder result = new StringBuilder();
        for (String part : value.split("_")) {
            if (!part.isEmpty()) result.append(part.substring(0, 1).toUpperCase(Locale.ROOT)).append(part.substring(1));
        }
        return result.toString();
    }

    private static boolean bool(ForgeConfigSpec.BooleanValue value, boolean fallback) {
        try { return value.get(); } catch (RuntimeException ignored) { return fallback; }
    }

    private static int integer(ForgeConfigSpec.IntValue value, int fallback) {
        try { return value.get(); } catch (RuntimeException ignored) { return fallback; }
    }

    private static List<? extends String> list(ForgeConfigSpec.ConfigValue<List<? extends String>> value) {
        try { return value.get(); } catch (RuntimeException ignored) { return List.of(); }
    }
}
