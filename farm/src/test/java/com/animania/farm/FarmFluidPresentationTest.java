package com.animania.farm;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class FarmFluidPresentationTest {
    private static final Set<String> FLUIDS = Set.of(
            "milk_holstein", "milk_friesian", "milk_jersey",
            "milk_goat", "milk_sheep", "animania_honey");

    @Test
    void fluidTypesUseTranslatedLegacyNames() throws Exception {
        JsonObject english = readJson("/assets/animania_farm/lang/en_us.json");
        JsonObject chinese = readJson("/assets/animania_farm/lang/zh_cn.json");

        for (String id : FLUIDS) {
            String descriptionId = "fluid.animania_farm." + id;
            assertTrue(english.has(descriptionId), id + " English name");
            assertTrue(chinese.has(descriptionId), id + " Chinese name");
        }
        assertEquals("蜂蜜", chinese.get("fluid.animania_farm.animania_honey").getAsString());
        assertEquals("荷斯坦牛牛奶", chinese.get("fluid.animania_farm.milk_holstein").getAsString());
    }

    @Test
    void allFluidSpritesAreIncludedInTheBlockAtlas() throws Exception {
        JsonObject atlas = readJson("/assets/minecraft/atlases/blocks.json");
        Set<String> sprites = new HashSet<>();
        atlas.getAsJsonArray("sources").forEach(source ->
                sprites.add(source.getAsJsonObject().get("resource").getAsString()));

        for (String id : FLUIDS) {
            for (String suffix : new String[]{"_still", "_flow"}) {
                String sprite = "animania_farm:fluids/" + id + suffix;
                assertTrue(sprites.contains(sprite), sprite + " atlas entry");
                try (var texture = getClass().getResourceAsStream(
                        "/assets/animania_farm/textures/fluids/" + id + suffix + ".png")) {
                    assertNotNull(texture, sprite + " texture");
                }
            }
        }
    }

    private JsonObject readJson(String path) throws Exception {
        var stream = getClass().getResourceAsStream(path);
        assertNotNull(stream, path);
        try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }
}
