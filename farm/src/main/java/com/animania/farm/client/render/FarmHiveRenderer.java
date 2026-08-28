package com.animania.farm.client.render;

import com.animania.farm.FarmHiveBlockEntity;
import com.animania.farm.FarmHiveBlock;
import com.animania.farm.client.model.FarmHiveModel;
import com.animania.farm.client.model.FarmNativeAnimations;
import com.animania.farm.client.model.FarmNativeModelLayers;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/** Native renderer for the player and wild hive legacy model models. */
public final class FarmHiveRenderer implements BlockEntityRenderer<FarmHiveBlockEntity> {
    private static final ResourceLocation BEE_HIVE = new ResourceLocation("animania_farm", "textures/entity/props/bee_hive.png");
    private final FarmHiveModel hive;
    private final FarmHiveModel wildHive;

    public FarmHiveRenderer(BlockEntityRendererProvider.Context context) {
        // Use Forge's registered, baked ModelPart layers here. The direct
        // LegacyMeshCube path is useful for topology audits, but its handwritten
        // vertices are not handled consistently by OptiFine/shader buffer paths
        // and can leave an otherwise valid hive completely invisible.
        hive = new FarmHiveModel(
                context.bakeLayer(FarmNativeModelLayers.LAYERS.get("model_bee_hive")),
                FarmNativeAnimations.ALL.get("anim_bees"));
        wildHive = new FarmHiveModel(
                context.bakeLayer(FarmNativeModelLayers.LAYERS.get("model_wild_hive")),
                FarmNativeAnimations.ALL.get("anim_bees_wild"));
    }

    @Override
    public void render(FarmHiveBlockEntity entity, float partialTick, PoseStack pose,
                       MultiBufferSource buffers, int packedLight, int packedOverlay) {
        FarmHiveModel model = entity.isWild() ? wildHive : hive;
        // The 1.12 renderer deliberately used the shared bee-hive atlas for
        // both variants.  The wild-hive item texture has a mostly white
        // canvas and is not the CraftStudio model atlas.
        ResourceLocation texture = BEE_HIVE;
        double animationTicks = entity.getLevel() == null ? 0.0D : entity.getLevel().getGameTime() + partialTick;
        model.applyBeeAnimation((long) (animationTicks * 50.0D));
        pose.pushPose();
        if (entity.isWild()) {
            switch (entity.getBlockState().getValue(FarmHiveBlock.FACING)) {
                case NORTH -> pose.translate(0.5D, 1.0D, 0.75D);
                case SOUTH -> pose.translate(0.5D, 1.0D, 0.25D);
                case EAST -> pose.translate(0.25D, 1.0D, 0.5D);
                case WEST -> pose.translate(0.75D, 1.0D, 0.5D);
                default -> pose.translate(0.5D, 1.0D, 0.5D);
            }
        } else {
            pose.translate(0.5D, 1.5D, 0.5D);
        }
        pose.scale(1.0F, -1.0F, -1.0F);
        pose.mulPose(Axis.YP.rotationDegrees(entity.getBlockState().getValue(FarmHiveBlock.FACING).toYRot()));
        // Bee wings are zero-thickness, so retain a two-sided layer. The actual
        // hive cuboids now use Minecraft's baked geometry instead of a custom
        // vertex implementation.
        model.root().render(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(texture)), packedLight, OverlayTexture.NO_OVERLAY);
        pose.popPose();
    }
}
