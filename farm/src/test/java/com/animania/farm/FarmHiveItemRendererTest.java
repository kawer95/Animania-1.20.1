package com.animania.farm;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

final class FarmHiveItemRendererTest {
    @Test
    void bothHiveItemsUseVisibleLegacySprites() throws Exception {
        for (String[] entry : new String[][]{{"hive", "bee_hive"}, {"wild_hive", "wild_hive"}}) {
            String id = entry[0];
            var stream = getClass().getResourceAsStream(
                    "/assets/animania_farm/models/item/" + id + ".json");
            assertNotNull(stream, id);
            try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                var model = JsonParser.parseReader(reader).getAsJsonObject();
                assertEquals("minecraft:item/generated", model.get("parent").getAsString(), id);
                assertEquals("animania_farm:item/" + entry[1],
                        model.getAsJsonObject("textures").get("layer0").getAsString(), id);
            }
            try (var texture = getClass().getResourceAsStream(
                    "/assets/animania_farm/textures/item/" + entry[1] + ".png")) {
                assertNotNull(texture, id + " texture");
            }
        }
    }
}
