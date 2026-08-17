package com.animania.extra.client.render;

import com.animania.extra.ExtraHamsterWheelBlockEntity;
import com.animania.extra.ExtraHamsterWheelKinematics;
import com.animania.extra.client.model.ExtraLegacyPropModels;
import com.animania.extra.client.model.ExtraNativeModelLayers;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.animania.extra.ExtraHamsterWheelBlock;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

/** Native animated renderer for the legacy hamster wheel and runner model. */
public final class ExtraHamsterWheelRenderer implements BlockEntityRenderer<ExtraHamsterWheelBlockEntity> {
    private static final ResourceLocation WHEEL_TEXTURE = new ResourceLocation("animania_extra", "textures/entity/tileentities/hamster_wheel.png");
    private final ModelPart wheel;
    private final ModelPart wheelRotor;
    private final Set<ModelPart> rotorParts;
    private final ModelPart hamster;

    public ExtraHamsterWheelRenderer(BlockEntityRendererProvider.Context context) {
        wheel = ExtraLegacyPropModels.create("model_hamster_wheel");
        wheelRotor = wheel.getChild("base1").getChild("wheel1");
        rotorParts = Collections.newSetFromMap(new IdentityHashMap<>());
        wheelRotor.getAllParts().forEach(rotorParts::add);
        hamster = context.bakeLayer(ExtraNativeModelLayers.LAYERS.get("hamster"));
    }

    @Override
    public void render(ExtraHamsterWheelBlockEntity entity, float partialTick, PoseStack pose,
                       MultiBufferSource buffers, int packedLight, int packedOverlay) {
        wheel.getAllParts().forEach(ModelPart::resetPose);
        pose.pushPose();
        pose.translate(0.5D, 1.5D, 0.5D);
        pose.scale(1.0F, -1.0F, -1.0F);
        pose.mulPose(Axis.YP.rotationDegrees(entity.getBlockState().getValue(ExtraHamsterWheelBlock.FACING).toYRot()));
        var wheelBuffer = buffers.getBuffer(RenderType.entityCutoutNoCull(WHEEL_TEXTURE));
        wheelRotor.visible = false;
        wheel.render(pose, wheelBuffer, packedLight, OverlayTexture.NO_OVERLAY);
        wheelRotor.visible = true;

        // CraftStudio animated Wheel1 as a node whose pivot was moved to the
        // centre of the complete ring (0, 13, 0 model pixels). Rendering the
        // child by itself drops Base1's parent transform, which made the ring
        // orbit an offset point. Keep the exact legacy hierarchy and rotate
        // all rotor descendants together around the real axle instead.
        wheel.getAllParts().forEach(part -> part.skipDraw = !rotorParts.contains(part));
        pose.pushPose();
        pose.translate(0.0F, 13.0F / 16.0F, 0.0F);
        float rotorAngle = entity.getLevel() == null ? 0.0F
                : ExtraHamsterWheelKinematics.rotorAngle(entity.getLevel().getGameTime(), partialTick,
                        entity.isRunning());
        pose.mulPose(Axis.ZP.rotation(rotorAngle));
        pose.translate(0.0F, -13.0F / 16.0F, 0.0F);
        wheel.render(pose, wheelBuffer, packedLight, OverlayTexture.NO_OVERLAY);
        pose.popPose();
        wheel.getAllParts().forEach(part -> part.skipDraw = false);
        if (entity.hasHamster()) {
            pose.pushPose();
            pose.scale(0.5F, 0.5F, 0.5F);
            pose.translate(0.0D, 0.9D, 0.0D);
            // Face against the moving lower rim. Keeping the old -90 yaw
            // after the native coordinate conversion made the runner travel
            // visually in the same direction as the belt.
            pose.mulPose(Axis.YP.rotationDegrees(ExtraHamsterWheelKinematics.HAMSTER_YAW_DEGREES));
            ResourceLocation hamsterTexture = new ResourceLocation("animania_extra",
                    "textures/entity/rodents/hamster_" + entity.hamsterVariant() + ".png");
            hamster.render(pose, buffers.getBuffer(RenderType.entityCutout(hamsterTexture)), packedLight, OverlayTexture.NO_OVERLAY);
            pose.popPose();
        }
        pose.popPose();
    }
}
