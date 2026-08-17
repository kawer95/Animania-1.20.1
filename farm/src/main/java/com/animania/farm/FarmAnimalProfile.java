package com.animania.farm;

import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Animal;

/**
 * Exact 1.12 physical and combat defaults for every Farm registry family.
 * Breed-specific goat dimensions are kept here because those old subclasses
 * overrode their family base size in their constructors.
 */
public record FarmAnimalProfile(double maxHealth, double movementSpeed, double attackDamage,
                                float width, float height) {
    public static FarmAnimalProfile forId(String id) {
        if (FarmLegacyIds.isVehicle(id)) {
            return switch (id) {
                case "cart", "tiller" -> new FarmAnimalProfile(10.0D, 0.1D, 0.0D, 2.0F, 1.2F);
                case "wagon" -> new FarmAnimalProfile(10.0D, 0.1D, 0.0D, 2.5F, 1.2F);
                default -> throw unknown(id);
            };
        }
        if (id.startsWith("hen_")) return new FarmAnimalProfile(6.0D, 0.29D, 1.5D, 0.5F, 0.7F);
        if (id.startsWith("rooster_")) return new FarmAnimalProfile(6.0D, 0.29D, 2.0D, 0.6F, 0.8F);
        if (id.startsWith("chick_")) return new FarmAnimalProfile(6.0D, 0.29D, 0.0D, 1.1F, 1.5F);

        if (id.startsWith("bull_")) return new FarmAnimalProfile(24.0D, 0.20D, 4.0D, 1.6F, 1.8F);
        if (id.startsWith("cow_")) return new FarmAnimalProfile(18.0D, 0.20D, 2.0D, 1.4F, 1.8F);
        if (id.startsWith("calf_")) return new FarmAnimalProfile(10.0D, 0.26D, 0.0D, 1.6F, 3.6F);

        if (id.startsWith("buck_")) return goat(id, 20.0D, 0.265D, "buck");
        if (id.startsWith("doe_")) return goat(id, 15.0D, 0.265D, "doe");
        if (id.startsWith("kid_")) return goat(id, 8.0D, 0.315D, "kid");

        if (id.startsWith("stallion_")) return new FarmAnimalProfile(24.0D, 0.20D, 4.0D, 1.8F, 2.2F);
        if (id.startsWith("mare_")) return new FarmAnimalProfile(20.0D, 0.20D, 4.0D, 1.8F, 2.2F);
        if (id.startsWith("foal_")) return new FarmAnimalProfile(12.0D, 0.20D, 4.0D, 2.2F, 3.0F);

        if (id.startsWith("hog_")) return new FarmAnimalProfile(14.0D, 0.265D, 0.0D, 1.0F, 1.0F);
        if (id.startsWith("sow_")) return new FarmAnimalProfile(12.0D, 0.265D, 0.0D, 1.1F, 1.0F);
        if (id.startsWith("piglet_")) return new FarmAnimalProfile(8.0D, 0.315D, 0.0D, 1.1F, 1.1F);

        if (id.startsWith("ram_")) return new FarmAnimalProfile(20.0D, 0.20D, 0.0D, 1.2F, 1.0F);
        if (id.startsWith("ewe_")) return new FarmAnimalProfile(15.0D, 0.265D, 0.0D, 1.0F, 1.0F);
        if (id.startsWith("lamb_")) return new FarmAnimalProfile(8.0D, 0.315D, 0.0D, 1.0F, 1.0F);
        throw unknown(id);
    }

    private static FarmAnimalProfile goat(String id, double health, double speed, String sex) {
        String breed = id.substring(id.indexOf('_') + 1);
        float width = 1.0F;
        float height = 1.0F;
        switch (breed) {
            case "alpine" -> {
                if (sex.equals("buck")) { width = 1.6F; height = 1.4F; }
                else if (sex.equals("doe")) { width = 1.6F; height = 1.3F; }
            }
            case "angora" -> { width = 1.6F; height = 1.4F; }
            case "fainting" -> { width = 1.1F; height = 1.0F; }
            case "kiko" -> {
                if (sex.equals("buck")) width = 1.2F;
                else if (sex.equals("doe")) width = 1.3F;
            }
            case "kinder" -> {
                if (sex.equals("buck")) { width = 1.3F; height = 1.2F; }
                else if (sex.equals("doe")) { width = 1.4F; height = 1.2F; }
            }
            case "nigerian_dwarf" -> {
                if (sex.equals("buck")) { width = 1.2F; height = 1.2F; }
                else if (sex.equals("doe")) { width = 1.1F; height = 1.2F; }
            }
            case "pygmy" -> { }
            default -> throw unknown(id);
        }
        return new FarmAnimalProfile(health, speed, 0.0D, width, height);
    }

    public AttributeSupplier.Builder attributes() {
        AttributeSupplier.Builder builder = Animal.createMobAttributes()
                .add(Attributes.MAX_HEALTH, maxHealth)
                .add(Attributes.MOVEMENT_SPEED, movementSpeed);
        if (attackDamage > 0.0D) builder.add(Attributes.ATTACK_DAMAGE, attackDamage);
        return builder;
    }

    private static IllegalArgumentException unknown(String id) {
        return new IllegalArgumentException("Unknown Farm legacy entity id: " + id);
    }
}
