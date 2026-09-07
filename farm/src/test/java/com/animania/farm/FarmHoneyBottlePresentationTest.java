package com.animania.farm;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class FarmHoneyBottlePresentationTest {
    @Test
    void primaryAndCompatibilityItemsUseTheLegacyBottleSprite() throws Exception {
        for (String id : new String[]{"honey_bottle", "honey_jar"}) {
            JsonObject model = readJson("/assets/animania_farm/models/item/" + id + ".json");
            assertEquals("animania_farm:item/bottle_honey",
                    model.getAsJsonObject("textures").get("layer0").getAsString(), id);
        }
    }

    @Test
    void bothRegistryIdsHaveAChineseBottleName() throws Exception {
        JsonObject chinese = readJson("/assets/animania_farm/lang/zh_cn.json");
        assertEquals("蜂蜜瓶", chinese.get("item.animania_farm.honey_bottle").getAsString());
        assertEquals("蜂蜜瓶", chinese.get("item.animania_farm.honey_jar").getAsString());
    }

    @Test
    void recipesProduceTheLegacyIdWhileAcceptingExistingCompatibilityItems() throws Exception {
        JsonObject conversion = readJson("/data/animania_farm/recipes/honey_bottle.json");
        assertEquals("animania_farm:honey_bottle",
                conversion.getAsJsonObject("result").get("item").getAsString());

        JsonObject honeyTag = readJson("/data/animania/tags/items/legacy_oredict/foodhoney.json");
        String values = honeyTag.getAsJsonArray("values").toString();
        assertTrue(values.contains("animania_farm:honey_bottle"));
        assertTrue(values.contains("animania_farm:honey_jar"));
    }

    private JsonObject readJson(String path) throws Exception {
        var stream = getClass().getResourceAsStream(path);
        assertNotNull(stream, path);
        try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }
}
