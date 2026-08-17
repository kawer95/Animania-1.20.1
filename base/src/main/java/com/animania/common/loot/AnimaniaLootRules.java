package com.animania.common.loot;

import com.animania.api.data.AnimalGender;
import com.animania.common.entity.AnimaniaAnimalEntity;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.DyeColor;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.resources.ResourceLocation;

/**
 * Data-provider friendly predicates/functions replacing the 1.12 custom loot
 * property serializers.  Entity death and interaction code call these rules
 * on the server, so no client-side loot mutation is possible.
 */
public final class AnimaniaLootRules {
    private AnimaniaLootRules() { }

    public static boolean isFed(AnimaniaAnimalEntity animal) { return animal != null && animal.getHunger() > 0; }
    public static boolean isWatered(AnimaniaAnimalEntity animal) { return animal != null && animal.getThirst() > 0; }
    public static boolean isMale(AnimaniaAnimalEntity animal) {
        return animal != null && animal.getGender() == AnimalGender.MALE;
    }

    public static ItemStack addMore(ItemStack stack, RandomSource random, int minimum, int maximum) {
        if (stack == null || stack.isEmpty()) return ItemStack.EMPTY;
        ItemStack result = stack.copy();
        result.grow(addMoreCount(result.getCount(), random, minimum, maximum) - result.getCount());
        return result;
    }

    /** Pure count variant used by data-generation and unit tests without bootstrapping Minecraft registries. */
    public static int addMoreCount(int count, RandomSource random, int minimum, int maximum) {
        int low = Math.max(0, minimum);
        int high = Math.max(low, maximum);
        return count + (low == high ? low : random.nextInt(high - low + 1) + low);
    }

    public static ItemStack woolDrop(AnimaniaAnimalEntity animal) {
        if (animal == null || animal.isSheared() || !isSheep(animal)) return ItemStack.EMPTY;
        DyeColor color = DyeColor.byId(animal.getWoolColor());
        ResourceLocation id = new ResourceLocation("minecraft", color.getName() + "_wool");
        var item = ForgeRegistries.ITEMS.getValue(id);
        return new ItemStack(item == null ? Items.WHITE_WOOL : item);
    }

    private static boolean isSheep(AnimaniaAnimalEntity animal) {
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(animal.getType());
        if (id == null) return false;
        String path = id.getPath();
        return "animania_farm".equals(id.getNamespace())
                && (path.startsWith("ewe_") || path.startsWith("ram_") || path.startsWith("lamb_"));
    }
}
