package com.animania.client.model;

// Generated from the pinned LGPL-3.0 Animania 1.12 Java models; do not edit by hand.
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;

public final class BaseLegacyModelLayers {
    public static final Map<String, ModelLayerLocation> LAYERS = new LinkedHashMap<>();
    static {
        LAYERS.put("salt_lick", new ModelLayerLocation(new ResourceLocation("animania", "legacy/salt_lick"), "main"));
        LAYERS.put("nest", new ModelLayerLocation(new ResourceLocation("animania", "legacy/nest"), "main"));
        LAYERS.put("trough", new ModelLayerLocation(new ResourceLocation("animania", "legacy/trough"), "main"));
        // The 1.12 ModelTrough used a separate 16x16 texture size for the
        // food planes. Keep that coordinate space separate from the 128x64
        // wooden trough shell instead of relying on atlas UV rescaling.
        LAYERS.put("trough_food", new ModelLayerLocation(new ResourceLocation("animania", "legacy/trough_food"), "main"));
        LAYERS.put("water_bottle", new ModelLayerLocation(new ResourceLocation("animania", "legacy/water_bottle"), "main"));
    }
    private BaseLegacyModelLayers() {}
    public static LayerDefinition create(String id) {
        return switch (id) {
            case "salt_lick" -> salt_lick();
            case "nest" -> nest();
            case "trough" -> trough();
            case "trough_food" -> trough_food();
            case "water_bottle" -> water_bottle();
            default -> throw new IllegalArgumentException("Unknown Base legacy model " + id);
        };
    }
    private static LayerDefinition salt_lick() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition block = root.addOrReplaceChild("block", CubeListBuilder.create().texOffs(6, 6).addBox(-5.0F, -5.0F, -5.0F, 10.0F, 10.0F, 10.0F), PartPose.offsetAndRotation(0.0F, 19.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        return LayerDefinition.create(mesh, 64, 32);
    }
    private static LayerDefinition nest() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition nest1 = root.addOrReplaceChild("nest1", CubeListBuilder.create().texOffs(0, 0).addBox(-3.0F, -2.0F, -8.0F, 6.0F, 4.0F, 3.0F), PartPose.offsetAndRotation(0.0F, 22.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition fluff3 = root.addOrReplaceChild("fluff3", CubeListBuilder.create().texOffs(-16, 38).addBox(-8.0F, 0.0F, -8.0F, 16.0F, 0.0F, 16.0F), PartPose.offsetAndRotation(0.0F, 21.5F, 0.0F, 0.0F, 1.133858F, 0.0F));
        PartDefinition fluff1 = root.addOrReplaceChild("fluff1", CubeListBuilder.create().texOffs(-16, 38).addBox(-8.0F, 0.0F, -8.0F, 16.0F, 0.0F, 16.0F), PartPose.offsetAndRotation(0.0F, 22.5F, 0.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition nest2 = root.addOrReplaceChild("nest2", CubeListBuilder.create().texOffs(0, 7).addBox(-3.0F, -2.0F, 5.0F, 6.0F, 4.0F, 3.0F), PartPose.offsetAndRotation(0.0F, 22.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition nest3 = root.addOrReplaceChild("nest3", CubeListBuilder.create().texOffs(0, 14).addBox(-3.0F, -2.0F, 5.0F, 6.0F, 4.0F, 3.0F), PartPose.offsetAndRotation(0.0F, 22.0F, 0.0F, 0.0F, 1.570796F, 0.0F));
        PartDefinition nest4 = root.addOrReplaceChild("nest4", CubeListBuilder.create().texOffs(19, 0).addBox(-3.0F, -2.0F, 5.0F, 6.0F, 4.0F, 3.0F), PartPose.offsetAndRotation(0.0F, 22.0F, 0.0F, 0.0F, -1.570796F, 0.0F));
        PartDefinition nest5 = root.addOrReplaceChild("nest5", CubeListBuilder.create().texOffs(18, 7).addBox(-3.5F, -2.0F, 5.0F, 7.0F, 4.0F, 3.0F), PartPose.offsetAndRotation(0.0F, 21.9F, 0.0F, 0.0F, -0.785398F, 0.0F));
        PartDefinition nest6 = root.addOrReplaceChild("nest6", CubeListBuilder.create().texOffs(18, 14).addBox(-3.5F, -2.0F, 5.0F, 7.0F, 4.0F, 3.0F), PartPose.offsetAndRotation(0.0F, 21.9F, 0.0F, 0.0F, 0.785398F, 0.0F));
        PartDefinition nest7 = root.addOrReplaceChild("nest7", CubeListBuilder.create().texOffs(18, 20).addBox(-3.5F, -2.0F, 5.0F, 7.0F, 4.0F, 3.0F), PartPose.offsetAndRotation(0.0F, 21.9F, 0.0F, 0.0F, 2.356194F, 0.0F));
        PartDefinition nest8 = root.addOrReplaceChild("nest8", CubeListBuilder.create().texOffs(41, 0).addBox(-3.5F, -2.0F, 5.0F, 7.0F, 4.0F, 3.0F), PartPose.offsetAndRotation(0.0F, 21.9F, 0.0F, 0.0F, -2.356194F, 0.0F));
        PartDefinition block = root.addOrReplaceChild("block", CubeListBuilder.create().texOffs(13, 8).addBox(-5.5F, -1.5F, -5.5F, 11.0F, 3.0F, 11.0F), PartPose.offsetAndRotation(0.0F, 22.5F, 0.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition fluff2 = root.addOrReplaceChild("fluff2", CubeListBuilder.create().texOffs(-16, 38).addBox(-8.0F, 0.0F, -8.0F, 16.0F, 0.0F, 16.0F), PartPose.offsetAndRotation(0.0F, 23.8F, 0.0F, 0.0F, 0.590696F, 0.0F));
        PartDefinition fluff4 = root.addOrReplaceChild("fluff4", CubeListBuilder.create().texOffs(18, 38).addBox(-8.0F, 0.0F, -8.0F, 16.0F, 0.0F, 16.0F), PartPose.offsetAndRotation(0.0F, 19.7F, 0.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition fluff5 = root.addOrReplaceChild("fluff5", CubeListBuilder.create().texOffs(18, 38).addBox(-8.0F, 0.0F, -8.0F, 16.0F, 0.0F, 16.0F), PartPose.offsetAndRotation(0.0F, 19.65F, 0.0F, 0.0F, 0.357443F, 0.0F));
        PartDefinition feather1 = root.addOrReplaceChild("feather1", CubeListBuilder.create().texOffs(62, 0).addBox(-6.5F, 0.0F, -20.0F, 13.0F, 0.0F, 40.0F), PartPose.offsetAndRotation(3.0F, 60.0F, 2.0F, 0.280387F, 0.518223F, 0.044849F));
        PartDefinition feather2 = root.addOrReplaceChild("feather2", CubeListBuilder.create().texOffs(62, 0).addBox(-6.6F, 0.0F, -20.0F, 13.0F, 0.0F, 40.0F), PartPose.offsetAndRotation(2.0F, 60.0F, -2.0F, 0.2142F, 2.028859F, -0.122275F));
        PartDefinition feather3 = root.addOrReplaceChild("feather3", CubeListBuilder.create().texOffs(62, 0).addBox(-6.4F, 0.0F, -20.0F, 13.0F, 0.0F, 40.0F), PartPose.offsetAndRotation(-4.0F, 60.75F, 0.0F, 0.405563F, -1.776956F, -0.121615F));
        PartDefinition feather4 = root.addOrReplaceChild("feather4", CubeListBuilder.create().texOffs(62, 0).addBox(-6.5F, 0.01F, -20.0F, 13.0F, 0.0F, 40.0F), PartPose.offsetAndRotation(-1.0F, 60.0F, -4.0F, 0.288018F, -2.569335F, 0.125796F));
        PartDefinition feather5 = root.addOrReplaceChild("feather5", CubeListBuilder.create().texOffs(62, 0).addBox(-6.5F, 0.01F, -20.0F, 13.0F, 0.0F, 40.0F), PartPose.offsetAndRotation(-1.0F, 60.0F, 4.0F, 0.276448F, -0.356893F, 0.177139F));
        PartDefinition egg1 = root.addOrReplaceChild("egg1", CubeListBuilder.create().texOffs(68, 2).addBox(-1.5F, -1.5F, -1.5F, 3.0F, 3.0F, 3.0F), PartPose.offsetAndRotation(-2.5F, 20.5F, 1.0F, -0.240044F, 0.160175F, -0.194144F));
        PartDefinition egg1a = root.addOrReplaceChild("egg1a", CubeListBuilder.create().texOffs(71, 4).addBox(-1.0F, -0.5F, -1.0F, 2.0F, 1.0F, 2.0F), PartPose.offsetAndRotation(-2.768189F, 18.83211F, 1.45686F, -0.240044F, 0.160175F, -0.194144F));
        PartDefinition egg1b = root.addOrReplaceChild("egg1b", CubeListBuilder.create().texOffs(72, 3).addBox(-0.5F, -0.5F, -0.5F, 1.0F, 1.0F, 1.0F), PartPose.offsetAndRotation(-2.845569F, 18.35441F, 1.582717F, -0.240044F, 0.160175F, -0.194144F));
        PartDefinition egg1c = root.addOrReplaceChild("egg1c", CubeListBuilder.create().texOffs(71, 6).addBox(-1.0F, -0.5F, -1.0F, 2.0F, 1.0F, 2.0F), PartPose.offsetAndRotation(-2.270879F, 21.92846F, 0.603732F, -0.240044F, 0.160175F, -0.194144F));
        PartDefinition egg2 = root.addOrReplaceChild("egg2", CubeListBuilder.create().texOffs(73, 4).addBox(-1.5F, -1.5F, -1.5F, 3.0F, 3.0F, 3.0F), PartPose.offsetAndRotation(0.0F, 20.5F, -2.25F, 0.0F, -0.377763F, -1.277677F));
        PartDefinition egg2a = root.addOrReplaceChild("egg2a", CubeListBuilder.create().texOffs(73, 3).addBox(-1.0F, -0.5F, -1.0F, 2.0F, 1.0F, 2.0F), PartPose.offsetAndRotation(-1.557231F, 19.99435F, -2.867943F, 0.0F, -0.377763F, -1.277677F));
        PartDefinition egg2b = root.addOrReplaceChild("egg2b", CubeListBuilder.create().texOffs(75, 6).addBox(-0.5F, -0.5F, -0.5F, 1.0F, 1.0F, 1.0F), PartPose.offsetAndRotation(-2.000357F, 19.84989F, -3.049029F, 0.0F, -0.377763F, -1.277677F));
        PartDefinition egg2c = root.addOrReplaceChild("egg2c", CubeListBuilder.create().texOffs(78, 2).addBox(-1.0F, -0.5F, -1.0F, 2.0F, 1.0F, 2.0F), PartPose.offsetAndRotation(1.336568F, 20.93341F, -1.724865F, 0.0F, -0.377763F, -1.277677F));
        PartDefinition egg3 = root.addOrReplaceChild("egg3", CubeListBuilder.create().texOffs(76, 3).addBox(-1.5F, -1.5F, -1.5F, 3.0F, 3.0F, 3.0F), PartPose.offsetAndRotation(2.0F, 20.5F, 2.0F, -0.774313F, 0.288834F, 0.507352F));
        PartDefinition egg3a = root.addOrReplaceChild("egg3a", CubeListBuilder.create().texOffs(77, 6).addBox(-1.0F, -0.5F, -1.0F, 2.0F, 1.0F, 2.0F), PartPose.offsetAndRotation(3.119674F, 19.40652F, 2.783019F, -0.774313F, 0.288834F, 0.507352F));
        PartDefinition egg3b = root.addOrReplaceChild("egg3b", CubeListBuilder.create().texOffs(78, 4).addBox(-0.5F, -0.5F, -0.5F, 1.0F, 1.0F, 1.0F), PartPose.offsetAndRotation(3.438589F, 19.09068F, 3.003399F, -0.774313F, 0.288834F, 0.507352F));
        PartDefinition egg3c = root.addOrReplaceChild("egg3c", CubeListBuilder.create().texOffs(75, 2).addBox(-1.0F, -0.5F, -1.0F, 2.0F, 1.0F, 2.0F), PartPose.offsetAndRotation(1.039286F, 21.43386F, 1.3255F, -0.774313F, 0.288834F, 0.507352F));
        PartDefinition b_egg1 = root.addOrReplaceChild("b_egg1", CubeListBuilder.create().texOffs(68, 22).addBox(-1.5F, -1.5F, -1.5F, 3.0F, 3.0F, 3.0F), PartPose.offsetAndRotation(-2.5F, 20.5F, 1.0F, -0.240044F, 0.160175F, -0.194144F));
        PartDefinition b_egg1a = root.addOrReplaceChild("b_egg1a", CubeListBuilder.create().texOffs(71, 24).addBox(-1.0F, -0.5F, -1.0F, 2.0F, 1.0F, 2.0F), PartPose.offsetAndRotation(-2.768189F, 18.83211F, 1.45686F, -0.240044F, 0.160175F, -0.194144F));
        PartDefinition b_egg1b = root.addOrReplaceChild("b_egg1b", CubeListBuilder.create().texOffs(72, 23).addBox(-0.5F, -0.5F, -0.5F, 1.0F, 1.0F, 1.0F), PartPose.offsetAndRotation(-2.845569F, 18.35441F, 1.582717F, -0.240044F, 0.160175F, -0.194144F));
        PartDefinition b_egg1c = root.addOrReplaceChild("b_egg1c", CubeListBuilder.create().texOffs(71, 26).addBox(-1.0F, -0.5F, -1.0F, 2.0F, 1.0F, 2.0F), PartPose.offsetAndRotation(-2.270879F, 21.92846F, 0.603732F, -0.240044F, 0.160175F, -0.194144F));
        PartDefinition b_egg2 = root.addOrReplaceChild("b_egg2", CubeListBuilder.create().texOffs(73, 24).addBox(-1.5F, -1.5F, -1.5F, 3.0F, 3.0F, 3.0F), PartPose.offsetAndRotation(0.0F, 20.5F, -2.25F, 0.0F, -0.377763F, -1.277677F));
        PartDefinition b_egg2a = root.addOrReplaceChild("b_egg2a", CubeListBuilder.create().texOffs(73, 23).addBox(-1.0F, -0.5F, -1.0F, 2.0F, 1.0F, 2.0F), PartPose.offsetAndRotation(-1.557231F, 19.99435F, -2.867943F, 0.0F, -0.377763F, -1.277677F));
        PartDefinition b_egg2b = root.addOrReplaceChild("b_egg2b", CubeListBuilder.create().texOffs(75, 26).addBox(-0.5F, -0.5F, -0.5F, 1.0F, 1.0F, 1.0F), PartPose.offsetAndRotation(-2.000357F, 19.84989F, -3.049029F, 0.0F, -0.377763F, -1.277677F));
        PartDefinition b_egg2c = root.addOrReplaceChild("b_egg2c", CubeListBuilder.create().texOffs(78, 22).addBox(-1.0F, -0.5F, -1.0F, 2.0F, 1.0F, 2.0F), PartPose.offsetAndRotation(1.336568F, 20.93341F, -1.724865F, 0.0F, -0.377763F, -1.277677F));
        PartDefinition b_egg3 = root.addOrReplaceChild("b_egg3", CubeListBuilder.create().texOffs(76, 23).addBox(-1.5F, -1.5F, -1.5F, 3.0F, 3.0F, 3.0F), PartPose.offsetAndRotation(2.0F, 20.5F, 2.0F, -0.774313F, 0.288834F, 0.507352F));
        PartDefinition b_egg3a = root.addOrReplaceChild("b_egg3a", CubeListBuilder.create().texOffs(77, 26).addBox(-1.0F, -0.5F, -1.0F, 2.0F, 1.0F, 2.0F), PartPose.offsetAndRotation(3.119674F, 19.40652F, 2.783019F, -0.774313F, 0.288834F, 0.507352F));
        PartDefinition b_egg3b = root.addOrReplaceChild("b_egg3b", CubeListBuilder.create().texOffs(78, 24).addBox(-0.5F, -0.5F, -0.5F, 1.0F, 1.0F, 1.0F), PartPose.offsetAndRotation(3.438589F, 19.09068F, 3.003399F, -0.774313F, 0.288834F, 0.507352F));
        PartDefinition b_egg3c = root.addOrReplaceChild("b_egg3c", CubeListBuilder.create().texOffs(75, 22).addBox(-1.0F, -0.5F, -1.0F, 2.0F, 1.0F, 2.0F), PartPose.offsetAndRotation(1.039286F, 21.43386F, 1.3255F, -0.774313F, 0.288834F, 0.507352F));
        PartDefinition bl_egg1 = root.addOrReplaceChild("bl_egg1", CubeListBuilder.create().texOffs(68, 32).addBox(-1.5F, -1.5F, -1.5F, 3.0F, 3.0F, 3.0F), PartPose.offsetAndRotation(-2.5F, 20.5F, 1.0F, -0.240044F, 0.160175F, -0.194144F));
        PartDefinition bl_egg1a = root.addOrReplaceChild("bl_egg1a", CubeListBuilder.create().texOffs(71, 34).addBox(-1.0F, -0.5F, -1.0F, 2.0F, 1.0F, 2.0F), PartPose.offsetAndRotation(-2.768189F, 18.83211F, 1.45686F, -0.240044F, 0.160175F, -0.194144F));
        PartDefinition bl_egg1b = root.addOrReplaceChild("bl_egg1b", CubeListBuilder.create().texOffs(72, 33).addBox(-0.5F, -0.5F, -0.5F, 1.0F, 1.0F, 1.0F), PartPose.offsetAndRotation(-2.845569F, 18.35441F, 1.582717F, -0.240044F, 0.160175F, -0.194144F));
        PartDefinition bl_egg1c = root.addOrReplaceChild("bl_egg1c", CubeListBuilder.create().texOffs(71, 36).addBox(-1.0F, -0.5F, -1.0F, 2.0F, 1.0F, 2.0F), PartPose.offsetAndRotation(-2.270879F, 21.92846F, 0.603732F, -0.240044F, 0.160175F, -0.194144F));
        PartDefinition bl_egg2 = root.addOrReplaceChild("bl_egg2", CubeListBuilder.create().texOffs(73, 34).addBox(-1.5F, -1.5F, -1.5F, 3.0F, 3.0F, 3.0F), PartPose.offsetAndRotation(0.0F, 20.5F, -2.25F, 0.0F, -0.377763F, -1.277677F));
        PartDefinition bl_egg2a = root.addOrReplaceChild("bl_egg2a", CubeListBuilder.create().texOffs(73, 33).addBox(-1.0F, -0.5F, -1.0F, 2.0F, 1.0F, 2.0F), PartPose.offsetAndRotation(-1.557231F, 19.99435F, -2.867943F, 0.0F, -0.377763F, -1.277677F));
        PartDefinition bl_egg2b = root.addOrReplaceChild("bl_egg2b", CubeListBuilder.create().texOffs(75, 36).addBox(-0.5F, -0.5F, -0.5F, 1.0F, 1.0F, 1.0F), PartPose.offsetAndRotation(-2.000357F, 19.84989F, -3.049029F, 0.0F, -0.377763F, -1.277677F));
        PartDefinition bl_egg2c = root.addOrReplaceChild("bl_egg2c", CubeListBuilder.create().texOffs(78, 32).addBox(-1.0F, -0.5F, -1.0F, 2.0F, 1.0F, 2.0F), PartPose.offsetAndRotation(1.336568F, 20.93341F, -1.724865F, 0.0F, -0.377763F, -1.277677F));
        PartDefinition bl_egg3 = root.addOrReplaceChild("bl_egg3", CubeListBuilder.create().texOffs(76, 33).addBox(-1.5F, -1.5F, -1.5F, 3.0F, 3.0F, 3.0F), PartPose.offsetAndRotation(2.0F, 20.5F, 2.0F, -0.774313F, 0.288834F, 0.507352F));
        PartDefinition bl_egg3a = root.addOrReplaceChild("bl_egg3a", CubeListBuilder.create().texOffs(77, 36).addBox(-1.0F, -0.5F, -1.0F, 2.0F, 1.0F, 2.0F), PartPose.offsetAndRotation(3.119674F, 19.40652F, 2.783019F, -0.774313F, 0.288834F, 0.507352F));
        PartDefinition bl_egg3b = root.addOrReplaceChild("bl_egg3b", CubeListBuilder.create().texOffs(78, 34).addBox(-0.5F, -0.5F, -0.5F, 1.0F, 1.0F, 1.0F), PartPose.offsetAndRotation(3.438589F, 19.09068F, 3.003399F, -0.774313F, 0.288834F, 0.507352F));
        PartDefinition bl_egg3c = root.addOrReplaceChild("bl_egg3c", CubeListBuilder.create().texOffs(75, 32).addBox(-1.0F, -0.5F, -1.0F, 2.0F, 1.0F, 2.0F), PartPose.offsetAndRotation(1.039286F, 21.43386F, 1.3255F, -0.774313F, 0.288834F, 0.507352F));
        PartDefinition w_egg1 = root.addOrReplaceChild("w_egg1", CubeListBuilder.create().texOffs(68, 42).addBox(-1.5F, -1.5F, -1.5F, 3.0F, 3.0F, 3.0F), PartPose.offsetAndRotation(-2.5F, 20.5F, 1.0F, -0.240044F, 0.160175F, -0.194144F));
        PartDefinition w_egg1a = root.addOrReplaceChild("w_egg1a", CubeListBuilder.create().texOffs(71, 44).addBox(-1.0F, -0.5F, -1.0F, 2.0F, 1.0F, 2.0F), PartPose.offsetAndRotation(-2.768189F, 18.83211F, 1.45686F, -0.240044F, 0.160175F, -0.194144F));
        PartDefinition w_egg1b = root.addOrReplaceChild("w_egg1b", CubeListBuilder.create().texOffs(72, 43).addBox(-0.5F, -0.5F, -0.5F, 1.0F, 1.0F, 1.0F), PartPose.offsetAndRotation(-2.845569F, 18.35441F, 1.582717F, -0.240044F, 0.160175F, -0.194144F));
        PartDefinition w_egg1c = root.addOrReplaceChild("w_egg1c", CubeListBuilder.create().texOffs(71, 46).addBox(-1.0F, -0.5F, -1.0F, 2.0F, 1.0F, 2.0F), PartPose.offsetAndRotation(-2.270879F, 21.92846F, 0.603732F, -0.240044F, 0.160175F, -0.194144F));
        PartDefinition w_egg2 = root.addOrReplaceChild("w_egg2", CubeListBuilder.create().texOffs(73, 44).addBox(-1.5F, -1.5F, -1.5F, 3.0F, 3.0F, 3.0F), PartPose.offsetAndRotation(0.0F, 20.5F, -2.25F, 0.0F, -0.377763F, -1.277677F));
        PartDefinition w_egg2a = root.addOrReplaceChild("w_egg2a", CubeListBuilder.create().texOffs(73, 43).addBox(-1.0F, -0.5F, -1.0F, 2.0F, 1.0F, 2.0F), PartPose.offsetAndRotation(-1.557231F, 19.99435F, -2.867943F, 0.0F, -0.377763F, -1.277677F));
        PartDefinition w_egg2b = root.addOrReplaceChild("w_egg2b", CubeListBuilder.create().texOffs(75, 46).addBox(-0.5F, -0.5F, -0.5F, 1.0F, 1.0F, 1.0F), PartPose.offsetAndRotation(-2.000357F, 19.84989F, -3.049029F, 0.0F, -0.377763F, -1.277677F));
        PartDefinition w_egg2c = root.addOrReplaceChild("w_egg2c", CubeListBuilder.create().texOffs(78, 42).addBox(-1.0F, -0.5F, -1.0F, 2.0F, 1.0F, 2.0F), PartPose.offsetAndRotation(1.336568F, 20.93341F, -1.724865F, 0.0F, -0.377763F, -1.277677F));
        PartDefinition w_egg3 = root.addOrReplaceChild("w_egg3", CubeListBuilder.create().texOffs(76, 43).addBox(-1.5F, -1.5F, -1.5F, 3.0F, 3.0F, 3.0F), PartPose.offsetAndRotation(2.0F, 20.5F, 2.0F, -0.774313F, 0.288834F, 0.507352F));
        PartDefinition w_egg3a = root.addOrReplaceChild("w_egg3a", CubeListBuilder.create().texOffs(77, 46).addBox(-1.0F, -0.5F, -1.0F, 2.0F, 1.0F, 2.0F), PartPose.offsetAndRotation(3.119674F, 19.40652F, 2.783019F, -0.774313F, 0.288834F, 0.507352F));
        PartDefinition w_egg3b = root.addOrReplaceChild("w_egg3b", CubeListBuilder.create().texOffs(78, 44).addBox(-0.5F, -0.5F, -0.5F, 1.0F, 1.0F, 1.0F), PartPose.offsetAndRotation(3.438589F, 19.09068F, 3.003399F, -0.774313F, 0.288834F, 0.507352F));
        PartDefinition w_egg3c = root.addOrReplaceChild("w_egg3c", CubeListBuilder.create().texOffs(75, 42).addBox(-1.0F, -0.5F, -1.0F, 2.0F, 1.0F, 2.0F), PartPose.offsetAndRotation(1.039286F, 21.43386F, 1.3255F, -0.774313F, 0.288834F, 0.507352F));
        return LayerDefinition.create(mesh, 128, 64);
    }
    private static LayerDefinition trough() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition block1 = root.addOrReplaceChild("block1", CubeListBuilder.create().texOffs(2, 2).addBox(-1.0F, -5.0F, -6.0F, 2.0F, 10.0F, 12.0F), PartPose.offsetAndRotation(-7.0F, 17.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition base2 = root.addOrReplaceChild("base2", CubeListBuilder.create().texOffs(4, 4).addBox(-1.0F, -1.0F, -5.0F, 2.0F, 2.0F, 10.0F), PartPose.offsetAndRotation(22.0F, 23.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition base1 = root.addOrReplaceChild("base1", CubeListBuilder.create().texOffs(4, 4).addBox(-1.0F, -1.0F, -5.0F, 2.0F, 2.0F, 10.0F), PartPose.offsetAndRotation(-6.0F, 23.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition block2 = root.addOrReplaceChild("block2", CubeListBuilder.create().texOffs(2, 2).addBox(-1.0F, -5.0F, -6.0F, 2.0F, 10.0F, 12.0F), PartPose.offsetAndRotation(23.0F, 17.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition block3 = root.addOrReplaceChild("block3", CubeListBuilder.create().texOffs(1, 26).addBox(-14.0F, -4.0F, -1.0F, 28.0F, 8.0F, 2.0F), PartPose.offsetAndRotation(8.0F, 18.0F, -5.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition block4 = root.addOrReplaceChild("block4", CubeListBuilder.create().texOffs(1, 26).addBox(-14.0F, -4.0F, -1.0F, 28.0F, 8.0F, 2.0F), PartPose.offsetAndRotation(8.0F, 18.0F, 5.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition block5 = root.addOrReplaceChild("block5", CubeListBuilder.create().texOffs(3, 42).addBox(-14.0F, -0.5F, -4.0F, 28.0F, 1.0F, 8.0F), PartPose.offsetAndRotation(8.0F, 21.5F, 0.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition feed = root.addOrReplaceChild("feed", CubeListBuilder.create().texOffs(56, 1).addBox(-14.0F, -0.5F, -4.0F, 28.0F, 1.0F, 8.0F), PartPose.offsetAndRotation(8.0F, 16.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition feed_a = root.addOrReplaceChild("feed_a", CubeListBuilder.create().texOffs(0, 0).addBox(-4.5F, 0.0F, -5.0F, 16.0F, 0.0F, 16.0F), PartPose.offsetAndRotation(-1.0F, 15.0F, 2.5F, 0.556656F, 0.693478F, 0.418777F));
        PartDefinition feed_b = root.addOrReplaceChild("feed_b", CubeListBuilder.create().texOffs(0, 0).addBox(-4.5F, 0.0F, -5.0F, 16.0F, 0.0F, 16.0F), PartPose.offsetAndRotation(8.0F, 15.0F, 1.000001F, 0.332123F, 1.126472F, 0.456871F));
        PartDefinition feed_c = root.addOrReplaceChild("feed_c", CubeListBuilder.create().texOffs(0, 0).addBox(-4.5F, 0.0F, -5.0F, 16.0F, 0.0F, 16.0F), PartPose.offsetAndRotation(17.0F, 15.0F, 2.500001F, 0.556138F, 0.694848F, 0.419501F));
        PartDefinition feed_d = root.addOrReplaceChild("feed_d", CubeListBuilder.create().texOffs(0, 0).addBox(-4.5F, 0.0F, -5.0F, 16.0F, 0.0F, 16.0F), PartPose.offsetAndRotation(14.0F, 15.0F, -1.999999F, 0.329293F, -2.02634F, 0.499879F));
        PartDefinition feed_e = root.addOrReplaceChild("feed_e", CubeListBuilder.create().texOffs(0, 0).addBox(-4.5F, 0.0F, -5.0F, 16.0F, 0.0F, 16.0F), PartPose.offsetAndRotation(2.0F, 15.0F, -0.999999F, 0.329293F, -2.02634F, 0.341495F));
        PartDefinition feed_f = root.addOrReplaceChild("feed_f", CubeListBuilder.create().texOffs(0, 0).addBox(-4.5F, 0.0F, -5.0F, 16.0F, 0.0F, 16.0F), PartPose.offsetAndRotation(9.000001F, 14.0F, -1.999999F, 0.223693F, -1.865655F, 0.009323F));
        PartDefinition feed_g = root.addOrReplaceChild("feed_g", CubeListBuilder.create().texOffs(0, 0).addBox(-4.5F, 0.0F, -5.0F, 16.0F, 0.0F, 16.0F), PartPose.offsetAndRotation(0.0F, 14.0F, -0.999999F, 0.202956F, -1.465355F, 0.095173F));
        PartDefinition feed_h = root.addOrReplaceChild("feed_h", CubeListBuilder.create().texOffs(0, 0).addBox(-4.5F, 0.0F, -5.0F, 16.0F, 0.0F, 16.0F), PartPose.offsetAndRotation(17.0F, 14.0F, -0.999999F, 0.286131F, 2.831252F, 0.411317F));
        PartDefinition feed_a1 = root.addOrReplaceChild("feed_a1", CubeListBuilder.create().texOffs(0, 0).addBox(-4.5F, 0.0F, -5.0F, 16.0F, 0.0F, 16.0F), PartPose.offsetAndRotation(-1.0F, 17.0F, 2.5F, 0.556656F, 0.693478F, 0.418777F));
        PartDefinition feed_b1 = root.addOrReplaceChild("feed_b1", CubeListBuilder.create().texOffs(0, 0).addBox(-4.5F, 0.0F, -5.0F, 16.0F, 0.0F, 16.0F), PartPose.offsetAndRotation(8.0F, 17.0F, 1.000001F, 0.332123F, 1.126472F, 0.456871F));
        PartDefinition feed_c1 = root.addOrReplaceChild("feed_c1", CubeListBuilder.create().texOffs(0, 0).addBox(-4.5F, 0.0F, -5.0F, 16.0F, 0.0F, 16.0F), PartPose.offsetAndRotation(17.0F, 17.0F, 2.500001F, 0.556138F, 0.694848F, 0.419501F));
        PartDefinition feed_d1 = root.addOrReplaceChild("feed_d1", CubeListBuilder.create().texOffs(0, 0).addBox(-4.5F, 0.0F, -5.0F, 16.0F, 0.0F, 16.0F), PartPose.offsetAndRotation(14.0F, 17.0F, -1.999999F, 0.329293F, -2.02634F, 0.499879F));
        PartDefinition feed_e1 = root.addOrReplaceChild("feed_e1", CubeListBuilder.create().texOffs(0, 0).addBox(-4.5F, 0.0F, -5.0F, 16.0F, 0.0F, 16.0F), PartPose.offsetAndRotation(2.0F, 17.0F, -0.999999F, 0.329293F, -2.02634F, 0.341495F));
        PartDefinition feed_f1 = root.addOrReplaceChild("feed_f1", CubeListBuilder.create().texOffs(0, 0).addBox(-4.5F, 0.0F, -5.0F, 16.0F, 0.0F, 16.0F), PartPose.offsetAndRotation(9.000001F, 16.0F, -1.999999F, 0.223693F, -1.865655F, 0.009323F));
        PartDefinition feed_g1 = root.addOrReplaceChild("feed_g1", CubeListBuilder.create().texOffs(0, 0).addBox(-4.5F, 0.0F, -5.0F, 16.0F, 0.0F, 16.0F), PartPose.offsetAndRotation(0.0F, 16.0F, -0.999999F, 0.202956F, -1.465355F, 0.095173F));
        PartDefinition feed_h1 = root.addOrReplaceChild("feed_h1", CubeListBuilder.create().texOffs(0, 0).addBox(-4.5F, 0.0F, -5.0F, 16.0F, 0.0F, 16.0F), PartPose.offsetAndRotation(17.0F, 16.0F, -0.999999F, 0.286131F, 2.831252F, 0.411317F));
        PartDefinition slop1 = root.addOrReplaceChild("slop1", CubeListBuilder.create().texOffs(56, 12).addBox(-14.0F, -0.5F, -4.0F, 28.0F, 1.0F, 8.0F), PartPose.offsetAndRotation(8.0F, 16.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition slop2 = root.addOrReplaceChild("slop2", CubeListBuilder.create().texOffs(56, 12).addBox(-14.0F, -0.5F, -4.0F, 28.0F, 1.0F, 8.0F), PartPose.offsetAndRotation(8.0F, 18.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition slop3 = root.addOrReplaceChild("slop3", CubeListBuilder.create().texOffs(56, 12).addBox(-14.0F, -0.5F, -4.0F, 28.0F, 1.0F, 8.0F), PartPose.offsetAndRotation(8.0F, 20.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition water2 = root.addOrReplaceChild("water2", CubeListBuilder.create().texOffs(56, 54).addBox(-14.0F, -0.5F, -4.0F, 28.0F, 1.0F, 8.0F), PartPose.offsetAndRotation(8.0F, 18.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition water3 = root.addOrReplaceChild("water3", CubeListBuilder.create().texOffs(56, 54).addBox(-14.0F, -0.5F, -4.0F, 28.0F, 1.0F, 8.0F), PartPose.offsetAndRotation(8.0F, 20.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition water1 = root.addOrReplaceChild("water1", CubeListBuilder.create().texOffs(0, 0).addBox(-14.0F, -0.5F, -4.0F, 28.0F, 1.0F, 8.0F), PartPose.offsetAndRotation(8.0F, 16.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        return LayerDefinition.create(mesh, 128, 64);
    }

    /**
     * Food planes from ModelTrough. In the 1.12 model these parts called
     * {@code setTextureSize(16, 16)} even though the shell used 128x64. A
     * separate baked layer preserves the full item sprite instead of sampling
     * only the upper-left fraction of an atlas entry.
     */
    private static LayerDefinition trough_food() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("feed_a", CubeListBuilder.create().texOffs(0, 0).addBox(-4.5F, 0.0F, -5.0F, 16.0F, 0.0F, 16.0F), PartPose.offsetAndRotation(-1.0F, 15.0F, 2.5F, 0.556656F, 0.693478F, 0.418777F));
        root.addOrReplaceChild("feed_b", CubeListBuilder.create().texOffs(0, 0).addBox(-4.5F, 0.0F, -5.0F, 16.0F, 0.0F, 16.0F), PartPose.offsetAndRotation(8.0F, 15.0F, 1.000001F, 0.332123F, 1.126472F, 0.456871F));
        root.addOrReplaceChild("feed_c", CubeListBuilder.create().texOffs(0, 0).addBox(-4.5F, 0.0F, -5.0F, 16.0F, 0.0F, 16.0F), PartPose.offsetAndRotation(17.0F, 15.0F, 2.500001F, 0.556138F, 0.694848F, 0.419501F));
        root.addOrReplaceChild("feed_d", CubeListBuilder.create().texOffs(0, 0).addBox(-4.5F, 0.0F, -5.0F, 16.0F, 0.0F, 16.0F), PartPose.offsetAndRotation(14.0F, 15.0F, -1.999999F, 0.329293F, -2.02634F, 0.499879F));
        root.addOrReplaceChild("feed_e", CubeListBuilder.create().texOffs(0, 0).addBox(-4.5F, 0.0F, -5.0F, 16.0F, 0.0F, 16.0F), PartPose.offsetAndRotation(2.0F, 15.0F, -0.999999F, 0.329293F, -2.02634F, 0.341495F));
        root.addOrReplaceChild("feed_f", CubeListBuilder.create().texOffs(0, 0).addBox(-4.5F, 0.0F, -5.0F, 16.0F, 0.0F, 16.0F), PartPose.offsetAndRotation(9.000001F, 14.0F, -1.999999F, 0.223693F, -1.865655F, 0.009323F));
        root.addOrReplaceChild("feed_g", CubeListBuilder.create().texOffs(0, 0).addBox(-4.5F, 0.0F, -5.0F, 16.0F, 0.0F, 16.0F), PartPose.offsetAndRotation(0.0F, 14.0F, -0.999999F, 0.202956F, -1.465355F, 0.095173F));
        root.addOrReplaceChild("feed_h", CubeListBuilder.create().texOffs(0, 0).addBox(-4.5F, 0.0F, -5.0F, 16.0F, 0.0F, 16.0F), PartPose.offsetAndRotation(17.0F, 14.0F, -0.999999F, 0.286131F, 2.831252F, 0.411317F));
        root.addOrReplaceChild("feed_a1", CubeListBuilder.create().texOffs(0, 0).addBox(-4.5F, 0.0F, -5.0F, 16.0F, 0.0F, 16.0F), PartPose.offsetAndRotation(-1.0F, 17.0F, 2.5F, 0.556656F, 0.693478F, 0.418777F));
        root.addOrReplaceChild("feed_b1", CubeListBuilder.create().texOffs(0, 0).addBox(-4.5F, 0.0F, -5.0F, 16.0F, 0.0F, 16.0F), PartPose.offsetAndRotation(8.0F, 17.0F, 1.000001F, 0.332123F, 1.126472F, 0.456871F));
        root.addOrReplaceChild("feed_c1", CubeListBuilder.create().texOffs(0, 0).addBox(-4.5F, 0.0F, -5.0F, 16.0F, 0.0F, 16.0F), PartPose.offsetAndRotation(17.0F, 17.0F, 2.500001F, 0.556138F, 0.694848F, 0.419501F));
        root.addOrReplaceChild("feed_d1", CubeListBuilder.create().texOffs(0, 0).addBox(-4.5F, 0.0F, -5.0F, 16.0F, 0.0F, 16.0F), PartPose.offsetAndRotation(14.0F, 17.0F, -1.999999F, 0.329293F, -2.02634F, 0.499879F));
        root.addOrReplaceChild("feed_e1", CubeListBuilder.create().texOffs(0, 0).addBox(-4.5F, 0.0F, -5.0F, 16.0F, 0.0F, 16.0F), PartPose.offsetAndRotation(2.0F, 17.0F, -0.999999F, 0.329293F, -2.02634F, 0.341495F));
        root.addOrReplaceChild("feed_f1", CubeListBuilder.create().texOffs(0, 0).addBox(-4.5F, 0.0F, -5.0F, 16.0F, 0.0F, 16.0F), PartPose.offsetAndRotation(9.000001F, 16.0F, -1.999999F, 0.223693F, -1.865655F, 0.009323F));
        root.addOrReplaceChild("feed_g1", CubeListBuilder.create().texOffs(0, 0).addBox(-4.5F, 0.0F, -5.0F, 16.0F, 0.0F, 16.0F), PartPose.offsetAndRotation(0.0F, 16.0F, -0.999999F, 0.202956F, -1.465355F, 0.095173F));
        root.addOrReplaceChild("feed_h1", CubeListBuilder.create().texOffs(0, 0).addBox(-4.5F, 0.0F, -5.0F, 16.0F, 0.0F, 16.0F), PartPose.offsetAndRotation(17.0F, 16.0F, -0.999999F, 0.286131F, 2.831252F, 0.411317F));
        return LayerDefinition.create(mesh, 16, 16);
    }

    private static LayerDefinition water_bottle() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition shape1 = root.addOrReplaceChild("shape1", CubeListBuilder.create().mirror().texOffs(22, 0).addBox(0.0F, -2.5F, -0.5F, 1.0F, 5.0F, 1.0F), PartPose.offsetAndRotation(-0.5F, 20.0F, 4.0F, -0.785398F, 0.0F, 0.0F));
        PartDefinition shape3 = root.addOrReplaceChild("shape3", CubeListBuilder.create().mirror().texOffs(0, 16).addBox(0.0F, 0.0F, 0.0F, 3.0F, 2.0F, 3.0F), PartPose.offsetAndRotation(-1.5F, 17.0F, 4.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition shape4 = root.addOrReplaceChild("shape4", CubeListBuilder.create().mirror().texOffs(33, 0).addBox(0.0F, 0.0F, 0.0F, 4.0F, 9.0F, 4.0F), PartPose.offsetAndRotation(-2.0F, 7.5F, 3.5F, 0.0F, 0.0F, 0.0F));
        PartDefinition shape2 = root.addOrReplaceChild("shape2", CubeListBuilder.create().mirror().texOffs(0, 0).addBox(0.0F, 0.0F, 0.0F, 5.0F, 10.0F, 5.0F), PartPose.offsetAndRotation(-2.5F, 7.0F, 3.0F, 0.0F, 0.0F, 0.0F));
        return LayerDefinition.create(mesh, 64, 32);
    }
}
