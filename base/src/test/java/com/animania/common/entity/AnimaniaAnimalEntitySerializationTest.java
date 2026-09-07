package com.animania.common.entity;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** NBT compatibility coverage for the shared replacement of GenericBehavior. */
class AnimaniaAnimalEntitySerializationTest {
    @Test
    void legacyCareBreedingAndVisualFieldsHaveStableModernKeys() throws Exception {
        String entity = Files.readString(Path.of("src/main/java/com/animania/common/entity/AnimaniaAnimalEntity.java"));
        for (String key : new String[]{"AnimaniaGender", "AnimaniaVariant", "AnimaniaHunger", "AnimaniaThirst",
                "AnimaniaSleeping", "AnimaniaPregnant", "AnimaniaSterilized", "AnimaniaSheared",
                "AnimaniaTamed", "AnimaniaSitting", "AnimaniaFed", "AnimaniaWatered",
                "AnimaniaFedTimer", "AnimaniaWateredTimer",
                "AnimaniaChildGrowthTimer", "MateUUID", "ParentUUID", "InBall", "BallColor", "DyeColor", "CrowTime"}) {
            assertTrue(entity.contains("\"" + key + "\""), "missing NBT key " + key);
        }
        assertTrue(entity.contains("addAdditionalSaveData"));
        assertTrue(entity.contains("readAdditionalSaveData"));
    }
}
