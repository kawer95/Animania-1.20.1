package com.animania.farm;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class FarmVehicleItemPresentationTest {
    private static final Path ITEM_MODELS = Path.of(
            "src/main/resources/assets/animania_farm/models/item");
    private static final Path ITEM_TEXTURES = Path.of(
            "src/main/resources/assets/animania_farm/textures/item");

    @Test
    void registeredVehiclesUseTheirPreservedLegacyIconsAndDisplayTransforms() throws Exception {
        for (String vehicle : new String[]{"cart", "wagon", "tiller"}) {
            JsonObject activeModel = readModel(vehicle);
            assertEquals("animania_farm:item/item_" + vehicle,
                    activeModel.get("parent").getAsString(),
                    vehicle + " points at a generated placeholder model");

            JsonObject preservedModel = readModel("item_" + vehicle);
            assertEquals("minecraft:item/generated", preservedModel.get("parent").getAsString());
            assertEquals("animania_farm:item/item_" + vehicle,
                    preservedModel.getAsJsonObject("textures").get("layer0").getAsString());
            assertTrue(preservedModel.has("display"),
                    vehicle + " lost the legacy inventory/hand display transforms");
            assertTrue(Files.size(ITEM_TEXTURES.resolve("item_" + vehicle + ".png")) > 0,
                    vehicle + " is missing its preserved legacy icon");
        }
    }

    private static JsonObject readModel(String name) throws Exception {
        return JsonParser.parseString(Files.readString(ITEM_MODELS.resolve(name + ".json"))).getAsJsonObject();
    }
}
