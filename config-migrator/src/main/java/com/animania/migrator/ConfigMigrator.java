package com.animania.migrator;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Read-only 1.12 configuration converter.  It never overwrites an output
 * file and emits a machine-readable report for every migrated/defaulted or
 * unsupported key.
 */
public final class ConfigMigrator {
    private static final Pattern KEY_VALUE = Pattern.compile("^\\s*([A-Za-z0-9_.-]+)\\s*=\\s*(.*?)\\s*(?:#.*)?$");
    private static final Map<String, String> ALIASES = Map.ofEntries(
            Map.entry("hungerUpdateInterval", "hungerInterval"),
            Map.entry("thirstUpdateInterval", "thirstInterval"),
            Map.entry("pregnancyTime", "gestationTicks"),
            Map.entry("gestationTime", "gestationTicks"),
            Map.entry("gestationTimer", "gestationTicks"),
            Map.entry("spawnAnimaniaAnimals", "enableNaturalSpawns"),
            Map.entry("hivePlayermadeHoneyRate", "hivePlayerHoneyRate"),
            Map.entry("hivePlayerMadeHoneyRate", "hivePlayerHoneyRate"),
            // The 1.12 hive toggle remains a distinct Farm setting in 1.20.1;
            // do not collapse it into the addon-wide natural-spawn switch.
            Map.entry("hiveSpawningFrequency", "hiveSpawningFrequency"),
            Map.entry("hamsterWheelRFGeneration", "hamsterWheelGeneration"),
            Map.entry("hamsterWheelEnergyGeneration", "hamsterWheelGeneration"));
    private static final Map<String, String> VALUE_ALIASES = Map.ofEntries(
            Map.entry("animania:block_straw", "animania:straw"),
            Map.entry("minecraft:grass", "minecraft:grass_block"),
            Map.entry("animania:brown_egg", "animania_farm:brown_egg"),
            Map.entry("animania:peacock_egg_blue", "animania_extra:peacock_egg_blue"),
            Map.entry("animania:peacock_egg_white", "animania_extra:peacock_egg_white"),
            Map.entry("animania:prime_mutton", "animania_farm:raw_prime_mutton"),
            Map.entry("animania:prime_rabbit", "animania_extra:raw_prime_rabbit"),
            Map.entry("animania_prime_chicken", "animania_farm:raw_prime_chicken"),
            Map.entry("animania:hamster_food", "animania_extra:hamster_food"),
            Map.entry("animania:cat_bed_1", "animania_catsdogs:cat_bed_1"),
            Map.entry("animania:cat_bed_2", "animania_catsdogs:cat_bed_2"),
            Map.entry("animania:dog_pillow", "animania_catsdogs:dog_pillow"));
    private static final Map<String, String> DEFAULTS = new LinkedHashMap<>(Map.ofEntries(
            Map.entry("hungerInterval", "2400"),
            Map.entry("thirstInterval", "1800"),
            Map.entry("gestationTicks", "20000"),
            Map.entry("childGrowthTick", "200"),
            Map.entry("feedTimer", "12000"),
            Map.entry("waterTimer", "12000"),
            Map.entry("playTimer", "12000"),
            Map.entry("laidTimer", "2000"),
            Map.entry("featherTimer", "12000"),
            Map.entry("woolRegrowthTimer", "8000"),
            Map.entry("starvationTimer", "400"),
            Map.entry("eggHatchChance", "2"),
            Map.entry("saltLickTick", "8000"),
            Map.entry("saltLickMaxUses", "200"),
            Map.entry("entityBreedingLimit", "15"),
            Map.entry("birthMultipleChance", "0.1"),
            Map.entry("animalLossChance", "0.0"),
            Map.entry("foodsGiveBonusEffects", "true"),
            Map.entry("showModUpdateNotification", "true"),
            Map.entry("showParts", "false"),
            Map.entry("showUnhappyParticles", "true"),
            Map.entry("allowSeedDispenserPlacement", "true"),
            Map.entry("shiftSeedPlacement", "false"),
            Map.entry("animalsStarve", "false"),
            Map.entry("allowMobRiding", "true"),
            Map.entry("allowTroughAutomation", "true"),
            Map.entry("fallDamageReduceMultiplier", "0.45"),
            Map.entry("waterRemovedAfterDrinking", "true"),
            Map.entry("plantsRemovedAfterEating", "true"),
            Map.entry("ambianceMode", "false"),
            Map.entry("animalsSleep", "true"),
            Map.entry("animalsCanAttackOthers", "true"),
            Map.entry("ticksBetweenAIFirings", "100"),
            Map.entry("tamedAnimalsTeleport", "true"),
            Map.entry("fancyEggs", "false"),
            Map.entry("fancyEggsRotate", "false"),
            Map.entry("eatFoodAnytime", "true"),
            Map.entry("birdsDropFeathers", "true"),
            Map.entry("aiBlockSearchRange", "16"),
            Map.entry("animalCapSearchRange", "80"),
            Map.entry("requireAnimalInteractionForAI", "true"),
            Map.entry("spawnFreshWaterSquids", "true"),
            Map.entry("feedToBreed", "true"),
            Map.entry("malesMateMultipleFemales", "false"),
            Map.entry("troughFood", "[\"minecraft:wheat\", \"simplecorn:corncob\", \"harvestcraft:barleyitem\", \"harvestcraft:oatsitem\", \"harvestcraft:ryeitem\", \"harvestcraft:cornitem\", \"minecraft:apple\", \"minecraft:carrot\", \"minecraft:beetroot\", \"minecraft:potato\", \"minecraft:poisonous_potato\", \"minecraft:wheat_seeds\", \"minecraft:melon_seeds\", \"minecraft:beetroot_seeds\", \"minecraft:pumpkin_seeds\", \"biomesoplenty:turnip_seeds\", \"minecraft:egg\", \"animania_farm:brown_egg\", \"listAllbeefraw\", \"minecraft:fish\"]"),
            Map.entry("slopIngredients", "[\"minecraft:carrot\", \"minecraft:beetroot\", \"minecraft:potato\", \"minecraft:poisonous_potato\", \"minecraft:bread\"]"),
            Map.entry("foodValueOverrides", "[]"),
            Map.entry("enableNaturalSpawns", "true"),
            Map.entry("enableVehicles", "true"),
            Map.entry("hiveWildHoneyRate", "700"),
            Map.entry("hivePlayerHoneyRate", "450"),
            Map.entry("hiveCapacity", "5000"),
            Map.entry("hiveSpawningFrequency", "3"),
            Map.entry("spawnProbabilityCows", "9"),
            Map.entry("spawnProbabilityHorses", "8"),
            Map.entry("spawnProbabilityPigs", "9"),
            Map.entry("spawnProbabilityChickens", "9"),
            Map.entry("spawnProbabilityGoats", "8"),
            Map.entry("spawnProbabilitySheep", "8"),
            Map.entry("chickenFood", "[\"minecraft:wheat_seeds\", \"minecraft:melon_seeds\", \"minecraft:beetroot_seeds\", \"minecraft:pumpkin_seeds\", \"simplecorn:corncob\", \"biomesoplenty:turnip_seeds\", \"harvestcraft:cornitem\"]"),
            Map.entry("cowFood", "[\"minecraft:wheat\", \"simplecorn:corncob\", \"harvestcraft:barleyitem\", \"harvestcraft:oatsitem\", \"harvestcraft:ryeitem\", \"harvestcraft:cornitem\"]"),
            Map.entry("goatFood", "[\"minecraft:wheat\", \"minecraft:string\", \"minecraft:stick\", \"minecraft:apple\", \"simplecorn:corncob\", \"harvestcraft:barleyitem\", \"harvestcraft:oatsitem\", \"harvestcraft:ryeitem\", \"harvestcraft:cornitem\"]"),
            Map.entry("horseFood", "[\"minecraft:wheat\", \"harvestcraft:barleyitem\", \"harvestcraft:oatsitem\", \"harvestcraft:ryeitem\", \"minecraft:apple\", \"minecraft:carrot\"]"),
            Map.entry("sheepFood", "[\"minecraft:wheat\", \"harvestcraft:barleyitem\", \"harvestcraft:oatsitem\", \"harvestcraft:ryeitem\"]"),
            Map.entry("pigFood", "[\"minecraft:carrot\", \"minecraft:beetroot\", \"minecraft:potato\", \"minecraft:poisonous_potato\", \"minecraft:bread\"]"),
            Map.entry("hamsterWheelCapacity", "200000"),
            Map.entry("hamsterWheelGeneration", "20"),
            Map.entry("hamsterWheelUseTime", "2000"),
            Map.entry("spawnProbabilityHedgehogs", "8"),
            Map.entry("spawnProbabilityFerrets", "8"),
            Map.entry("spawnProbabilityHamsters", "8"),
            Map.entry("spawnProbabilityPeacocks", "8"),
            Map.entry("spawnProbabilityAmphibians", "8"),
            Map.entry("spawnProbabilityRabbits", "8"),
            Map.entry("ferretFood", "[\"minecraft:mutton\", \"minecraft:egg\", \"animania_farm:brown_egg\", \"animania_extra:peacock_egg_blue\", \"animania_extra:peacock_egg_white\", \"animania_farm:raw_prime_mutton\", \"animania_extra:raw_prime_rabbit\", \"minecraft:rabbit\", \"minecraft:chicken\", \"animania_farm:raw_prime_chicken\"]"),
            Map.entry("hamsterFood", "[\"animania_extra:hamster_food\", \"minecraft:wheat_seeds\", \"minecraft:melon_seeds\", \"minecraft:beetroot_seeds\", \"minecraft:pumpkin_seeds\", \"simplecorn:corncob\", \"biomesoplenty:turnip_seeds\", \"harvestcraft:cornitem\", \"minecraft:apple\"]"),
            Map.entry("hedgehogFood", "[\"minecraft:carrot\", \"minecraft:beetroot\", \"minecraft:egg\", \"animania_farm:brown_egg\", \"animania_extra:peacock_egg_blue\", \"animania_extra:peacock_egg_white\", \"animania_farm:raw_prime_mutton\", \"animania_extra:raw_prime_rabbit\", \"minecraft:rabbit\", \"minecraft:chicken\", \"animania_farm:raw_prime_chicken\", \"minecraft:apple\"]"),
            Map.entry("peacockFood", "[\"minecraft:wheat_seeds\", \"minecraft:melon_seeds\", \"minecraft:beetroot_seeds\", \"minecraft:pumpkin_seeds\", \"simplecorn:corncob\", \"biomesoplenty:turnip_seeds\", \"harvestcraft:cornitem\"]"),
            Map.entry("rabbitFood", "[\"minecraft:wheat\", \"minecraft:carrot\", \"minecraft:beetroot\", \"minecraft:apple\"]"),
            Map.entry("spawnLimitCats", "20"),
            Map.entry("spawnLimitDogs", "20"),
            Map.entry("spawnProbabilityCats", "4"),
            Map.entry("spawnProbabilityDogs", "5"),
            Map.entry("replaceVanillaWolves", "true"),
            Map.entry("replaceVanillaOcelots", "true"),
            Map.entry("catFood", "[\"minecraft:fish\"]"),
            Map.entry("dogFood", "[\"listAllbeefraw\"]"),
            Map.entry("petBowlFood", "[\"minecraft:fish\", \"listAllbeefraw\", \"animania_extra:hamster_food\"]")));

