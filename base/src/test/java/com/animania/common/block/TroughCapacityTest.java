package com.animania.common.block;

import com.animania.common.AnimaniaBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

    @Test
    void animalInteractionRangeCoversBothTroughHalvesPlusConfiguredMargins() {
        BlockPos controller = new BlockPos(10, 20, 30);

        assertEquals(280, countInteractionBlocks(controller, Direction.EAST));
        assertTrue(TroughInteractionRange.contains(
                controller, Direction.EAST, new BlockPos(7, 18, 27)));
        assertTrue(TroughInteractionRange.contains(
                controller, Direction.EAST, new BlockPos(14, 22, 33)));
        assertFalse(TroughInteractionRange.contains(
                controller, Direction.EAST, new BlockPos(6, 20, 30)));
        assertFalse(TroughInteractionRange.contains(
                controller, Direction.EAST, new BlockPos(10, 23, 30)));

        assertEquals(280, countInteractionBlocks(controller, Direction.NORTH));
        assertTrue(TroughInteractionRange.contains(
                controller, Direction.NORTH, new BlockPos(7, 18, 26)));
        assertTrue(TroughInteractionRange.contains(
                controller, Direction.NORTH, new BlockPos(13, 22, 33)));
        assertFalse(TroughInteractionRange.contains(
                controller, Direction.NORTH, new BlockPos(10, 20, 25)));
    }

    private static int countInteractionBlocks(BlockPos controller, Direction facing) {
        int count = 0;
        for (int x = controller.getX() - 5; x <= controller.getX() + 5; x++) {
            for (int y = controller.getY() - 4; y <= controller.getY() + 4; y++) {
                for (int z = controller.getZ() - 5; z <= controller.getZ() + 5; z++) {
                    if (TroughInteractionRange.contains(
                            controller, facing, new BlockPos(x, y, z))) count++;
                }
            }
        }
        return count;
    }
}
