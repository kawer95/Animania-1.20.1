package com.animania.client.render;

import com.animania.client.model.LegacyAnimalModel;
import com.animania.client.model.LegacyAnimationProfile;
import com.animania.client.model.LegacyPoseDefinition;
import com.animania.client.model.LegacyRenderTransform;
import com.animania.client.model.LegacyPetAnimationDefinition;
import com.animania.client.AnimaniaClientDiagnostics;
import com.animania.common.entity.AnimaniaAnimalEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;

/** Client-only renderer using native ModelPart animation and legacy-compatible IDs. */
public class AnimaniaAnimalRenderer extends MobRenderer<AnimaniaAnimalEntity, LegacyAnimalModel> {
    private final float modelScale;
    private final LegacyRenderTransform renderTransform;

    public AnimaniaAnimalRenderer(EntityRendererProvider.Context context, ModelLayerLocation layer,
                                  LegacyAnimationProfile profile, float modelScale) {
        this(context, layer, profile, LegacyPoseDefinition.EMPTY, LegacyPetAnimationDefinition.EMPTY,
                LegacyRenderTransform.EMPTY, modelScale);
    }

    public AnimaniaAnimalRenderer(EntityRendererProvider.Context context, ModelLayerLocation layer,
                                  LegacyAnimationProfile profile, LegacyPoseDefinition sittingPose,
                                  float modelScale) {
        this(context, layer, profile, sittingPose, LegacyPetAnimationDefinition.EMPTY,
                LegacyRenderTransform.EMPTY, modelScale);
    }

    public AnimaniaAnimalRenderer(EntityRendererProvider.Context context, ModelLayerLocation layer,
                                  LegacyAnimationProfile profile, LegacyPoseDefinition sittingPose,
                                  LegacyRenderTransform renderTransform, float modelScale) {
        this(context, layer, profile, sittingPose, LegacyPetAnimationDefinition.EMPTY, renderTransform, modelScale);
    }

    public AnimaniaAnimalRenderer(EntityRendererProvider.Context context, ModelLayerLocation layer,
                                  LegacyAnimationProfile profile, LegacyPoseDefinition sittingPose,
                                  LegacyPetAnimationDefinition petAnimation,
                                  LegacyRenderTransform renderTransform, float modelScale) {
        super(context, new LegacyAnimalModel(context.bakeLayer(layer), profile, sittingPose, petAnimation), 0.45f);
        this.modelScale = modelScale;
        this.renderTransform = renderTransform;
        addLayer(new AnimaniaHamsterBallLayer(this, context.bakeLayer(com.animania.client.model.AnimaniaHamsterBallModel.LAYER)));
        addLayer(new AnimaniaBlinkingLayer(this));
        addLayer(new AnimaniaMudLayer(this));
        addLayer(new AnimaniaMooshroomLayer(this, context.getBlockRenderDispatcher()));
    }

    @Override
    public ResourceLocation getTextureLocation(AnimaniaAnimalEntity entity) {
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        ResourceLocation defaultTexture = new ResourceLocation("animania", "textures/entity/default.png");
        if (id == null) {
            AnimaniaClientDiagnostics.textureResolution(defaultTexture, defaultTexture, defaultTexture, "unregistered_entity");
            return defaultTexture;
        }
        ResourceLocation resolved = LegacyAnimalTextures.resolve(id, entity);
        // The preserved 1.12 tree uses nested family directories, while some
        // modern addon packs intentionally keep an ID-named fallback at the
        // entity root.  Never hand the renderer a missing location: that is
        // the direct cause of the purple/black checkerboard seen for a bad
        // variant or an old save carrying an obsolete variant string.
        if (Minecraft.getInstance().getResourceManager().getResource(resolved).isPresent()) {
            AnimaniaClientDiagnostics.textureResolution(id, resolved, resolved, "requested");
            return resolved;
        }
        ResourceLocation flat = new ResourceLocation(id.getNamespace(),
                "textures/entity/" + id.getPath() + ".png");
        if (Minecraft.getInstance().getResourceManager().getResource(flat).isPresent()) {
            AnimaniaClientDiagnostics.textureResolution(id, resolved, flat, "flat_fallback");
            return flat;
        }
        if (Minecraft.getInstance().getResourceManager().getResource(defaultTexture).isPresent()) {
            AnimaniaClientDiagnostics.textureResolution(id, resolved, defaultTexture, "default_fallback");
            return defaultTexture;
        }
        AnimaniaClientDiagnostics.textureResolution(id, resolved, defaultTexture, "default_missing");
        return defaultTexture;
    }

    @Override
    protected void scale(AnimaniaAnimalEntity entity, PoseStack poseStack, float partialTickTime) {
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        boolean pets = id != null && "animania_catsdogs".equals(id.getNamespace());
        String path = id == null ? "" : id.getPath();
        boolean cat = path.startsWith("queen_") || path.startsWith("tom_") || path.startsWith("kitten_");
        boolean fox = path.endsWith("_fox");
        boolean child = path.startsWith("puppy_") || path.startsWith("kitten_");

        // RenderDogGeneric applies its factory translation before scaling;
        // RenderFox applies the 0.1 Y translation at the very end instead.
        if (!fox) poseStack.translate(renderTransform.x(), renderTransform.y(), renderTransform.z());
        float scale = child ? modelScale * (1.0F + 0.8F * entity.growthProgress()) : modelScale;
        poseStack.scale(scale, scale, scale);

        if (pets && entity.isSleeping()) {
            if (cat) {
                poseStack.translate(-0.25F, entity.getBbHeight() - 1.45F, -0.25F);
                poseStack.mulPose(Axis.ZP.rotationDegrees(6.0F));
                poseStack.translate(0.0F, 0.6F, 0.0F);
                if (child) poseStack.translate(0.0F, 0.4F, 0.0F);
            } else if (fox) {
                poseStack.translate(-0.25F, entity.getBbHeight() - 0.9F, -0.25F);
                poseStack.mulPose(Axis.ZP.rotationDegrees(6.0F));
                poseStack.translate(0.0F, -0.3F, 0.0F);
            } else {
                poseStack.translate(0.0F, -0.1F, 0.0F);
            }
        }
        if (!pets && entity.isSleeping()) applyLegacySleepingTransform(path, entity, poseStack);
        if (fox) poseStack.translate(renderTransform.x(), renderTransform.y(), renderTransform.z());
    }