    static {
        // The 1.12 addon configs contained a long tail of behavior, spawn
        // family and bed/biome keys.  Keep every key addressable so the
        // converter can report a migrated value instead of silently dropping
        // it.  Bed and biome lists are intentionally emitted as strings: the
        // modern addon configs validate and consume them through Forge's
        // ConfigValue API.
        putDefaults("allowEggThrowing", "false", "cheeseMaturityTime", "24000", "cowsMilkableAtSpawn", "false",
                "sleepAllowedWagon", "true", "disableRollingVehicles", "false", "disableSaltCreation", "false", "saltCreationAmount", "16",
                "chickensDropEggs", "false", "hiveSpawning", "true", "roostersFight", "false",
                "replaceVanillaCows", "true", "replaceVanillaPigs", "true", "replaceVanillaChickens", "true",
                "replaceVanillaSheep", "true", "replaceVanillaHorses", "false", "spawnAnimaniaChickens", "true",
                "spawnAnimaniaCows", "true", "spawnAnimaniaPigs", "true", "spawnAnimaniaHorses", "true",
                "spawnAnimaniaGoats", "true", "spawnAnimaniaSheep", "true", "numberCowFamilies", "2",
                "numberPigFamilies", "2", "numberChickenFamilies", "2", "numberHorseFamilies", "2",
                "numberGoatFamilies", "1", "numberSheepFamilies", "3", "spawnLimitCows", "40",
                "spawnLimitPigs", "40", "spawnLimitChickens", "40", "spawnLimitHorses", "40",
                "spawnLimitGoats", "40", "spawnLimitSheep", "40", "chickenBed", "animania:straw",
                "chickenBed2", "minecraft:grass_block", "cowBed", "animania:straw",
                "cowBed2", "minecraft:grass_block", "goatBed", "animania:straw",
                "goatBed2", "minecraft:grass_block", "horseBed", "animania:straw",
                "horseBed2", "minecraft:grass_block", "pigBed", "animania:straw",
                "pigBed2", "minecraft:grass_block", "sheepBed", "animania:straw",
                "sheepBed2", "minecraft:grass_block", "hiveValidBiomeTypes", "[\"JUNGLE\",\"CONIFEROUS\",\"SWAMP\",\"FOREST\",\"PLAINS\"]",
                "chickenPlymouthRockBiomeTypes", "[\"MOUNTAIN\"]", "chickenLeghornBiomeTypes", "[\"PLAINS\"]",
                "chickenOrpingtonBiomeTypes", "[\"JUNGLE\",\"SWAMP\"]", "chickenWyandotteBiomeTypes", "[\"FOREST\"]",
                "chickenRhodeIslandRedBiomeTypes", "[\"FOREST\"]", "cowHolsteinBiomeTypes", "[\"FOREST\"]",
                "cowFriesianBiomeTypes", "[\"PLAINS\"]", "cowAngusBiomeTypes", "[\"JUNGLE\",\"MESA\",\"SWAMP\"]",
                "cowHerefordBiomeTypes", "[\"MOUNTAIN\",\"HILLS\"]", "cowHighlandBiomeTypes", "[\"MOUNTAIN\",\"HILLS\"]",
                "cowJerseyBiomeTypes", "[\"WASTELAND\",\"SWAMP\"]", "cowLonghornBiomeTypes", "[\"SAVANNA\"]",
                "cowMooshroomBiomeTypes", "[\"MUSHROOM\",\"MAGICAL\"]", "draftHorseBiomeTypes", "[\"PLAINS\",\"SAVANNA\",\"MESA\"]",
                "pigYorkshireBiomeTypes", "[\"PLAINS\"]", "pigOldSpotBiomeTypes", "[\"FOREST\"]",
                "pigLargeBlackBiomeTypes", "[\"SWAMP\",\"DENSE\"]", "pigLargeWhiteBiomeTypes", "[\"FOREST\"]",
                "pigDurocBiomeTypes", "[\"JUNGLE\"]", "pigHampshireBiomeTypes", "[\"MOUNTAIN\",\"HILLS\"]",
                "goatAlpineBiomeTypes", "[\"MOUNTAIN\",\"HILLS\"]", "goatAngoraBiomeTypes", "[\"PLAINS\"]",
                "goatFaintingBiomeTypes", "[\"PLAINS\"]", "goatKikoBiomeTypes", "[\"MOUNTAIN\",\"HILLS\"]",
                "goatKinderBiomeTypes", "[\"SAVANNA\",\"MESA\"]", "goatNigerianDwarfBiomeTypes", "[\"SANDY\"]",
                "goatPygmyBiomeTypes", "[\"SAVANNA\",\"MESA\"]", "sheepDorsetBiomeTypes", "[\"HILLS\"]",
                "sheepFriesianBiomeTypes", "[\"PLAINS\"]", "sheepJacobBiomeTypes", "[\"FOREST\"]",
                "sheepMerinoBiomeTypes", "[\"PLAINS\"]", "sheepSuffolkBiomeTypes", "[\"SAVANNA\",\"MESA\"]",
                "sheepDorperBiomeTypes", "[\"SAVANNA\"]");
        putDefaults("replaceVanillaRabbits", "true", "spawnAnimaniaRodents", "true", "spawnAnimaniaPeacocks", "true",
                "spawnAnimaniaAmphibians", "true", "spawnAnimaniaRabbits", "true", "numberRabbitFamilies", "2",
                "spawnLimitHedgehogs", "40", "spawnLimitFerrets", "40", "spawnLimitHamsters", "40",
                "spawnLimitPeacocks", "40", "spawnLimitAmphibians", "40", "spawnLimitRabbits", "40",
                "ferretBed", "animania:straw", "ferretBed2", "minecraft:grass_block",
                "hamsterBed", "animania:straw", "hamsterBed2", "", "hedgehogBed", "animania:straw",
                "hedgehogBed2", "minecraft:grass_block", "peacockBed", "animania:straw",
                "peacockBed2", "minecraft:grass_block", "rabbitBed", "animania:straw",
                "rabbitBed2", "minecraft:grass_block", "toadBiomeTypes", "[\"SWAMP\",\"FOREST\"]",
                "frogBiomeTypes", "[\"SWAMP\",\"RIVER\"]", "dartFrogBiomeTypes", "[\"JUNGLE\",\"FOREST\"]",
                "hamsterBiomeTypes", "[\"BEACH\",\"SANDY\"]", "ferretGrayBiomeTypes", "[\"SAVANNA\"]",
                "ferretWhiteBiomeTypes", "[\"SAVANNA\"]", "hedgehogBiomeTypes", "[\"FOREST\"]",
                "hedgehogAlbinoBiomeTypes", "[\"SWAMP\"]", "rabbitCottontailBiomeTypes", "[\"FOREST\"]",
                "rabbitChinchillaBiomeTypes", "[\"SAVANNA\"]", "rabbitDutchBiomeTypes", "[\"PLAINS\"]",
                "rabbitHavanaBiomeTypes", "[\"MOUNTAIN\",\"HILLS\"]", "rabbitJackBiomeTypes", "[\"SAVANNA\",\"SANDY\"]",
                "rabbitNewZealandBiomeTypes", "[\"FOREST\"]", "rabbitRexBiomeTypes", "[\"SAVANNA\"]",
                "rabbitLopBiomeTypes", "[\"PLAINS\",\"FOREST\"]", "peafowlCharcoalBiomeTypes", "[\"SWAMP\",\"JUNGLE\"]",
                "peafowlOpalBiomeTypes", "[\"SWAMP\",\"JUNGLE\"]", "peafowlPeachBiomeTypes", "[\"SWAMP\",\"JUNGLE\"]",
                "peafowlPurpleBiomeTypes", "[\"SWAMP\",\"JUNGLE\"]", "peafowlTaupeBiomeTypes", "[\"SWAMP\",\"JUNGLE\"]",
                "peafowlBlueBiomeTypes", "[\"SWAMP\",\"JUNGLE\"]", "peafowlWhiteBiomeTypes", "[\"SWAMP\",\"JUNGLE\"]");
        putDefaults("numberDogFamilies", "2", "numberCatFamilies", "2", "catBed", "animania_catsdogs:cat_bed_1",
                "catBed2", "animania_catsdogs:cat_bed_2", "dogBed", "animania_catsdogs:dog_pillow",
                "dogBed2", "animania:straw", "wolfBiomeTypes", "[\"MOUNTAIN\",\"FOREST\",\"SNOWY\",\"COLD\"]",
                "foxBiomeTypes", "[\"FOREST\",\"SNOWY\",\"COLD\"]", "ocelotBiomeTypes", "[\"HOT\",\"JUNGLE\",\"SAVANNA\"]");
    }

