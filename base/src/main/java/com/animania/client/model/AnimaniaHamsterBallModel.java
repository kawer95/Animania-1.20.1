package com.animania.client.model;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;

/** Exact native reconstruction of 1.12's sixteen-part {@code ModelRendererBall}. */
public final class AnimaniaHamsterBallModel {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
            new ResourceLocation("animania", "hamster_ball"), "main");

    private AnimaniaHamsterBallModel() { }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition ball = mesh.getRoot().addOrReplaceChild(
                "ball", CubeListBuilder.create(), PartPose.ZERO);
        shape(ball, "shape1", -5.001F, 4.001F, -5.002F, 10, 1, 10, 0.0F, 17.0F, 0.0F);
        shape(ball, "shape2", -5.002F, -6.002F, -5.003F, 10, 1, 10, 0.0F, 18.0F, 0.0F);
        shape(ball, "shape3", -5.003F, -5.003F, -5.004F, 1, 8, 10, 0.0F, 18.0F, 0.0F);
        shape(ball, "shape4", 4.01F, -5.004F, -5.005F, 1, 8, 10, 0.0F, 18.0F, 0.0F);
        shape(ball, "shape5", -4.02F, -5.005F, -6.006F, 8, 8, 2, 0.0F, 18.0F, 0.0F);
        shape(ball, "shape6", -3.01F, -4.002F, 6.007F, 6, 6, 1, 0.0F, 18.0F, 0.0F);
        shape(ball, "shape7", 5.01F, -5.006F, -4.003F, 1, 8, 8, 0.0F, 18.0F, 0.0F);
        shape(ball, "shape8", -6.01F, -5.007F, -4.005F, 1, 8, 8, 0.0F, 18.0F, 0.0F);
        shape(ball, "shape9", -4.01F, -7.004F, -4.009F, 8, 1, 8, 0.0F, 18.008F, 0.0F);
        shape(ball, "shape10", -4.01F, 5.009F, -4.010F, 8, 1, 8, 0.0F, 17.0F, 0.0F);
        shape(ball, "shape11", -3.01F, -4.01F, -7.011F, 6, 6, 1, 0.0F, 18.0F, 0.0F);
        shape(ball, "shape12", -7.01F, -4.002F, -3.012F, 1, 6, 6, 0.0F, 18.0F, 0.0F);
        shape(ball, "shape13", -4.01F, -5.011F, 4.013F, 8, 8, 2, 0.0F, 18.0F, 0.0F);
        shape(ball, "shape14", 6.01F, -4.012F, -3.014F, 1, 6, 6, 0.0F, 18.0F, 0.0F);
        shape(ball, "shape15", -3.02F, -8.002F, -3.015F, 6, 1, 6, 0.0F, 18.0F, 0.0F);
        shape(ball, "shape16", -3.03F, 6.002F, -3.016F, 6, 1, 6, 0.0F, 17.0F, 0.0F);
        return LayerDefinition.create(mesh, 64, 32);
    }

    private static void shape(PartDefinition parent, String name,
                              float x, float y, float z, int width, int height, int depth,
                              float pivotX, float pivotY, float pivotZ) {
        parent.addOrReplaceChild(name,
                CubeListBuilder.create().texOffs(0, 0).mirror()
                        .addBox(x, y, z, width, height, depth),
                PartPose.offset(pivotX, pivotY, pivotZ));
    }
}
