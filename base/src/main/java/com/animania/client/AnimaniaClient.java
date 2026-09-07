package com.animania.client;

import com.animania.client.model.AnimaniaAnimalModel;
import com.animania.client.model.AnimaniaVehicleModel;
import com.animania.client.model.BaseNativeModelLayers;
import com.animania.client.model.BaseLegacyModelLayers;
import com.animania.client.model.AnimaniaHamsterBallModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import com.animania.common.AnimaniaBlocks;
import com.animania.common.AnimaniaFluids;
import com.animania.common.item.AnimaniaEntityEggItem;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.registries.ForgeRegistries;

public final class AnimaniaClient {
    public static final ModelLayerLocation ANIMAL_LAYER = new ModelLayerLocation(new ResourceLocation("animania", "animal"), "main");
    public static final ModelLayerLocation VEHICLE_LAYER = new ModelLayerLocation(new ResourceLocation("animania", "vehicle"), "main");

    private AnimaniaClient() {
    }

    /** Register the Mod List screen without leaking Screen into common code. */
    public static void registerConfigScreen() {
        net.minecraftforge.fml.ModLoadingContext.get().registerExtensionPoint(
                net.minecraftforge.client.ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new net.minecraftforge.client.ConfigScreenHandler.ConfigScreenFactory(
                        com.animania.client.config.AnimaniaConfigScreen::new));
    }

    public static void registerLayers(net.minecraftforge.client.event.EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(ANIMAL_LAYER, AnimaniaAnimalModel::createBodyLayer);
        event.registerLayerDefinition(VEHICLE_LAYER, AnimaniaVehicleModel::createBodyLayer);
        event.registerLayerDefinition(AnimaniaHamsterBallModel.LAYER, AnimaniaHamsterBallModel::createBodyLayer);
        BaseNativeModelLayers.LAYERS.forEach((id, layer) ->
                event.registerLayerDefinition(layer, () -> BaseNativeModelLayers.create(id)));
        BaseLegacyModelLayers.LAYERS.forEach((id, layer) ->
                event.registerLayerDefinition(layer, () -> BaseLegacyModelLayers.create(id)));
        AnimaniaClientDiagnostics.layerDefinitions("animania", BaseLegacyModelLayers.LAYERS.size(),
                BaseNativeModelLayers.LAYERS.size() + 3);
    }

    public static void registerBlockEntityRenderers(net.minecraftforge.client.event.EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(com.animania.common.AnimaniaBlocks.TROUGH_BE.get(),
                com.animania.client.render.BaseTroughRenderer::new);
        event.registerBlockEntityRenderer(com.animania.common.AnimaniaBlocks.NEST_BE.get(),
                com.animania.client.render.BaseNestRenderer::new);
        event.registerBlockEntityRenderer(com.animania.common.AnimaniaBlocks.SALT_LICK_BE.get(),
                com.animania.client.render.BaseSaltLickRenderer::new);
        AnimaniaClientDiagnostics.rendererRegistrations("animania:block_entities", 0, 3);
    }

    public static void clientSetup(net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            // The legacy floor piles use transparent pixels in carpet-shaped
            // models. Rendering them as solid turns that transparency black.
            ItemBlockRenderTypes.setRenderLayer(AnimaniaBlocks.STRAW.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(AnimaniaBlocks.SEEDS.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(AnimaniaFluids.SOURCE_SLOP.get(), RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(AnimaniaFluids.FLOWING_SLOP.get(), RenderType.translucent());
        });
    }

    public static void registerCarryRenderer() {
        com.animania.client.render.AnimaniaCarryRenderer.register();
    }

    /** Restores the two tinted egg layers used by every non-random 1.12 animal egg. */
    public static void registerItemColors(RegisterColorHandlersEvent.Item event) {
        event.register((stack, tintIndex) -> tintIndex == 0
                        ? net.minecraft.world.item.alchemy.PotionUtils.getColor(net.minecraft.world.item.alchemy.Potions.WATER)
                        : 0xFFFFFFFF,
                com.animania.common.AnimaniaItems.WATER_BOTTLE.get());
        ForgeRegistries.ITEMS.getValues().stream()
                .filter(AnimaniaEntityEggItem.class::isInstance)
                .map(AnimaniaEntityEggItem.class::cast)
                .forEach(item -> event.register((stack, tintIndex) -> item.tintColor(tintIndex), item));
    }
}
