package com.animania.common.entity.goal;

import com.animania.common.config.AnimaniaConfig;
import com.animania.common.entity.AnimaniaAnimalEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.EnumSet;
import java.util.function.Predicate;

/** Source-faithful grass search and 160-tick chewing animation. */
public final class AnimaniaEatGrassGoal extends Goal {
    private final AnimaniaAnimalEntity animal;
    private final boolean consumesGrass;
    private final Predicate<BlockPos> targetFilter;
    private BlockPos target;
    private int delay;
    private int walkTries;
    private boolean eating;
    private boolean consumed;

    public AnimaniaEatGrassGoal(AnimaniaAnimalEntity animal) {
        this(animal, ignored -> true);
    }

    /** Target filtering allows isolated GameTests without changing production searches. */
    public AnimaniaEatGrassGoal(AnimaniaAnimalEntity animal, Predicate<BlockPos> targetFilter) {
        this.animal = animal;
        this.consumesGrass = consumesGrass(animal.registryNamespace(), animal.registryPath());
        this.targetFilter = targetFilter;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        if (!legacyMountGateAllows()) return false;
        if (++delay <= configured(AnimaniaConfig.AI_TICKS_BETWEEN_FIRINGS, 100)) return false;
        if (animal.isSleeping() || !animal.shouldSeekFood()) {
            delay = 0;
            return false;
        }
        if (animal.getRandom().nextInt(120) != 0) return false;
        delay = 0;
        return findTargetNow();
    }

    public boolean legacyMountGateAllows() {
        return !animal.isFarmHorse() || (!animal.isVehicle() && !animal.isPullingVehicle());
    }

    @Override
    public void start() {
        walkTries = 0;
        eating = false;
        consumed = false;
        if (target == null) return;
        if (consumesGrass) moveToTarget();
        else beginEating();
    }

    @Override
    public boolean canContinueToUse() {
        return animal.getEatingTicks() > 0 || (target != null && !animal.isSleeping()
                && animal.shouldSeekFood() && isEdible(animal.level().getBlockState(target)));
    }

    @Override
    public void tick() {
        if (target == null) return;
        if (!eating && consumesGrass) {
            double distance = animal.distanceToSqr(target.getX() + 0.5D, target.getY() + 0.5D,
                    target.getZ() + 0.5D);
            if (distance <= 2.5D) {
                beginEating();
            } else if (++walkTries % 40 == 0) {
                moveToTarget();
                animal.getLookControl().setLookAt(target.getX() + 0.5D, target.getY() + 0.5D,
                        target.getZ() + 0.5D, 10.0F, animal.getMaxHeadXRot());
            } else if (walkTries > 100 && animal.getNavigation().isDone()) {
                target = null;
            }
        }
        if (eating && consumesGrass && !consumed && animal.getEatingTicks() == 4) consumeTarget();
    }

    @Override
    public void stop() {
        target = null;
        walkTries = 0;
        eating = false;
        consumed = false;
        animal.setEatingTicks(0);
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    private void beginEating() {
        eating = true;
        animal.setEatingTicks(160);
        animal.getNavigation().stop();
        animal.level().broadcastEntityEvent(animal, (byte) 10);
    }

    private void consumeTarget() {
        BlockState state = animal.level().getBlockState(target);
        if (!isEdible(state)) return;
        consumed = true;
        animal.level().levelEvent(2001, target, Block.getId(state));
        if (configured(AnimaniaConfig.PLANTS_REMOVED_AFTER_EATING, true)) {
            animal.level().setBlock(target, Blocks.DIRT.defaultBlockState(), 2);
        }
        animal.setHunger(100);
    }

    private void moveToTarget() {
        animal.getNavigation().moveTo(target.getX() + 0.5D, target.getY() + 1.0D,
                target.getZ() + 0.5D, 1.0D);
    }

    /** Performs the legacy range-eight/range-four search and stores the result. */
    public boolean findTargetNow() {
        BlockPos origin = animal.blockPosition();
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;
        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-8, -4, -8), origin.offset(8, 4, 8))) {
            if (!targetFilter.test(pos)) continue;
            if (!isEdible(animal.level().getBlockState(pos))) continue;
            boolean alreadyAtDestination = animal.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D,
                    pos.getZ() + 0.5D) <= 2.5D;
            if (!alreadyAtDestination && animal.getNavigation().createPath(pos.above(), 0) == null) continue;
            double distance = pos.distSqr(origin);
            if (distance < bestDistance) {
                best = pos.immutable();
                bestDistance = distance;
            }
        }
        target = best;
        return target != null;
    }

    public BlockPos target() {
        return target;
    }

    public boolean consumesGrass() {
        return consumesGrass;
    }

    private boolean isEdible(BlockState state) {
        if (state.is(Blocks.GRASS_BLOCK)) return true;
        if (state.is(Blocks.MYCELIUM) && isMooshroom()) return true;
        ResourceLocation id = ForgeRegistries.BLOCKS.getKey(state.getBlock());
        if (id == null || !id.getNamespace().equals("desirepaths")) return false;
        String path = id.getPath();
        return path.startsWith("grass_worn_") && (path.endsWith("1") || path.endsWith("2") || path.endsWith("3"));
    }

    private boolean isMooshroom() {
        return animal.registryNamespace().equals("animania_farm") && animal.registryPath().endsWith("_mooshroom");
    }

    public static boolean supports(AnimaniaAnimalEntity animal) {
        return supports(animal.registryNamespace(), animal.registryPath());
    }

    public static boolean supports(String namespace, String path) {
        if (namespace.equals("animania_farm")) {
            return starts(path, "cow_", "bull_", "calf_", "ewe_", "ram_", "lamb_", "doe_", "buck_",
                    "kid_", "mare_", "stallion_", "foal_");
        }
        if (namespace.equals("animania_extra")) {
            return path.startsWith("hedgehog") || path.startsWith("ferret_")
                    || starts(path, "doe_", "buck_", "kit_");
        }
        return namespace.equals("animania_catsdogs")
                && starts(path, "queen_", "tom_", "kitten_", "female_", "male_", "puppy_");
    }

    public static boolean consumesGrass(String namespace, String path) {
        return namespace.equals("animania_farm") && starts(path, "cow_", "bull_", "calf_", "ewe_", "ram_",
                "lamb_", "doe_", "buck_", "kid_", "mare_", "stallion_", "foal_");
    }

    public static int legacyPriority(AnimaniaAnimalEntity animal) {
        String namespace = animal.registryNamespace();
        String path = animal.registryPath();
        if (namespace.equals("animania_extra")) {
            if (path.startsWith("hedgehog")) return 12;
            if (path.startsWith("ferret_")) return 11;
        }
        if (namespace.equals("animania_catsdogs")) return 11;
        if (namespace.equals("animania_farm") && starts(path, "mare_", "stallion_", "foal_")) return 6;
        return 8;
    }

    private static boolean starts(String value, String... prefixes) {
        for (String prefix : prefixes) if (value.startsWith(prefix)) return true;
        return false;
    }

    private static boolean configured(net.minecraftforge.common.ForgeConfigSpec.BooleanValue value, boolean fallback) {
        try { return value.get(); } catch (IllegalStateException ignored) { return fallback; }
    }

    private static int configured(net.minecraftforge.common.ForgeConfigSpec.IntValue value, int fallback) {
        try { return value.get(); } catch (IllegalStateException ignored) { return fallback; }
    }
}
