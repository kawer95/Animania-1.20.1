package com.animania.client.manual;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** Resource and API regression coverage for the native handbook. */
class ManualScreenTest {
    @Test
    void nativeManualLoadsBaseAndAddonResourceLayoutsWithoutPatchouli() throws Exception {
        String screen = Files.readString(Path.of("src/main/java/com/animania/client/manual/ManualScreen.java"));
        String item = Files.readString(Path.of("src/main/java/com/animania/common/item/ManualItem.java"));
        assertTrue(screen.contains("listResources(\"manual\""));
        assertTrue(screen.contains("listResources(\"animania/manual\""));
        assertTrue(screen.contains("JsonParser"));
        assertTrue(item.contains("ManualScreen.open"));
        assertTrue(!screen.contains("patchouli"));
    }

    @Test
    void handbookEntityPreviewsUseAStableThreeQuarterAngle() throws Exception {
        String screen = Files.readString(Path.of("src/main/java/com/animania/client/manual/ManualScreen.java"));
        assertTrue(screen.contains("FollowsAngle"));
        assertTrue(screen.contains("ENTITY_PREVIEW_YAW_COMPONENT, 0.0F, preview"),
                "entity previews should use the direct Forge angle component");
        assertTrue(screen.contains("ENTITY_PREVIEW_YAW_COMPONENT = 1.25F"),
                "one direct angle unit is 20 degrees, so 1.25 gives a 25-degree view");
    }

    @Test
    void handbookEntityPreviewsCacheEntitiesAndUseCanonicalVariants() throws Exception {
        String screen = Files.readString(Path.of("src/main/java/com/animania/client/manual/ManualScreen.java"));
        assertTrue(screen.contains("previewEntities"),
                "preview entities must survive render passes instead of rerolling variants");
        assertTrue(screen.contains("animal.setVariantName(\"default\")"),
                "catalogue previews should use a deterministic representative variant");
        assertTrue(screen.contains("previewEntities.values().forEach(LivingEntity::discard)"),
                "cached client-only previews must be released when the screen closes");
        assertTrue(!screen.contains("preview.discard()"),
                "cached previews must not be discarded at the end of every render pass");
    }

    @Test
    void craftingTagsDoNotRenderTheBarrierDiagnosticPlaceholder() throws Exception {
        String screen = Files.readString(Path.of("src/main/java/com/animania/client/manual/ManualScreen.java"));
        assertTrue(screen.contains("Items.BARRIER"),
                "unresolved tag placeholders must be recognized as barrier stacks");
        assertTrue(screen.contains("representativeFromIngredientJson(ingredient.toJson())"),
                "crafting icons should fall back to the ingredient JSON representative");
    }

    @Test
    void handbookImagesScaleTheFullTextureInsteadOfCroppingToTheDestinationSize() throws Exception {
        String screen = Files.readString(Path.of("src/main/java/com/animania/client/manual/ManualScreen.java"));
        assertTrue(screen.contains("drawWidth, drawHeight, 0.0F, 0.0F"),
                "image blit must use the destination size separately from the source rectangle");
        assertTrue(screen.contains("sourceSize[0], sourceSize[1], sourceSize[0], sourceSize[1]"),
                "image blit must sample the complete source texture when scaling");
    }

    @Test
    void allBaseManualPagesAreValidJson() throws Exception {
        Path manual = Path.of("src/main/resources/assets/animania/manual");
        long pages = Files.walk(manual).filter(path -> path.toString().endsWith(".json")).peek(path -> {
            try {
                com.google.gson.JsonParser.parseString(Files.readString(path));
            } catch (Exception error) {
                throw new AssertionError("invalid manual page " + path, error);
            }
        }).count();
        assertTrue(pages >= 10, "base manual page set unexpectedly small");
    }
}
