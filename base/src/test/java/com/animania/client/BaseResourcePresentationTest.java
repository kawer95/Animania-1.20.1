package com.animania.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

final class BaseResourcePresentationTest {
    @Test
    void activeBaseItemsAndProbeTextAreLocalized() throws Exception {
        JsonObject chinese = readJson("/assets/animania/lang/zh_cn.json");
        assertEquals("动物谷手册", chinese.get("item.animania.manual").getAsString());
        assertEquals("干草", chinese.get("item.animania.hay").getAsString());
        assertEquals("奶酪", chinese.get("item.animania.cheese").getAsString());
        assertEquals("水瓶", chinese.get("item.animania.water_bottle").getAsString());
        assertEquals("泔水", chinese.get("fluid.animania.slop").getAsString());
        assertEquals("动物谷注册项：%s", chinese.get("jei.animania.registry_info").getAsString());
    }

    @Test
    void genericCheeseNoLongerUsesTheManualPlaceholder() throws Exception {
        byte[] cheese = readBytes("/assets/animania/textures/item/cheese.png");
        byte[] manual = readBytes("/assets/animania/textures/item/animania_manual.png");
        assertFalse(Arrays.equals(cheese, manual), "generic cheese still uses the manual icon placeholder");
    }

    @Test
    void waterBottleUsesTheVanillaLayeredItemSprites() throws Exception {
        JsonObject model = readJson("/assets/animania/models/item/water_bottle.json");
        JsonObject textures = model.getAsJsonObject("textures");
        assertEquals("minecraft:item/potion_overlay", textures.get("layer0").getAsString());
        assertEquals("minecraft:item/potion", textures.get("layer1").getAsString());
    }

    private JsonObject readJson(String path) throws Exception {
        var stream = getClass().getResourceAsStream(path);
        assertNotNull(stream, path);
        try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }

    private byte[] readBytes(String path) throws Exception {
        try (var stream = getClass().getResourceAsStream(path)) {
            assertNotNull(stream, path);
            return stream.readAllBytes();
        }
    }
}
