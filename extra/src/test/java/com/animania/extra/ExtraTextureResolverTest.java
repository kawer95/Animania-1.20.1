package com.animania.extra;

import com.animania.client.render.LegacyAnimalTextures;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/** Every Extra registry ID and stored colour family must resolve to a texture. */
final class ExtraTextureResolverTest {
    @Test
    void everyExtraEntityHasAllStoredVariantTextures() {
        for (String id : ExtraLegacyIds.ALL) {
            List<String> variants = id.equals("hamster")
                    ? List.of("black", "brown", "darkbrown", "darkgray", "gray", "plum", "tarou", "white", "gold")
                    : id.equals("dartfrog") ? List.of("blue", "red", "yellow")
                    : id.equals("frog") ? List.of("default", "green")
                    : id.endsWith("_lop") ? List.of("black", "brown", "golden", "olive", "patch_black", "patch_brown", "patch_grey")
                    : List.of("default");
            for (String variant : variants) assertTexture(id, variant);
        }
    }

    private static void assertTexture(String id, String variant) {
        ResourceLocation texture = LegacyAnimalTextures.resolve(
                new ResourceLocation(AnimaniaExtra.MOD_ID, id), variant, false);
        String resource = "assets/" + texture.getNamespace() + "/" + texture.getPath();
        try (InputStream stream = ExtraTextureResolverTest.class.getClassLoader().getResourceAsStream(resource)) {
            assertNotNull(stream, id + " (" + variant + ") -> " + resource);
        } catch (java.io.IOException exception) {
            throw new AssertionError("Could not read " + resource, exception);
        }
    }
}
