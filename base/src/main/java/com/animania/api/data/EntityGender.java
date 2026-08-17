package com.animania.api.data;

import java.util.Objects;
import java.util.function.IntSupplier;

/** Legacy enum retained for source compatibility; RANDOM resolves server-side. */
public enum EntityGender {
    MALE, FEMALE, CHILD, RANDOM, NONE;

    public AnimalGender modernDefault() {
        return resolve(() -> 0);
    }

    /**
     * Resolve the legacy RANDOM/NONE values without exposing global mutable
     * randomness. Callers on the authoritative server supply a 0..2 roll.
     */
    public AnimalGender resolve(IntSupplier randomThree) {
        Objects.requireNonNull(randomThree, "randomThree");
        return switch (this) {
            case FEMALE -> AnimalGender.FEMALE;
            case MALE -> AnimalGender.MALE;
            case NONE -> AnimalGender.NONE;
            case CHILD -> AnimalGender.CHILD;
            case RANDOM -> switch (Math.floorMod(randomThree.getAsInt(), 3)) {
                case 0 -> AnimalGender.MALE;
                case 1 -> AnimalGender.FEMALE;
                default -> AnimalGender.CHILD;
            };
        };
    }
}
