package com.animania.client;

import com.animania.client.model.BaseLegacyModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Pins all Java-rendered Base models and the dynamic facility subparts. */
final class BaseLegacyModelConversionTest {
    @Test
    void everyBaseJavaModelBakesItsLegacyGeometry() {
        assertEquals(5, BaseLegacyModelLayers.LAYERS.size());
        BaseLegacyModelLayers.LAYERS.keySet().forEach(id -> {
            ModelPart root = BaseLegacyModelLayers.create(id).bakeRoot();
            assertTrue(root.getAllParts().anyMatch(part -> !part.isEmpty()), id + " baked with no cubes");
        });
    }

    @Test
    void facilitiesRetainDynamicContentParts() {
        ModelPart nest = BaseLegacyModelLayers.create("nest").bakeRoot();
        for (String part : new String[]{"egg1", "egg3c", "b_egg1", "bl_egg1", "w_egg3c"}) {
            assertTrue(nest.hasChild(part), "nest lost " + part);
            assertFalse(nest.getChild(part).isEmpty(), "nest part has no geometry: " + part);
        }
        ModelPart trough = BaseLegacyModelLayers.create("trough").bakeRoot();
        for (String part : new String[]{"feed", "feed_a", "slop1", "water1", "water3"}) {
            assertTrue(trough.hasChild(part), "trough lost " + part);
            assertFalse(trough.getChild(part).isEmpty(), "trough part has no geometry: " + part);
        }
        ModelPart food = BaseLegacyModelLayers.create("trough_food").bakeRoot();
        for (String part : new String[]{"feed_a", "feed_h", "feed_a1", "feed_h1"}) {
            assertTrue(food.hasChild(part), "trough food layer lost " + part);
            assertFalse(food.getChild(part).isEmpty(), "trough food part has no geometry: " + part);
        }
    }

    @Test
    void waterBottleKeepsItsAngledDrinkingTube() {
        ModelPart root = BaseLegacyModelLayers.create("water_bottle").bakeRoot();
        assertEquals(-0.785398F, root.getChild("shape1").xRot, 0.000001F);
    }
}