    private static void putDefaults(String... keyValues) {
        if ((keyValues.length & 1) != 0) throw new IllegalArgumentException("defaults must be key/value pairs");
        for (int i = 0; i < keyValues.length; i += 2) DEFAULTS.putIfAbsent(keyValues[i], keyValues[i + 1]);
    }

    private static final Map<String, String> MODULE_SECTIONS = Map.of(
            "base", "gameplay",
            "farm", "farm",
            "extra", "extra",
            "catsdogs", "catsdogs");

    private static final Set<String> FARM_KEYS = Set.of(
            "hiveWildHoneyRate", "hivePlayerHoneyRate", "hiveCapacity", "hiveSpawningFrequency", "hiveSpawning",
            "spawnProbabilityCows", "spawnProbabilityHorses", "spawnProbabilityPigs",
            "spawnProbabilityChickens", "spawnProbabilityGoats", "spawnProbabilitySheep",
            "chickenFood", "cowFood", "goatFood", "horseFood", "sheepFood", "pigFood",
            "allowEggThrowing", "cheeseMaturityTime", "cowsMilkableAtSpawn", "sleepAllowedWagon",
            "disableSaltCreation", "saltCreationAmount", "disableRollingVehicles", "chickensDropEggs", "roostersFight",
            "replaceVanillaCows", "replaceVanillaPigs", "replaceVanillaChickens", "replaceVanillaSheep", "replaceVanillaHorses",
            "spawnAnimaniaChickens", "spawnAnimaniaCows", "spawnAnimaniaPigs", "spawnAnimaniaHorses", "spawnAnimaniaGoats", "spawnAnimaniaSheep",
            "numberCowFamilies", "numberPigFamilies", "numberChickenFamilies", "numberHorseFamilies", "numberGoatFamilies", "numberSheepFamilies",
            "spawnLimitCows", "spawnLimitPigs", "spawnLimitChickens", "spawnLimitHorses", "spawnLimitGoats", "spawnLimitSheep",
            "chickenBed", "chickenBed2", "cowBed", "cowBed2", "goatBed", "goatBed2", "horseBed", "horseBed2", "pigBed", "pigBed2", "sheepBed", "sheepBed2",
            "hiveValidBiomeTypes", "chickenPlymouthRockBiomeTypes", "chickenLeghornBiomeTypes", "chickenOrpingtonBiomeTypes", "chickenWyandotteBiomeTypes", "chickenRhodeIslandRedBiomeTypes",
            "cowHolsteinBiomeTypes", "cowFriesianBiomeTypes", "cowAngusBiomeTypes", "cowHerefordBiomeTypes", "cowHighlandBiomeTypes", "cowJerseyBiomeTypes", "cowLonghornBiomeTypes", "cowMooshroomBiomeTypes",
            "draftHorseBiomeTypes", "pigYorkshireBiomeTypes", "pigOldSpotBiomeTypes", "pigLargeBlackBiomeTypes", "pigLargeWhiteBiomeTypes", "pigDurocBiomeTypes", "pigHampshireBiomeTypes",
            "goatAlpineBiomeTypes", "goatAngoraBiomeTypes", "goatFaintingBiomeTypes", "goatKikoBiomeTypes", "goatKinderBiomeTypes", "goatNigerianDwarfBiomeTypes", "goatPygmyBiomeTypes",
            "sheepDorsetBiomeTypes", "sheepFriesianBiomeTypes", "sheepJacobBiomeTypes", "sheepMerinoBiomeTypes", "sheepSuffolkBiomeTypes", "sheepDorperBiomeTypes");
    private static final Set<String> EXTRA_KEYS = Set.of(
            "hamsterWheelCapacity", "hamsterWheelGeneration", "hamsterWheelUseTime",
            "spawnProbabilityHedgehogs", "spawnProbabilityFerrets", "spawnProbabilityHamsters",
            "spawnProbabilityPeacocks", "spawnProbabilityAmphibians", "spawnProbabilityRabbits",
            "ferretFood", "hamsterFood", "hedgehogFood", "peacockFood", "rabbitFood",
            "replaceVanillaRabbits", "spawnAnimaniaRodents", "spawnAnimaniaPeacocks", "spawnAnimaniaAmphibians", "spawnAnimaniaRabbits", "numberRabbitFamilies",
            "spawnLimitHedgehogs", "spawnLimitFerrets", "spawnLimitHamsters", "spawnLimitPeacocks", "spawnLimitAmphibians", "spawnLimitRabbits",
            "ferretBed", "ferretBed2", "hamsterBed", "hamsterBed2", "hedgehogBed", "hedgehogBed2", "peacockBed", "peacockBed2", "rabbitBed", "rabbitBed2",
            "toadBiomeTypes", "frogBiomeTypes", "dartFrogBiomeTypes", "hamsterBiomeTypes", "ferretGrayBiomeTypes", "ferretWhiteBiomeTypes", "hedgehogBiomeTypes", "hedgehogAlbinoBiomeTypes",
            "rabbitCottontailBiomeTypes", "rabbitChinchillaBiomeTypes", "rabbitDutchBiomeTypes", "rabbitHavanaBiomeTypes", "rabbitJackBiomeTypes", "rabbitNewZealandBiomeTypes", "rabbitRexBiomeTypes", "rabbitLopBiomeTypes",
            "peafowlCharcoalBiomeTypes", "peafowlOpalBiomeTypes", "peafowlPeachBiomeTypes", "peafowlPurpleBiomeTypes", "peafowlTaupeBiomeTypes", "peafowlBlueBiomeTypes", "peafowlWhiteBiomeTypes");
    private static final Set<String> CATSDOGS_KEYS = Set.of(
            "spawnLimitCats", "spawnLimitDogs", "spawnProbabilityCats",
            "spawnProbabilityDogs", "replaceVanillaWolves", "replaceVanillaOcelots", "catFood", "dogFood", "petBowlFood",
            "numberDogFamilies", "numberCatFamilies", "catBed", "catBed2", "dogBed", "dogBed2", "wolfBiomeTypes", "foxBiomeTypes", "ocelotBiomeTypes");

