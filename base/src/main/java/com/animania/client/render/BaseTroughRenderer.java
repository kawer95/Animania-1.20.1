package com.animania.client.render;

import com.animania.client.model.BaseLegacyModelLayers;
import com.animania.common.AnimaniaBlocks;
import com.mojang.blaze3d.platform.NativeImage;
import com.animania.common.block.AnimaniaTroughBlock;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.fluids.FluidStack;
import org.joml.Vector3f;
import org.joml.Vector4f;

/** Native replacement for the legacy trough shell and its server-synchronised contents. */
public final class BaseTroughRenderer implements BlockEntityRenderer<AnimaniaBlocks.TroughEntity> {
    private static final ResourceLocation TROUGH_TEXTURE = new ResourceLocation(
            "animania", "textures/entity/tileentities/block_trough.png");
    private static final ResourceLocation WHEAT_TEXTURE = new ResourceLocation(
            "animania", "textures/entity/tileentities/wheat.png");
    private static final String[] SHELL = {"block1", "block2", "block3", "block4", "block5", "base1", "base2"};
    private static final String[] FOOD_PLANES_FIRST = {
            "feed_a", "feed_b", "feed_c", "feed_d", "feed_e", "feed_f", "feed_g", "feed_h"
    };
    private static final String[] FOOD_PLANES_SECOND = {
            "feed_a1", "feed_b1", "feed_c1", "feed_d1", "feed_e1", "feed_f1", "feed_g1", "feed_h1"
    };

    private final net.minecraft.client.model.geom.ModelPart model;
    private final net.minecraft.client.model.geom.ModelPart foodModel;

    public BaseTroughRenderer(BlockEntityRendererProvider.Context context) {
        model = context.bakeLayer(BaseLegacyModelLayers.LAYERS.get("trough"));
        foodModel = context.bakeLayer(BaseLegacyModelLayers.LAYERS.get("trough_food"));
    }

    @Override
    public void render(AnimaniaBlocks.TroughEntity entity, float partialTick, PoseStack pose,
                       MultiBufferSource buffers, int packedLight, int packedOverlay) {
        pose.pushPose();
        applyTroughTransform(entity, pose);

        // The trough shell is always rendered from its own atlas.  Content is
        // drawn in separate passes so a missing item/fluid can never leave a
        // stale visible ModelPart behind.
        BaseLegacyFacilityRenderSupport.hideAll(model);
        BaseLegacyFacilityRenderSupport.show(model, SHELL);
        BaseLegacyFacilityRenderSupport.render(model, pose, buffers, TROUGH_TEXTURE,
                packedLight, 1, 1, 1, 1);

        ItemStack food = entity.getItem(0);
        FluidStack fluid = entity.fluidSnapshot();
        // TileEntityTrough.update() exposed one mutually-exclusive content
        // state in 1.12.  Keep the same priority here so a transient/stale
        // client packet cannot draw a food tint over a liquid surface.
        switch (entity.content()) {
            case FOOD -> renderFoodContents(entity, pose, buffers, packedLight, food);
            case LIQUID -> renderFluidSurface(pose, buffers, fluid, packedLight);
            case EMPTY -> { }
        }
        BaseLegacyFacilityRenderSupport.hideAll(model);
        BaseLegacyFacilityRenderSupport.hideAll(foodModel);
        pose.popPose();
    }

    private static void applyTroughTransform(AnimaniaBlocks.TroughEntity entity, PoseStack pose) {
        net.minecraft.core.Direction facing = entity.getBlockState().getValue(AnimaniaTroughBlock.FACING);
        switch (facing) {
            case EAST -> pose.translate(1.5D, 1.5D, 0.5D);
            case WEST -> {
                pose.translate(-0.5D, 1.5D, 0.5D);
                pose.mulPose(Axis.YP.rotationDegrees(180));
            }
            case NORTH -> {
                pose.translate(0.5D, 1.5D, -0.5D);
                pose.mulPose(Axis.YP.rotationDegrees(90));
            }
            default -> {
                pose.translate(0.5D, 1.5D, 1.5D);
                pose.mulPose(Axis.YP.rotationDegrees(270));
            }
        }
        pose.scale(-1.0F, -1.0F, 1.0F);
    }

