package com.animania.client.render;

import com.animania.common.entity.AnimaniaAnimalEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;

/** Native 1.20.1 replacement for the legacy CarryRenderer. */
public final class AnimaniaCarryRenderer {
    private AnimaniaCarryRenderer() { }

    public static void register() {
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(AnimaniaCarryRenderer.class);
    }

    @SubscribeEvent
    public static void renderPlayer(RenderPlayerEvent.Post event) {
        render(event.getEntity(), AnimaniaCarryClientState.get(event.getEntity().getUUID()),
                event.getPoseStack(), event.getMultiBufferSource(), event.getPackedLight(), event.getPartialTick(), false);
    }

    @SubscribeEvent
    public static void renderHand(RenderHandEvent event) {
        Player player = Minecraft.getInstance().player;
        if (player == null) return;
        render(player, AnimaniaCarryClientState.get(player.getUUID()), event.getPoseStack(),
                event.getMultiBufferSource(), event.getPackedLight(), event.getPartialTick(), true);
    }

    private static void render(Player player, AnimaniaCarryClientState.CarriedState state, PoseStack pose,
                               MultiBufferSource buffers, int light, float partialTick, boolean firstPerson) {
        if (state == null || player.level() == null) return;
        ResourceLocation id;
        try {
            id = new ResourceLocation(state.type());
        } catch (IllegalArgumentException ignored) {
            return;
        }
        EntityType<?> raw = ForgeRegistries.ENTITY_TYPES.getValue(id);
        if (raw == null) return;
        Entity created = raw.create(player.level());
        if (!(created instanceof AnimaniaAnimalEntity animal)) return;
        animal.readAdditionalSaveData(state.animal());
        animal.tickCount = player.tickCount;
        pose.pushPose();
        if (firstPerson) {
            pose.translate(-0.20D, -0.12D, 0.42D);
            pose.mulPose(Axis.YP.rotationDegrees(180.0F));
            float scale = carryScale(animal) * 0.30F;
            pose.scale(scale, scale, scale);
        } else {
            pose.translate(-0.32D, player.isCrouching() ? 1.05D : 1.35D, 0.0D);
            pose.mulPose(Axis.YP.rotationDegrees(-player.getYRot()));
            float scale = carryScale(animal);
            pose.scale(scale, scale, scale);
        }
        Minecraft.getInstance().getEntityRenderDispatcher().render(animal, 0.0D, 0.0D, 0.0D,
                0.0F, partialTick, pose, buffers, light);
        pose.popPose();
        animal.discard();
    }

    private static float carryScale(AnimaniaAnimalEntity animal) {
        float extent = Math.max(animal.getBbWidth(), animal.getBbHeight());
        return extent <= 0.0F ? 0.65F : Math.min(0.9F, 0.65F / extent);
    }
}
