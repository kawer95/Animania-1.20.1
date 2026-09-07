package com.animania.common.entity.goal;

import com.animania.common.block.AnimalBlockInteractionRange;
import com.animania.common.block.AnimaniaStorageBlockEntity;
import com.animania.common.block.AnimaniaTroughBlock;
import com.animania.common.config.AnimaniaConfig;
import com.animania.common.entity.AnimaniaAnimalEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.FluidState;
import net.minecraftforge.fluids.capability.IFluidHandler;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/** Path-based replacement for 1.12 GenericAIFindWater. */
public final class AnimaniaFindWaterGoal extends Goal {
    private final AnimaniaAnimalEntity animal;
    private final boolean searchTroughs;
    private final boolean searchNaturalWater;
    private final Predicate<BlockPos> targetFilter;
    private final boolean enforceBiomeRules;
    private BlockPos target;
    private BlockPos approach;
    private boolean trough;
    private int delay;
    private int nextSearchDelay;
    private final Map<BlockPos, Long> rejectedTargets = new HashMap<>();

    public AnimaniaFindWaterGoal(AnimaniaAnimalEntity animal) {
        this(animal, true, true, ignored -> true, true);
    }

    /** Search controls are exposed for isolated GameTests; normal AI searches both sources. */
    public AnimaniaFindWaterGoal(AnimaniaAnimalEntity animal, boolean searchTroughs, boolean searchNaturalWater) {
        this(animal, searchTroughs, searchNaturalWater, ignored -> true, true);
    }

    /** Target filter is test injection only; production constructors accept every legal source. */
    public AnimaniaFindWaterGoal(AnimaniaAnimalEntity animal, boolean searchTroughs, boolean searchNaturalWater,
                                 Predicate<BlockPos> targetFilter) {
        this(animal, searchTroughs, searchNaturalWater, targetFilter, false);
    }

    private AnimaniaFindWaterGoal(AnimaniaAnimalEntity animal, boolean searchTroughs, boolean searchNaturalWater,
                                  Predicate<BlockPos> targetFilter, boolean enforceBiomeRules) {
        this.animal = animal;
        this.searchTroughs = searchTroughs;
        this.searchNaturalWater = searchNaturalWater;
        this.targetFilter = targetFilter;
        this.enforceBiomeRules = enforceBiomeRules;
        this.nextSearchDelay = animal.getRandom().nextInt(searchInterval() + 1);
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (++delay <= nextSearchDelay) return false;
        if (!animal.shouldSeekWater() || animal.isPassenger() || animal.isSleeping()) {
            scheduleNextSearch();
            return false;
        }
        if (configured(AnimaniaConfig.REQUIRE_ANIMAL_INTERACTION_FOR_AI, true) && !animal.hasInteracted()) {
            // The interaction gate still prevents unattended animals from
            // pathfinding across the enclosure. It must not suppress the new
            // direct-use behavior once an already-thirsty animal is standing
            // beside an ordinary water source, though.
            boolean foundDirectWater = selectDirectNaturalWater();
            scheduleNextSearch();
            return foundDirectWater;
        }
        if (animal.getRandom().nextInt(3) != 0) return false;
        scheduleNextSearch();
        target = searchTroughs ? findTarget(true) : null;
        trough = target != null;
        if (target == null && searchNaturalWater) target = findTarget(false);
        if (target == null) {
            scheduleBackoff();
            return false;
        }
        return true;
    }

    @Override
    public void start() {
        if (target != null && !inConsumptionRange()) {
            BlockPos destination = approach != null ? approach
                    : trough ? target : AnimalBlockInteractionRange.closestBlock(target, animal.blockPosition());
            if (!animal.getNavigation().moveTo(destination.getX() + 0.5D, destination.getY(),
                    destination.getZ() + 0.5D, 1.0D)) {
                rejectCurrentTarget();
            }
        }
    }

    @Override
    public boolean canContinueToUse() {
        if (target == null || !animal.shouldSeekWater() || animal.isSleeping() || !targetStillValid()) return false;
        if (!animal.getNavigation().isDone() || inConsumptionRange()) return true;
        rejectCurrentTarget();
        return false;
    }

