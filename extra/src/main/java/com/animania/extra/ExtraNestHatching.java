package com.animania.extra;

import com.animania.common.AnimaniaBlocks;
import com.animania.common.config.AnimaniaConfig;
import com.animania.common.entity.AnimaniaAnimalEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;
import java.util.Locale;

/** Restores the Extra addon callback used by the 1.12 nest random tick. */
public final class ExtraNestHatching {
    private ExtraNestHatching() {
    }

    public static boolean tryHatch(ServerLevel level, BlockPos pos, AnimaniaBlocks.NestEntity nest,
                                   RandomSource random) {
        var egg = nest.getItem(0);
        if (!egg.is(ExtraContent.ITEM_ENTRIES.get("peacock_egg_blue").get())
                && !egg.is(ExtraContent.ITEM_ENTRIES.get("peacock_egg_white").get())) return false;
        String motherBreed = normalizeBreed(nest.birdVariant());
        if (!ExtraLegacyIds.ALL.contains("peachick_" + motherBreed)) return false;

        AABB populationArea = new AABB(pos).inflate(15.0D);
        int nearbyPeafowl = level.getEntitiesOfClass(AnimaniaAnimalEntity.class, populationArea,
                ExtraNestHatching::isPeafowl).size();
        if (nearbyPeafowl >= configured(AnimaniaConfig.ENTITY_BREEDING_LIMIT, 15)) return false;

        List<AnimaniaAnimalEntity> peacocks = level.getEntitiesOfClass(AnimaniaAnimalEntity.class,
                new AABB(pos).inflate(3.0D), ExtraNestHatching::isPeacock);
        int hatchChance = configured(AnimaniaConfig.EGG_HATCH_CHANCE, 2);
        for (AnimaniaAnimalEntity peacock : peacocks) {
            if (random.nextInt(hatchChance) != 0) continue;
            ResourceLocation peacockId = ForgeRegistries.ENTITY_TYPES.getKey(peacock.getType());
            if (peacockId == null) continue;
            String fatherBreed = normalizeBreed(peacockId.getPath().substring("peacock_".length()));
            String childBreed = random.nextBoolean() ? fatherBreed : motherBreed;
            EntityType<?> childType = ForgeRegistries.ENTITY_TYPES.getValue(
                    new ResourceLocation(AnimaniaExtra.MOD_ID, "peachick_" + childBreed));
            if (childType == null || !(childType.create(level) instanceof AnimaniaAnimalEntity peachick)) continue;
            peachick.moveTo(pos.getX() + 0.5D, pos.getY() + 0.2D, pos.getZ() + 0.5D,
                    random.nextFloat() * 360.0F, 0.0F);
            peachick.setPersistenceRequired();
            if (!level.addFreshEntity(peachick)) continue;
            nest.removeEgg();
            level.playSound(null, pos, ExtraSounds.ALL.get("peacock1").get(), SoundSource.NEUTRAL, 0.5F, 1.4F);
            return true;
        }
        return false;
    }

    private static boolean isPeafowl(AnimaniaAnimalEntity animal) {
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(animal.getType());
        return id != null && AnimaniaExtra.MOD_ID.equals(id.getNamespace())
                && (id.getPath().startsWith("peahen_") || id.getPath().startsWith("peacock_")
                || id.getPath().startsWith("peachick_"));
    }

    private static boolean isPeacock(AnimaniaAnimalEntity animal) {
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(animal.getType());
        return id != null && AnimaniaExtra.MOD_ID.equals(id.getNamespace()) && id.getPath().startsWith("peacock_");
    }

    private static String normalizeBreed(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT).replace(' ', '_').replace('-', '_');
    }

    private static int configured(net.minecraftforge.common.ForgeConfigSpec.IntValue value, int fallback) {
        try { return Math.max(1, value.get()); }
        catch (RuntimeException ignored) { return fallback; }
    }
}
