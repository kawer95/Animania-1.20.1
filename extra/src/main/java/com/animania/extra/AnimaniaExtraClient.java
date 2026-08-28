package com.animania.extra;

import com.animania.client.render.AnimaniaAnimalRenderer;
import com.animania.client.AnimaniaClientDiagnostics;
import com.animania.extra.client.model.ExtraLegacyModelLayers;
import com.animania.extra.client.model.ExtraNativeModelLayers;
import com.animania.extra.client.render.ExtraHamsterWheelRenderer;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import com.animania.common.entity.AnimaniaAnimalEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.client.gui.screens.MenuScreens;
import com.animania.extra.client.screen.ExtraHamsterWheelScreen;

final class AnimaniaExtraClient {
    private AnimaniaExtraClient() { }
    static void registerConfigScreen() {
        net.minecraftforge.fml.ModLoadingContext.get().registerExtensionPoint(
                net.minecraftforge.client.ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new net.minecraftforge.client.ConfigScreenHandler.ConfigScreenFactory((minecraft, parent) ->
                        new com.animania.client.config.AnimaniaConfigScreen(parent,
                                net.minecraft.network.chat.Component.translatable("screen.animania_extra.config.title"),
                                AnimaniaExtra.MOD_ID, ExtraConfig.SPEC)));
    }
    static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        ExtraLegacyModelLayers.LAYERS.forEach((id, layer) -> event.registerLayerDefinition(layer, () -> ExtraLegacyModelLayers.create(id)));
        ExtraNativeModelLayers.LAYERS.forEach((id, layer) -> event.registerLayerDefinition(layer, () -> ExtraNativeModelLayers.create(id)));
        AnimaniaClientDiagnostics.layerDefinitions(AnimaniaExtra.MOD_ID, ExtraLegacyModelLayers.LAYERS.size(), ExtraNativeModelLayers.LAYERS.size());
    }
    static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            AnimaniaExtra.ENTITIES.forEach((id, type) -> EntityRenderers.register(
                    (EntityType<AnimaniaAnimalEntity>) type.get(),
                    context -> new AnimaniaAnimalRenderer(context, AnimaniaClientDiagnostics.requireLayer(AnimaniaExtra.MOD_ID, id, ExtraLegacyModelLayers.LAYERS.get(id)), ExtraLegacyModelLayers.profile(id), ExtraLegacyModelLayers.scale(id))));
            MenuScreens.register(ExtraContent.HAMSTER_WHEEL_MENU.get(), ExtraHamsterWheelScreen::new);
        });
        AnimaniaClientDiagnostics.rendererRegistrations(AnimaniaExtra.MOD_ID, AnimaniaExtra.ENTITIES.size(), 0);
    }
    static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ExtraContent.HAMSTER_WHEEL_BE.get(), ExtraHamsterWheelRenderer::new);
    }
    static void registerItemColors(RegisterColorHandlersEvent.Item event) {
        event.register((stack, tintIndex) -> stack.getItem() instanceof AnimaniaHamsterBallItem ball
                ? ball.tintColor(stack, tintIndex) : 0xFFFFFFFF,
                ExtraContent.ITEM_ENTRIES.get("hamster_ball_clear").get(),
                ExtraContent.ITEM_ENTRIES.get("hamster_ball_colored").get());
    }
}
