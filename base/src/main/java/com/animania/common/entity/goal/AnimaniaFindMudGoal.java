package com.animania.common.entity.goal;

import com.animania.common.AnimaniaBlocks;
import com.animania.common.config.AnimaniaConfig;
import com.animania.common.entity.AnimaniaAnimalEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.EnumSet;
import java.util.List;
import net.minecraft.world.phys.AABB;

/** Server-authoritative port of the farm pig mud-seeking care goal. */
public final class AnimaniaFindMudGoal extends Goal {
    private final AnimaniaAnimalEntity pig;
    private BlockPos target;
    private int delay;

    public AnimaniaFindMudGoal(AnimaniaAnimalEntity pig) {
        this.pig = pig;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (!supports(pig) || pig.isPlaying() || pig.isSleeping() || pig.isSitting()
                || pig.isLeashed() || pig.isPassenger() || !pig.level().isDay()) return false;
        if (++delay < configured(AnimaniaConfig.AI_TICKS_BETWEEN_FIRINGS, 100)) return false;
        delay = 0;
        if (isMud(pig.level().getBlockState(pig.blockPosition()).getBlock())
                || isMud(pig.level().getBlockState(pig.blockPosition().below()).getBlock())) {
            pig.enterMud();
            return false;
        }
        target = findNearestMud();
        return target != null && pig.getRandom().nextInt(200) != 0;
    }

    @Override
    public void start() {
        if (target != null) pig.getNavigation().moveTo(
                target.getX() + 0.5D, target.getY() + 1.0D, target.getZ() + 0.5D, 1.2D);
    }

    @Override
    public boolean canContinueToUse() {
        return target != null && !pig.getNavigation().isDone() && !pig.isPlaying()
                && !pig.isSleeping() && pig.level().isDay();
    }

    @Override
    public void tick() {
        if (target == null) return;
        if (pig.distanceToSqr(target.getX() + 0.5D, target.getY() + 1.0D, target.getZ() + 0.5D) <= 2.25D
                || isMud(pig.level().getBlockState(pig.blockPosition()).getBlock())
                || isMud(pig.level().getBlockState(pig.blockPosition().below()).getBlock())) {
            pig.getNavigation().stop();
            pig.enterMud();
        }
    }

    @Override
    public void stop() {
        target = null;
    }

    public BlockPos targetMud() {
        return target;
    }

    private BlockPos findNearestMud() {
        BlockPos origin = pig.blockPosition();
        List<AnimaniaAnimalEntity> nearbyPigs = pig.level().getEntitiesOfClass(AnimaniaAnimalEntity.class,
                pig.getBoundingBox().inflate(12.0D, 4.0D, 12.0D), AnimaniaFindMudGoal::supports);
        return BlockPos.findClosestMatch(origin, 10, 2, candidate -> {
            if (!isMud(pig.level().getBlockState(candidate).getBlock())) return false;
            AABB crowdArea = new AABB(candidate).inflate(2.0D);
            int occupants = 0;
            for (AnimaniaAnimalEntity nearbyPig : nearbyPigs) {
                if (nearbyPig.getBoundingBox().intersects(crowdArea) && ++occupants >= 2) return false;
            }
            return true;
        }).map(BlockPos::immutable).orElse(null);
    }

    private static boolean isMud(Block block) {
        if (block == AnimaniaBlocks.MUD.get()) return true;
        ResourceLocation id = ForgeRegistries.BLOCKS.getKey(block);
        return id != null && id.getPath().equals("mud");
    }

    public static boolean supports(AnimaniaAnimalEntity animal) {
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(animal.getType());
        if (id == null) return false;
        String path = id.getPath();
        return path.startsWith("sow_") || path.startsWith("hog_") || path.startsWith("piglet_");
    }

    private static int configured(net.minecraftforge.common.ForgeConfigSpec.IntValue value, int fallback) {
        try { return value.get(); } catch (IllegalStateException ignored) { return fallback; }
    }
}
