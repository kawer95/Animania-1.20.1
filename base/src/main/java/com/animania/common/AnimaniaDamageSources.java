package com.animania.common;

import com.animania.Animania;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.level.Level;

/** Native 1.20 damage types replacing the three mutable 1.12 DamageSource fields. */
public final class AnimaniaDamageSources {
    public static final ResourceKey<DamageType> PEPE = key("pepe");
    public static final ResourceKey<DamageType> BEE = key("animania_bee");
    public static final ResourceKey<DamageType> KILLER_RABBIT = key("killer_rabbit");

    public static DamageSource pepe(Level level) { return source(level, PEPE); }
    public static DamageSource bee(Level level) { return source(level, BEE); }
    public static DamageSource killerRabbit(Level level) { return source(level, KILLER_RABBIT); }

    private static DamageSource source(Level level, ResourceKey<DamageType> key) {
        return new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(key));
    }
    private static ResourceKey<DamageType> key(String path) {
        return ResourceKey.create(Registries.DAMAGE_TYPE,
                new ResourceLocation(Animania.MOD_ID, path));
    }
    private AnimaniaDamageSources() { }
}
