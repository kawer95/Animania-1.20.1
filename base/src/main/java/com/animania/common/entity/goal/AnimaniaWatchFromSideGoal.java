package com.animania.common.entity.goal;

import com.animania.common.entity.AnimaniaAnimalEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;

import java.util.EnumSet;

/** Exact side-biased daytime look used by 1.12 peafowl. */
public final class AnimaniaWatchFromSideGoal extends Goal {
    private final AnimaniaAnimalEntity bird;
    private Player target;
    private int lookTicks;

    public AnimaniaWatchFromSideGoal(AnimaniaAnimalEntity bird) {
        this.bird = bird;
        setFlags(EnumSet.of(Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (!bird.isLegacyDaytime() || bird.isSleeping() || bird.getRandom().nextFloat() >= 0.02F) return false;
        target = bird.level().getNearestPlayer(bird, 6.0D);
        return target != null;
    }

    @Override
    public boolean canContinueToUse() {
        return target != null && target.isAlive() && lookTicks > 0 && bird.distanceToSqr(target) <= 36.0D
                && !bird.isSleeping();
    }

    @Override
    public void start() {
        lookTicks = 40 + bird.getRandom().nextInt(40);
    }

    @Override
    public void tick() {
        if (target == null) return;
        bird.getLookControl().setLookAt(target.getX() + 20.0D, target.getEyeY(), target.getZ(),
                bird.getMaxHeadYRot(), bird.getMaxHeadXRot());
        lookTicks--;
    }

    @Override
    public void stop() {
        target = null;
        lookTicks = 0;
    }
}
