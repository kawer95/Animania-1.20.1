package com.animania.common.entity.goal;

import com.animania.common.entity.AnimaniaAnimalEntity;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;

/** Sleeping-aware replacement for the 1.12 EntityAICatAttack wrapper. */
public final class AnimaniaCatAttackGoal extends MeleeAttackGoal {
    private final AnimaniaAnimalEntity cat;

    public AnimaniaCatAttackGoal(AnimaniaAnimalEntity cat) {
        super(cat, 1.0D, true);
        this.cat = cat;
    }

    @Override
    public boolean canUse() {
        return !blocked() && super.canUse();
    }

    @Override
    public boolean canContinueToUse() {
        return !blocked() && super.canContinueToUse();
    }

    private boolean blocked() {
        return cat.isSleeping() || cat.isSitting() || cat.isPassenger();
    }
}
