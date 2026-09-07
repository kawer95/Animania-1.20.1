package com.animania.common.entity.goal;

import com.animania.common.AnimaniaBlocks;
import com.animania.common.config.AnimaniaConfig;
import com.animania.common.entity.AnimaniaAnimalEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.EnumSet;

/**
 * Restores the distinct 1.12 ferret/hedgehog nest-foraging goals.  The old
 * implementation consumed chicken eggs from a nest; hedgehogs additionally
 * uprooted mature carrots, potatoes, and beetroot crops.  All mutations are
 * made on the server and use the modern block-entity inventory.
 */
public final class AnimaniaFindNestFoodGoal extends Goal {
    private final AnimaniaAnimalEntity animal;
    private final double speed;
    private BlockPos target;
    private boolean crop;
    private int delay;

    public AnimaniaFindNestFoodGoal(AnimaniaAnimalEntity animal) {
        this(animal, 1.0D);
    }

    public AnimaniaFindNestFoodGoal(AnimaniaAnimalEntity animal, double speed) {
        this.animal = animal;
        this.speed = speed;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    public static boolean supports(AnimaniaAnimalEntity animal) {
        return animal.registryNamespace().equals("animania_extra")
                && (animal.registryPath().startsWith("ferret_") || animal.registryPath().startsWith("hedgehog"));
    }

    public static boolean hedgehogCrops(AnimaniaAnimalEntity animal) {
        return supports(animal) && animal.registryPath().startsWith("hedgehog");
    }

    @Override
    public boolean canUse() {
        if (animal.level().isClientSide || !supports(animal)) return false;
        if (++delay < configured(AnimaniaConfig.AI_TICKS_BETWEEN_FIRINGS, 100)) return false;
        delay = 0;
        if (animal.isSleeping() || !animal.shouldSeekFood() || animal.isPassenger()
                || (configured(AnimaniaConfig.REQUIRE_ANIMAL_INTERACTION_FOR_AI, true) && !animal.hasInteracted())) {
            return false;
        }
        target = findNearest();
        return target != null;
    }

    @Override
    public void start() {
        if (target != null) animal.getNavigation().moveTo(target.getX() + 0.5D, target.getY(), target.getZ() + 0.5D, speed);
    }

    @Override
    public boolean canContinueToUse() {
        return target != null && !animal.isSleeping() && animal.shouldSeekFood()
                && !animal.getNavigation().isDone();
    }

    @Override
    public void tick() {
        if (target == null || animal.distanceToSqr(target.getX() + 0.5D, target.getY() + 0.5D,
                target.getZ() + 0.5D) > 3.0D) return;
        boolean consumed = crop ? consumeCrop() : consumeNest();
        if (consumed) {
            animal.setHunger(100);
            animal.setThirst(100);
            animal.setEatingTicks(160);
            animal.markInteracted();
            animal.getNavigation().stop();
            target = null;
        }
    }

    @Override
    public void stop() {
        target = null;
        crop = false;
    }

    public BlockPos target() { return target; }
    public boolean targetsCrop() { return crop; }

    private BlockPos findNearest() {
        int range = Math.max(4, configured(AnimaniaConfig.AI_BLOCK_SEARCH_RANGE, 16));
        BlockPos origin = animal.blockPosition();
        BlockPos best = null;
        double distance = Double.MAX_VALUE;
        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-range, -2, -range), origin.offset(range, 2, range))) {
            BlockState state = animal.level().getBlockState(pos);
            boolean nest = state.is(AnimaniaBlocks.NEST.get())
                    && animal.level().getBlockEntity(pos) instanceof AnimaniaBlocks.NestEntity nestEntity
                    && isChickenEgg(nestEntity.getItem(0));
            boolean field = hedgehogCrops(animal) && isHedgehogCrop(state);
            if (!nest && !field) continue;
            double candidate = pos.distSqr(origin);
            if (candidate < distance) {
                distance = candidate;
                best = pos.immutable();
                crop = field;
            }
        }
        return best;
    }

    private boolean consumeNest() {
        if (!(animal.level().getBlockEntity(target) instanceof AnimaniaBlocks.NestEntity nest)) return false;
        ItemStack egg = nest.removeItem(0, 1);
        return !egg.isEmpty() && isChickenEgg(egg);
    }

    private boolean consumeCrop() {
        BlockState state = animal.level().getBlockState(target);
        if (!isHedgehogCrop(state)) return false;
        if (state.getBlock() instanceof CropBlock cropBlock && !cropBlock.isMaxAge(state)) return false;
        return animal.level().destroyBlock(target, false);
    }

    private static boolean isChickenEgg(ItemStack stack) {
        if (stack.isEmpty()) return false;
        var id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (id == null) return false;
        return (id.getNamespace().equals("minecraft") && id.getPath().equals("egg"))
                || (id.getNamespace().equals("animania_farm") && id.getPath().equals("brown_egg"));
    }

    private static boolean isHedgehogCrop(BlockState state) {
        return state.is(Blocks.CARROTS) || state.is(Blocks.BEETROOTS) || state.is(Blocks.POTATOES);
    }

    private static int configured(net.minecraftforge.common.ForgeConfigSpec.IntValue value, int fallback) {
        try { return value.get(); } catch (IllegalStateException ignored) { return fallback; }
    }

    private static boolean configured(net.minecraftforge.common.ForgeConfigSpec.BooleanValue value, boolean fallback) {
        try { return value.get(); } catch (IllegalStateException ignored) { return fallback; }
    }
}
