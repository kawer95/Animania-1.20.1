package com.animania.extra;

import com.animania.api.AnimaniaApi;
import com.animania.api.data.AnimalGender;
import com.animania.api.data.SpeciesDefinition;
import com.animania.common.entity.AnimaniaAnimalEntity;
import com.animania.common.entity.AnimaniaSleepProfiles;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.animal.Rabbit;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.entity.SpawnPlacementRegisterEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.RegisterGameTestsEvent;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.phys.AABB;

import java.util.LinkedHashMap;
import java.util.Map;

@Mod(AnimaniaExtra.MOD_ID)
public final class AnimaniaExtra {
    public static final String MOD_ID = "animania_extra";
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, MOD_ID);
    public static final Map<String, RegistryObject<EntityType<?>>> ENTITIES = new LinkedHashMap<>();

    static { ExtraLegacyIds.ALL.forEach(AnimaniaExtra::register); }

    private static void register(String id) {
        RegistryObject<EntityType<?>> registered = ENTITY_TYPES.register(id,
                () -> EntityType.Builder.of(AnimaniaAnimalEntity::new, MobCategory.CREATURE)
                        .sized(sizeFor(id, true), sizeFor(id, false)).clientTrackingRange(8).updateInterval(3)
                        .build(MOD_ID + ":" + id));
        ENTITIES.put(id, registered);
        AnimaniaApi.registerSpecies(new SpeciesDefinition(new ResourceLocation(MOD_ID, id), family(id), gender(id), sizeFor(id, true), sizeFor(id, false), 20000));
    }

    public AnimaniaExtra() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        ENTITY_TYPES.register(bus);
        ExtraSounds.SOUNDS.register(bus);
        ExtraWorldgen.BIOME_MODIFIER_SERIALIZERS.register(bus);
        ExtraContent.ITEMS.register(bus);
        ExtraContent.BLOCKS.register(bus);
        ExtraContent.BLOCK_ENTITIES.register(bus);
        ExtraContent.MENUS.register(bus);
        ExtraTab.TABS.register(bus);
        AnimaniaApi.registerFoodMatcher(MOD_ID, (id, stack) -> ExtraConfig.matchesSpeciesFood(id, stack));
        AnimaniaSleepProfiles.register(MOD_ID, AnimaniaExtra::sleepProfile);
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, ExtraConfig.SPEC);
        bus.addListener(this::attributes);
        bus.addListener(this::spawnPlacements);
        bus.addListener(this::registerGameTests);
        bus.addListener(this::commonSetup);
        bus.addListener(this::gatherData);
        MinecraftForge.EVENT_BUS.addListener(AnimaniaExtra::replaceVanillaRabbit);
        MinecraftForge.EVENT_BUS.addListener(AnimaniaExtra::extraAnimalTick);
        MinecraftForge.EVENT_BUS.addListener(AnimaniaExtra::limitNaturalExtraSpawns);
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> bus.addListener(AnimaniaExtraClient::onClientSetup));
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> bus.addListener(AnimaniaExtraClient::registerLayers));
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> bus.addListener(AnimaniaExtraClient::registerRenderers));
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> bus.addListener(AnimaniaExtraClient::registerItemColors));
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> ExtraContent.ITEM_ENTRIES.values().forEach(entry -> {
            if (entry.get() instanceof com.animania.common.item.AnimaniaEntityEggItem egg) {
                com.animania.common.item.AnimaniaEntityEggItem.registerDispenserBehavior(egg);
            }
        }));
    }

    private void gatherData(GatherDataEvent event) {
        event.getGenerator().addProvider(event.includeServer(),
                new ExtraDataProvider(event.getGenerator().getPackOutput()));
    }

    private static void extraAnimalTick(LivingEvent.LivingTickEvent event) {
        if (!(event.getEntity() instanceof AnimaniaAnimalEntity animal) || animal.level().isClientSide) return;
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(animal.getType());
        if (id != null && MOD_ID.equals(id.getNamespace()) && id.getPath().startsWith("peahen_")) animal.tryLayPeafowlEgg();
    }

    private void attributes(EntityAttributeCreationEvent event) {
        ENTITIES.forEach((id, type) -> {
            var attributes = AnimaniaAnimalEntity.createAttributes();
            double health;
            double speed;
            double attack = 1.0D;
            if (id.startsWith("doe_")) { health = 9.0D; speed = 0.265D; }
            else if (id.startsWith("buck_")) { health = 8.0D; speed = 0.265D; }
            else if (id.startsWith("kit_")) { health = 3.0D; speed = 0.315D; }
            else if (id.equals("hamster")) { health = 10.0D; speed = 0.30000001192092896D; }
            else if (id.startsWith("ferret_")) { health = 8.0D; speed = 0.35D; attack = 0.5D; }
            else if (id.startsWith("hedgehog")) { health = 8.0D; speed = 0.25D; }
            else if (id.startsWith("peacock_") || id.startsWith("peahen_") || id.startsWith("peachick_")) {
                health = 7.0D; speed = 0.25D; attack = 1.5D;
            } else { health = 3.0D; speed = 0.30000001192092896D; }
            attributes.add(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH, health)
                    .add(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED, speed)
                    .add(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE, attack);
            event.put((EntityType<? extends LivingEntity>) type.get(), attributes.build());
        });
    }

    private void spawnPlacements(SpawnPlacementRegisterEvent event) {
        // Forge may dispatch this event before the common config is loaded in a
        // GameTest/dev bootstrap. Use the default during that early window.
        if (!spawnsEnabled()) return;
        ENTITIES.forEach((id, type) -> {
            if (familySpawnsEnabled(id)) {
                event.register((EntityType<? extends AnimaniaAnimalEntity>) type.get(), SpawnPlacements.Type.ON_GROUND,
                        Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, AnimaniaAnimalEntity::checkAnimalSpawnRules, SpawnPlacementRegisterEvent.Operation.OR);
            }
        });
    }

    private static boolean spawnsEnabled() {
        try {
            return ExtraConfig.ENABLE_SPAWNS.get();
        } catch (IllegalStateException ignored) {
            return true;
        }
    }

    public static void limitNaturalExtraSpawns(MobSpawnEvent.PositionCheck event) {
        if (!(event.getEntity() instanceof AnimaniaAnimalEntity animal)
                || (event.getSpawnType() != MobSpawnType.NATURAL && event.getSpawnType() != MobSpawnType.CHUNK_GENERATION)) return;
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(animal.getType());
        if (id == null || !MOD_ID.equals(id.getNamespace())) return;
        String family = spawnFamily(id.getPath());
        int limit = switch (family) {
            case "hedgehog" -> configured(ExtraConfig.SPAWN_LIMIT_HEDGEHOGS, 40);
            case "ferret" -> configured(ExtraConfig.SPAWN_LIMIT_FERRETS, 40);
            case "hamster" -> configured(ExtraConfig.SPAWN_LIMIT_HAMSTERS, 30);
            case "peafowl" -> configured(ExtraConfig.SPAWN_LIMIT_PEACOCKS, 30);
            case "amphibian" -> configured(ExtraConfig.SPAWN_LIMIT_AMPHIBIANS, 30);
            case "rabbit" -> configured(ExtraConfig.SPAWN_LIMIT_RABBITS, 40);
            default -> Integer.MAX_VALUE;
        };
        AABB range = new AABB(event.getX(), event.getY(), event.getZ(), event.getX(), event.getY(), event.getZ()).inflate(100.0D);
        int nearby = event.getLevel().getLevel().getEntitiesOfClass(AnimaniaAnimalEntity.class, range, other -> {
            ResourceLocation otherId = ForgeRegistries.ENTITY_TYPES.getKey(other.getType());
            return otherId != null && MOD_ID.equals(otherId.getNamespace())
                    && family.equals(spawnFamily(otherId.getPath()));
        }).size();
        if (nearby >= limit) event.setResult(Event.Result.DENY);
    }

    private static String spawnFamily(String id) {
        if (id.startsWith("hedgehog")) return "hedgehog";
        if (id.startsWith("ferret_")) return "ferret";
        if (id.equals("hamster")) return "hamster";
        if (id.startsWith("peacock_") || id.startsWith("peahen_") || id.startsWith("peachick_")) return "peafowl";
        if (id.equals("frog") || id.equals("toad") || id.equals("dartfrog")) return "amphibian";
        if (id.startsWith("buck_") || id.startsWith("doe_") || id.startsWith("kit_")) return "rabbit";
        return id;
    }

    private static int configured(net.minecraftforge.common.ForgeConfigSpec.IntValue value, int fallback) {
        try { return value.get(); }
        catch (IllegalStateException ignored) { return fallback; }
    }

    private static boolean familySpawnsEnabled(String id) {
        if (id.startsWith("buck_") || id.startsWith("doe_") || id.startsWith("kit_")) return configured(ExtraConfig.SPAWN_ANIMANIA_RABBITS);
        if (id.startsWith("peacock_") || id.startsWith("peahen_") || id.startsWith("peachick_")) return configured(ExtraConfig.SPAWN_ANIMANIA_PEACOCKS);
        if (id.equals("toad") || id.equals("frog") || id.equals("dartfrog")) return configured(ExtraConfig.SPAWN_ANIMANIA_AMPHIBIANS);
        if (id.equals("hamster") || id.startsWith("ferret_") || id.startsWith("hedgehog")) return configured(ExtraConfig.SPAWN_ANIMANIA_RODENTS);
        return true;
    }

    private static boolean configured(net.minecraftforge.common.ForgeConfigSpec.BooleanValue value) {
        try {
            return value.get();
        } catch (IllegalStateException ignored) {
            return true;
        }
    }

    private static void replaceVanillaRabbit(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !configured(ExtraConfig.REPLACE_VANILLA_RABBITS)) return;
        Entity vanilla = event.getEntity();
        if (!(vanilla instanceof Rabbit rabbit)) return;
        boolean baby = rabbit.isBaby();
        String selected = ExtraLegacyIds.ALL.stream()
                .filter(id -> baby ? id.startsWith("kit_") : (id.startsWith("doe_") || id.startsWith("buck_")))
                .skip(event.getLevel().getRandom().nextInt(Math.max(1, (int) ExtraLegacyIds.ALL.stream()
                        .filter(id -> baby ? id.startsWith("kit_") : (id.startsWith("doe_") || id.startsWith("buck_"))).count())))
                .findFirst().orElse(null);
        if (selected == null) return;
        EntityType<?> registered = ENTITIES.get(selected).get();
        if (!(registered.create(event.getLevel()) instanceof AnimaniaAnimalEntity replacement)) return;
        replacement.moveTo(vanilla.getX(), vanilla.getY(), vanilla.getZ(), vanilla.getYRot(), vanilla.getXRot());
        replacement.setUUID(vanilla.getUUID());
        replacement.setCustomName(vanilla.getCustomName());
        replacement.setCustomNameVisible(vanilla.isCustomNameVisible());
        replacement.setPersistenceRequired();
        if (baby) replacement.setAge(-AnimaniaAnimalEntity.childGrowthDuration());
        if (event.getLevel().addFreshEntity(replacement)) event.setCanceled(true);
    }

    private void registerGameTests(RegisterGameTestsEvent event) {
        event.register(com.animania.extra.gametest.AnimaniaExtraGameTests.class);
    }

    private static AnimalGender gender(String id) {
        if (id.startsWith("kit_") || id.startsWith("peachick_")) return AnimalGender.CHILD;
        if (id.startsWith("doe_") || id.startsWith("peahen_")) return AnimalGender.FEMALE;
        if (id.startsWith("buck_") || id.startsWith("peacock_")) return AnimalGender.MALE;
        return AnimalGender.NONE;
    }

    private static String family(String id) {
        int underscore = id.indexOf('_');
        return underscore > 0 ? id.substring(underscore + 1) : id;
    }

    private static AnimaniaSleepProfiles.Profile sleepProfile(String id) {
        String family = id.startsWith("ferret_") ? "ferret"
                : id.equals("hamster") ? "hamster"
                : id.startsWith("hedgehog") ? "hedgehog"
                : id.startsWith("peacock_") || id.startsWith("peahen_") || id.startsWith("peachick_") ? "peacock"
                : id.startsWith("buck_") || id.startsWith("doe_") || id.startsWith("kit_") ? "rabbit" : null;
        if (family == null) return null;
        java.util.function.LongPredicate schedule = family.equals("hamster") || family.equals("hedgehog")
                ? AnimaniaSleepProfiles.DAY : family.equals("rabbit") ? AnimaniaSleepProfiles.RABBIT : AnimaniaSleepProfiles.NIGHT;
        return new AnimaniaSleepProfiles.Profile(
                () -> configured(ExtraConfig.BED_BLOCKS.get(family + "Bed")),
                () -> configured(ExtraConfig.BED_BLOCKS.get(family + "Bed2")), schedule);
    }

    private static String configured(net.minecraftforge.common.ForgeConfigSpec.ConfigValue<String> value) {
        try { return value.get(); } catch (IllegalStateException ignored) { return value.getDefault(); }
    }

    private static float sizeFor(String id, boolean width) {
        if (id.startsWith("kit_") || id.startsWith("peachick_")) return width ? 0.35f : 0.45f;
        if (id.equals("hamster")) return width ? 0.5f : 0.3f;
        return width ? 0.7f : 0.8f;
    }
}
