package com.animania.farm;

import com.animania.client.render.AnimaniaMudLayer;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;

final class AnimaniaMudLayerTest {
    @Test
    void mapsAllLegacyPigRolesAndHampshireSpecialCaseToPreservedTextures() throws Exception {
        assertTexture("sow_duroc", "pig_muddy.png");
        assertTexture("hog_hampshire", "pig_muddy_hampshire.png");
        assertTexture("piglet_yorkshire", "piglet_muddy.png");
        assertNull(AnimaniaMudLayer.textureFor(new ResourceLocation("animania_farm", "cow_angus")));
        assertNull(AnimaniaMudLayer.textureFor(new ResourceLocation("animania_extra", "hamster")));
    }

    private static void assertTexture(String entity, String filename) throws Exception {
        ResourceLocation texture = AnimaniaMudLayer.textureFor(
                new ResourceLocation("animania_farm", entity));
        assertNotNull(texture);
        assertTrue(texture.getPath().endsWith(filename));
        String resource = "assets/" + texture.getNamespace() + "/" + texture.getPath();
        try (InputStream stream = AnimaniaMudLayerTest.class.getClassLoader().getResourceAsStream(resource)) {
            assertNotNull(stream, resource);
        }
    }
}
