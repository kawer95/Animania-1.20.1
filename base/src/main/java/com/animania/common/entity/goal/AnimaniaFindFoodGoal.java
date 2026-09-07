package com.animania.common.entity.goal;

import com.animania.common.AnimaniaBlocks;
import com.animania.common.block.AnimalBlockInteractionRange;
import com.animania.common.block.AnimaniaStorageBlockEntity;
import com.animania.common.block.AnimaniaTroughBlock;
import com.animania.common.config.AnimaniaConfig;
import com.animania.common.entity.AnimaniaAnimalEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.IPlantable;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Path-based replacement for 1.12 GenericAIFindFood. */
public final class AnimaniaFindFoodGoal extends Goal {
    private enum Kind { STORAGE_ITEM, STORAGE_SLOP, BLOCK }
    private final AnimaniaAnimalEntity animal;
    private final boolean searchStorage;
    private final boolean searchBlocks;
    private BlockPos target;
    private BlockPos approach;
    private Kind kind;
    private int delay;
    private int nextSearchDelay;
    private final Map<BlockPos, Long> rejectedTargets = new HashMap<>();

    public AnimaniaFindFoodGoal(AnimaniaAnimalEntity animal) {
        this(animal, true, true);
    }

    /** Search controls are exposed for isolated GameTests; normal AI searches both sources. */
    public AnimaniaFindFoodGoal(AnimaniaAnimalEntity animal, boolean searchStorage, boolean searchBlocks) {
        this.animal = animal;
        this.searchStorage = searchStorage;
        this.searchBlocks = searchBlocks;
        this.nextSearchDelay = animal.getRandom().nextInt(searchInterval() + 1);
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (++delay <= nextSearchDelay) return false;
        if (!animal.shouldSeekFood() || animal.isPassenger() || animal.isSleeping()
                || (configured(AnimaniaConfig.REQUIRE_ANIMAL_INTERACTION_FOR_AI, true) && !animal.hasInteracted())) {
            scheduleNextSearch();
            return false;
        }
        if (animal.getRandom().nextInt(3) != 0) return false;
        scheduleNextSearch();
        target = searchStorage ? findStorageTarget() : null;
        if (target == null && searchBlocks && eatsBlocks()) {
            target = findBlockTarget();
            if (target != null) {
                kind = Kind.BLOCK;
            }
        }
        if (target == null) {
            scheduleBackoff();
            return false;
        }
        return true;
    }

    @Override public void start() {
        if (target != null && !inConsumptionRange()) {
            BlockPos destination = approach != null ? approach : target;
            if (!animal.getNavigation().moveTo(destination.getX() + 0.5D, destination.getY(),
                    destination.getZ() + 0.5D, 1.0D)) {
                rejectCurrentTarget();
            }
        }
    }

    @Override public boolean canContinueToUse() {
        if (target == null || !animal.shouldSeekFood() || animal.isSleeping() || !targetStillValid()) return false;
        if (!animal.getNavigation().isDone() || inConsumptionRange()) return true;
        rejectCurrentTarget();
        return false;
    }

    @Override public void tick() {
        if (target == null || !inConsumptionRange()) return;
        boolean consumed = switch (kind) {
            case STORAGE_ITEM -> consumeStorageItem();
            case STORAGE_SLOP -> consumeStorageSlop();
            case BLOCK -> consumeBlock();
        };
        if (consumed) {
            animal.setEatingTicks(160);
            animal.getNavigation().stop();
            abandonTarget();
        } else {
            abandonTarget();
        }
    }

    @Override public void stop() {
        animal.getNavigation().stop();
        abandonTarget();
    }
    public BlockPos target() { return target; }
    public BlockPos approach() { return approach; }
    public int nextSearchDelay() { return nextSearchDelay; }