    @Override
    public void tick() {
        if (target == null || !inConsumptionRange()) return;
        int amount = halfAmount() ? 50 : 100;
        boolean consumed = false;
        if (trough && animal.level().getBlockEntity(target) instanceof AnimaniaStorageBlockEntity storage) {
            if (storage.drainFluid(amount, stack -> stack.getFluid().is(FluidTags.WATER),
                    IFluidHandler.FluidAction.EXECUTE) == amount) {
                animal.setThirst(100);
                animal.markInteracted();
                consumed = true;
            }
        } else {
            FluidState fluid = animal.level().getFluidState(target);
            if (isDrinkableNaturalWater(target, fluid)) {
                animal.setThirst(100);
                animal.markInteracted();
                consumed = true;
                if (!halfAmount() && configured(AnimaniaConfig.WATER_REMOVED_AFTER_DRINKING, true)) {
                    consumeNaturalWater(target);
                }
            }
        }
        if (consumed) animal.setEatingTicks(80);
        animal.getNavigation().stop();
        abandonTarget();
    }

    @Override
    public void stop() {
        animal.getNavigation().stop();
        abandonTarget();
    }

    public BlockPos target() { return target; }
    public BlockPos approach() { return approach; }
    public boolean targetsTrough() { return trough; }
    public int nextSearchDelay() { return nextSearchDelay; }

    private BlockPos findTarget(boolean wantTrough) {
        pruneRejectedTargets();
        int range = Math.max(1, configured(AnimaniaConfig.AI_BLOCK_SEARCH_RANGE, 16));
        BlockPos origin = animal.blockPosition();
        if (wantTrough) {
            BlockPos bestTarget = null;
            BlockPos bestApproach = null;
            double bestDistance = Double.MAX_VALUE;
            for (BlockPos pos : BlockPos.betweenClosed(
                    origin.offset(-range, -2, -range), origin.offset(range, 2, range))) {
                if (!targetFilter.test(pos) || isRejected(pos) || !isValidStorageTarget(pos)) continue;
                boolean troughBlock = isTrough(pos);
                boolean directlyReachable = troughBlock || AnimalBlockInteractionRange.contains(pos, animal);
                BlockPos candidateApproach = directlyReachable ? null : findStorageApproach(pos);
                if (!directlyReachable && candidateApproach == null) continue;
                double distance = pos.distSqr(origin);
                if (distance < bestDistance) {
                    bestTarget = pos.immutable();
                    bestApproach = candidateApproach;
                    bestDistance = distance;
                }
            }
            if (bestTarget != null) {
                approach = bestApproach;
            }
            return bestTarget;
        }
        BlockPos bestTarget = null;
        double bestDistance = Double.MAX_VALUE;
        for (BlockPos pos : BlockPos.betweenClosed(
                origin.offset(-range, -2, -range), origin.offset(range, 2, range))) {
            if (!targetFilter.test(pos) || isRejected(pos) || !isValidNaturalTarget(pos)) continue;
            double distance = pos.distSqr(origin);
            if (distance < bestDistance) {
                bestTarget = pos.immutable();
                bestDistance = distance;
            }
        }
        if (bestTarget != null) {
            approach = null;
        }
        return bestTarget;
    }

