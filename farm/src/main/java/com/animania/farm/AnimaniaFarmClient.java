package com.animania.farm;

import com.animania.client.render.AnimaniaAnimalRenderer;
import com.animania.client.render.AnimaniaVehicleRenderer;
import com.animania.client.AnimaniaClientDiagnostics;
import com.animania.farm.client.model.FarmLegacyModelLayers;
import com.animania.farm.client.model.FarmNativeModelLayers;
import com.animania.farm.client.model.FarmNativeAnimations;
import com.animania.farm.client.render.FarmHiveRenderer;
import com.animania.common.entity.AnimaniaVehicleEntity;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import com.animania.common.entity.AnimaniaAnimalEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

final class AnimaniaFarmClient {
    private AnimaniaFarmClient() {
    }

    static void registerConfigScreen() {
        net.minecraftforge.fml.ModLoadingContext.get().registerExtensionPoint(
                net.minecraftforge.client.ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new net.minecraftforge.client.ConfigScreenHandler.ConfigScreenFactory((minecraft, parent) ->
                        new com.animania.client.config.AnimaniaConfigScreen(parent,
                                net.minecraft.network.chat.Component.translatable("screen.animania_farm.config.title"),
                                AnimaniaFarm.MOD_ID, FarmConfig.SPEC)));
    }

    static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        FarmLegacyModelLayers.LAYERS.forEach((id, layer) -> event.registerLayerDefinition(layer, () -> FarmLegacyModelLayers.create(id)));
        FarmNativeModelLayers.LAYERS.forEach((id, layer) -> event.registerLayerDefinition(layer, () -> FarmNativeModelLayers.create(id)));
        AnimaniaClientDiagnostics.layerDefinitions(AnimaniaFarm.MOD_ID, FarmLegacyModelLayers.LAYERS.size(), FarmNativeModelLayers.LAYERS.size());
    }

    static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
        ItemProperties.register(FarmContent.ITEM_ENTRIES.get("animania_wool").get(),
                new ResourceLocation(AnimaniaFarm.MOD_ID, "wool_variant"),
                (stack, level, entity, seed) -> (FarmWoolBlockItem.variant(stack).ordinal() + 1) / 10.0F);
        FarmFluids.ALL.values().forEach(fluid -> {
            ItemBlockRenderTypes.setRenderLayer(fluid.source.get(), RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(fluid.flowing.get(), RenderType.translucent());
        });
        AnimaniaFarm.ENTITIES.forEach((id, type) -> {
            if (FarmLegacyIds.isVehicle(id)) {
                String model = id.equals("cart") ? "model_cart_chest" : "model_" + id;
                String animation = id.equals("cart") ? "anim_cart_chest" : "anim_" + id;
                EntityRenderers.register((EntityType<AnimaniaVehicleEntity>) (EntityType<?>) type.get(),
                        context -> new AnimaniaVehicleRenderer(context, AnimaniaClientDiagnostics.requireLayer(AnimaniaFarm.MOD_ID, id, FarmNativeModelLayers.LAYERS.get(model)),
                                FarmNativeAnimations.ALL.get(animation)));
            } else {
                EntityRenderers.register((EntityType<AnimaniaAnimalEntity>) (EntityType<?>) type.get(),
                        context -> new AnimaniaAnimalRenderer(context, AnimaniaClientDiagnostics.requireLayer(AnimaniaFarm.MOD_ID, id, FarmLegacyModelLayers.LAYERS.get(id)), FarmLegacyModelLayers.profile(id), FarmLegacyModelLayers.scale(id)));
            }
        });
        AnimaniaClientDiagnostics.rendererRegistrations(AnimaniaFarm.MOD_ID,
                AnimaniaFarm.ENTITIES.size() - FarmLegacyIds.VEHICLE_IDS.size(), FarmLegacyIds.VEHICLE_IDS.size());
        EntityRenderers.register(FarmContent.BROWN_EGG_PROJECTILE.get(), ThrownItemRenderer::new);
        });
    }

    static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(FarmContent.HIVE_BE.get(), FarmHiveRenderer::new);
        event.registerBlockEntityRenderer(FarmContent.WILD_HIVE_BE.get(), FarmHiveRenderer::new);
    }

    static void registerBlockColors(RegisterColorHandlersEvent.Block event) {
        event.register((state, level, pos, tintIndex) -> {
            if (tintIndex != 0) return 0xFFFFFFFF;
            return level == null || pos == null ? 0x3F76E4 : BiomeColors.getAverageWaterColor(level, pos);
        }, FarmContent.CHEESE_MOLD.get());
    }
}
