package com.animania.common.command;

import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;

/** Pure, registry-independent family mapping for the vanilla conversion command. */
public final class AnimaniaConversion {
    private AnimaniaConversion() { }

    @Nullable
    public static ResourceLocation vanillaTypeIdFor(@Nullable ResourceLocation id) {
        if (id == null) return null;
        String path = id.getPath();
        if (!"animania_farm".equals(id.getNamespace()) && !"animania_extra".equals(id.getNamespace())
                && !"animania_catsdogs".equals(id.getNamespace())) return null;
        if ("animania_catsdogs".equals(id.getNamespace())) {
            if (path.startsWith("tom_") || path.startsWith("queen_") || path.startsWith("kitten_")) return mc("cat");
            if (path.startsWith("male_") || path.startsWith("female_") || path.startsWith("puppy_")) return mc("wolf");
            return null;
        }
        if ("animania_extra".equals(id.getNamespace())) {
            if (path.startsWith("doe_") || path.startsWith("buck_") || path.startsWith("kit_")) return mc("rabbit");
            return null;
        }
        if (path.startsWith("cow_") || path.startsWith("bull_") || path.startsWith("calf_")) return mc("cow");
        if (path.startsWith("ewe_") || path.startsWith("ram_") || path.startsWith("lamb_")) return mc("sheep");
        if (path.startsWith("sow_") || path.startsWith("hog_") || path.startsWith("piglet_")) return mc("pig");
        if (path.startsWith("hen_") || path.startsWith("rooster_") || path.startsWith("chick_")) return mc("chicken");
        if (path.startsWith("mare_") || path.startsWith("stallion_") || path.startsWith("foal_")) return mc("horse");
        return null;
    }

    private static ResourceLocation mc(String path) {
        return new ResourceLocation("minecraft", path);
    }
}
