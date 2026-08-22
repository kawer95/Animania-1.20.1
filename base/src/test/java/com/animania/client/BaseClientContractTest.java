package com.animania.client;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Native client registration and CraftStudio removal coverage. */
class BaseClientContractTest {
    @Test
    void allFacilityRenderersUseNativeModelPartsAndNoCraftStudioRuntime() throws Exception {
        String client = Files.readString(Path.of("src/main/java/com/animania/client/AnimaniaClient.java"));
        String renderer = Files.readString(Path.of("src/main/java/com/animania/client/render/AnimaniaAnimalRenderer.java"));
        assertTrue(client.contains("registerBlockEntityRenderer"));
        assertTrue(client.contains("BaseTroughRenderer"));
        assertTrue(client.contains("BaseNestRenderer"));
        assertTrue(client.contains("BaseSaltLickRenderer"));
        assertTrue(renderer.contains("LegacyAnimalModel"));
        String main = Files.readString(Path.of("src/main/java/com/animania/client/model/LegacyAnimalModel.java"));
        assertTrue(!main.toLowerCase(java.util.Locale.ROOT).contains("craftstudio"));
    }

    @Test
    void peacockFanRestoresTheLegacyPerRendererThirdScale() throws Exception {
        String main = Files.readString(Path.of("src/main/java/com/animania/client/model/LegacyAnimalModel.java"));
        assertTrue(main.contains("LEGACY_PEACOCK_FAN_RENDER_SCALE"));
        assertTrue(main.contains("fan_node_a"));
        assertTrue(main.contains("pose.scale(LEGACY_PEACOCK_FAN_RENDER_SCALE"));
        assertTrue(main.contains("renderRootWithLegacyFanScale"));
        assertTrue(main.contains("fan.visible = true"),
                "fan roots must be made visible during their explicit scaled pass");
        assertTrue(main.contains("fan.skipDraw = false"),
                "fan roots must not inherit the coloured-pass skipDraw flag");
    }

    @Test
    void pigletTailRestoresTheLegacyPerRendererScale() throws Exception {
        String main = Files.readString(Path.of("src/main/java/com/animania/client/model/LegacyAnimalModel.java"));
        assertTrue(main.contains("LEGACY_PIGLET_TAIL_RENDER_SCALE = 0.8F"));
        assertTrue(main.contains("entity.registryPath().startsWith(\"piglet_\")"));
        assertTrue(main.contains("pose.scale(LEGACY_PIGLET_TAIL_RENDER_SCALE"));
        assertTrue(main.contains("pigletTail.render(pose, consumer"));
    }

    @Test
    void rabbitHeadUsesTheLegacyNeckRuntimePose() throws Exception {
        String main = Files.readString(Path.of("src/main/java/com/animania/client/model/LegacyAnimalModel.java"));
        assertTrue(main.contains("if (isRabbitId(entity.registryPath()))"));
        assertTrue(main.contains("neck.xRot = 0.0F"));
        assertTrue(main.contains("neck.yRot = netHeadYaw * Mth.DEG_TO_RAD"));
        assertTrue(main.contains("|| isRabbitId(id)"));
    }

    @Test
    void chickenModelsRestoreTheirLegacyRuntimeRestPose() throws Exception {
        String main = Files.readString(Path.of("src/main/java/com/animania/client/model/LegacyAnimalModel.java"));
        assertTrue(main.contains("if (isChickenId(entity.registryPath())) applyChickenRestPose"));
        assertTrue(main.contains("setRotation(child(\"neck/crest\"), 0.3490659F"));
        assertTrue(main.contains("setRotation(child(\"neck/beak_top\"), 0.3169494F"));
        assertTrue(main.contains("setRotation(child(\"neck/beak_top\"), 0.7268012F"));
        assertTrue(main.contains("setRotation(child(\"feather1\"), 0.5097123F"));
    }

    @Test
    void horseSaddleRenderingIsGatedBySyncedSaddleState() throws Exception {
        String main = Files.readString(Path.of("src/main/java/com/animania/client/model/LegacyAnimalModel.java"));
        assertTrue(main.contains("LEGACY_HORSE_SADDLE_PARTS"));
        assertTrue(main.contains("this.saddleParts = resolve(root, LEGACY_HORSE_SADDLE_PARTS)"));
        assertTrue(main.contains("for (ModelPart part : saddleParts) part.visible = entity.isSaddled();"));
    }
    @Test
    void troughRendererSeparatesFluidAndItemPassesAndClearsStaleParts() throws Exception {
        String renderer = Files.readString(Path.of("src/main/java/com/animania/client/render/BaseTroughRenderer.java"));
        assertTrue(renderer.contains("renderFluidSurface"));
        assertTrue(renderer.contains("IClientFluidTypeExtensions.of"));
        assertTrue(renderer.contains("InventoryMenu.BLOCK_ATLAS"));
        assertTrue(renderer.contains("properties.getTintColor(fluid)"));
        assertTrue(renderer.contains("float minX = -6.0F / 16.0F"));
        assertTrue(renderer.contains("float maxX = 22.0F / 16.0F"));
        assertTrue(renderer.contains("FOOD_PLANES_FIRST"));
        assertTrue(renderer.contains("FOOD_PLANES_SECOND"));
        assertTrue(renderer.contains("RenderType.entityCutout(foodTexture)"),
                "zero-thickness legacy food planes must use face culling to avoid z-fighting");
        assertFalse(renderer.contains("entityCutoutNoCull(foodTexture)"),
                "drawing both sides of zero-thickness food planes causes camera-dependent dark speckles");
        assertFalse(renderer.contains("sprite.wrap"),
                "legacy repeating item UVs must not expand into neighbouring atlas sprites");
        assertTrue(renderer.contains("getParticleIcon"));
        assertTrue(renderer.contains("WHEAT_TEXTURE"));
        assertTrue(renderer.contains("trough_food"));
        assertTrue(renderer.contains("legacyBrighten"));
        assertTrue(renderer.contains("withPrefix(\"textures/\").withSuffix(\".png\")"));
        assertTrue(renderer.contains("show(model, \"feed\")"));
        assertTrue(renderer.contains("foodTint(stack, entity.getLevel())"));
        assertTrue(renderer.contains("BaseLegacyFacilityRenderSupport.hideAll(model)"));
        assertTrue(renderer.contains("entity.fluidSnapshot()"));
        assertTrue(renderer.contains("pose.scale(-1.0F, -1.0F, 1.0F)"));
    }

    @Test
    void bothTroughHalvesRejectTheFlowingFluidPartialBlockReplacementPath() throws Exception {
        String controller = Files.readString(Path.of("src/main/java/com/animania/common/block/AnimaniaTroughBlock.java"));
        String companion = Files.readString(Path.of("src/main/java/com/animania/common/block/AnimaniaInvisibleBlock.java"));
        for (String source : new String[] {controller, companion}) {
            assertTrue(source.contains("implements LiquidBlockContainer"));
            assertTrue(source.contains("boolean canPlaceLiquid"));
            assertTrue(source.contains("boolean placeLiquid"));
            assertTrue(source.contains("FluidState fluid) { return false; }"));
        }
    }

}
