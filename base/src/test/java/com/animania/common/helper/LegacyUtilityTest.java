package com.animania.common.helper;

import com.animania.api.data.Pose;
import com.animania.api.data.AnimalGender;
import com.animania.api.data.EntityGender;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LegacyUtilityTest {
    @Test
    void poseNamesRemainStable() {
        assertArrayEquals(new Pose[]{Pose.SITTING, Pose.SLEEPING}, Pose.values());
    }

    @Test
    void tickConstantsAndFormattingMatchLegacyValues() {
        assertEquals(20, TimeHelper.SECOND);
        assertEquals(1_200, TimeHelper.MINUTE);
        assertEquals(72_000, TimeHelper.HOUR);
        assertEquals(1_728_000, TimeHelper.DAY);
        assertEquals("", TimeHelper.getTime(0));
        assertEquals("1 Second", TimeHelper.getTime(20));
        assertEquals("2 Days, 1 Hour, 2 Minutes, 3 Seconds",
                TimeHelper.getTime(2 * TimeHelper.DAY + TimeHelper.HOUR + 2 * TimeHelper.MINUTE + 3 * TimeHelper.SECOND));
        assertEquals("", TimeHelper.getTime(-20));
    }

    @Test
    void romanFormatterPreservesSubtractiveNotationAndRejectsOldCrashCases() {
        assertEquals("I", RomanNumberHelper.toRoman(1));
        assertEquals("IV", RomanNumberHelper.toRoman(4));
        assertEquals("MCMXCIV", RomanNumberHelper.toRoman(1994));
        assertThrows(IllegalArgumentException.class, () -> RomanNumberHelper.toRoman(0));
        assertThrows(IllegalArgumentException.class, () -> RomanNumberHelper.toRoman(-1));
    }

    @Test
    void invalidConfigExceptionRetainsCheckedMessageContract() {
        InvalidConfigException exception = new InvalidConfigException("bad value");
        assertEquals("bad value", exception.getMessage());
        assertInstanceOf(Exception.class, exception);
    }

    @Test
    void legacyGenderResolutionPreservesEveryBranchIncludingRandom() {
        assertEquals(AnimalGender.MALE, EntityGender.MALE.resolve(() -> 2));
        assertEquals(AnimalGender.FEMALE, EntityGender.FEMALE.resolve(() -> 0));
        assertEquals(AnimalGender.CHILD, EntityGender.CHILD.resolve(() -> 1));
        assertEquals(AnimalGender.NONE, EntityGender.NONE.resolve(() -> 2));
        assertEquals(AnimalGender.MALE, EntityGender.RANDOM.resolve(() -> 0));
        assertEquals(AnimalGender.FEMALE, EntityGender.RANDOM.resolve(() -> 1));
        assertEquals(AnimalGender.CHILD, EntityGender.RANDOM.resolve(() -> 2));
        assertEquals(AnimalGender.CHILD, EntityGender.RANDOM.resolve(() -> -1));
        assertEquals(AnimalGender.MALE, EntityGender.RANDOM.modernDefault());
    }
}
