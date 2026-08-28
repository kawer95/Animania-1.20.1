package com.animania.farm;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class FarmKaleidoscopeCookeryCompatibilityTest {
    @Test
    void millstoneTagCoversEveryAdultVanillaReplacementFamily() throws IOException {
        Set<String> expected = farmIds(id -> startsWithAny(id,
                "buck_", "doe_", "bull_", "cow_", "ewe_", "ram_", "mare_", "stallion_"));

        assertEquals(expected, values("millstone_bindable"));
    }

    @Test
    void pigOilTagCoversEveryFarmPigType() throws IOException {
        Set<String> expected = farmIds(id -> startsWithAny(id, "hog_", "sow_", "piglet_"));

        assertEquals(expected, values("pig_oil_source"));
    }

    private static Set<String> farmIds(Predicate<String> filter) {
        return FarmLegacyIds.ALL.stream()
                .filter(filter)
                .map(id -> "animania_farm:" + id)
                .collect(Collectors.toUnmodifiableSet());
    }

    private static Set<String> values(String name) throws IOException {
        String resource = "/data/kaleidoscope_cookery/tags/entity_types/" + name + ".json";
        try (var stream = FarmKaleidoscopeCookeryCompatibilityTest.class.getResourceAsStream(resource)) {
            assertNotNull(stream, "missing compatibility tag " + resource);
            try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                assertFalse(json.get("replace").getAsBoolean(), "compatibility tags must merge with the owning mod");
                return json.getAsJsonArray("values").asList().stream()
                        .map(value -> value.getAsString())
                        .collect(Collectors.toUnmodifiableSet());
            }
        }
    }

    private static boolean startsWithAny(String value, String... prefixes) {
        for (String prefix : prefixes) {
            if (value.startsWith(prefix)) return true;
        }
        return false;
    }
}
