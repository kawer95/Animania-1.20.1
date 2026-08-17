package com.animania.catsdogs;

import com.animania.client.render.LegacyAnimalTextures;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/** Every cat/dog sex, child and coat variant resolves to a real texture. */
final class CatsDogsTextureResolverTest {
    @Test
    void everyPetEntityHasAllStoredCoatTextures() {
        for (String id : CatsDogsLegacyIds.ALL) {
            List<String> variants = id.endsWith("_chihuahua") || id.endsWith("_collie")
                    ? List.of("0", "1")
                    : id.endsWith("_labrador") || id.endsWith("_poodle")
                    ? List.of("0", "1", "2")
                    : id.endsWith("_wolf")
                    ? List.of("0", "1", "2", "3", "4", "5", "6", "7")
                    : List.of("default");
            for (String variant : variants) assertTexture(id, variant);
        }
    }

    private static void assertTexture(String id, String variant) {
        ResourceLocation texture = LegacyAnimalTextures.resolve(
                new ResourceLocation(AnimaniaCatsDogs.MOD_ID, id), variant, false);
        String resource = "assets/" + texture.getNamespace() + "/" + texture.getPath();
        try (InputStream stream = CatsDogsTextureResolverTest.class.getClassLoader().getResourceAsStream(resource)) {
            assertNotNull(stream, id + " (" + variant + ") -> " + resource);
        } catch (java.io.IOException exception) {
            throw new AssertionError("Could not read " + resource, exception);
        }
    }
}
