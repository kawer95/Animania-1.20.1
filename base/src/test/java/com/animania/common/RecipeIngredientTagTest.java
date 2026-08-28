package com.animania.common;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Prevents item IDs and obsolete ore-dictionary aliases being emitted as empty item tags. */
class RecipeIngredientTagTest {
    private static final List<String> OBSOLETE_EMPTY_TAGS = List.of(
            "minecraft:iron_ingots", "minecraft:iron_nuggets", "minecraft:gold_nuggets",
            "minecraft:leather", "minecraft:string", "minecraft:seeds", "minecraft:sugar",
            "minecraft:red_wool", "minecraft:cooked_porkchop",
            "animania_farm:friesian_cheese_wedge",
            "minecraft:black_dye", "minecraft:red_dye", "minecraft:green_dye",
            "minecraft:brown_dye", "minecraft:blue_dye", "minecraft:purple_dye",
            "minecraft:cyan_dye", "minecraft:light_gray_dye", "minecraft:gray_dye",
            "minecraft:pink_dye", "minecraft:lime_dye", "minecraft:yellow_dye",
            "minecraft:light_blue_dye", "minecraft:magenta_dye", "minecraft:orange_dye",
            "minecraft:white_dye");

    @Test
    void migratedRecipesUseExistingForge1201Tags() throws Exception {
        for (String module : List.of("base", "farm", "catsdogs", "extra")) {
            Path root = module.equals("base") ? Path.of("src/main/resources/data")
                    : Path.of("..").resolve(module).resolve("src/main/resources/data");
            try (Stream<Path> paths = Files.walk(root)) {
                for (Path recipe : paths.filter(path -> path.toString().contains("recipes"))
                        .filter(path -> path.toString().endsWith(".json")).toList()) {
                    String json = Files.readString(recipe);
                    for (String obsolete : OBSOLETE_EMPTY_TAGS) {
                        assertFalse(json.contains("\"tag\": \"" + obsolete + "\""),
                                () -> recipe + " still uses empty tag " + obsolete);
                    }
                }
            }
        }

        String knife = Files.readString(Path.of("../farm/src/main/resources/data/animania_farm/recipes/carving_knife.json"));
        assertTrue(knife.contains("\"tag\": \"forge:ingots/iron\""));
        assertTrue(knife.contains("\"tag\": \"forge:rods/wooden\""));
        String wheel = Files.readString(Path.of("../farm/src/main/resources/data/animania_farm/recipes/wheel.json"));
        assertTrue(wheel.contains("\"tag\": \"forge:nuggets/iron\""));
    }
}
