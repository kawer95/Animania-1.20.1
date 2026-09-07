package com.animania.common.block;

import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnimalBlockInteractionRangeTest {
    @Test
    void includesTwoBlocksHorizontallyAndOneBlockVertically() {
        BlockPos resource = new BlockPos(10, 20, 30);

        assertTrue(AnimalBlockInteractionRange.contains(resource, resource.offset(-2, -1, -2)));
        assertTrue(AnimalBlockInteractionRange.contains(resource, resource.offset(2, 1, 2)));
        assertFalse(AnimalBlockInteractionRange.contains(resource, resource.offset(3, 0, 0)));
        assertFalse(AnimalBlockInteractionRange.contains(resource, resource.offset(0, 2, 0)));
    }

    @Test
    void volumeContainsEveryBlockInTheFiveByFiveByThreeBounds() {
        BlockPos resource = BlockPos.ZERO;
        int included = 0;
        for (int x = -3; x <= 3; x++) {
            for (int y = -2; y <= 2; y++) {
                for (int z = -3; z <= 3; z++) {
                    if (AnimalBlockInteractionRange.contains(resource, new BlockPos(x, y, z))) included++;
                }
            }
        }

        assertEquals(75, included);
    }

    @Test
    void closestBlockPointsAtTheNearestEdgeWithoutReservingAStandingSlot() {
        BlockPos resource = new BlockPos(10, 20, 30);

        assertEquals(new BlockPos(9, 20, 31),
                AnimalBlockInteractionRange.closestBlock(resource, new BlockPos(2, 5, 40)));
        assertEquals(new BlockPos(11, 20, 29),
                AnimalBlockInteractionRange.closestBlock(resource, new BlockPos(11, 20, 29)));
    }
}