    /**
     * The 1.12 renderer used a fluid atlas sprite rather than the trough atlas.
     * Emit the same horizontal surface directly so water and slop use their
     * registered still textures and the height follows the stored amount.
     */
    private static void renderFluidSurface(PoseStack pose, MultiBufferSource buffers,
                                           FluidStack fluid, int packedLight) {
        IClientFluidTypeExtensions properties = IClientFluidTypeExtensions.of(fluid.getFluid());
        ResourceLocation stillTexture = properties.getStillTexture(fluid);
        if (stillTexture == null) return;
        TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
                .apply(stillTexture);
        int tint = properties.getTintColor(fluid);
        float red = ((tint >> 16) & 0xFF) / 255.0F;
        float green = ((tint >> 8) & 0xFF) / 255.0F;
        float blue = (tint & 0xFF) / 255.0F;
        float alpha = ((tint >>> 24) & 0xFF) / 255.0F;
        float amount = Math.max(0.0F, Math.min(1000.0F, fluid.getAmount()));
        float y = 1.0F + 0.3122F * (1.0F - amount / 1000.0F);
        VertexConsumer consumer = buffers.getBuffer(RenderType.entityTranslucent(InventoryMenu.BLOCK_ATLAS));
        PoseStack.Pose last = pose.last();
        // The converted trough spans x=-6..22 model pixels. The previous
        // -14..14 bounds were centred on the block origin rather than the
        // two-block model centre, shifting every liquid surface half a block.
        float minX = -6.0F / 16.0F;
        float maxX = 22.0F / 16.0F;
        float minZ = -4.0F / 16.0F;
        float maxZ = 4.0F / 16.0F;
        emit(consumer, last, minX, y, minZ, sprite.getU0(), sprite.getV0(), red, green, blue, alpha, packedLight);
        emit(consumer, last, maxX, y, minZ, sprite.getU1(), sprite.getV0(), red, green, blue, alpha, packedLight);
        emit(consumer, last, maxX, y, maxZ, sprite.getU1(), sprite.getV1(), red, green, blue, alpha, packedLight);
        emit(consumer, last, minX, y, maxZ, sprite.getU0(), sprite.getV1(), red, green, blue, alpha, packedLight);
    }

    private static void emit(VertexConsumer consumer, PoseStack.Pose pose,
                             float x, float y, float z, float u, float v,
                             float red, float green, float blue, float alpha, int packedLight) {
        Vector4f position = pose.pose().transform(new Vector4f(x, y, z, 1.0F));
        Vector3f normal = pose.normal().transform(new Vector3f(0.0F, -1.0F, 0.0F)).normalize();
        consumer.vertex(position.x(), position.y(), position.z(), red, green, blue, alpha,
                u, v, OverlayTexture.NO_OVERLAY, packedLight,
                normal.x(), normal.y(), normal.z());
    }

