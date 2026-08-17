package com.animania.extra;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.LinkedHashMap;
import java.util.Map;

/** All 52 source-derived Extra sound events, using legal lowercase IDs. */
public final class ExtraSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, AnimaniaExtra.MOD_ID);
    public static final Map<String, RegistryObject<SoundEvent>> ALL = new LinkedHashMap<>();

    static {
        ExtraSoundCatalog.IDS.forEach(ExtraSounds::add);
    }

    private static void add(String id) {
        ALL.put(id, SOUNDS.register(id, () -> SoundEvent.createVariableRangeEvent(
                new ResourceLocation(AnimaniaExtra.MOD_ID, id))));
    }

    private ExtraSounds() { }
}
