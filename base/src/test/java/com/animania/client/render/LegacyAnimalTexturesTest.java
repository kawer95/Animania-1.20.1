package com.animania.client.render;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Regression coverage for the module-local texture namespaces used by entity renderers. */
final class LegacyAnimalTexturesTest {
    @Test
    void resolvesFarmTextureInFarmNamespace() {
        ResourceLocation actual = LegacyAnimalTextures.resolve(
                new ResourceLocation("animania_farm", "cow_angus"), "default", false);
        assertEquals("animania_farm", actual.getNamespace());
        assertEquals("textures/entity/cows/cow_angus.png", actual.getPath());
    }

    @Test
    void resolvesExtraTextureInExtraNamespace() {
        ResourceLocation actual = LegacyAnimalTextures.resolve(
                new ResourceLocation("animania_extra", "hamster"), "black", false);
        assertEquals("animania_extra", actual.getNamespace());
        assertEquals("textures/entity/rodents/hamster_black.png", actual.getPath());
    }

    @Test
    void resolvesCatsDogsTextureInCatsDogsNamespace() {
        ResourceLocation actual = LegacyAnimalTextures.resolve(
                new ResourceLocation("animania_catsdogs", "queen_tabby"), "default", false);
        assertEquals("animania_catsdogs", actual.getNamespace());
        assertEquals("textures/entity/cats/tabby.png", actual.getPath());
    }
}
