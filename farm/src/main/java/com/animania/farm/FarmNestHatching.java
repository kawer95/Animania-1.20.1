package com.animania.farm;

import com.animania.common.AnimaniaBlocks;
import com.animania.common.config.AnimaniaConfig;
import com.animania.common.entity.AnimaniaAnimalEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;
import java.util.Locale;

/** Restores the Farm addon callback used by the 1.12 nest random tick. */
public final class FarmNestHatching {
    private FarmNestHatching() {
    }

    public static boolean tryHatch(ServerLevel level, BlockPos pos, AnimaniaBlocks.NestEntity nest,
                                   RandomSource random) {
        var egg = nest.getItem(0);
        if (!egg.is(Items.EGG) && !egg.is(FarmContent.ITEM_ENTRIES.get("brown_egg").get())) return false;
        String motherBreed = normalizeBreed(nest.birdVariant());
        if (!FarmLegacyIds.ALL.contains("chick_" + motherBreed)) return false;

        AABB populationArea = new AABB(pos).inflate(15.0D);
        int nearbyChickens = level.getEntitiesOfClass(AnimaniaAnimalEntity.class, populationArea,
                FarmNestHatching::isChicken).size();
        if (nearbyChickens >= configured(AnimaniaConfig.ENTITY_BREEDING_LIMIT, 15)) return false;

        List<AnimaniaAnimalEntity> roosters = level.getEntitiesOfClass(AnimaniaAnimalEntity.class,
                new AABB(pos).inflate(3.0D), FarmNestHatching::isRooster);
        int hatchChance = configured(AnimaniaConfig.EGG_HATCH_CHANCE, 2);
        for (AnimaniaAnimalEntity rooster : roosters) {
            if (random.nextInt(hatchChance) != 0) continue;
            ResourceLocation roosterId = ForgeRegistries.ENTITY_TYPES.getKey(rooster.getType());
            if (roosterId == null) continue;
            String fatherBreed = normalizeBreed(roosterId.getPath().substring("rooster_".length()));
            String childBreed = random.nextBoolean() ? fatherBreed : motherBreed;
            EntityType<?> childType = ForgeRegistries.ENTITY_TYPES.getValue(
                    new ResourceLocation(AnimaniaFarm.MOD_ID, "chick_" + childBreed));
            if (childType == null || !(childType.create(level) instanceof AnimaniaAnimalEntity chick)) continue;
            chick.moveTo(pos.getX() + 0.5D, pos.getY() + 0.2D, pos.getZ() + 0.5D,
                    random.nextFloat() * 360.0F, 0.0F);
            chick.setPersistenceRequired();
            if (!level.addFreshEntity(chick)) continue;
            nest.removeEgg();
            level.playSound(null, pos, FarmSounds.ALL.get("cluck1").get(), SoundSource.NEUTRAL, 0.5F, 1.4F);
            return true;
        }
        return false;
    }

    private static boolean isChicken(AnimaniaAnimalEntity animal) {
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(animal.getType());
        return id != null && AnimaniaFarm.MOD_ID.equals(id.getNamespace())
                && (id.getPath().startsWith("hen_") || id.getPath().startsWith("rooster_")
                || id.getPath().startsWith("chick_"));
    }

    private static boolean isRooster(AnimaniaAnimalEntity animal) {
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(animal.getType());
        return id != null && AnimaniaFarm.MOD_ID.equals(id.getNamespace()) && id.getPath().startsWith("rooster_");
    }

    private static String normalizeBreed(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT).replace(' ', '_').replace('-', '_');
    }

    private static int configured(net.minecraftforge.common.ForgeConfigSpec.IntValue value, int fallback) {
        try { return Math.max(1, value.get()); }
        catch (RuntimeException ignored) { return fallback; }
    }
}
