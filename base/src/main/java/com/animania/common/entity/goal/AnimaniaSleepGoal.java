package com.animania.common.entity.goal;

import com.animania.common.config.AnimaniaConfig;
import com.animania.common.entity.AnimaniaAnimalEntity;
import com.animania.common.entity.AnimaniaSleepProfiles;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.block.Block;

import java.util.EnumSet;

/** Bed-seeking sleep AI preserving the 1.12 timing, weather and wake rules. */
public final class AnimaniaSleepGoal extends Goal {
    private final AnimaniaAnimalEntity animal;
    private BlockPos bedPos;
    private int delay;
    private int walkTries;

    public AnimaniaSleepGoal(AnimaniaAnimalEntity animal) {
        this.animal = animal;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        AnimaniaSleepProfiles.Profile profile = AnimaniaSleepProfiles.resolve(animal).orElse(null);
        if (profile == null || !configured(AnimaniaConfig.ANIMALS_SLEEP, true)) {
            wake();
            return false;
        }
        long time = animal.level().getDayTime();
        if (animal.isSleeping()) {
            if (!profile.shouldSleep(time) || animal.isOnFire() || animal.isSitting()
                    || animal.isLeashed() || animal.isPassenger()
                    || (animal.level().isRainingAt(animal.blockPosition())
                    && animal.level().canSeeSky(animal.blockPosition()))) {
                wake();
            }
            return false;
        }
        if (animal.isSitting() || animal.isLeashed() || animal.isPassenger() || animal.isOnFire()
                || animal.level().isRainingAt(animal.blockPosition()) || !profile.shouldSleep(time)) return false;
        if (++delay <= configured(AnimaniaConfig.AI_TICKS_BETWEEN_FIRINGS, 100) + animal.getRandom().nextInt(100)) {
            return false;
        }
        delay = 0;
        if (animal.getRandom().nextInt(3) != 0) return false;
        bedPos = findBed(profile);
        return bedPos != null;
    }

    @Override
    public void start() {
        walkTries = 0;
        moveToBed();
    }

    @Override
    public boolean canContinueToUse() {
        AnimaniaSleepProfiles.Profile profile = AnimaniaSleepProfiles.resolve(animal).orElse(null);
        return profile != null && bedPos != null && !animal.isSleeping() && profile.shouldSleep(animal.level().getDayTime())
                && isConfiguredBed(profile, bedPos) && !animal.isOnFire() && !animal.isLeashed()
                && !animal.isPassenger() && !animal.isSitting();
    }

    @Override
    public void tick() {
        if (bedPos == null) return;
        if (animal.distanceToSqr(bedPos.getX() + 0.5D, bedPos.getY() + 1.0D, bedPos.getZ() + 0.5D) <= 2.25D) {
            animal.getNavigation().stop();
            animal.setSleeping(true);
            animal.setDeltaMovement(0.0D, animal.getDeltaMovement().y, 0.0D);
        } else if (animal.getNavigation().isDone()) {
            if (++walkTries > 100) {
                bedPos = null;
            } else if (walkTries % 40 == 0) {
                moveToBed();
            }
        }
    }

    @Override
    public void stop() {
        bedPos = null;
        walkTries = 0;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    public BlockPos targetBed() {
        return bedPos;
    }

    /** Performs the configured bed search immediately; exposed for deterministic GameTests. */
    public boolean findTargetNow() {
        AnimaniaSleepProfiles.Profile profile = AnimaniaSleepProfiles.resolve(animal).orElse(null);
        return findTargetNow(profile);
    }

    /** Uses an isolated profile without mutating the live Forge configuration. */
    public boolean findTargetNow(AnimaniaSleepProfiles.Profile profile) {
        bedPos = profile == null ? null : findBed(profile);
        return bedPos != null;
    }

    private BlockPos findBed(AnimaniaSleepProfiles.Profile profile) {
        Block primary = profile.primaryBlock();
        Block secondary = profile.secondaryBlock();
        if (primary == null && secondary == null) return null;
        int range = Math.max(1, configured(AnimaniaConfig.AI_BLOCK_SEARCH_RANGE, 16));
        int verticalRange = Math.max(1, range / 2);
        BlockPos origin = animal.blockPosition();
        BlockPos bestSecondary = null;
        for (BlockPos candidate : BlockPos.withinManhattan(origin, range, verticalRange, range)) {
            Block block = animal.level().getBlockState(candidate).getBlock();
            if ((block != primary && block != secondary) || !animal.level().getBlockState(candidate.above()).isAir()) continue;
            // Keep scanning for a preferred bed, but do not recalculate a path for
            // every block of a large fallback surface such as a grass pasture.
            if (block != primary && bestSecondary != null) continue;
            if (!isReachableBed(candidate)) continue;
            if (block == primary) return candidate.immutable();
            bestSecondary = candidate.immutable();
        }
        return bestSecondary;
    }

    /**
     * A partial path toward an inaccessible bed used to make fenced animals
     * pile up at the same wall or corner.  Require both enough body room and a
     * path that reaches the block above the configured bedding.
     */
    private boolean isReachableBed(BlockPos bed) {
        BlockPos standing = bed.above();
        double x = standing.getX() + 0.5D;
        double z = standing.getZ() + 0.5D;
        // An animal already occupying the destination proves that the space is usable;
        // this also avoids edge-of-GameTest structure collision artifacts.
        if (animal.distanceToSqr(x, standing.getY(), z) <= 2.25D) return true;
        if (!animal.level().noCollision(animal, animal.getBoundingBox().move(
                x - animal.getX(), standing.getY() - animal.getY(), z - animal.getZ()).deflate(0.01D))) {
            return false;
        }
        Path path = animal.getNavigation().createPath(standing, 0);
        return path != null && path.canReach();
    }

    private void wake() {
        if (animal.isSleeping()) animal.setSleeping(false);
    }

    private void moveToBed() {
        if (bedPos != null) animal.getNavigation().moveTo(
                bedPos.getX() + 0.5D, bedPos.getY() + 1.0D, bedPos.getZ() + 0.5D, 0.8D);
    }

    private boolean isConfiguredBed(AnimaniaSleepProfiles.Profile profile, BlockPos position) {
        Block block = animal.level().getBlockState(position).getBlock();
        return block == profile.primaryBlock() || block == profile.secondaryBlock();
    }

    private static boolean configured(net.minecraftforge.common.ForgeConfigSpec.BooleanValue value, boolean fallback) {
        try { return value.get(); } catch (IllegalStateException ignored) { return fallback; }
    }

    private static int configured(net.minecraftforge.common.ForgeConfigSpec.IntValue value, int fallback) {
        try { return value.get(); } catch (IllegalStateException ignored) { return fallback; }
    }
}
