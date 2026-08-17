package com.animania.common;

import com.animania.Animania;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** Base sound events retained from the 1.12 common module. */
public final class AnimaniaSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, Animania.MOD_ID);
    public static final RegistryObject<SoundEvent> ZAP = register("zap");
    public static final RegistryObject<SoundEvent> COMBO = register("combo");

    private static RegistryObject<SoundEvent> register(String id) {
        return SOUNDS.register(id, () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(Animania.MOD_ID, id)));
    }

    private AnimaniaSounds() { }
}
