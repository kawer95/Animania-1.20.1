package com.animania.common.entity;

import java.util.Optional;

/**
 * Source-derived movement/observation values used by the generic 1.12 goals.
 * Unsupported animals deliberately receive no generic goal.  Legacy horses
 * used dedicated wander/look implementations; their modern equivalents share
 * these profiles but retain the horse-only gates in the goal classes.
 */
public final class AnimaniaLegacyGoalProfiles {
    private AnimaniaLegacyGoalProfiles() {}

    public record Profile(double panicSpeed, Double wanderSpeed, boolean watchesPlayers, boolean looksIdle) {}

    public static Optional<Profile> resolve(AnimaniaAnimalEntity animal) {
        return resolve(animal.registryNamespace(), animal.registryPath());
    }

    public static Optional<Profile> resolve(String namespace, String path) {
        if ("animania_catsdogs".equals(namespace)) {
            if (starts(path, "queen_", "tom_", "kitten_", "female_", "male_", "puppy_")) {
                return Optional.of(new Profile(1.5D, 1.2D, true, true));
            }
            return Optional.empty();
        }
        if ("animania_farm".equals(namespace)) {
            if (starts(path, "cow_", "bull_", "calf_")) return profile(2.0D, 1.0D, true, true);
            if (starts(path, "ewe_", "ram_", "lamb_")) return profile(2.2D, 1.0D, true, true);
            if (starts(path, "sow_", "hog_", "piglet_")) return profile(1.5D, 1.0D, true, true);
            if (starts(path, "doe_", "buck_", "kid_")) return profile(1.4D, 1.0D, true, true);
            if (starts(path, "hen_", "rooster_", "chick_")) return profile(1.4D, 1.0D, false, true);
            // 1.12 used dedicated horse wander/look implementations.  The
            // modern goals carry those horse-only day/puller gates, so horses
            // must still be registered here at the original 1.0D speed.
            if (starts(path, "mare_", "stallion_", "foal_")) return profile(2.0D, 1.0D, true, true);
            return Optional.empty();
        }
        if ("animania_extra".equals(namespace)) {
            if (path.equals("frog") || path.equals("dartfrog") || path.equals("toad"))
                return profile(2.2D, 0.6D, true, false);
            if (path.startsWith("hamster")) return profile(1.4D, 1.1D, true, false);
            if (path.startsWith("hedgehog")) return profile(1.5D, 1.0D, true, true);
            if (path.startsWith("ferret_")) return profile(1.5D, 1.2D, true, true);
            if (starts(path, "peacock_", "peahen_", "peachick_")) return profile(1.4D, 1.0D, true, true);
            if (starts(path, "doe_", "buck_", "kit_")) return profile(2.5D, 1.8D, true, true);
        }
        return Optional.empty();
    }

    private static Optional<Profile> profile(double panic, Double wander, boolean watch, boolean idle) {
        return Optional.of(new Profile(panic, wander, watch, idle));
    }

    private static boolean starts(String value, String... prefixes) {
        for (String prefix : prefixes) if (value.startsWith(prefix)) return true;
        return false;
    }
}
