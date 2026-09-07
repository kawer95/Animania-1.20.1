package com.animania.farm;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

final class FarmContainerProductPresentationTest {
    private static final Map<String, String> FLUID_BUCKETS = Map.of(
            "milk_holstein_bucket", "milk_holstein",
            "milk_friesian_bucket", "milk_friesian",
            "milk_jersey_bucket", "milk_jersey",
            "milk_goat_bucket", "milk_goat",
            "milk_sheep_bucket", "milk_sheep",
            "animania_honey_bucket", "animania_honey");

    @Test
    void animalAndHiveBucketProductsRenderTheirActualFluid() throws Exception {
        for (var entry : FLUID_BUCKETS.entrySet()) {
            JsonObject model = readJson("/assets/animania_farm/models/item/" + entry.getKey() + ".json");
            assertEquals("forge:item/bucket", model.get("parent").getAsString(), entry.getKey());
            assertEquals("forge:fluid_container", model.get("loader").getAsString(), entry.getKey());
            assertEquals("animania_farm:" + entry.getValue(), model.get("fluid").getAsString(), entry.getKey());
        }
    }

    @Test
    void bottleProductsRetainTheirExactLegacySprites() throws Exception {
        assertItemTexture("milk_bottle", "animania_farm:item/bottle_milk");
        assertItemTexture("honey_bottle", "animania_farm:item/bottle_honey");
        assertItemTexture("honey_jar", "animania_farm:item/bottle_honey");
    }

    @Test
    void saltUsesThePreservedLegacySpriteInsteadOfTheGeneratedPlaceholder() throws Exception {
        assertItemTexture("salt", "animania:item/salt");
        try (var texture = getClass().getResourceAsStream("/assets/animania/textures/item/salt.png")) {
            assertNotNull(texture, "preserved legacy salt texture");
        }
    }

    private void assertItemTexture(String id, String expected) throws Exception {
        JsonObject model = readJson("/assets/animania_farm/models/item/" + id + ".json");
        assertEquals(expected, model.getAsJsonObject("textures").get("layer0").getAsString(), id);
    }

    private JsonObject readJson(String path) throws Exception {
        var stream = getClass().getResourceAsStream(path);
        assertNotNull(stream, path);
        try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }
}
