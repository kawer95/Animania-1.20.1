package com.animania.extra;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ExtraHamsterWheelKinematicsTest {
    @Test
    void preservesLegacyWheelPeriodButFacesRunnerAgainstTheLowerRim() {
        assertEquals(-(float) Math.PI / 2.0F,
                ExtraHamsterWheelKinematics.rotorAngle(20L, 0.0F, true), 0.00001F);
        assertEquals(-(float) Math.PI,
                ExtraHamsterWheelKinematics.rotorAngle(40L, 0.0F, true), 0.00001F);
        assertEquals(0.0F, ExtraHamsterWheelKinematics.rotorAngle(40L, 0.0F, false));

        assertTrue(ExtraHamsterWheelKinematics.ROTOR_RADIANS_PER_TICK < 0.0F,
                "legacy wheel rotation must remain clockwise");
        assertEquals(90.0F, ExtraHamsterWheelKinematics.HAMSTER_YAW_DEGREES,
                "runner must face against the lower rim after native coordinate conversion");
    }
}