    private BlockPos findStorageTarget() {
        pruneRejectedTargets();
        int range = Math.max(1, configured(AnimaniaConfig.AI_BLOCK_SEARCH_RANGE, 16));
        BlockPos origin = animal.blockPosition();
        BlockPos bestTarget = null;
        BlockPos bestApproach = null;
        Kind bestKind = null;
        double bestDistance = Double.MAX_VALUE;
        for (BlockPos pos : BlockPos.betweenClosed(
                origin.offset(-range, -2, -range), origin.offset(range, 2, range))) {
            if (isRejected(pos)) continue;
            Kind candidateKind = storageKind(pos);
            if (candidateKind == null) continue;
            boolean trough = isTrough(pos);
            boolean directlyReachable = trough || AnimalBlockInteractionRange.contains(pos, animal);
            BlockPos candidateApproach = directlyReachable ? null : findStorageApproach(pos);
            if (!directlyReachable && candidateApproach == null) continue;
            double distance = pos.distSqr(origin);
            if (distance < bestDistance) {
                bestTarget = pos.immutable();
                bestApproach = candidateApproach;
                bestKind = candidateKind;
                bestDistance = distance;
            }
        }
        if (bestTarget != null) {
            approach = bestApproach;
            kind = bestKind;
        }
        return bestTarget;
    }

    private Kind storageKind(BlockPos pos) {
        if (!(animal.level().getBlockEntity(pos) instanceof AnimaniaStorageBlockEntity storage)) return null;
        if (!storage.providesAnimalFood()) return null;
        for (int slot = 0; slot < storage.getContainerSize(); slot++) {
            ItemStack stack = storage.getItem(slot);
            if (!stack.isEmpty() && animal.acceptsFood(stack) && storageAcceptsFood(pos, stack)) {
                return Kind.STORAGE_ITEM;
            }
        }
        return isPig() && storage.fluidAmount(AnimaniaFindFoodGoal::isSlop) >= 100 ? Kind.STORAGE_SLOP : null;
    }

    private BlockPos findBlockTarget() {
        pruneRejectedTargets();
        return findNearest(pos -> !isRejected(pos) && isFoodBlock(animal.level().getBlockState(pos)));
    }

    private boolean inConsumptionRange() {
        if (target == null) return false;
        if (kind == Kind.BLOCK) {
            return animal.distanceToSqr(target.getX() + 0.5D, target.getY() + 0.5D,
                    target.getZ() + 0.5D) <= 4.0D;
        }
        BlockState state = animal.level().getBlockState(target);
        if (state.getBlock() instanceof AnimaniaTroughBlock trough) {
            return trough.isWithinAnimalInteractionRange(target, state, animal);
        }
        return AnimalBlockInteractionRange.contains(target, animal);
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
        if (!animal.level().getBlockState(pos).getCollisionShape(animal.level(), pos).isEmpty()
                || !animal.level().getBlockState(pos.above())
                .getCollisionShape(animal.level(), pos.above()).isEmpty()
                || animal.level().getBlockState(pos.below())
                .getCollisionShape(animal.level(), pos.below()).isEmpty()) return false;
        double x = pos.getX() + 0.5D;
        double z = pos.getZ() + 0.5D;
        return animal.level().noCollision(animal, animal.getBoundingBox().move(
                x - animal.getX(), pos.getY() - animal.getY(), z - animal.getZ()).deflate(0.01D));
    }

    private BlockPos findNearest(java.util.function.Predicate<BlockPos> matcher) {
        int range = Math.max(1, configured(AnimaniaConfig.AI_BLOCK_SEARCH_RANGE, 16));
        BlockPos origin = animal.blockPosition();
        return BlockPos.findClosestMatch(origin, range, 2, matcher).map(BlockPos::immutable).orElse(null);
    }

    private boolean targetStillValid() {
        if (target == null || kind == null) return false;
        if (kind == Kind.BLOCK) return isFoodBlock(animal.level().getBlockState(target));
        Kind currentKind = storageKind(target);
        if (currentKind == null) return false;
        kind = currentKind;
        return true;
    }

