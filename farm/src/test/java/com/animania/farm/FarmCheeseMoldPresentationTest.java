package com.animania.farm;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class FarmCheeseMoldPresentationTest {
    @Test
    void containerBlockUsesTheLegacyModelRenderPathAndNonOpaqueShape() throws Exception {
        String block = Files.readString(Path.of("src/main/java/com/animania/farm/FarmCheeseMoldBlock.java"));
        String content = Files.readString(Path.of("src/main/java/com/animania/farm/FarmContent.java"));
        assertTrue(block.contains("RenderShape getRenderShape"));
        assertTrue(block.contains("return RenderShape.MODEL"));
        assertTrue(content.contains("sound(SoundType.WOOD).noOcclusion()"));
    }

    @Test
    void everyBlockStateVariantRetainsTheFivePartLegacyMoldShell() throws Exception {
        JsonObject blockstate = readJson("/assets/animania_farm/blockstates/cheese_mold.json");
        JsonObject variants = blockstate.getAsJsonObject("variants");
        assertEquals(FarmCheeseMoldBlock.Variant.values().length, variants.size());
        for (FarmCheeseMoldBlock.Variant variant : FarmCheeseMoldBlock.Variant.values()) {
            String key = "variant=" + variant.getSerializedName();
            assertTrue(variants.has(key), key);
            String modelId = variants.getAsJsonObject(key).get("model").getAsString();
            JsonObject model = readModel(modelId.substring(modelId.lastIndexOf('/') + 1));
            JsonArray elements = model.getAsJsonArray("elements");
            assertTrue(elements.size() >= 5, modelId + " lost the legacy shell");
            assertEquals("NorthWall", elements.get(0).getAsJsonObject().get("name").getAsString());
            assertEquals("WestWall", elements.get(1).getAsJsonObject().get("name").getAsString());
            assertEquals("EastWall", elements.get(2).getAsJsonObject().get("name").getAsString());
            assertEquals("SouthWall", elements.get(3).getAsJsonObject().get("name").getAsString());
            assertEquals("Bottom", elements.get(4).getAsJsonObject().get("name").getAsString());
        }
    }

    @Test
    void milkVariantsUseTheRegisteredFluidSprites() throws Exception {
        Map<String, String> expected = Map.of(
                "cow", "milk_friesian_flow",
                "friesian", "milk_friesian_flow",
                "holstein", "milk_holstein_flow",
                "jersey", "milk_jersey_flow",
                "goat", "milk_goat_flow",
                "sheep", "milk_sheep_flow");

        for (var entry : expected.entrySet()) {
            JsonObject model = readModel("mold_" + entry.getKey() + "_milk");
            assertEquals("animania_farm:fluids/" + entry.getValue(),
                    model.getAsJsonObject("textures").get("5").getAsString());
            assertContentLayerHasThickness(model);
        }
    }

    @Test
    void waterVariantIsTintedAndSaltUsesTheBaseSaltTexture() throws Exception {
        JsonObject water = readModel("mold_water");
        assertContentLayerHasThickness(water);
        JsonObject faces = lastElement(water).getAsJsonObject("faces");
        faces.entrySet().forEach(face -> assertEquals(0,
                face.getValue().getAsJsonObject().get("tintindex").getAsInt(), face.getKey()));

        JsonObject salt = readModel("mold_salt");
        assertEquals("animania:block/salt", salt.getAsJsonObject("textures").get("5").getAsString());
        assertContentLayerHasThickness(salt);
    }

    @Test
    void cheeseVariantsHaveAVisibleSurfaceThickness() throws Exception {
        for (String milk : new String[]{"cow", "friesian", "holstein", "jersey", "goat", "sheep"}) {
            assertContentLayerHasThickness(readModel("mold_" + milk + "_cheese"));
        }
    }

    private void assertContentLayerHasThickness(JsonObject model) {
        JsonObject layer = lastElement(model);
        JsonArray from = layer.getAsJsonArray("from");
        JsonArray to = layer.getAsJsonArray("to");
        assertTrue(from.get(1).getAsDouble() < to.get(1).getAsDouble(), "flat content layer in " + layer);
    }

    private JsonObject lastElement(JsonObject model) {
        JsonArray elements = model.getAsJsonArray("elements");
        return elements.get(elements.size() - 1).getAsJsonObject();
    }

    private JsonObject readModel(String id) throws Exception {
        return readJson("/assets/animania_farm/models/block/" + id + ".json");
    }

    private JsonObject readJson(String path) throws Exception {
        var stream = getClass().getResourceAsStream(path);
        assertNotNull(stream, path);
        try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }
}
