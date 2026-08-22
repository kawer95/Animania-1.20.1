package com.animania.farm;

import com.animania.Animania;
import com.animania.api.AnimaniaApi;
import com.animania.api.data.AnimalGender;
import com.animania.api.data.SpeciesDefinition;
import com.animania.common.entity.AnimaniaAnimalEntity;
import com.animania.common.entity.AnimaniaVehicleEntity;
import com.animania.common.entity.AnimaniaSleepProfiles;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.animal.MushroomCow;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.entity.SpawnPlacementRegisterEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.RegisterGameTestsEvent;
import net.minecraftforge.event.level.ChunkDataEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.ChunkPos;
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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

@Mod(AnimaniaFarm.MOD_ID)
public final class AnimaniaFarm {
    private static final org.slf4j.Logger LOGGER = com.mojang.logging.LogUtils.getLogger();
    public static final String MOD_ID = "animania_farm";
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, MOD_ID);
    public static final Map<String, RegistryObject<EntityType<?>>> ENTITIES = new LinkedHashMap<>();
    private static final Queue<PendingHive> PENDING_HIVES = new ConcurrentLinkedQueue<>();

    private record PendingHive(net.minecraft.server.level.ServerLevel level, ChunkPos chunk) { }

    static {
        FarmLegacyIds.ALL.forEach(AnimaniaFarm::register);
    }

    private static void register(String id) {
        FarmAnimalProfile profile = FarmAnimalProfile.forId(id);
        RegistryObject<EntityType<?>> registered = FarmLegacyIds.VEHICLE_IDS.contains(id)
                ? ENTITY_TYPES.register(id, () -> EntityType.Builder.of(AnimaniaVehicleEntity::new, MobCategory.MISC)
                .sized(profile.width(), profile.height()).clientTrackingRange(8).updateInterval(3)
                .build(MOD_ID + ":" + id))
                : ENTITY_TYPES.register(id, () -> EntityType.Builder.of(AnimaniaAnimalEntity::new, MobCategory.CREATURE)
                .sized(profile.width(), profile.height()).clientTrackingRange(8).updateInterval(3)
                .build(MOD_ID + ":" + id));
        ENTITIES.put(id, registered);
        if (!FarmLegacyIds.VEHICLE_IDS.contains(id)) {
            AnimaniaApi.registerSpecies(new SpeciesDefinition(new ResourceLocation(MOD_ID, id), family(id), gender(id),
                    profile.width(), profile.height(), 20000));
        }
    }

    public AnimaniaFarm() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        AnimaniaVehicleEntity.setWagonSleepRule(() -> configured(FarmConfig.SLEEP_ALLOWED_WAGON));
        ENTITY_TYPES.register(bus);
        FarmSounds.SOUNDS.register(bus);
        FarmContent.ITEMS.register(bus);
        FarmContent.BLOCKS.register(bus);
        FarmContent.BLOCK_ENTITIES.register(bus);
        FarmFluids.FLUID_TYPES.register(bus);
        FarmFluids.FLUIDS.register(bus);
        FarmFluids.BLOCKS.register(bus);
        FarmFluids.ITEMS.register(bus);
        FarmTab.TABS.register(bus);
        FarmRecipes.SERIALIZERS.register(bus);
        FarmWorldgen.BIOME_MODIFIER_SERIALIZERS.register(bus);
        AnimaniaApi.registerFoodMatcher(MOD_ID, (id, stack) -> FarmConfig.matchesSpeciesFood(id, stack));
        AnimaniaSleepProfiles.register(MOD_ID, AnimaniaFarm::sleepProfile);
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, FarmConfig.SPEC);
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                ModLoadingContext.get().registerExtensionPoint(
                        net.minecraftforge.client.ConfigScreenHandler.ConfigScreenFactory.class,
                        () -> new net.minecraftforge.client.ConfigScreenHandler.ConfigScreenFactory((minecraft, parent) ->
                                new com.animania.client.config.AnimaniaConfigScreen(parent,
                                        net.minecraft.network.chat.Component.translatable("screen.animania_farm.config.title"),
                                        MOD_ID, FarmConfig.SPEC))));
        bus.addListener(this::attributes);
        bus.addListener(this::spawnPlacements);
        bus.addListener(this::registerGameTests);
        bus.addListener(this::commonSetup);
        bus.addListener(this::gatherData);
        // The 1.12 addon replaced vanilla farm animals at the world boundary.
        // Keep that behavior server-side while preserving the vanilla UUID and
        // age/name state on the modern registry entity.
        MinecraftForge.EVENT_BUS.addListener(AnimaniaFarm::replaceVanillaFarmAnimal);
        MinecraftForge.EVENT_BUS.addListener(AnimaniaFarm::markNaturalVanillaFarmAnimal);
        MinecraftForge.EVENT_BUS.addListener(AnimaniaFarm::farmAnimalTick);
        MinecraftForge.EVENT_BUS.addListener(AnimaniaFarm::limitNaturalFarmSpawns);
        MinecraftForge.EVENT_BUS.addListener(AnimaniaFarm::decorateHiveOnChunkLoad);
        MinecraftForge.EVENT_BUS.addListener(AnimaniaFarm::processHiveQueue);
        MinecraftForge.EVENT_BUS.addListener(FarmEggThrowHandler::onRightClickItem);
        MinecraftForge.EVENT_BUS.addListener(AnimaniaFarm::boostRiddenPig);
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> bus.addListener(AnimaniaFarmClient::onClientSetup));
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> bus.addListener(AnimaniaFarmClient::registerLayers));
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> bus.addListener(AnimaniaFarmClient::registerRenderers));
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> FarmContent.ITEM_ENTRIES.values().forEach(entry -> {
            if (entry.get() instanceof com.animania.common.item.AnimaniaEntityEggItem egg) {
                com.animania.common.item.AnimaniaEntityEggItem.registerDispenserBehavior(egg);
            }
        }));
    }

    private void gatherData(GatherDataEvent event) {
        event.getGenerator().addProvider(event.includeServer(),
                new FarmDataProvider(event.getGenerator().getPackOutput()));
    }

    private void attributes(EntityAttributeCreationEvent event) {
        ENTITIES.forEach((id, type) -> {
            if (!FarmLegacyIds.VEHICLE_IDS.contains(id)) {
                var attributes = FarmAnimalProfile.forId(id).attributes();
                if (id.startsWith("mare_") || id.startsWith("stallion_")) {
                    // EntityHorse supplied this attribute in 1.12; the ported
                    // entity is an Animal, so register the same jump strength.
                    attributes.add(net.minecraft.world.entity.ai.attributes.Attributes.JUMP_STRENGTH, 0.7D);
                }
                event.put((EntityType<? extends LivingEntity>) type.get(), attributes.build());
            }
        });
    }

    private void spawnPlacements(SpawnPlacementRegisterEvent event) {
        // Spawn placement registration can fire before Forge has loaded the common config
        // (notably during GameTest bootstrap). Treat that early window as the configured
        // default and let biome modifiers apply the runtime spawn toggle afterwards.
        if (!spawnsEnabled()) return;
        ENTITIES.forEach((id, type) -> {
            if (!FarmLegacyIds.VEHICLE_IDS.contains(id) && familySpawnsEnabled(id)) {
                event.register((EntityType<? extends AnimaniaAnimalEntity>) type.get(), SpawnPlacements.Type.ON_GROUND,
                        Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, AnimaniaAnimalEntity::checkAnimalSpawnRules, SpawnPlacementRegisterEvent.Operation.OR);
            }
        });
    }

    private static boolean spawnsEnabled() {
        try {
            return FarmConfig.ENABLE_SPAWNS.get();
        } catch (IllegalStateException ignored) {
            return true;
        }
    }

    public static void limitNaturalFarmSpawns(MobSpawnEvent.PositionCheck event) {
        if (!(event.getEntity() instanceof AnimaniaAnimalEntity animal)
                || (event.getSpawnType() != MobSpawnType.NATURAL && event.getSpawnType() != MobSpawnType.CHUNK_GENERATION)) return;
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(animal.getType());
        if (id == null || !MOD_ID.equals(id.getNamespace())) return;
        String spawnFamily = spawnFamily(id.getPath());
        int limit = switch (spawnFamily) {
            case "cow" -> configured(FarmConfig.SPAWN_LIMIT_COWS, 40);
            case "pig" -> configured(FarmConfig.SPAWN_LIMIT_PIGS, 40);
            case "chicken" -> configured(FarmConfig.SPAWN_LIMIT_CHICKENS, 40);
            case "horse" -> configured(FarmConfig.SPAWN_LIMIT_HORSES, 40);
            case "goat" -> configured(FarmConfig.SPAWN_LIMIT_GOATS, 40);
            case "sheep" -> configured(FarmConfig.SPAWN_LIMIT_SHEEP, 40);
            default -> Integer.MAX_VALUE;
        };
        AABB range = new AABB(event.getX(), event.getY(), event.getZ(), event.getX(), event.getY(), event.getZ()).inflate(100.0D);
        long nearby = event.getLevel().getLevel().getEntitiesOfClass(AnimaniaAnimalEntity.class, range, other -> {
            ResourceLocation otherId = ForgeRegistries.ENTITY_TYPES.getKey(other.getType());
            return otherId != null && MOD_ID.equals(otherId.getNamespace())
                    && spawnFamily.equals(spawnFamily(otherId.getPath()));
        }).size();
        if (nearby >= limit) event.setResult(Event.Result.DENY);
    }

    private static void replaceVanillaFarmAnimal(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) return;
        Entity vanilla = event.getEntity();
        if (!vanilla.getPersistentData().getBoolean("AnimaniaNaturalFarmSpawn") || vanilla.hasCustomName()) return;
        String family;
        boolean enabled;
        if (vanilla instanceof Cow || vanilla instanceof MushroomCow) {
            family = "cow";
            enabled = configured(FarmConfig.REPLACE_VANILLA_COWS);
        } else if (vanilla instanceof Pig) {
            family = "pig";
            enabled = configured(FarmConfig.REPLACE_VANILLA_PIGS);
        } else if (vanilla instanceof Chicken) {
            family = "chicken";
            enabled = configured(FarmConfig.REPLACE_VANILLA_CHICKENS);
        } else if (vanilla instanceof Sheep) {
            family = "sheep";
            enabled = configured(FarmConfig.REPLACE_VANILLA_SHEEP);
        } else if (vanilla instanceof Horse) {
            family = "horse";
            enabled = configured(FarmConfig.REPLACE_VANILLA_HORSES);
        } else {
            return;
        }
        if (!enabled) return;
        boolean baby = vanilla instanceof AgeableMob ageable && ageable.isBaby();
        String childPrefix = switch (family) {
            case "cow" -> "calf_";
            case "pig" -> "piglet_";
            case "chicken" -> "chick_";
            case "sheep" -> "lamb_";
            case "horse" -> "foal_";
            default -> "";
        };
        String femalePrefix = switch (family) {
            case "cow" -> "cow_";
            case "pig" -> "sow_";
            case "chicken" -> "hen_";
            case "sheep" -> "ewe_";
            case "horse" -> "mare_";
            default -> "";
        };
        String malePrefix = switch (family) {
            case "cow" -> "bull_";
            case "pig" -> "hog_";
            case "chicken" -> "rooster_";
            case "sheep" -> "ram_";
            case "horse" -> "stallion_";
            default -> "";
        };
        net.minecraft.core.Holder<net.minecraft.world.level.biome.Biome> biome =
                event.getLevel().getBiome(vanilla.blockPosition());
        java.util.List<String> adultCandidates = FarmLegacyIds.ALL.stream()
                .filter(id -> id.startsWith(femalePrefix))
                .filter(id -> FarmSpawnBiomeModifier.matchesConfiguredBiome(id, biome))
                .toList();
        if (vanilla instanceof MushroomCow) {
            adultCandidates = adultCandidates.stream().filter(id -> id.endsWith("_mooshroom")).toList();
        }
        if (adultCandidates.isEmpty()) return;
        String female = adultCandidates.get(event.getLevel().getRandom().nextInt(adultCandidates.size()));
        String breed = female.substring(femalePrefix.length());
        String selected = baby ? childPrefix + breed
                : event.getLevel().getRandom().nextBoolean() ? femalePrefix + breed : malePrefix + breed;
        EntityType<?> type = ENTITIES.get(selected).get();
        if (!(type.create(event.getLevel()) instanceof AnimaniaAnimalEntity replacement)) return;
        replacement.moveTo(vanilla.getX(), vanilla.getY(), vanilla.getZ(), vanilla.getYRot(), vanilla.getXRot());
        // EntityJoinLevelEvent fires after the source UUID has entered the
        // section manager. Reusing it makes addFreshEntity reject the
        // replacement as a duplicate and defeats the transactional hand-off.
        replacement.setCustomName(vanilla.getCustomName());
        replacement.setCustomNameVisible(vanilla.isCustomNameVisible());
        replacement.setPersistenceRequired();
        if (baby) replacement.setAge(-AnimaniaAnimalEntity.childGrowthDuration());
        else replacement.setAge(0);
        if (!baby && family.equals("cow") && replacement.getGender() == AnimalGender.FEMALE
                && configured(FarmConfig.COWS_MILKABLE_AT_SPAWN)) replacement.setMilkReady(true);
        replacement.getPersistentData().putBoolean("AnimaniaReplacedVanilla", true);
        boolean added = event.getLevel().addFreshEntity(replacement);
        LOGGER.debug("Farm natural replacement {} ({}) -> {} ({}) added={}",
                vanilla.getUUID(), vanilla.getType(), replacement.getUUID(), replacement.getType(), added);
        if (added) event.setCanceled(true);
    }

    private static void markNaturalVanillaFarmAnimal(MobSpawnEvent.FinalizeSpawn event) {
        if (event.getSpawnType() != MobSpawnType.NATURAL && event.getSpawnType() != MobSpawnType.CHUNK_GENERATION) return;
        Entity entity = event.getEntity();
        if (entity instanceof Cow || entity instanceof Pig || entity instanceof Chicken
                || entity instanceof Sheep || entity instanceof Horse) {
            entity.getPersistentData().putBoolean("AnimaniaNaturalFarmSpawn", true);
        }
    }

    /** Apply addon-only legacy toggles without coupling Base to Farm config classes. */
    private static void farmAnimalTick(LivingEvent.LivingTickEvent event) {
        if (!(event.getEntity() instanceof AnimaniaAnimalEntity animal) || animal.level().isClientSide) return;
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(animal.getType());
        if (id == null || !MOD_ID.equals(id.getNamespace())) return;
        String path = id.getPath();
        if (path.startsWith("hen_")) animal.tryLayFarmEgg(configured(FarmConfig.CHICKENS_DROP_EGGS));
        if (path.startsWith("rooster_")) animal.configureRoosterCombat(configured(FarmConfig.ROOSTERS_FIGHT));
    }

    private static void boostRiddenPig(net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickItem event) {
        if (!event.getItemStack().is(net.minecraft.world.item.Items.CARROT_ON_A_STICK)
                || !(event.getEntity().getVehicle() instanceof AnimaniaAnimalEntity animal)
                || !animal.isFarmPig() || !animal.boost()) return;
        if (!event.getEntity().level().isClientSide && !event.getEntity().getAbilities().instabuild) {
            event.getItemStack().hurtAndBreak(1, event.getEntity(), player -> player.broadcastBreakEvent(event.getHand()));
        }
        event.setCancellationResult(net.minecraft.world.InteractionResult.sidedSuccess(event.getEntity().level().isClientSide));
        event.setCanceled(true);
    }

    /**
     * Forge 1.20.1 no longer exposes the 1.12 tree-decoration event.  A
     * full-chunk data hook provides the same once-per-chunk semantics without
     * touching vanilla worldgen registries: the marker is persisted in the
     * chunk data, so unloading/reloading cannot duplicate wild hives.
     */
    private static void decorateHiveOnChunkLoad(ChunkDataEvent.Load event) {
        if (!(event.getLevel() instanceof net.minecraft.server.level.ServerLevel level)
                || event.getStatus() != ChunkStatus.ChunkType.LEVELCHUNK
                || !configured(FarmConfig.HIVE_SPAWNING)
                || event.getData().getBoolean("AnimaniaHiveDecorated")) return;
        event.getData().putBoolean("AnimaniaHiveDecorated", true);
        // ChunkDataEvent.Load may run while a generation worker owns the
        // chunk lock.  Only enqueue immutable coordinates here; all level
        // reads/writes happen during the server tick after publication.
        PENDING_HIVES.add(new PendingHive(level, event.getChunk().getPos()));
    }

    private static void processHiveQueue(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        for (int count = 0; count < 8; count++) {
            PendingHive pending = PENDING_HIVES.poll();
            if (pending == null) return;
            placeWildHive(pending.level(), pending.chunk());
        }
    }

    private static void placeWildHive(net.minecraft.server.level.ServerLevel level, ChunkPos chunk) {
        if (!configured(FarmConfig.HIVE_SPAWNING)) return;
        int frequency = Math.max(0, Math.min(10, configured(FarmConfig.HIVE_SPAWNING_FREQUENCY, 3)));
        if (frequency == 0 || level.random.nextInt(200) >= frequency) return;
        if (!level.getChunkSource().hasChunk(chunk.x, chunk.z)) return;
        net.minecraft.world.level.chunk.LevelChunk loaded = level.getChunkSource().getChunkNow(chunk.x, chunk.z);
        if (loaded == null) return;
        int x = chunk.getMinBlockX() + level.random.nextInt(16);
        int z = chunk.getMinBlockZ() + level.random.nextInt(16);
        // Read the heightmap owned by the event's already-loaded chunk.  A
        // level.getHeight call here would synchronously request the same
        // chunk while the server is preparing its spawn region.
        int top = loaded.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x & 15, z & 15);
        // Search a short vertical band for a safe air pocket on a tree or
        // other sturdy surface, matching the old tree decorator's intent.
        for (int offset = 0; offset <= 6; offset++) {
            BlockPos candidate = new BlockPos(x, top + offset, z);
            if (!level.isEmptyBlock(candidate) || !level.getBlockState(candidate.below()).isFaceSturdy(level, candidate.below(), net.minecraft.core.Direction.UP)) continue;
            if (!FarmSpawnBiomeModifier.matchesConfiguredBiome("hive", level.getBiome(candidate))) continue;
            level.setBlock(candidate, FarmContent.WILD_HIVE.get().defaultBlockState(), 3);
            break;
        }
    }

    private static boolean configured(net.minecraftforge.common.ForgeConfigSpec.BooleanValue value) {
        try {
            return value.get();
        } catch (IllegalStateException ignored) {
            return true;
        }
    }

    private static int configured(net.minecraftforge.common.ForgeConfigSpec.IntValue value, int fallback) {
        try {
            return value.get();
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    private static boolean familySpawnsEnabled(String id) {
        if (id.startsWith("cow_") || id.startsWith("bull_") || id.startsWith("calf_")) return configured(FarmConfig.SPAWN_ANIMANIA_COWS);
        if (id.startsWith("sow_") || id.startsWith("hog_") || id.startsWith("piglet_")) return configured(FarmConfig.SPAWN_ANIMANIA_PIGS);
        if (id.startsWith("hen_") || id.startsWith("rooster_") || id.startsWith("chick_")) return configured(FarmConfig.SPAWN_ANIMANIA_CHICKENS);
        if (id.startsWith("ewe_") || id.startsWith("ram_") || id.startsWith("lamb_")) return configured(FarmConfig.SPAWN_ANIMANIA_SHEEP);
        if (id.startsWith("mare_") || id.startsWith("stallion_") || id.startsWith("foal_")) return configured(FarmConfig.SPAWN_ANIMANIA_HORSES);
        if (id.startsWith("doe_") || id.startsWith("buck_") || id.startsWith("kid_")) return configured(FarmConfig.SPAWN_ANIMANIA_GOATS);
        return true;
    }

    private void registerGameTests(RegisterGameTestsEvent event) {
        event.register(com.animania.farm.gametest.AnimaniaFarmGameTests.class);
    }

    private static AnimalGender gender(String id) {
        if (id.startsWith("chick_") || id.startsWith("calf_") || id.startsWith("kid_") || id.startsWith("lamb_")
                || id.startsWith("piglet_") || id.startsWith("foal_")) return AnimalGender.CHILD;
        if (id.startsWith("hen_") || id.startsWith("cow_") || id.startsWith("doe_") || id.startsWith("ewe_") || id.startsWith("sow_") || id.startsWith("mare_")) return AnimalGender.FEMALE;
        return AnimalGender.MALE;
    }

    private static String family(String id) {
        int underscore = id.indexOf('_');
        return underscore > 0 ? id.substring(underscore + 1) : id;
    }

    private static String spawnFamily(String id) {
        if (id.startsWith("cow_") || id.startsWith("bull_") || id.startsWith("calf_")) return "cow";
        if (id.startsWith("sow_") || id.startsWith("hog_") || id.startsWith("piglet_")) return "pig";
        if (id.startsWith("hen_") || id.startsWith("rooster_") || id.startsWith("chick_")) return "chicken";
        if (id.startsWith("mare_") || id.startsWith("stallion_") || id.startsWith("foal_")) return "horse";
        if (id.startsWith("doe_") || id.startsWith("buck_") || id.startsWith("kid_")) return "goat";
        if (id.startsWith("ewe_") || id.startsWith("ram_") || id.startsWith("lamb_")) return "sheep";
        return id;
    }

    private static AnimaniaSleepProfiles.Profile sleepProfile(String id) {
        String family = id.startsWith("hen_") || id.startsWith("rooster_") || id.startsWith("chick_") ? "chicken"
                : id.startsWith("cow_") || id.startsWith("bull_") || id.startsWith("calf_") ? "cow"
                : id.startsWith("doe_") || id.startsWith("buck_") || id.startsWith("kid_") ? "goat"
                : id.startsWith("stallion_") || id.startsWith("mare_") || id.startsWith("foal_") ? "horse"
                : id.startsWith("sow_") || id.startsWith("hog_") || id.startsWith("piglet_") ? "pig"
                : id.startsWith("ewe_") || id.startsWith("ram_") || id.startsWith("lamb_") ? "sheep" : null;
        if (family == null) return null;
        return new AnimaniaSleepProfiles.Profile(
                () -> configured(FarmConfig.BED_BLOCKS.get(family + "Bed")),
                () -> configured(FarmConfig.BED_BLOCKS.get(family + "Bed2")), AnimaniaSleepProfiles.NIGHT);
    }

    private static String configured(net.minecraftforge.common.ForgeConfigSpec.ConfigValue<String> value) {
        try { return value.get(); } catch (IllegalStateException ignored) { return value.getDefault(); }
    }

}
