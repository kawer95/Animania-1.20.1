package com.animania.common.block;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;

/** Shared direct-interaction volume for single-block animal resources. */
public final class AnimalBlockInteractionRange {
    private static final int HORIZONTAL_MARGIN = 2;
    private static final int VERTICAL_MARGIN = 1;

    private AnimalBlockInteractionRange() {
    }

    public static boolean contains(BlockPos resourcePos, BlockPos animalPos) {
        return Math.abs(animalPos.getX() - resourcePos.getX()) <= HORIZONTAL_MARGIN
                && Math.abs(animalPos.getY() - resourcePos.getY()) <= VERTICAL_MARGIN
                && Math.abs(animalPos.getZ() - resourcePos.getZ()) <= HORIZONTAL_MARGIN;
    }

    /** Large animals may interact as soon as any part of their body enters the block volume. */
    public static boolean contains(BlockPos resourcePos, Entity animal) {
        AABB interactionVolume = new AABB(
                resourcePos.getX() - HORIZONTAL_MARGIN,
                resourcePos.getY() - VERTICAL_MARGIN,
                resourcePos.getZ() - HORIZONTAL_MARGIN,
                resourcePos.getX() + HORIZONTAL_MARGIN + 1,
                resourcePos.getY() + VERTICAL_MARGIN + 1,
                resourcePos.getZ() + HORIZONTAL_MARGIN + 1);
        return animal.getBoundingBox().intersects(interactionVolume);
    }

    /**
     * Nearest block one layer inside the volume. Pathfinding may stop short of
     * its destination, so aiming at the outer boundary can leave small animals
     * just outside the direct-interaction box.
     */
    public static BlockPos closestBlock(BlockPos resourcePos, BlockPos origin) {
        int horizontalNavigationMargin = Math.max(0, HORIZONTAL_MARGIN - 1);
        int verticalNavigationMargin = Math.max(0, VERTICAL_MARGIN - 1);
        return new BlockPos(
                Mth.clamp(origin.getX(), resourcePos.getX() - horizontalNavigationMargin,
                        resourcePos.getX() + horizontalNavigationMargin),
                Mth.clamp(origin.getY(), resourcePos.getY() - verticalNavigationMargin,
                        resourcePos.getY() + verticalNavigationMargin),
                Mth.clamp(origin.getZ(), resourcePos.getZ() - horizontalNavigationMargin,
                        resourcePos.getZ() + horizontalNavigationMargin));
    }
}
