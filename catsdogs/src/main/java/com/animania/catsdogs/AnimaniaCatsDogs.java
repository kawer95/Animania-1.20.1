package com.animania.catsdogs;

import com.animania.api.AnimaniaApi;
import com.animania.api.data.AnimalGender;
import com.animania.api.data.SpeciesDefinition;
import com.animania.common.entity.AnimaniaAnimalEntity;
import com.animania.common.entity.AnimaniaSleepProfiles;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Ocelot;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.entity.SpawnPlacementRegisterEvent;
import net.minecraftforge.event.RegisterGameTestsEvent;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.phys.AABB;

import java.util.LinkedHashMap;
import java.util.Map;

@Mod(AnimaniaCatsDogs.MOD_ID)
public final class AnimaniaCatsDogs {
    public static final String MOD_ID = "animania_catsdogs";
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, MOD_ID);
    public static final Map<String, RegistryObject<EntityType<?>>> ENTITIES = new LinkedHashMap<>();

    static { CatsDogsLegacyIds.ALL.forEach(AnimaniaCatsDogs::register); }

    private static void register(String id) {
        RegistryObject<EntityType<?>> registered = ENTITY_TYPES.register(id,
                () -> EntityType.Builder.of(AnimaniaAnimalEntity::new, MobCategory.CREATURE)
                        .sized(sizeFor(id, true), sizeFor(id, false)).clientTrackingRange(8).updateInterval(3)
                        .build(MOD_ID + ":" + id));
        ENTITIES.put(id, registered);
        AnimaniaApi.registerSpecies(new SpeciesDefinition(new ResourceLocation(MOD_ID, id), family(id), gender(id), sizeFor(id, true), sizeFor(id, false), 20000));
    }

    public AnimaniaCatsDogs() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        ENTITY_TYPES.register(bus);
        CatsDogsWorldgen.BIOME_MODIFIER_SERIALIZERS.register(bus);
        CatsDogsContent.ITEMS.register(bus);
        CatsDogsContent.BLOCKS.register(bus);
        CatsDogsContent.BLOCK_ENTITIES.register(bus);
        CatsDogsPetSeller.POI_TYPES.register(bus);
        CatsDogsPetSeller.PROFESSIONS.register(bus);
        CatsDogsTab.TABS.register(bus);
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, CatsDogsConfig.SPEC);
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                ModLoadingContext.get().registerExtensionPoint(
                        net.minecraftforge.client.ConfigScreenHandler.ConfigScreenFactory.class,
                        () -> new net.minecraftforge.client.ConfigScreenHandler.ConfigScreenFactory((minecraft, parent) ->
                                new com.animania.client.config.AnimaniaConfigScreen(parent,
                                        net.minecraft.network.chat.Component.translatable("screen.animania_catsdogs.config.title"),
                                        MOD_ID, CatsDogsConfig.SPEC))));
        AnimaniaSleepProfiles.register(MOD_ID, AnimaniaCatsDogs::sleepProfile);
        AnimaniaApi.registerFoodMatcher(MOD_ID, (id, stack) -> {
            String path = id.getPath();
            boolean cat = path.startsWith("queen_") || path.startsWith("tom_") || path.startsWith("kitten_");
            return cat ? CatsDogsConfig.matchesCatFood(stack) : CatsDogsConfig.matchesDogFood(stack);
        });
        bus.addListener(this::attributes);
        bus.addListener(this::spawnPlacements);
        bus.addListener(this::registerGameTests);
        bus.addListener(this::commonSetup);
        bus.addListener(this::gatherData);
        MinecraftForge.EVENT_BUS.addListener(AnimaniaCatsDogs::replaceVanillaCompanion);
        MinecraftForge.EVENT_BUS.addListener(AnimaniaCatsDogs::limitNaturalCompanionSpawns);
        MinecraftForge.EVENT_BUS.register(CatsDogsPetSeller.class);
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> bus.addListener(AnimaniaCatsDogsClient::onClientSetup));
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> bus.addListener(AnimaniaCatsDogsClient::registerLayers));
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> bus.addListener(AnimaniaCatsDogsClient::registerRenderers));
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> CatsDogsContent.ITEM_ENTRIES.values().forEach(entry -> {
            if (entry.get() instanceof com.animania.common.item.AnimaniaEntityEggItem egg) {
                com.animania.common.item.AnimaniaEntityEggItem.registerDispenserBehavior(egg);
            }
        }));
    }

    private void gatherData(GatherDataEvent event) {
        event.getGenerator().addProvider(event.includeServer(),
                new CatsDogsDataProvider(event.getGenerator().getPackOutput()));
    }

    private void attributes(EntityAttributeCreationEvent event) {
        ENTITIES.forEach((id, type) -> {
            var attributes = AnimaniaAnimalEntity.createAttributes()
                    .add(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH,
                            id.startsWith("tom_") || id.startsWith("male_") ? 20.0D
                                    : id.startsWith("kitten_") || id.startsWith("puppy_") ? 12.0D : 18.0D)
                    .add(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED,
                            id.startsWith("kitten_") || id.startsWith("puppy_") ? 0.315D : 0.3D)
                    .add(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE, 2.5D);
            event.put((EntityType<? extends LivingEntity>) type.get(), attributes.build());
        });
    }

    private void spawnPlacements(SpawnPlacementRegisterEvent event) {
        // Spawn placement registration can precede Forge common-config loading in
        // GameTest/dev startup, so use the default until the value is available.
        if (!spawnsEnabled()) return;
        ENTITIES.values().forEach(type -> event.register((EntityType<? extends AnimaniaAnimalEntity>) type.get(), SpawnPlacements.Type.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, AnimaniaAnimalEntity::checkAnimalSpawnRules, SpawnPlacementRegisterEvent.Operation.OR));
    }

    private static boolean spawnsEnabled() {
        try {
            return CatsDogsConfig.ENABLE_SPAWNS.get();
        } catch (IllegalStateException ignored) {
            return true;
        }
    }

    public static void limitNaturalCompanionSpawns(MobSpawnEvent.PositionCheck event) {
        if (!(event.getEntity() instanceof AnimaniaAnimalEntity animal)
                || (event.getSpawnType() != MobSpawnType.NATURAL && event.getSpawnType() != MobSpawnType.CHUNK_GENERATION)) return;
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(animal.getType());
        if (id == null || !MOD_ID.equals(id.getNamespace())) return;
        boolean cat = isCat(id.getPath());
        int limit = configured(cat ? CatsDogsConfig.SPAWN_LIMIT_CATS : CatsDogsConfig.SPAWN_LIMIT_DOGS, 20);
        AABB range = new AABB(event.getX(), event.getY(), event.getZ(), event.getX(), event.getY(), event.getZ()).inflate(100.0D);
        int nearby = event.getLevel().getLevel().getEntitiesOfClass(AnimaniaAnimalEntity.class, range, other -> {
            ResourceLocation otherId = ForgeRegistries.ENTITY_TYPES.getKey(other.getType());
            return otherId != null && MOD_ID.equals(otherId.getNamespace()) && cat == isCat(otherId.getPath());
        }).size();
        if (nearby >= limit) event.setResult(Event.Result.DENY);
    }

    private static boolean isCat(String id) {
        return id.startsWith("queen_") || id.startsWith("tom_") || id.startsWith("kitten_");
    }

    private static int configured(net.minecraftforge.common.ForgeConfigSpec.IntValue value, int fallback) {
        try { return value.get(); }
        catch (IllegalStateException ignored) { return fallback; }
    }

    private void registerGameTests(RegisterGameTestsEvent event) {
        event.register(com.animania.catsdogs.gametest.AnimaniaCatsDogsGameTests.class);
    }

    /** Replace vanilla companions at the world boundary while preserving tame state. */
    private static void replaceVanillaCompanion(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) return;
        net.minecraft.world.entity.Entity vanilla = event.getEntity();
        boolean dog = vanilla instanceof Wolf;
        boolean cat = vanilla instanceof Ocelot;
        if ((!dog || !replaceWolves()) && (!cat || !replaceOcelots())) return;
        boolean baby = vanilla instanceof net.minecraft.world.entity.AgeableMob ageable && ageable.isBaby();
        String femalePrefix = dog ? "female_" : "queen_";
        String malePrefix = dog ? "male_" : "tom_";
        String childPrefix = dog ? "puppy_" : "kitten_";
        java.util.List<String> candidates = ENTITIES.keySet().stream()
                .filter(id -> baby ? id.startsWith(childPrefix) : (id.startsWith(femalePrefix) || id.startsWith(malePrefix)))
                .toList();
        if (candidates.isEmpty()) return;
        String selected = candidates.get(event.getLevel().getRandom().nextInt(candidates.size()));
        EntityType<?> registered = ENTITIES.get(selected).get();
        if (!(registered.create(event.getLevel()) instanceof AnimaniaAnimalEntity replacement)) return;
        replacement.moveTo(vanilla.getX(), vanilla.getY(), vanilla.getZ(), vanilla.getYRot(), vanilla.getXRot());
        replacement.setUUID(vanilla.getUUID());
        replacement.setCustomName(vanilla.getCustomName());
        replacement.setCustomNameVisible(vanilla.isCustomNameVisible());
        if (baby) replacement.setAge(-AnimaniaAnimalEntity.childGrowthDuration());
        else replacement.setAge(0);
        if (dog && ((Wolf) vanilla).isTame()) {
            replacement.setTamed(true);
            replacement.setOwnerUUID(((Wolf) vanilla).getOwnerUUID());
            replacement.setSitting(((Wolf) vanilla).isOrderedToSit());
        }
        replacement.setPersistenceRequired();
        if (event.getLevel().addFreshEntity(replacement)) event.setCanceled(true);
    }

    private static boolean replaceWolves() {
        try { return CatsDogsConfig.REPLACE_VANILLA_WOLVES.get(); }
        catch (IllegalStateException ignored) { return true; }
    }

    private static boolean replaceOcelots() {
        try { return CatsDogsConfig.REPLACE_VANILLA_OCELOTS.get(); }
        catch (IllegalStateException ignored) { return true; }
    }

    private static AnimalGender gender(String id) {
        if (id.startsWith("kitten_") || id.startsWith("puppy_")) return AnimalGender.CHILD;
        if (id.startsWith("queen_") || id.startsWith("female_")) return AnimalGender.FEMALE;
        return AnimalGender.MALE;
    }

    private static String family(String id) {
        int underscore = id.indexOf('_');
        return underscore > 0 ? id.substring(underscore + 1) : id;
    }

    private static AnimaniaSleepProfiles.Profile sleepProfile(String id) {
        boolean cat = id.startsWith("queen_") || id.startsWith("tom_") || id.startsWith("kitten_");
        boolean dog = id.startsWith("female_") || id.startsWith("male_") || id.startsWith("puppy_");
        if (!cat && !dog) return null;
        return new AnimaniaSleepProfiles.Profile(
                () -> configured(cat ? CatsDogsConfig.CAT_BED : CatsDogsConfig.DOG_BED),
                () -> configured(cat ? CatsDogsConfig.CAT_BED2 : CatsDogsConfig.DOG_BED2), AnimaniaSleepProfiles.NIGHT);
    }

    private static String configured(net.minecraftforge.common.ForgeConfigSpec.ConfigValue<String> value) {
        try { return value.get(); } catch (IllegalStateException ignored) { return value.getDefault(); }
    }

    private static float sizeFor(String id, boolean width) {
        if (id.startsWith("kitten_") || id.startsWith("puppy_")) return width ? 0.35f : 0.45f;
        return width ? 0.75f : 0.9f;
    }
}
