package com.animania.farm;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.LinkedHashMap;
import java.util.Map;

/** All 96 source-derived Farm sound events, using legal lowercase IDs. */
public final class FarmSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, AnimaniaFarm.MOD_ID);
    public static final Map<String, RegistryObject<SoundEvent>> ALL = new LinkedHashMap<>();

    static {
        FarmSoundCatalog.IDS.forEach(FarmSounds::add);
    }

    private static void add(String id) {
        ALL.put(id, SOUNDS.register(id, () -> SoundEvent.createVariableRangeEvent(
                new ResourceLocation(AnimaniaFarm.MOD_ID, id))));
    }

    private FarmSounds() { }
}
