package com.animania.common.entity.goal;

import com.animania.common.AnimaniaBlocks;
import com.animania.common.block.AnimaniaStorageBlockEntity;
import com.animania.common.config.AnimaniaConfig;
import com.animania.common.entity.AnimaniaAnimalEntity;
import net.minecraft.core.BlockPos;
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

import java.util.EnumSet;

/** Path-based replacement for 1.12 GenericAIFindFood. */
public final class AnimaniaFindFoodGoal extends Goal {
    private enum Kind { STORAGE_ITEM, STORAGE_SLOP, BLOCK }
    private final AnimaniaAnimalEntity animal;
    private final boolean searchStorage;
    private final boolean searchBlocks;
    private BlockPos target;
    private Kind kind;
    private int delay;

    public AnimaniaFindFoodGoal(AnimaniaAnimalEntity animal) {
        this(animal, true, true);
    }

    /** Search controls are exposed for isolated GameTests; normal AI searches both sources. */
    public AnimaniaFindFoodGoal(AnimaniaAnimalEntity animal, boolean searchStorage, boolean searchBlocks) {
        this.animal = animal;
        this.searchStorage = searchStorage;
        this.searchBlocks = searchBlocks;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (++delay <= configured(AnimaniaConfig.AI_TICKS_BETWEEN_FIRINGS, 100)) return false;
        if (animal.getHunger() >= 100 || animal.isPassenger() || animal.isSleeping()
                || (configured(AnimaniaConfig.REQUIRE_ANIMAL_INTERACTION_FOR_AI, true) && !animal.hasInteracted())) {
            delay = 0;
            return false;
        }
        if (animal.getRandom().nextInt(3) != 0) return false;
        delay = 0;
        target = searchStorage ? findStorageTarget() : null;
        if (target == null && searchBlocks && eatsBlocks()) {
            target = findBlockTarget();
            if (target != null) kind = Kind.BLOCK;
        }
        return target != null;
    }

    @Override public void start() {
        if (target != null) animal.getNavigation().moveTo(target.getX() + 0.5D, target.getY(), target.getZ() + 0.5D, 1.0D);
    }

    @Override public boolean canContinueToUse() {
        return target != null && animal.getHunger() < 100 && !animal.isSleeping() && !animal.getNavigation().isDone();
    }

    @Override public void tick() {
        if (target == null || animal.distanceToSqr(target.getX() + 0.5D, target.getY() + 0.5D,
                target.getZ() + 0.5D) > 4.0D) return;
        boolean consumed = switch (kind) {
            case STORAGE_ITEM -> consumeStorageItem();
            case STORAGE_SLOP -> consumeStorageSlop();
            case BLOCK -> consumeBlock();
        };
        if (consumed) {
            animal.setEatingTicks(160);
            animal.getNavigation().stop();
        }
    }

    @Override public void stop() { target = null; kind = null; }
    public BlockPos target() { return target; }

    private BlockPos findStorageTarget() {
        BlockPos found = findNearest((pos, state) -> storageKind(pos) != null);
        if (found != null) kind = storageKind(found);
        return found;
    }

    private Kind storageKind(BlockPos pos) {
        if (!(animal.level().getBlockEntity(pos) instanceof AnimaniaStorageBlockEntity storage)) return null;
        for (int slot = 0; slot < storage.getContainerSize(); slot++) {
            ItemStack stack = storage.getItem(slot);
            if (!stack.isEmpty() && animal.acceptsFood(stack) && storageAcceptsFood(pos, stack)) {
                return Kind.STORAGE_ITEM;
            }
        }
        return isPig() && storage.fluidAmount(AnimaniaFindFoodGoal::isSlop) >= 100 ? Kind.STORAGE_SLOP : null;
    }

    private BlockPos findBlockTarget() { return findNearest((pos, state) -> isFoodBlock(state)); }

    private BlockPos findNearest(BlockMatcher matcher) {
        int range = Math.max(1, configured(AnimaniaConfig.AI_BLOCK_SEARCH_RANGE, 16));
        BlockPos origin = animal.blockPosition();
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;
        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-range, -2, -range), origin.offset(range, 2, range))) {
            if (!matcher.matches(pos, animal.level().getBlockState(pos))) continue;
            double distance = pos.distSqr(origin);
            if (distance < bestDistance) { best = pos.immutable(); bestDistance = distance; }
        }
        return best;
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
    private interface BlockMatcher { boolean matches(BlockPos pos, BlockState state); }
    private static boolean configured(net.minecraftforge.common.ForgeConfigSpec.BooleanValue value, boolean fallback) {
        try { return value.get(); } catch (IllegalStateException ignored) { return fallback; }
    }
    private static int configured(net.minecraftforge.common.ForgeConfigSpec.IntValue value, int fallback) {
        try { return value.get(); } catch (IllegalStateException ignored) { return fallback; }
    }
}
