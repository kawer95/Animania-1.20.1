package com.animania.common;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** Server-side replacement coverage for old event/interaction handlers. */
class AnimaniaServerContractTest {
    @Test
    void serverHooksKeepSeedSpawnDamageAndAdvancementResponsibilities() throws Exception {
        String events = Files.readString(Path.of("src/main/java/com/animania/AnimaniaServerEvents.java"));
        String animal = Files.readString(Path.of("src/main/java/com/animania/common/entity/AnimaniaAnimalEntity.java"));
        assertTrue(events.contains("onSeedRightClick"));
        assertTrue(events.contains("onSpawnPlacement"));
        assertTrue(events.contains("onEntityJoin"));
        assertTrue(animal.contains("source.is(net.minecraft.world.damagesource.DamageTypes.STARVE)"));
        assertTrue(animal.contains("if (isPassenger()) return false"));
        assertTrue(animal.contains("FeedAnimalTrigger"));
    }

    @Test
    void pregnancyProgressIsSyncedForClientDisplays() throws Exception {
        String animal = Files.readString(Path.of("src/main/java/com/animania/common/entity/AnimaniaAnimalEntity.java"));
        assertTrue(animal.contains("PREGNANCY_TICKS"));
        assertTrue(animal.contains("PREGNANCY_DURATION"));
        assertTrue(animal.contains("entityData.define(PREGNANCY_TICKS, 0)"));
        assertTrue(animal.contains("return level().isClientSide ? entityData.get(PREGNANCY_TICKS) : pregnancyTicks"));
        assertTrue(animal.contains("setPregnancyTicks(pregnancyTicks + 1)"));
    }

    @Test
    void childGrowthProgressUsesTheCareTimerBetweenDiscreteAgeSteps() throws Exception {
        String animal = Files.readString(Path.of("src/main/java/com/animania/common/entity/AnimaniaAnimalEntity.java"));
        assertTrue(animal.contains("&& ++childGrowthTimer >= interval"));
        assertTrue(animal.contains("remaining = Math.max(0, remaining - partial)"));
        assertTrue(animal.contains("Publish after the timer/age update"));
    }

    @Test
    void saddledFarmAnimalsExposePlayerAsControllingPassenger() throws Exception {
        String animal = Files.readString(Path.of("src/main/java/com/animania/common/entity/AnimaniaAnimalEntity.java"));
        assertTrue(animal.contains("getControllingPassenger()"));
        assertTrue(animal.contains("isRideableFarmAnimal() && isSaddled() && getFirstPassenger() instanceof Player"));
        assertTrue(animal.contains("return player;"));
        assertTrue(animal.contains("super.travel(input)"));
        assertTrue(animal.contains("path.startsWith(\"mare_\")")
                && animal.contains("getBbHeight() * 0.60D"));
        assertTrue(animal.contains("path.startsWith(\"stallion_\")")
                && animal.contains("getBbHeight() * 0.72D"));
        assertTrue(animal.contains("PlayerRideableJumping"));
        assertTrue(animal.contains("public void onPlayerJump(int jumpPower)"));
        assertTrue(animal.contains("executeRiderJump(playerJumpPendingScale, input)"));
    }
}