    /** Selects only water that already overlaps the direct interaction volume; no standing place is reserved. */
    private boolean selectDirectNaturalWater() {
        if (!searchNaturalWater) return false;
        pruneRejectedTargets();
        BlockPos origin = animal.blockPosition();
        BlockPos bestTarget = null;
        double bestDistance = Double.MAX_VALUE;
        // The interaction volume is two blocks horizontally and one block
        // vertically. The extra scan block accounts for large entity bounds.
        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-3, -2, -3), origin.offset(3, 2, 3))) {
            if (!targetFilter.test(pos) || isRejected(pos) || !isValidNaturalTarget(pos)
                    || !AnimalBlockInteractionRange.contains(pos, animal)) continue;
            double distance = pos.distSqr(origin);
            if (distance < bestDistance) {
                bestTarget = pos.immutable();
                bestDistance = distance;
            }
        }
        if (bestTarget == null) return false;
        target = bestTarget;
        approach = null;
        trough = false;
        return true;
    }

    private boolean isValidStorageTarget(BlockPos pos) {
        return animal.level().getBlockEntity(pos) instanceof AnimaniaStorageBlockEntity storage
                && storage.providesAnimalWater()
                && storage.fluidAmount(stack -> stack.getFluid().is(FluidTags.WATER)) >= (halfAmount() ? 50 : 100);
    }

    private BlockPos findStorageApproach(BlockPos storagePos) {
        List<BlockPos> candidates = storageApproachCandidates(storagePos);
        candidates.sort(Comparator.comparingDouble(pos -> pos.distSqr(animal.blockPosition())));
        for (BlockPos horizontal : candidates) {
            BlockPos standing = findStandingPosition(horizontal);
            if (standing == null) continue;
            return standing;
        }
        return null;
    }

    private List<BlockPos> storageApproachCandidates(BlockPos storagePos) {
        int laneDistance = 2;
        List<BlockPos> candidates = new ArrayList<>();
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            candidates.add(storagePos.relative(direction, laneDistance));
        }
        return candidates;
    }

    private BlockPos findStandingPosition(BlockPos horizontal) {
        for (int yOffset : new int[]{0, 1, -1, 2}) {
            BlockPos candidate = new BlockPos(horizontal.getX(), horizontal.getY() + yOffset, horizontal.getZ());
            if (!isClearStandingPosition(candidate)) continue;
            return candidate.immutable();
        }
        return null;
    }

    private boolean isClearStandingPosition(BlockPos pos) {
        if (!animal.level().getFluidState(pos).isEmpty() || !animal.level().getFluidState(pos.above()).isEmpty()
                || !animal.level().getBlockState(pos).getCollisionShape(animal.level(), pos).isEmpty()
                || !animal.level().getBlockState(pos.above())
                .getCollisionShape(animal.level(), pos.above()).isEmpty()
                || animal.level().getBlockState(pos.below())
                .getCollisionShape(animal.level(), pos.below()).isEmpty()) return false;
        double x = pos.getX() + 0.5D;
        double z = pos.getZ() + 0.5D;
        return animal.level().noCollision(animal, animal.getBoundingBox().move(
                x - animal.getX(), pos.getY() - animal.getY(), z - animal.getZ()).deflate(0.01D));
    }

    private boolean targetStillValid() {
        if (target == null) return false;
        if (trough) {
            return isValidStorageTarget(target);
        }
        return isValidNaturalTarget(target);
    }

    private boolean isValidNaturalTarget(BlockPos pos) {
        FluidState fluid = animal.level().getFluidState(pos);
        if (!isDrinkableNaturalWater(pos, fluid)) return false;
        if (!enforceBiomeRules) return true;
        var biome = animal.level().getBiome(pos);
        return !biome.is(BiomeTags.IS_OCEAN) && !biome.is(BiomeTags.IS_BEACH);
    }

    private void abandonTarget() {
        target = null;
        approach = null;
        trough = false;
    }

    private boolean isTrough(BlockPos pos) {
        return animal.level().getBlockState(pos).getBlock() instanceof AnimaniaTroughBlock;
    }

    private void rejectCurrentTarget() {
        if (target != null) {
            scheduleBackoff();
            rejectedTargets.put(target.immutable(), animal.level().getGameTime() + nextSearchDelay);
        }
        animal.getNavigation().stop();
        abandonTarget();
    }

    private boolean isRejected(BlockPos pos) {
        Long expires = rejectedTargets.get(pos);
        return expires != null && expires >= animal.level().getGameTime();
    }

    private void pruneRejectedTargets() {
        long gameTime = animal.level().getGameTime();
        rejectedTargets.entrySet().removeIf(entry -> entry.getValue() < gameTime);
    }

    private void scheduleNextSearch() {
        int interval = searchInterval();
        delay = 0;
        nextSearchDelay = interval + animal.getRandom().nextInt(Math.max(2, interval / 4 + 1));
    }

    private void scheduleBackoff() {
        int interval = searchInterval();
        delay = 0;
        nextSearchDelay = interval * 2 + animal.getRandom().nextInt(Math.max(2, interval * 2 + 1));
    }

    private int searchInterval() {
        return Math.max(1, configured(AnimaniaConfig.AI_TICKS_BETWEEN_FIRINGS, 100));
    }

    private boolean inConsumptionRange() {
        if (target == null) return false;
        if (!trough) {
            return AnimalBlockInteractionRange.contains(target, animal);
        }
        BlockState state = animal.level().getBlockState(target);
        if (state.getBlock() instanceof AnimaniaTroughBlock troughBlock) {
            return troughBlock.isWithinAnimalInteractionRange(target, state, animal);
        }
        return AnimalBlockInteractionRange.contains(target, animal);
    }

    /**
     * A bucket can only take a source fluid.  Waterlogged blocks expose the
     * same source FluidState as a water block, so they are valid drinking
     * targets as well; the host block is handled separately when the source is
     * consumed.
     */
    private boolean isDrinkableNaturalWater(BlockPos pos, FluidState fluid) {
        BlockState state = animal.level().getBlockState(pos);
        return fluid.is(FluidTags.WATER) && fluid.isSource()
                && (state.getBlock() instanceof LiquidBlock || isWaterlogged(state));
    }

    /**
     * Consume water with bucket semantics: remove a standalone source block,
     * or clear only WATERLOGGED on a slab/stair/etc.  Never replace a
     * waterlogged host with air.
     */
    private void consumeNaturalWater(BlockPos pos) {
        BlockState state = animal.level().getBlockState(pos);
        if (!isDrinkableNaturalWater(pos, animal.level().getFluidState(pos))) return;
        if (isWaterlogged(state)) {
            animal.level().setBlock(pos, state.setValue(BlockStateProperties.WATERLOGGED, false), 3);
        } else if (state.getBlock() instanceof LiquidBlock) {
            // Do not punch a temporary or permanent hole in a vanilla-style
            // infinite pool. An isolated source still follows the configured
            // consume-after-drinking rule.
            if (hasTwoAdjacentWaterSources(pos)) return;
            animal.level().setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        }
    }

    private boolean hasTwoAdjacentWaterSources(BlockPos pos) {
        int adjacentSources = 0;
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            FluidState adjacent = animal.level().getFluidState(pos.relative(direction));
            if (adjacent.is(FluidTags.WATER) && adjacent.isSource() && ++adjacentSources >= 2) return true;
        }
        return false;
    }

    private static boolean isWaterlogged(BlockState state) {
        return state.hasProperty(BlockStateProperties.WATERLOGGED)
                && state.getValue(BlockStateProperties.WATERLOGGED);
    }

    private boolean halfAmount() {
        String path = animal.registryPath();
        return path.startsWith("hen_") || path.startsWith("rooster_") || path.startsWith("chick_")
                || path.startsWith("peahen_") || path.startsWith("peacock_") || path.startsWith("peachick_")
                || path.startsWith("doe_") && animal.registryNamespace().equals("animania_extra")
                || path.startsWith("buck_") && animal.registryNamespace().equals("animania_extra")
                || path.startsWith("kit_") || path.startsWith("ferret_") || path.startsWith("hedgehog")
                || path.equals("hamster") || path.startsWith("tom_") || path.startsWith("queen_")
                || path.startsWith("kitten_") || path.startsWith("male_") || path.startsWith("female_")
                || path.startsWith("puppy_");
    }

    private static boolean configured(net.minecraftforge.common.ForgeConfigSpec.BooleanValue value, boolean fallback) {
        try { return value.get(); } catch (IllegalStateException ignored) { return fallback; }
    }
    private static int configured(net.minecraftforge.common.ForgeConfigSpec.IntValue value, int fallback) {
        try { return value.get(); } catch (IllegalStateException ignored) { return fallback; }
    }
}
