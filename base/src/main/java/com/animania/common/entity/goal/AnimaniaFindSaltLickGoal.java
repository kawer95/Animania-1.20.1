package com.animania.common.entity.goal;

import com.animania.common.block.AnimalBlockInteractionRange;
import com.animania.common.block.AnimaniaSaltLickBlockEntity;
import com.animania.common.config.AnimaniaConfig;
import com.animania.common.entity.AnimaniaAnimalEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

/** Path-based replacement for 1.12 GenericAIFindSaltLick. */
public final class AnimaniaFindSaltLickGoal extends Goal {
    private final AnimaniaAnimalEntity animal;
    private BlockPos target;
    private BlockPos approach;
    private int delay;
    private final Set<BlockPos> rejectedTargets = new HashSet<>();
    private final Predicate<BlockPos> targetFilter;

    public AnimaniaFindSaltLickGoal(AnimaniaAnimalEntity animal) {
        this(animal, ignored -> true);
    }

    /** Target filtering is exposed so GameTests can isolate fixtures placed in a shared test level. */
    public AnimaniaFindSaltLickGoal(AnimaniaAnimalEntity animal, Predicate<BlockPos> targetFilter) {
        this.animal = animal;
        this.targetFilter = targetFilter;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override public boolean canUse() {
        if (targetStillValid()) {
            if (canSeekSalt()) return true;
            clearTarget();
            return false;
        }
        if (++delay <= configured(AnimaniaConfig.SALT_LICK_TICK, 8000)) return false;
        if (!canSeekSalt()) {
            delay = 0;
            return false;
        }
        if (animal.getRandom().nextInt(3) != 0) return false;
        delay = 0;
        return findTargetNow();
    }

    @Override public void start() {
        moveToCurrentOrNextTarget();
    }
    @Override public boolean canContinueToUse() {
        if (!targetStillValid() || animal.getHealth() >= animal.getMaxHealth() || animal.isSleeping()) return false;
        if (!animal.getNavigation().isDone() || inConsumptionRange()) return true;
        rejectedTargets.add(target);
        target = findNearest();
        return moveToCurrentOrNextTarget();
    }
    @Override public void tick() {
        if (!inConsumptionRange()) return;
        if (animal.level().getBlockEntity(target) instanceof AnimaniaSaltLickBlockEntity lick && lick.use(animal)) {
            animal.setEatingTicks(80);
            animal.getNavigation().stop();
            clearTarget();
        }
    }
    @Override public void stop() {
        animal.getNavigation().stop();
        clearTarget();
    }
    public BlockPos target() { return target; }
    public BlockPos approach() { return approach; }

    private BlockPos findNearest() {
        int range = Math.max(1, configured(AnimaniaConfig.AI_BLOCK_SEARCH_RANGE, 16));
        BlockPos origin = animal.blockPosition();
        BlockPos best = null;
        BlockPos bestApproach = null;
        double bestDistance = Double.MAX_VALUE;
        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-range, -2, -range), origin.offset(range, 2, range))) {
            if (rejectedTargets.contains(pos) || !targetFilter.test(pos)) continue;
            if (!(animal.level().getBlockEntity(pos) instanceof AnimaniaSaltLickBlockEntity lick) || lick.usesLeft() <= 0) continue;
            boolean directlyReachable = inConsumptionRange(pos);
            BlockPos reachableApproach = directlyReachable ? null : findReachableApproach(pos);
            if (!directlyReachable && reachableApproach == null) continue;
            double distance = pos.distSqr(origin);
            if (distance < bestDistance) {
                best = pos.immutable();
                bestApproach = reachableApproach;
                bestDistance = distance;
            }
        }
        approach = bestApproach;
        return best;
    }

    /** Finds a navigation waypoint only when the animal is outside the direct interaction volume. */
    private BlockPos findReachableApproach(BlockPos saltPos) {
        BlockPos origin = animal.blockPosition();
        BlockPos bestNear = null;
        double bestNearDistance = Double.MAX_VALUE;
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos interactionSide = saltPos.relative(direction);
            if (!isClearApproachBlock(interactionSide)) continue;
            double nearDistance = interactionSide.distSqr(origin);
            if (nearDistance < bestNearDistance) {
                bestNear = interactionSide.immutable();
                bestNearDistance = nearDistance;
            }
        }
        return bestNear;
    }

    private boolean isClearApproachBlock(BlockPos pos) {
        return animal.level().getBlockState(pos).getCollisionShape(animal.level(), pos).isEmpty()
                && animal.level().getBlockState(pos.above())
                .getCollisionShape(animal.level(), pos.above()).isEmpty();
    }

    private boolean targetStillValid() {
        return target != null && (inConsumptionRange() || approach != null)
                && animal.level().getBlockEntity(target) instanceof AnimaniaSaltLickBlockEntity lick
                && lick.usesLeft() > 0;
    }

    private boolean canSeekSalt() {
        return supports(animal) && animal.getHealth() < animal.getMaxHealth()
                && !animal.isPassenger() && !animal.isSleeping()
                && (!AnimaniaFindMudGoal.supports(animal) || !animal.isMuddy());
    }

    private boolean inConsumptionRange() {
        return inConsumptionRange(target);
    }

    private boolean inConsumptionRange(BlockPos saltPos) {
        return saltPos != null && AnimalBlockInteractionRange.contains(saltPos, animal);
    }

    private void clearTarget() {
        target = null;
        approach = null;
        rejectedTargets.clear();
    }

    private boolean moveToCurrentOrNextTarget() {
        while (target != null) {
            if (inConsumptionRange()) return true;
            if (approach != null && animal.getNavigation().moveTo(
                    approach.getX() + 0.5D, approach.getY(), approach.getZ() + 0.5D, 1.0D)) return true;
            rejectedTargets.add(target);
            target = findNearest();
        }
        return false;
    }

    /** The 1.12 goal was registered only by cattle, goats, sheep, pigs and horses. */
    public static boolean supports(AnimaniaAnimalEntity animal) {
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(animal.getType());
        if (id == null || !id.getNamespace().equals("animania_farm")) return false;
        String path = id.getPath();
        return path.startsWith("cow_") || path.startsWith("bull_") || path.startsWith("calf_")
                || path.startsWith("doe_") || path.startsWith("buck_") || path.startsWith("kid_")
                || path.startsWith("ewe_") || path.startsWith("ram_") || path.startsWith("lamb_")
                || path.startsWith("sow_") || path.startsWith("hog_") || path.startsWith("piglet_")
                || path.startsWith("mare_") || path.startsWith("stallion_") || path.startsWith("foal_");
    }

    /** Selects a target immediately; normal runtime behavior still uses the configured interval in {@link #canUse()}. */
    public boolean findTargetNow() {
        rejectedTargets.clear();
        target = findNearest();
        return target != null;
    }

    private static int configured(net.minecraftforge.common.ForgeConfigSpec.IntValue value, int fallback) {
        try { return value.get(); } catch (IllegalStateException ignored) { return fallback; }
    }
}