    private void abandonTarget() {
        target = null;
        approach = null;
        kind = null;
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

    private boolean consumeStorageItem() {
        if (!(animal.level().getBlockEntity(target) instanceof AnimaniaStorageBlockEntity storage)) return false;
        for (int slot = 0; slot < storage.getContainerSize(); slot++) {
            ItemStack stored = storage.getItem(slot);
            if (stored.isEmpty() || !animal.acceptsFood(stored) || !storageAcceptsFood(target, stored)) continue;
            if (!animal.feed(stored.copyWithCount(1))) return false;
            animal.setHunger(100);
            if (configured(AnimaniaConfig.PLANTS_REMOVED_AFTER_EATING, true)) storage.removeItem(slot, 1);
            animal.markInteracted();
            return true;
        }
        return false;
    }

    private boolean consumeStorageSlop() {
        if (!(animal.level().getBlockEntity(target) instanceof AnimaniaStorageBlockEntity storage)) return false;
        if (storage.drainFluid(100, AnimaniaFindFoodGoal::isSlop, IFluidHandler.FluidAction.EXECUTE) != 100) return false;
        animal.setHunger(100);
        animal.markInteracted();
        return true;
    }

    private boolean consumeBlock() {
        BlockState state = animal.level().getBlockState(target);
        if (!isFoodBlock(state)) return false;
        animal.setHunger(100);
        if (configured(AnimaniaConfig.PLANTS_REMOVED_AFTER_EATING, true)) animal.level().destroyBlock(target, false);
        return true;
    }

    private boolean eatsBlocks() {
        String path = animal.registryPath();
        return !(path.startsWith("tom_") || path.startsWith("queen_") || path.startsWith("kitten_")
                || path.startsWith("male_") || path.startsWith("female_") || path.startsWith("puppy_")
                || path.startsWith("ferret_") || path.startsWith("hedgehog") || path.equals("hamster"));
    }

    private boolean isFoodBlock(BlockState state) {
        String path = animal.registryPath();
        if (path.startsWith("hen_") || path.startsWith("rooster_") || path.startsWith("chick_")
                || path.startsWith("peahen_") || path.startsWith("peacock_") || path.startsWith("peachick_")) {
            return state.is(AnimaniaBlocks.SEEDS.get());
        }
        if (path.startsWith("doe_") || path.startsWith("buck_") || path.startsWith("kit_")) {
            ResourceLocation type = ForgeRegistries.ENTITY_TYPES.getKey(animal.getType());
            if (type != null && type.getNamespace().equals("animania_extra")) {
                return state.getBlock() instanceof net.minecraft.world.level.block.CarrotBlock
                        || state.is(BlockTags.FLOWERS) || state.is(net.minecraft.world.level.block.Blocks.TALL_GRASS);
            }
        }
        if (isPig() && isSlop(state.getFluidState().getType())) return state.getFluidState().isSource();
        return state.getBlock() instanceof CropBlock || state.getBlock() instanceof BushBlock
                || state.getBlock() instanceof IPlantable;
    }

    private boolean isPig() {
        String path = animal.registryPath();
        return path.startsWith("sow_") || path.startsWith("hog_") || path.startsWith("piglet_");
    }

    private boolean storageAcceptsFood(BlockPos pos, ItemStack stack) {
        ResourceLocation blockId = ForgeRegistries.BLOCKS.getKey(animal.level().getBlockState(pos).getBlock());
        // The legacy pet bowl owns its own configurable food list. Requiring the
        // Base trough list as well incorrectly rejects valid cat/dog food.
        if (blockId != null && blockId.getNamespace().equals("animania_catsdogs")
                && blockId.getPath().equals("pet_bowl")) return true;
        return AnimaniaConfig.matchesTroughFood(stack);
    }

    private static boolean isSlop(net.minecraftforge.fluids.FluidStack stack) { return isSlop(stack.getFluid()); }
    private static boolean isSlop(net.minecraft.world.level.material.Fluid fluid) {
        ResourceLocation id = ForgeRegistries.FLUIDS.getKey(fluid);
        return id != null && id.getNamespace().equals("animania") && id.getPath().equals("slop");
    }
    private static boolean configured(net.minecraftforge.common.ForgeConfigSpec.BooleanValue value, boolean fallback) {
        try { return value.get(); } catch (IllegalStateException ignored) { return fallback; }
    }
    private static int configured(net.minecraftforge.common.ForgeConfigSpec.IntValue value, int fallback) {
        try { return value.get(); } catch (IllegalStateException ignored) { return fallback; }
    }
}
