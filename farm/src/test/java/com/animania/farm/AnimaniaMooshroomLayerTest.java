package com.animania.farm;

import com.animania.client.render.AnimaniaMooshroomLayer;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class AnimaniaMooshroomLayerTest {
    @Test
    void limitsMushroomGeometryToTheTwoLegacyAdultRenderers() {
        assertTrue(AnimaniaMooshroomLayer.supports(id("cow_mooshroom")));
        assertTrue(AnimaniaMooshroomLayer.supports(id("bull_mooshroom")));
        assertFalse(AnimaniaMooshroomLayer.supports(id("calf_mooshroom")));
        assertFalse(AnimaniaMooshroomLayer.supports(id("cow_angus")));
    }

    private static ResourceLocation id(String path) {
        return new ResourceLocation("animania_farm", path);
    }
}
