package com.animania.common.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

/** Pure coordinate rules for the two-block trough's animal interaction volume. */
final class TroughInteractionRange {
    private static final int HORIZONTAL_MARGIN = 3;
    private static final int VERTICAL_MARGIN = 2;

    private TroughInteractionRange() {
    }

    static boolean contains(BlockPos controller, Direction facing, BlockPos animalPos) {
        BlockPos companion = controller.relative(facing);
        int minX = Math.min(controller.getX(), companion.getX()) - HORIZONTAL_MARGIN;
        int maxX = Math.max(controller.getX(), companion.getX()) + HORIZONTAL_MARGIN;
        int minZ = Math.min(controller.getZ(), companion.getZ()) - HORIZONTAL_MARGIN;
        int maxZ = Math.max(controller.getZ(), companion.getZ()) + HORIZONTAL_MARGIN;
        return animalPos.getX() >= minX && animalPos.getX() <= maxX
                && animalPos.getY() >= controller.getY() - VERTICAL_MARGIN
                && animalPos.getY() <= controller.getY() + VERTICAL_MARGIN
                && animalPos.getZ() >= minZ && animalPos.getZ() <= maxZ;
    }
}
