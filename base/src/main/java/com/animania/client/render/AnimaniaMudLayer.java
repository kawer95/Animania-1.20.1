package com.animania.client.render;

import com.animania.client.model.LegacyAnimalModel;
import com.animania.common.entity.AnimaniaAnimalEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;

/** One native layer replaces the 18 breed/sex-specific 1.12 pig mud layers. */
public final class AnimaniaMudLayer extends RenderLayer<AnimaniaAnimalEntity, LegacyAnimalModel> {
    private static final ResourceLocation ADULT = new ResourceLocation(
            "animania_farm", "textures/entity/pigs/pig_muddy.png");
    private static final ResourceLocation HAMPSHIRE = new ResourceLocation(
            "animania_farm", "textures/entity/pigs/pig_muddy_hampshire.png");
    private static final ResourceLocation PIGLET = new ResourceLocation(
            "animania_farm", "textures/entity/pigs/piglet_muddy.png");

    public AnimaniaMudLayer(RenderLayerParent<AnimaniaAnimalEntity, LegacyAnimalModel> parent) {
        super(parent);
    }

    public static ResourceLocation textureFor(ResourceLocation entityId) {
        if (entityId == null || !"animania_farm".equals(entityId.getNamespace())) return null;
        String path = entityId.getPath();
        if (path.startsWith("piglet_")) return PIGLET;
        if (!(path.startsWith("sow_") || path.startsWith("hog_"))) return null;
        return path.endsWith("_hampshire") ? HAMPSHIRE : ADULT;
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffers, int packedLight,
                       AnimaniaAnimalEntity entity, float limbSwing, float limbSwingAmount,
                       float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        if (!entity.isMuddy()) return;
        ResourceLocation texture = textureFor(ForgeRegistries.ENTITY_TYPES.getKey(entity.getType()));
        if (texture == null) return;
        poseStack.pushPose();
        poseStack.scale(1.01F, 1.01F, 1.01F);
        VertexConsumer consumer = buffers.getBuffer(RenderType.entityTranslucent(texture));
        getParentModel().renderToBuffer(poseStack, consumer, packedLight, OverlayTexture.NO_OVERLAY,
                1.0F, 1.0F, 1.0F, 1.0F);
        poseStack.popPose();
    }
}
