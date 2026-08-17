package com.animania.farm;

import com.animania.client.render.LegacyAnimalTextures;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/** Every Farm registry ID must resolve to a real preserved 1.12 texture. */
final class FarmTextureResolverTest {
    @Test
    void everyFarmEntityHasDefaultAndSpecialStateTextures() {
        for (String id : FarmLegacyIds.ALL) {
            assertTexture(id, "default", false);
            if (id.endsWith("_draft")) {
                for (String variant : List.of("black", "bw1", "bw2", "grey", "red", "white")) {
                    assertTexture(id, variant, false);
                }
            }
            if (id.equals("buck_angora") || id.equals("doe_angora")) assertTexture(id, "default", true);
            if (id.startsWith("ewe_") || id.startsWith("ram_") || id.startsWith("lamb_")) {
                List<String> variants = id.endsWith("_friesian") ? List.of("white", "brown", "black")
                        : id.endsWith("_dorset") || id.endsWith("_merino") || id.endsWith("_suffolk")
                        ? List.of("white", "brown") : List.of("default");
                for (String variant : variants) {
                    assertTexture(id, variant, false);
                    assertTexture(id, variant, true);
                }
            }
        }
    }

    private static void assertTexture(String id, String variant, boolean sheared) {
        ResourceLocation texture = LegacyAnimalTextures.resolve(
                new ResourceLocation(AnimaniaFarm.MOD_ID, id), variant, sheared);
        String resource = "assets/" + texture.getNamespace() + "/" + texture.getPath();
        try (InputStream stream = FarmTextureResolverTest.class.getClassLoader().getResourceAsStream(resource)) {
            assertNotNull(stream, id + " -> " + resource);
        } catch (java.io.IOException exception) {
            throw new AssertionError("Could not read " + resource, exception);
        }
    }
}
