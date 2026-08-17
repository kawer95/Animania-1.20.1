package com.animania.client.render;

import com.animania.client.model.AnimaniaHamsterBallModel;
import com.animania.client.model.LegacyAnimalModel;
import com.animania.common.entity.AnimaniaAnimalEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/** Renders the translucent cage without coupling Base to Extra's Java code. */
public final class AnimaniaHamsterBallLayer extends RenderLayer<AnimaniaAnimalEntity, LegacyAnimalModel> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(
            "animania_extra", "textures/entity/rodents/hamster_ball.png");
    private final ModelPart ball;
    private static final float[][] LEGACY_COLORS = {
            {0.1F, 0.1F, 0.1F}, {1.0F, 0.0F, 0.0F}, {0.4F, 0.49F, 0.2F},
            {0.6F, 0.49F, 0.2F}, {0.2F, 0.5F, 1.0F}, {0.5F, 0.25F, 0.7F},
            {0.28F, 0.50F, 0.6F}, {0.6F, 0.6F, 0.6F}, {0.3F, 0.3F, 0.3F},
            {0.95F, 0.50F, 0.65F}, {0.5F, 0.8F, 0.01F}, {1.0F, 1.0F, 0.0F},
            {0.4F, 0.6F, 0.847F}, {0.7F, 0.3F, 0.85F}, {0.85F, 0.5F, 0.2F},
            {1.0F, 1.0F, 1.0F}
    };

    public AnimaniaHamsterBallLayer(RenderLayerParent<AnimaniaAnimalEntity, LegacyAnimalModel> parent,
                                    ModelPart modelRoot) {
        super(parent);
        this.ball = modelRoot.getChild("ball");
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffers, int packedLight,
                       AnimaniaAnimalEntity entity, float limbSwing, float limbSwingAmount,
                       float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        if (!entity.isHamster() || !entity.isInBall()) return;
        int color = entity.getBallColor();
        float red = 1.0F, green = 1.0F, blue = 1.0F;
        if (color >= 0 && color < LEGACY_COLORS.length) {
            red = LEGACY_COLORS[color][0];
            green = LEGACY_COLORS[color][1];
            blue = LEGACY_COLORS[color][2];
        }
        VertexConsumer consumer = buffers.getBuffer(RenderType.entityTranslucent(TEXTURE));
        poseStack.pushPose();
        // Literal port of ModelRendererBall.render: its GL translations were
        // entity-space units, not model-pixel offsets. Omitting these is what
        // previously placed the cage on top of the hamster's head.
        poseStack.translate(0.0D, 1.0D, 0.0D);
        poseStack.mulPose(Axis.XP.rotationDegrees(((int) limbSwing) * 20.0F));
        poseStack.translate(-0.1D, -1.9D, 0.0D);
        poseStack.scale(1.7F, 1.7F, 1.7F);
        ball.render(poseStack, consumer, packedLight, OverlayTexture.NO_OVERLAY, red, green, blue, 1.0F);
        poseStack.popPose();
    }
}
