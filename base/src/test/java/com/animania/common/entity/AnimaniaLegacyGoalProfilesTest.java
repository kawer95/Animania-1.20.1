package com.animania.common.entity;

import com.animania.common.entity.goal.AnimaniaSmallCreatureFloatGoal;
import com.animania.common.entity.goal.AnimaniaEatGrassGoal;
import com.animania.common.entity.goal.AnimaniaFollowOwnerGoal;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AnimaniaLegacyGoalProfilesTest {
    @Test
    void preservesSourceDerivedFamilySpeedsAndGoalMembership() {
        assertProfile("animania_farm", "cow_angus", 2.0D, 1.0D, true, true);
        assertProfile("animania_farm", "ewe_dorper", 2.2D, 1.0D, true, true);
        assertProfile("animania_farm", "sow_duroc", 1.5D, 1.0D, true, true);
        assertProfile("animania_farm", "doe_alpine", 1.4D, 1.0D, true, true);
        assertProfile("animania_farm", "hen_leghorn", 1.4D, 1.0D, false, true);
        // Legacy horses used EntityAIWanderHorses at 1.0D and retained the
        // normal player-watch/idle goals; only the implementation was horse-specific.
        assertProfile("animania_farm", "mare_draft", 2.0D, 1.0D, true, true);

        assertProfile("animania_extra", "hamster", 1.4D, 1.1D, true, false);
        assertProfile("animania_extra", "hedgehog", 1.5D, 1.0D, true, true);
        assertProfile("animania_extra", "ferret_grey", 1.5D, 1.2D, true, true);
        assertProfile("animania_extra", "peacock_blue", 1.4D, 1.0D, true, true);
        assertProfile("animania_extra", "doe_rex", 2.5D, 1.8D, true, true);

        assertProfile("animania_catsdogs", "queen_siamese", 1.5D, 1.2D, true, true);
        assertProfile("animania_catsdogs", "male_husky", 1.5D, 1.2D, true, true);

        assertProfile("animania_extra", "dartfrog", 2.2D, 0.6D, true, false);
        assertProfile("animania_extra", "toad", 2.2D, 0.6D, true, false);
        assertProfile("animania_extra", "frog", 2.2D, 0.6D, true, false);

        assertTrue(AnimaniaSmallCreatureFloatGoal.supports("animania_farm", "chick_leghorn"));
        assertTrue(AnimaniaSmallCreatureFloatGoal.supports("animania_extra", "hamster"));
        assertTrue(AnimaniaSmallCreatureFloatGoal.supports("animania_extra", "hedgehog_albino"));
        assertTrue(AnimaniaSmallCreatureFloatGoal.supports("animania_extra", "ferret_white"));
        assertFalse(AnimaniaSmallCreatureFloatGoal.supports("animania_extra", "doe_rex"));
        assertFalse(AnimaniaSmallCreatureFloatGoal.supports("animania_extra", "dartfrog"));

        assertTrue(AnimaniaEatGrassGoal.supports("animania_farm", "cow_angus"));
        assertTrue(AnimaniaEatGrassGoal.supports("animania_farm", "mare_draft"));
        assertTrue(AnimaniaEatGrassGoal.supports("animania_extra", "doe_rex"));
        assertTrue(AnimaniaEatGrassGoal.supports("animania_catsdogs", "female_collie"));
        assertFalse(AnimaniaEatGrassGoal.supports("animania_extra", "hamster"));
        assertTrue(AnimaniaEatGrassGoal.consumesGrass("animania_farm", "ewe_dorper"));
        assertFalse(AnimaniaEatGrassGoal.consumesGrass("animania_extra", "doe_rex"));
        assertFalse(AnimaniaEatGrassGoal.consumesGrass("animania_catsdogs", "tom_siamese"));

        assertTrue(AnimaniaFollowOwnerGoal.supports("animania_catsdogs", "male_labrador"));
        assertTrue(AnimaniaFollowOwnerGoal.supports("animania_extra", "hamster"));
        assertTrue(AnimaniaFollowOwnerGoal.supports("animania_extra", "hedgehog"));
        assertFalse(AnimaniaFollowOwnerGoal.supports("animania_extra", "doe_rex"));
    }

    private static void assertProfile(String namespace, String path, double panic, Double wander,
                                      boolean watches, boolean idle) {
        var profile = AnimaniaLegacyGoalProfiles.resolve(namespace, path).orElseThrow();
        assertEquals(panic, profile.panicSpeed());
        assertEquals(wander, profile.wanderSpeed());
        assertEquals(watches, profile.watchesPlayers());
        assertEquals(idle, profile.looksIdle());
    }
}
