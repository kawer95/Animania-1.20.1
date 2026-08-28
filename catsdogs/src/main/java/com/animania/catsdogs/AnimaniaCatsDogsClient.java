package com.animania.catsdogs;

import com.animania.client.render.AnimaniaAnimalRenderer;
import com.animania.client.AnimaniaClientDiagnostics;
import com.animania.catsdogs.client.model.CatsDogsLegacyModelLayers;
import com.animania.catsdogs.client.model.CatsDogsNativeModelLayers;
import com.animania.catsdogs.client.model.CatsDogsPetBowlModel;
import com.animania.catsdogs.client.render.CatsDogsPetBowlRenderer;
import com.animania.catsdogs.client.render.CatsDogsPetFacilityRenderer;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import com.animania.common.entity.AnimaniaAnimalEntity;
import net.minecraft.world.entity.EntityType;

final class AnimaniaCatsDogsClient {
    private AnimaniaCatsDogsClient() { }
    static void registerConfigScreen() {
        net.minecraftforge.fml.ModLoadingContext.get().registerExtensionPoint(
                net.minecraftforge.client.ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new net.minecraftforge.client.ConfigScreenHandler.ConfigScreenFactory((minecraft, parent) ->
                        new com.animania.client.config.AnimaniaConfigScreen(parent,
                                net.minecraft.network.chat.Component.translatable("screen.animania_catsdogs.config.title"),
                                AnimaniaCatsDogs.MOD_ID, CatsDogsConfig.SPEC)));
    }
    static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        CatsDogsLegacyModelLayers.LAYERS.forEach((id, layer) -> event.registerLayerDefinition(layer, () -> CatsDogsLegacyModelLayers.create(id)));
        CatsDogsNativeModelLayers.LAYERS.forEach((id, layer) -> event.registerLayerDefinition(layer, () -> CatsDogsNativeModelLayers.create(id)));
        event.registerLayerDefinition(CatsDogsPetBowlModel.LAYER, CatsDogsPetBowlModel::create);
        AnimaniaClientDiagnostics.layerDefinitions(AnimaniaCatsDogs.MOD_ID, CatsDogsLegacyModelLayers.LAYERS.size(), CatsDogsNativeModelLayers.LAYERS.size());
    }
    static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(CatsDogsContent.PET_BOWL_BE.get(), CatsDogsPetBowlRenderer::new);
        event.registerBlockEntityRenderer(CatsDogsContent.PET_PROP_BE.get(), CatsDogsPetFacilityRenderer::new);
    }
    static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> AnimaniaCatsDogs.ENTITIES.forEach((id, type) -> EntityRenderers.register(
                (EntityType<AnimaniaAnimalEntity>) type.get(),
                context -> new AnimaniaAnimalRenderer(context,
                        AnimaniaClientDiagnostics.requireLayer(AnimaniaCatsDogs.MOD_ID, id, CatsDogsLegacyModelLayers.LAYERS.get(id)),
                        CatsDogsLegacyModelLayers.profile(id), CatsDogsLegacyModelLayers.sittingPose(id),
                        CatsDogsLegacyModelLayers.petAnimation(id),
                        CatsDogsLegacyModelLayers.transform(id), CatsDogsLegacyModelLayers.scale(id)))));
        AnimaniaClientDiagnostics.rendererRegistrations(AnimaniaCatsDogs.MOD_ID, AnimaniaCatsDogs.ENTITIES.size(), 0);
    }
}
