package com.animania.common.entity.goal;

import com.animania.common.block.AnimaniaStorageBlockEntity;
import com.animania.common.config.AnimaniaConfig;
import com.animania.common.entity.AnimaniaAnimalEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.FluidState;
import net.minecraftforge.fluids.capability.IFluidHandler;

import java.util.EnumSet;
import java.util.function.Predicate;

/** Path-based replacement for 1.12 GenericAIFindWater. */
public final class AnimaniaFindWaterGoal extends Goal {
    private final AnimaniaAnimalEntity animal;
    private final boolean searchTroughs;
    private final boolean searchNaturalWater;
    private final Predicate<BlockPos> targetFilter;
    private final boolean enforceBiomeRules;
    private BlockPos target;
    private boolean trough;
    private int delay;

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
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (++delay <= configured(AnimaniaConfig.AI_TICKS_BETWEEN_FIRINGS, 100)) return false;
        if (animal.getThirst() >= 100 || animal.isPassenger() || animal.isSleeping()
                || (configured(AnimaniaConfig.REQUIRE_ANIMAL_INTERACTION_FOR_AI, true) && !animal.hasInteracted())) {
            delay = 0;
            return false;
        }
        if (animal.getRandom().nextInt(3) != 0) return false;
        delay = 0;
        target = searchTroughs ? findTarget(true) : null;
        trough = target != null;
        if (target == null && searchNaturalWater) target = findTarget(false);
        return target != null;
    }

    @Override
    public void start() {
        if (target != null) animal.getNavigation().moveTo(target.getX() + 0.5D, target.getY(), target.getZ() + 0.5D, 1.0D);
    }

    @Override
    public boolean canContinueToUse() {
        return target != null && animal.getThirst() < 100 && !animal.isSleeping()
                && !animal.getNavigation().isDone();
    }

    @Override
    public void tick() {
        if (target == null || animal.distanceToSqr(target.getX() + 0.5D, target.getY() + 0.5D,
                target.getZ() + 0.5D) > 4.0D) return;
        int amount = halfAmount() ? 50 : 100;
        if (trough && animal.level().getBlockEntity(target) instanceof AnimaniaStorageBlockEntity storage) {
            if (storage.drainFluid(amount, stack -> stack.getFluid().is(FluidTags.WATER),
                    IFluidHandler.FluidAction.EXECUTE) == amount) {
                animal.setThirst(100);
                animal.markInteracted();
            }
        } else {
            FluidState fluid = animal.level().getFluidState(target);
            if (isDrinkableNaturalWater(target, fluid)) {
                animal.setThirst(100);
                if (!halfAmount() && configured(AnimaniaConfig.WATER_REMOVED_AFTER_DRINKING, true)) {
                    consumeNaturalWater(target);
                }
            }
        }
        animal.setEatingTicks(80);
        animal.getNavigation().stop();
    }

    @Override
    public void stop() {
        target = null;
        trough = false;
    }

    public BlockPos target() { return target; }
    public boolean targetsTrough() { return trough; }

    private BlockPos findTarget(boolean wantTrough) {
        int range = Math.max(1, configured(AnimaniaConfig.AI_BLOCK_SEARCH_RANGE, 16));
        BlockPos origin = animal.blockPosition();
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;
        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-range, -2, -range), origin.offset(range, 2, range))) {
            if (!targetFilter.test(pos)) continue;
            boolean match;
            if (wantTrough) {
                match = animal.level().getBlockEntity(pos) instanceof AnimaniaStorageBlockEntity storage
                        && storage.fluidAmount(stack -> stack.getFluid().is(FluidTags.WATER)) >= (halfAmount() ? 50 : 100);
            } else {
                FluidState fluid = animal.level().getFluidState(pos);
                var biome = animal.level().getBiome(pos);
                match = isDrinkableNaturalWater(pos, fluid)
                        && (!enforceBiomeRules || (!biome.is(BiomeTags.IS_OCEAN) && !biome.is(BiomeTags.IS_BEACH)));
            }
            if (!match) continue;
            double distance = pos.distSqr(origin);
            if (distance < bestDistance) { best = pos.immutable(); bestDistance = distance; }
        }
        return best;
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
            animal.level().setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        }
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
