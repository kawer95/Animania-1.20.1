package com.animania.farm;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import com.animania.common.item.AnimaniaVehicleItem;
import com.animania.common.item.AnimaniaEntityEggItem;
import com.animania.common.item.AnimaniaFoodItem;
import com.animania.common.entity.AnimaniaAnimalEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Farm items, produce, cheese, tools and fluid-facing blocks from the 1.12 ledger. */
public final class FarmContent {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, AnimaniaFarm.MOD_ID);
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, AnimaniaFarm.MOD_ID);
    public static final DeferredRegister<net.minecraft.world.level.block.entity.BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, AnimaniaFarm.MOD_ID);
    public static final RegistryObject<EntityType<FarmBrownEggProjectile>> BROWN_EGG_PROJECTILE = AnimaniaFarm.ENTITY_TYPES.register(
            "brown_egg_projectile", () -> EntityType.Builder.<FarmBrownEggProjectile>of(FarmBrownEggProjectile::new, MobCategory.MISC)
                    .sized(0.25F, 0.25F).clientTrackingRange(4).updateInterval(10)
                    .build(AnimaniaFarm.MOD_ID + ":brown_egg_projectile"));
    public static final Map<String, RegistryObject<Item>> ITEM_ENTRIES = new LinkedHashMap<>();
    public static final Map<String, RegistryObject<Block>> BLOCK_ENTRIES = new LinkedHashMap<>();
    public static final List<String> ITEM_IDS = List.of(
            "truffle", "carving_knife", "salt", "riding_crop", "milk_bottle", "honey_jar", "honey_bottle", "brown_egg",
            "raw_prime_steak", "raw_prime_beef", "cooked_prime_steak", "cooked_prime_beef", "raw_horse", "cooked_horse",
            "raw_prime_pork", "raw_prime_bacon", "cooked_prime_pork", "cooked_prime_bacon", "raw_prime_chicken", "cooked_prime_chicken",
            "raw_chevon", "cooked_chevon", "raw_prime_chevon", "cooked_prime_chevon", "raw_prime_mutton", "cooked_prime_mutton",
            "plain_omelette", "cheese_omelette", "bacon_omelette", "truffle_omelette", "super_omelette", "friesian_cheese_wheel",
            "friesian_cheese_wedge", "holstein_cheese_wheel", "holstein_cheese_wedge", "jersey_cheese_wheel", "jersey_cheese_wedge",
            "goat_cheese_wheel", "goat_cheese_wedge", "sheep_cheese_wheel", "sheep_cheese_wedge", "truffle_soup", "chocolate_truffle",
            // 1.12 ItemEntityEgg prepended entity_egg_ to these registry IDs.
            // Keep the exact IDs here so the creative tab and data generator
            // cannot accidentally create inert plain Items for random eggs.
            "entity_egg_cow_random", "entity_egg_chicken_random", "entity_egg_pig_random",
            "entity_egg_goat_random", "entity_egg_sheep_random", "cart", "wagon", "tiller", "wheel");
    public static final List<String> BLOCK_IDS = List.of("animania_wool");

    public static final RegistryObject<Block> CHEESE_FRIESIAN = cheeseBlock("cheese_friesian", "friesian");
    public static final RegistryObject<Block> CHEESE_HOLSTEIN = cheeseBlock("cheese_holstein", "holstein");
    public static final RegistryObject<Block> CHEESE_JERSEY = cheeseBlock("cheese_jersey", "jersey");
    public static final RegistryObject<Block> CHEESE_GOAT = cheeseBlock("cheese_goat", "goat");
    public static final RegistryObject<Block> CHEESE_SHEEP = cheeseBlock("cheese_sheep", "sheep");
    public static final Map<String, RegistryObject<Block>> CHEESE_BLOCKS = Map.of(
            "friesian", CHEESE_FRIESIAN, "holstein", CHEESE_HOLSTEIN, "jersey", CHEESE_JERSEY,
            "goat", CHEESE_GOAT, "sheep", CHEESE_SHEEP);
    public static final RegistryObject<Block> HIVE = BLOCKS.register("hive", () -> new FarmHiveBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_YELLOW).strength(1.3f).sound(SoundType.WOOD).noOcclusion(), false));
    public static final RegistryObject<Block> WILD_HIVE = BLOCKS.register("wild_hive", () -> new FarmHiveBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_YELLOW).strength(1.3f).sound(SoundType.GRASS).noOcclusion(), true));
    public static final RegistryObject<net.minecraft.world.level.block.entity.BlockEntityType<FarmHiveBlockEntity>> HIVE_BE = BLOCK_ENTITIES.register("hive",
            () -> net.minecraft.world.level.block.entity.BlockEntityType.Builder.of(FarmHiveBlockEntity::createHive, HIVE.get()).build(null));
    public static final RegistryObject<net.minecraft.world.level.block.entity.BlockEntityType<FarmHiveBlockEntity>> WILD_HIVE_BE = BLOCK_ENTITIES.register("wild_hive",
            () -> net.minecraft.world.level.block.entity.BlockEntityType.Builder.of(FarmHiveBlockEntity::createWildHive, WILD_HIVE.get()).build(null));

    public static final RegistryObject<Block> CHEESE_MOLD = BLOCKS.register("cheese_mold", () ->
            new FarmCheeseMoldBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_YELLOW)
                    .strength(1.2f).sound(SoundType.WOOD).noOcclusion()));
    public static final RegistryObject<net.minecraft.world.level.block.entity.BlockEntityType<FarmCheeseMoldBlockEntity>> CHEESE_MOLD_BE =
            BLOCK_ENTITIES.register("cheese_mold", () -> net.minecraft.world.level.block.entity.BlockEntityType.Builder
                    .of(FarmCheeseMoldBlockEntity::new, CHEESE_MOLD.get()).build(null));

    static {
        ITEM_IDS.stream().filter(id -> !isCheeseWheel(id)).forEach(id -> {
            if (id.startsWith("entity_egg_")) {
                registerGenericItem(id);
                return;
            }
            ITEM_ENTRIES.put(id, ITEMS.register(id, () -> {
            if (FarmLegacyIds.isVehicle(id)) {
                return new AnimaniaVehicleItem(() -> AnimaniaFarm.ENTITIES.get(id).get(), new Item.Properties().stacksTo(1), () -> {
                    try {
                        return !FarmConfig.DISABLE_ROLLING_VEHICLES.get();
                    } catch (RuntimeException ignored) {
                        return true;
                    }
                });
            }
            if (id.equals("milk_bottle")) return new FarmMilkBottleItem();
            if (id.equals("honey_jar") || id.equals("honey_bottle")) return new FarmHoneyJarItem();
            if (id.equals("brown_egg")) return new FarmBrownEggItem();
            if (id.equals("carving_knife")) return new FarmCarvingKnifeItem();
            if (id.equals("riding_crop")) return new FarmRidingCropItem();
            return new AnimaniaFoodItem(itemProperties(id));
            }));
        });
        FarmLegacyIds.ALL.stream().filter(id -> !FarmLegacyIds.isVehicle(id)).forEach(id -> registerGenericItem("entity_egg_" + id));
        BLOCK_IDS.forEach(id -> {
            RegistryObject<Block> block = BLOCKS.register(id, () -> new FarmWoolBlock(
                    BlockBehaviour.Properties.of().mapColor(MapColor.WOOL).strength(0.8f).sound(SoundType.WOOL)));
            BLOCK_ENTRIES.put(id, block);
            ITEM_ENTRIES.put(id, ITEMS.register(id, () -> new FarmWoolBlockItem(block.get(), new Item.Properties())));
        });
        CHEESE_BLOCKS.forEach((family, block) -> {
            BLOCK_ENTRIES.put("cheese_" + family, block);
            ITEM_ENTRIES.put(family + "_cheese_wheel", ITEMS.register(family + "_cheese_wheel", () -> new BlockItem(block.get(), new Item.Properties())));
        });
        BLOCK_ENTRIES.put("hive", HIVE);
        BLOCK_ENTRIES.put("wild_hive", WILD_HIVE);
        // The 1.12 items used ordinary generated sprites. Keeping them as plain
        // BlockItems also lets JEI, inventory screens and shader renderers use
        // Minecraft's stable baked-item path instead of a custom BEWLR.
        ITEM_ENTRIES.put("hive", ITEMS.register("hive", () -> new BlockItem(HIVE.get(), new Item.Properties())));
        ITEM_ENTRIES.put("wild_hive", ITEMS.register("wild_hive", () -> new BlockItem(WILD_HIVE.get(), new Item.Properties())));
        BLOCK_ENTRIES.put("cheese_mold", CHEESE_MOLD);
        ITEM_ENTRIES.put("cheese_mold", ITEMS.register("cheese_mold", () -> new BlockItem(CHEESE_MOLD.get(), new Item.Properties())));
    }

    private static void registerGenericItem(String id) {
        if (!ITEM_ENTRIES.containsKey(id)) {
            if (id.startsWith("entity_egg_")) {
                String target = id.substring("entity_egg_".length());
                registerEggItem(id, target);
            } else {
                ITEM_ENTRIES.put(id, ITEMS.register(id, () -> new Item(new Item.Properties().stacksTo(16))));
            }
        }
    }

    private static void registerEggItem(String id, String target) {
        if (!ITEM_ENTRIES.containsKey(id)) {
            ITEM_ENTRIES.put(id, ITEMS.register(id, () -> new AnimaniaEntityEggItem(
                    () -> eggCandidates(target), new Item.Properties(), true, target)));
        }
    }

    @SuppressWarnings("unchecked")
    private static EntityType<? extends AnimaniaAnimalEntity> animalType(String id) {
        var entry = AnimaniaFarm.ENTITIES.get(id);
        return entry == null ? null : (EntityType<? extends AnimaniaAnimalEntity>) (EntityType<?>) entry.get();
    }

    private static List<EntityType<? extends AnimaniaAnimalEntity>> eggCandidates(String target) {
        if (!target.endsWith("_random")) {
            EntityType<? extends AnimaniaAnimalEntity> type = animalType(target);
            return type == null ? List.of() : List.of(type);
        }
        String family = target.substring(0, target.length() - "_random".length());
        return FarmLegacyIds.ALL.stream()
                .filter(id -> switch (family) {
                    case "cow" -> id.startsWith("cow_") || id.startsWith("bull_");
                    case "chicken" -> id.startsWith("hen_") || id.startsWith("rooster_");
                    case "pig" -> id.startsWith("sow_") || id.startsWith("hog_");
                    case "goat" -> id.startsWith("doe_") || id.startsWith("buck_");
                    case "sheep" -> id.startsWith("ewe_") || id.startsWith("ram_");
                    default -> false;
                })
                .map(FarmContent::animalType)
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    private static Item.Properties itemProperties(String id) {
        net.minecraft.world.food.FoodProperties.Builder food = new net.minecraft.world.food.FoodProperties.Builder();
        boolean edible = true;
        switch (id) {
            case "truffle" -> food.nutrition(2).saturationMod(0.7F);
            case "plain_omelette" -> food.nutrition(5).saturationMod(0.6F);
            case "cheese_omelette" -> food.nutrition(5).saturationMod(0.7F).effect(() -> new MobEffectInstance(MobEffects.HEAL, 1), 1.0F);
            case "bacon_omelette" -> food.nutrition(5).saturationMod(0.7F).effect(() -> new MobEffectInstance(MobEffects.DAMAGE_BOOST, 600, 0), 1.0F);
            case "truffle_omelette" -> food.nutrition(5).saturationMod(0.8F).effect(() -> new MobEffectInstance(MobEffects.REGENERATION, 600, 1), 1.0F);
            case "super_omelette" -> food.nutrition(5).saturationMod(0.9F)
                    .effect(() -> new MobEffectInstance(MobEffects.REGENERATION, 600, 1), 1.0F)
                    .effect(() -> new MobEffectInstance(MobEffects.DAMAGE_BOOST, 600, 0), 1.0F);
            case "truffle_soup" -> food.nutrition(10).saturationMod(0.6F).effect(() -> new MobEffectInstance(MobEffects.REGENERATION, 1200, 1), 1.0F);
            case "chocolate_truffle" -> food.nutrition(6).saturationMod(0.7F).effect(() -> new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 1200, 3), 1.0F);
            case "cooked_prime_beef", "cooked_prime_pork" -> food.nutrition(12).saturationMod(0.5F);
            case "cooked_prime_steak", "cooked_prime_bacon", "cooked_prime_chicken" -> food.nutrition(8).saturationMod(0.5F);
            case "cooked_horse" -> food.nutrition(12).saturationMod(0.5F).effect(() -> new MobEffectInstance(MobEffects.DAMAGE_BOOST, 600, 0), 1.0F);
            case "cooked_chevon" -> food.nutrition(5).saturationMod(0.5F);
            case "cooked_prime_chevon" -> food.nutrition(10).saturationMod(0.5F);
            case "cooked_prime_mutton" -> food.nutrition(12).saturationMod(0.5F);
            case "friesian_cheese_wedge", "holstein_cheese_wedge", "jersey_cheese_wedge", "goat_cheese_wedge", "sheep_cheese_wedge" -> food.nutrition(3).saturationMod(0.9F);
            default -> {
                com.animania.common.item.LegacyRawFoodProfile raw = com.animania.common.item.LegacyRawFoodProfile.forItemId(id);
                if (raw != null) raw.apply(food);
                else edible = false;
            }
        }
        if (!edible) return new Item.Properties();
        if (AnimaniaConfigCompat.eatFoodAnytime()) food.alwaysEat();
        Item.Properties properties = new Item.Properties().food(food.build());
        if (id.equals("truffle_soup")) properties.stacksTo(1).craftRemainder(Items.BOWL);
        return properties;
    }

    private static boolean isCheeseWheel(String id) {
        return id.endsWith("_cheese_wheel");
    }

    private static RegistryObject<Block> cheeseBlock(String id, String family) {
        return BLOCKS.register(id, () -> new FarmCheeseBlock(family,
                BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_YELLOW).strength(0.6f).sound(SoundType.WOOL).noOcclusion()));
    }

    /** Keep the base config optional at class-load time for datagen. */
    private static final class AnimaniaConfigCompat {
        private static boolean eatFoodAnytime() {
            try {
                return com.animania.common.config.AnimaniaConfig.EAT_FOOD_ANYTIME.get();
            } catch (RuntimeException ignored) {
                return true;
            }
        }
    }

    private FarmContent() { }
}