    private ConfigMigrator() {
    }

    public static void main(String[] args) throws IOException {
        Arguments arguments = Arguments.parse(args);
        if (!Files.isDirectory(arguments.input())) {
            throw new IllegalArgumentException("Input directory does not exist: " + arguments.input());
        }
        Files.createDirectories(arguments.output());
        List<ReportEntry> report = new ArrayList<>();
        Map<String, String> migrated = new LinkedHashMap<>();
        Map<String, Map<String, String>> moduleValues = new LinkedHashMap<>();
        MODULE_SECTIONS.keySet().forEach(module -> moduleValues.put(module, new LinkedHashMap<>()));
        List<Path> inputs;
        try (var stream = Files.walk(arguments.input())) {
            inputs = stream.filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".cfg") || path.toString().endsWith(".toml"))
                    .sorted(Comparator.naturalOrder()).toList();
        }
        for (Path input : inputs) {
            parseFile(arguments.input(), input, migrated, moduleValues, report);
        }
        for (Map.Entry<String, String> entry : DEFAULTS.entrySet()) {
            if (!migrated.containsKey(entry.getKey())) {
                migrated.put(entry.getKey(), entry.getValue());
                String module = moduleFor(entry.getKey(), "default");
                moduleValues.get(module).putIfAbsent(entry.getKey(), entry.getValue());
                report.add(new ReportEntry(entry.getKey(), "defaulted", entry.getValue(), module + "." + MODULE_SECTIONS.get(module) + "." + entry.getKey()));
            }
        }
        // Keys shared by multiple modules have independent Forge config
        // namespaces. Populate each addon's default even when the old file
        // only contained the Base section.
        moduleValues.get("farm").putIfAbsent("enableNaturalSpawns", "true");
        moduleValues.get("farm").putIfAbsent("enableVehicles", "true");
        moduleValues.get("farm").putIfAbsent("spawnWeight", "8");
        moduleValues.get("extra").putIfAbsent("enableNaturalSpawns", "true");
        moduleValues.get("extra").putIfAbsent("spawnWeight", "5");
        moduleValues.get("catsdogs").putIfAbsent("enableNaturalSpawns", "true");
        // Every addon owns a Forge common-config file. Keep the historical
        // Base filename and emit addon files beside it; no addon key is put
        // into Base's [gameplay] section.
        for (String module : MODULE_SECTIONS.keySet()) {
            Map<String, String> values = moduleValues.get(module);
            Path config = arguments.output().resolve(module.equals("base") ? "animania-common.toml" : "animania_" + module + "-common.toml");
            refuseOverwrite(config);
            Files.writeString(config, toml(values, MODULE_SECTIONS.get(module)), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE_NEW);
        }
        Path reportFile = arguments.output().resolve("animania-config-migration-report.json");
        refuseOverwrite(reportFile);
        Files.writeString(reportFile, reportJson(arguments.input(), arguments.output(), report), StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
        System.out.println("Migrated " + inputs.size() + " input file(s) to " + arguments.output());
        System.out.println("Report: " + reportFile);
    }

    private static void parseFile(Path root, Path input, Map<String, String> migrated,
                                  Map<String, Map<String, String>> moduleValues,
                                  List<ReportEntry> report) throws IOException {
        String section = "";
        for (String line : Files.readAllLines(input, StandardCharsets.UTF_8)) {
            String trimmed = line.trim();
            if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
                section = trimmed.substring(1, trimmed.length() - 1);
                continue;
            }
            Matcher matcher = KEY_VALUE.matcher(line);
            if (!matcher.matches()) continue;
            String original = matcher.group(1);
            String key = ALIASES.getOrDefault(original, original);
            String value = normalizeLegacyValue(stripQuotes(matcher.group(2)));
            if (!DEFAULTS.containsKey(key)) {
                report.add(new ReportEntry(relative(root, input) + ":" + original, "unmigratable", value, "no 1.20.1 mapping"));
                continue;
            }
            migrated.put(key, value);
            String module = moduleFor(original, section);
            moduleValues.get(module).put(key, value);
            report.add(new ReportEntry(relative(root, input) + ":" + original, "migrated", value,
                    module + "." + MODULE_SECTIONS.get(module) + "." + key));
        }
    }

    private static String moduleFor(String originalKey, String section) {
        if (FARM_KEYS.contains(originalKey) || originalKey.equals("hivePlayermadeHoneyRate")
                || originalKey.equals("hivePlayerMadeHoneyRate") || originalKey.equals("hiveSpawning")
                || originalKey.equals("hiveSpawningFrequency")) return "farm";
        if (EXTRA_KEYS.contains(originalKey) || originalKey.equals("hamsterWheelRFGeneration")
                || originalKey.equals("hamsterWheelEnergyGeneration")) return "extra";
        if (CATSDOGS_KEYS.contains(originalKey)) return "catsdogs";
        if (section != null && section.toLowerCase().contains("catsdog")) return "catsdogs";
        if (section != null && section.toLowerCase().contains("extra")) return "extra";
        if (section != null && section.toLowerCase().contains("farm")) return "farm";
        return "base";
    }

    private static String toml(Map<String, String> values, String section) {
        StringBuilder toml = new StringBuilder("# Generated by animania-config-migrator; input was read-only.\n[")
                .append(section).append("]\n");
        for (Map.Entry<String, String> entry : values.entrySet()) {
            toml.append(entry.getKey()).append('=').append(normalizeValue(entry.getValue())).append('\n');
        }
        return toml.toString();
    }

    private static String normalizeValue(String value) {
        String trimmed = value.trim();
        if ((trimmed.startsWith("[") && trimmed.endsWith("]")) || (trimmed.startsWith("{") && trimmed.endsWith("}"))) return trimmed;
        if (trimmed.equalsIgnoreCase("true") || trimmed.equalsIgnoreCase("false") || trimmed.matches("-?[0-9]+(?:\\.[0-9]+)?")) return trimmed;
        return "\"" + trimmed.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    private static String normalizeLegacyValue(String value) {
        String normalized = value;
        for (Map.Entry<String, String> alias : VALUE_ALIASES.entrySet()) {
            normalized = normalized.replace(alias.getKey(), alias.getValue());
        }
        return normalized;
    }

    private static String stripQuotes(String value) {
        String trimmed = value.trim();
        if (trimmed.length() > 1 && trimmed.startsWith("\"") && trimmed.endsWith("\"")) return trimmed.substring(1, trimmed.length() - 1);
        return trimmed;
    }

    private static String relative(Path root, Path path) {
        return root.relativize(path).toString().replace('\\', '/');
    }

    private static void refuseOverwrite(Path path) {
        if (Files.exists(path)) throw new IllegalStateException("Refusing to overwrite existing file: " + path);
    }

    private static String reportJson(Path input, Path output, List<ReportEntry> entries) {
        StringBuilder json = new StringBuilder("{\n  \"input\":\"").append(escape(input.toString())).append("\",\n  \"output\":\"").append(escape(output.toString())).append("\",\n  \"entries\":[");
        for (int i = 0; i < entries.size(); i++) {
            ReportEntry entry = entries.get(i);
            if (i > 0) json.append(',');
            json.append("\n    {\"source\":\"").append(escape(entry.source())).append("\",\"status\":\"").append(entry.status()).append("\",\"value\":\"").append(escape(entry.value())).append("\",\"target\":\"").append(escape(entry.target())).append("\"}");
        }
        return json.append("\n  ]\n}\n").toString();
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }

    private record ReportEntry(String source, String status, String value, String target) {
    }

    private record Arguments(Path input, Path output) {
        private static Arguments parse(String[] args) {
            Path input = null;
            Path output = null;
            for (int i = 0; i < args.length - 1; i++) {
                if (args[i].equals("--input")) input = Path.of(args[++i]);
                if (args[i].equals("--output")) output = Path.of(args[++i]);
            }
            if (input == null || output == null) throw new IllegalArgumentException("Usage: java -jar animania-config-migrator.jar --input <1.12-config-dir> --output <new-dir>");
            return new Arguments(input.toAbsolutePath().normalize(), output.toAbsolutePath().normalize());
        }
    }
}