    /**
     * Reproduce the 1.12 solid-feed layer plus its two rows of item-texture
     * planes.  The old port rendered three ground item models, which loses the
     * legacy texture spread and makes every food look like the same small pile.
     */
    private void renderFoodContents(AnimaniaBlocks.TroughEntity entity, PoseStack pose,
                                    MultiBufferSource buffers, int packedLight, ItemStack stack) {
        int count = Math.min(3, Math.max(1, stack.getCount()));
        float[] tint = foodTint(stack, entity.getLevel());

        BaseLegacyFacilityRenderSupport.hideAll(model);
        BaseLegacyFacilityRenderSupport.show(model, "feed");
        pose.pushPose();
        pose.translate(0.0D, 0.17D * (3 - count), 0.0D);
        BaseLegacyFacilityRenderSupport.render(model, pose, buffers, TROUGH_TEXTURE,
                packedLight, tint[0], tint[1], tint[2], 1.0F);
        pose.popPose();

        BakedModel itemModel = Minecraft.getInstance().getItemRenderer()
                .getModel(stack, entity.getLevel(), null, 0);
        TextureAtlasSprite sprite = itemModel.getParticleIcon();
        if (sprite == null && !stack.is(Items.WHEAT)) return;

        BaseLegacyFacilityRenderSupport.hideAll(foodModel);
        BaseLegacyFacilityRenderSupport.show(foodModel, FOOD_PLANES_FIRST);
        // The legacy renderer bound the item's standalone PNG. Its model UVs
        // intentionally exceed 0..1 and repeat that one texture. Expanding
        // those UVs inside the shared 1.20 atlas samples neighbouring item
        // sprites instead (the straw/grey fragments seen in game).
        ResourceLocation foodTexture = stack.is(Items.WHEAT)
                ? WHEAT_TEXTURE
                : sprite.contents().name().withPrefix("textures/").withSuffix(".png");
        // Legacy explicitly enabled face culling for these zero-thickness food
        // planes. Rendering both sides puts two faces at the same depth and
        // causes camera-dependent dark speckles (z-fighting).
        VertexConsumer consumer = buffers.getBuffer(RenderType.entityCutout(foodTexture));
        pose.pushPose();
        pose.translate(0.0D, 0.2D * (3 - count), 0.0D);
        pose.mulPose(Axis.YP.rotationDegrees(-10.0F));
        pose.scale(0.8F, 0.8F, 0.8F);
        pose.translate(0.0D, 0.25D, -0.1D);
        foodModel.render(pose, consumer, packedLight, OverlayTexture.NO_OVERLAY, 1, 1, 1, 1);
        pose.mulPose(Axis.YP.rotationDegrees(180.0F));
        pose.translate(-1.4D, -0.1D, 0.0D);
        BaseLegacyFacilityRenderSupport.hideAll(foodModel);
        BaseLegacyFacilityRenderSupport.show(foodModel, FOOD_PLANES_SECOND);
        foodModel.render(pose, consumer, packedLight, OverlayTexture.NO_OVERLAY, 1, 1, 1, 1);
        pose.popPose();
    }

    /**
     * The legacy renderer tinted the solid feed layer from the actual food
     * sprite (wheat used its historical fixed brown).  Sampling the atlas
     * sprite keeps seeds, grain, vegetables and addon foods visually distinct.
     */
    private static float[] foodTint(ItemStack stack, Level level) {
        if (stack.is(Items.WHEAT)) return new float[]{160.0F / 255.0F, 124.0F / 255.0F, 89.0F / 255.0F};

        BakedModel itemModel = Minecraft.getInstance().getItemRenderer()
                .getModel(stack, level, null, 0);
        TextureAtlasSprite sprite = itemModel.getParticleIcon();
        if (sprite == null) return new float[]{0.63F, 0.49F, 0.35F};

        NativeImage image = sprite.contents().getOriginalImage();
        double red = 0.0D;
        double green = 0.0D;
        double blue = 0.0D;
        for (int x = 0; x < image.getWidth(); x++) {
            for (int y = 0; y < image.getHeight(); y++) {
                int pixel = image.getPixelRGBA(x, y);
                double alpha = FastColor.ABGR32.alpha(pixel) / 255.0D;
                red += FastColor.ABGR32.red(pixel) * alpha;
                green += FastColor.ABGR32.green(pixel) * alpha;
                blue += FastColor.ABGR32.blue(pixel) * alpha;
            }
        }
        int pixels = image.getWidth() * image.getHeight();
        if (pixels <= 0) return new float[]{0.63F, 0.49F, 0.35F};
        // The old renderer divided by the complete image area (not by the
        // sum of alpha). That deliberately softens sparse seed sprites and
        // prevents transparent pixels from turning the feed into neon liquid.
        return new float[]{
                legacyBrighten(red, pixels),
                legacyBrighten(green, pixels),
                legacyBrighten(blue, pixels)
        };
    }

    private static float legacyBrighten(double sum, int pixels) {
        int value = (int) sum / pixels;
        for (int i = 0; i < 3; i++) {
            value = value <= 3 ? 3 : Math.min(255, (int) (value / 0.7D));
        }
        return value / 255.0F;
    }
}