    /** Root transforms copied from the 1.12 addon renderers. */
    private static void applyLegacySleepingTransform(String path, AnimaniaAnimalEntity entity, PoseStack poseStack) {
        float timer = Math.max(-0.55F, Math.min(0.0F, entity.getSleepTimer()));
        float height = entity.getBbHeight();

        if (path.startsWith("bull_") || path.startsWith("cow_")) {
            poseStack.translate(-0.25F, height - 1.85F - timer, -0.25F);
            poseStack.mulPose(Axis.ZP.rotationDegrees(6.0F));
            return;
        }
        if (path.startsWith("calf_")) {
            poseStack.translate(-0.25F, height - 1.15F - timer, -0.25F);
            poseStack.mulPose(Axis.ZP.rotationDegrees(6.0F));
            return;
        }
        if (path.startsWith("ewe_") || path.startsWith("ram_")) {
            float offset = path.equals("ewe_dorper") ? 0.85F : 1.05F;
            poseStack.translate(-0.25F, height - offset - timer, -0.25F);
            poseStack.mulPose(Axis.ZP.rotationDegrees(6.0F));
            return;
        }
        if (path.startsWith("lamb_")) {
            poseStack.translate(-0.25F, height - 0.45F - timer, -0.25F);
            poseStack.mulPose(Axis.ZP.rotationDegrees(6.0F));
            return;
        }
        if (isGoatPath(path)) {
            float offset = path.startsWith("kid_") ? 0.50F
                    : (path.endsWith("_kiko") || path.endsWith("_pygmy") || path.endsWith("_fainting") ? 1.10F : 1.45F);
            poseStack.translate(-0.25F, height - offset - timer, -0.25F);
            poseStack.mulPose(Axis.ZP.rotationDegrees(6.0F));
            return;
        }
        if (path.startsWith("foal_")) {
            poseStack.translate(-0.25F, height - 1.25F - timer, -0.25F);
            poseStack.mulPose(Axis.ZP.rotationDegrees(6.0F));
            return;
        }
        if (path.startsWith("mare_") || path.startsWith("stallion_")) {
            poseStack.translate(-0.25F, height - 1.95F - timer, -0.25F);
            poseStack.mulPose(Axis.ZP.rotationDegrees(6.0F));
            return;
        }
        if (path.startsWith("hog_") || path.startsWith("sow_")) {
            poseStack.translate(0.0F, height - 1.25F, 0.0F);
            poseStack.mulPose(Axis.ZP.rotationDegrees(86.0F));
            return;
        }
        if (path.startsWith("piglet_")) {
            poseStack.translate(0.0F, height - 0.70F, 0.0F);
            poseStack.mulPose(Axis.ZP.rotationDegrees(86.0F));
            return;
        }
        if (path.startsWith("chick_")) {
            poseStack.translate(-0.25F, 0.10F, -0.25F);
            return;
        }
        if (path.startsWith("hen_") || path.startsWith("rooster_")) {
            poseStack.translate(-0.25F, 0.35F, -0.25F);
            return;
        }
        if (isRabbitPath(path)) {
            poseStack.translate(-0.25F, path.startsWith("kit_") ? 0.10F : 0.25F, -0.25F);
            return;
        }
        if (path.startsWith("peacock_")) {
            poseStack.translate(-0.25F, 0.45F, -0.45F);
            return;
        }
        if (path.startsWith("peachick_")) {
            poseStack.translate(-0.25F, 0.35F, -0.45F);
            return;
        }
        if (path.startsWith("peahen_")) {
            poseStack.translate(-0.25F, 0.45F, -0.45F);
            return;
        }
        if (path.equals("ferret_grey") || path.equals("ferret_white")) {
            poseStack.translate(0.0F, 0.20F, 0.0F);
            poseStack.mulPose(Axis.ZP.rotationDegrees(path.endsWith("_white") ? -10.0F : 10.0F));
            return;
        }
        if (path.equals("hamster")) {
            poseStack.translate(0.0F, 0.15F, 0.0F);
            poseStack.mulPose(Axis.ZP.rotationDegrees(20.0F));
            return;
        }
        if (path.startsWith("hedgehog")) poseStack.translate(0.0F, 0.15F, 0.0F);
    }

    private static boolean isGoatPath(String path) {
        if (!(path.startsWith("buck_") || path.startsWith("doe_") || path.startsWith("kid_"))) return false;
        return path.endsWith("_alpine") || path.endsWith("_angora") || path.endsWith("_fainting")
                || path.endsWith("_kiko") || path.endsWith("_kinder") || path.endsWith("_nigerian_dwarf")
                || path.endsWith("_pygmy");
    }

    private static boolean isRabbitPath(String path) {
        return path.startsWith("kit_")
                || ((path.startsWith("buck_") || path.startsWith("doe_")) && !isGoatPath(path));
    }
}
