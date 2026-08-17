package com.animania.api;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

/** Modern common-tag names for 1.12 OreDictionary categories not supplied by Forge itself. */
public final class AnimaniaLegacyTags {
    public static final TagKey<Item> MUD_STORAGE = forge("storage_blocks/mud");
    public static final TagKey<Item> SUGAR = forge("sugar");
    public static final TagKey<Item> BREAD = forge("foods/bread");
    public static final TagKey<Item> RAW_CHICKEN = forge("foods/raw_chicken");
    public static final TagKey<Item> RAW_BEEF = forge("foods/raw_beef");
    public static final TagKey<Item> RAW_PORK = forge("foods/raw_pork");
    public static final TagKey<Item> COOKED_CHICKEN = forge("foods/cooked_chicken");
    public static final TagKey<Item> COOKED_BEEF = forge("foods/cooked_beef");
    public static final TagKey<Item> COOKED_PORK = forge("foods/cooked_pork");

    private static TagKey<Item> forge(String path) {
        return TagKey.create(Registries.ITEM, new ResourceLocation("forge", path));
    }
    private AnimaniaLegacyTags() { }
}
