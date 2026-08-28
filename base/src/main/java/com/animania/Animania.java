package com.animania;

import com.animania.common.AnimaniaBlocks;
import com.animania.common.AnimaniaItems;
import com.animania.common.AnimaniaFluids;
import com.animania.common.AnimaniaTabs;
import com.animania.common.AnimaniaSounds;
import com.animania.common.recipe.AnimaniaRecipes;
import com.animania.common.config.AnimaniaConfig;
import com.animania.common.advancement.FeedAnimalTrigger;
import com.animania.network.AnimaniaNetwork;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.event.RegisterGameTestsEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

/** Entry point for the shared Animania API and content. */
@Mod(Animania.MOD_ID)
public final class Animania {
    public static final String MOD_ID = "animania";
    public static final String VERSION = "3.0.0";

    public Animania() {
        FeedAnimalTrigger.bootstrap();
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        AnimaniaItems.ITEMS.register(modBus);
        AnimaniaFluids.FLUID_TYPES.register(modBus);
        AnimaniaFluids.FLUIDS.register(modBus);
        AnimaniaFluids.BLOCKS.register(modBus);
        AnimaniaBlocks.BLOCKS.register(modBus);
        AnimaniaBlocks.ITEMS.register(modBus);
        AnimaniaBlocks.BLOCK_ENTITIES.register(modBus);
        AnimaniaTabs.TABS.register(modBus);
        AnimaniaSounds.SOUNDS.register(modBus);
        AnimaniaRecipes.SERIALIZERS.register(modBus);
        AnimaniaNetwork.register();
        modBus.addListener(this::registerGameTests);
        modBus.addListener(this::commonSetup);
        // Do not resolve TOP's optional classes unless TOP is actually loaded;
        // datagen and dedicated-server classpaths intentionally omit it.
        if (ModList.get().isLoaded("theoneprobe")) {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.animania.compat.top.AnimaniaTopProbeCompat.bootstrap());
        }
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, AnimaniaConfig.COMMON_SPEC);
        // Keep every Screen/ConfigScreenFactory reference out of the common
        // entrypoint bytecode. Dedicated servers inspect this constructor
        // before DistExecutor can discard a nested client-only lambda.
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> com.animania.client.AnimaniaClient::registerConfigScreen);
        MinecraftForge.EVENT_BUS.register(new AnimaniaServerEvents());
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> modBus.addListener(com.animania.client.AnimaniaClient::registerLayers));
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> modBus.addListener(com.animania.client.AnimaniaClient::registerBlockEntityRenderers));
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> modBus.addListener(com.animania.client.AnimaniaClient::clientSetup));
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> modBus.addListener(com.animania.client.AnimaniaClient::registerItemColors));
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.animania.client.AnimaniaClient.registerCarryRenderer());
        modBus.addListener(this::gatherData);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            com.animania.common.AnimaniaSeedPlacement.registerDispenserBehaviors();
            com.animania.common.item.AnimaniaEntityEggItem.registerDispenserBehavior(
                    (com.animania.common.item.AnimaniaEntityEggItem) AnimaniaItems.ENTITY_EGG_RANDOM.get());
        });
    }

    private void gatherData(GatherDataEvent event) {
        event.getGenerator().addProvider(event.includeServer(), new com.animania.data.AnimaniaDataProvider(event.getGenerator().getPackOutput()));
    }

    private void registerGameTests(RegisterGameTestsEvent event) {
        event.register(com.animania.gametest.AnimaniaBaseGameTests.class);
    }
}
