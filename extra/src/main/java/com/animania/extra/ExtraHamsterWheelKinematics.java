package com.animania.extra;

/** Shared, testable transform constants for the hamster-wheel renderer. */
public final class ExtraHamsterWheelKinematics {
    /** The legacy animation completes one clockwise revolution every 80 ticks. */
    public static final float ROTOR_RADIANS_PER_TICK = -(float) Math.PI / 40.0F;

    /**
     * The runner must face against the lower rim's travel direction. The
     * legacy renderer's -90 degree yaw made it face with the belt after the
     * native ModelPart coordinate conversion, which visually ran backwards.
     */
    public static final float HAMSTER_YAW_DEGREES = 90.0F;

    private ExtraHamsterWheelKinematics() {
    }

    public static float rotorAngle(long gameTime, float partialTick, boolean running) {
        return running ? (gameTime + partialTick) * ROTOR_RADIANS_PER_TICK : 0.0F;
    }
}
