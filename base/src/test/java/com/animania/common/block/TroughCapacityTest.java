package com.animania.common.block;

import com.animania.common.AnimaniaBlocks;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TroughCapacityTest {
    @Test
    void displayLevelsUseThirtyAndSixtyPercentBoundaries() {
        assertEquals(0, AnimaniaBlocks.TroughEntity.displayLevel(0, 64));
        assertEquals(1, AnimaniaBlocks.TroughEntity.displayLevel(19, 64));
        assertEquals(2, AnimaniaBlocks.TroughEntity.displayLevel(20, 64));
        assertEquals(2, AnimaniaBlocks.TroughEntity.displayLevel(38, 64));
        assertEquals(3, AnimaniaBlocks.TroughEntity.displayLevel(39, 64));

        assertEquals(1, AnimaniaBlocks.TroughEntity.displayLevel(900, 3000));
        assertEquals(2, AnimaniaBlocks.TroughEntity.displayLevel(1800, 3000));
        assertEquals(3, AnimaniaBlocks.TroughEntity.displayLevel(1801, 3000));
    }

    @Test
    void configAndEveryTroughPathUseTheLiveCapacities() throws Exception {
        String config = Files.readString(Path.of("src/main/java/com/animania/common/config/AnimaniaConfig.java"));
        String block = Files.readString(Path.of("src/main/java/com/animania/common/block/AnimaniaTroughBlock.java"));
        String entity = Files.readString(Path.of("src/main/java/com/animania/common/AnimaniaBlocks.java"));
        String renderer = Files.readString(Path.of("src/main/java/com/animania/client/render/BaseTroughRenderer.java"));

        assertTrue(config.contains("defineInRange(\"troughSolidCapacity\", 64, 3, 64)"));
        assertTrue(config.contains("defineInRange(\"troughFluidCapacity\", 3000, 1000, 3000)"));
        assertTrue(block.contains("trough.solidCapacity()"));
        assertTrue(block.contains("trough.fluidCapacity()"));
        assertTrue(entity.contains("syncConfiguredLimits()"));
        assertTrue(renderer.contains("entity.foodDisplayLevel()"));
        assertTrue(renderer.contains("entity.fluidDisplayLevel()"));
    }
}
