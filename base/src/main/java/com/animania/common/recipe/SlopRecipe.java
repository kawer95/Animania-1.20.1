package com.animania.common.recipe;

import com.animania.common.AnimaniaItems;
import com.animania.common.config.AnimaniaConfig;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.core.NonNullList;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Exact modern replacement for the 1.12 no-bucket slop recipe: two configured
 * pig foods and one vanilla or Animania milk bucket produce one slop bucket.
 */
public final class SlopRecipe extends CustomRecipe {
    public SlopRecipe(ResourceLocation id, CraftingBookCategory category) {
        super(id, category);
    }

    @Override
    public boolean matches(CraftingContainer container, Level level) {
        java.util.List<ItemStack> inputs = new java.util.ArrayList<>(container.getContainerSize());
        for (int slot = 0; slot < container.getContainerSize(); slot++) inputs.add(container.getItem(slot));
        return matchesInputs(inputs);
    }

    /** Shared predicate used by GameTests and the crafting implementation. */
    public static boolean matchesInputs(Iterable<ItemStack> inputs) {
        int food = 0;
        int milk = 0;
        for (ItemStack stack : inputs) {
            if (stack.isEmpty()) continue;
            if (AnimaniaConfig.matchesSlopIngredient(stack)) food++;
            else if (isMilkBucket(stack)) milk++;
            else return false;
        }
        return food == 2 && milk == 1;
    }

    public static boolean isMilkBucket(ItemStack stack) {
        if (stack.is(Items.MILK_BUCKET)) return true;
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        return id != null && id.getNamespace().equals("animania_farm")
                && id.getPath().startsWith("milk_") && id.getPath().endsWith("_bucket");
    }

    @Override
    public ItemStack assemble(CraftingContainer container, RegistryAccess registries) {
        return new ItemStack(AnimaniaItems.SLOP_BUCKET.get());
    }

    @Override
    public ItemStack getResultItem(RegistryAccess registries) {
        return new ItemStack(AnimaniaItems.SLOP_BUCKET.get());
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        Ingredient food = Ingredient.of(ForgeRegistries.ITEMS.getValues().stream()
                .map(ItemStack::new)
                .filter(AnimaniaConfig::matchesSlopIngredient));
        Ingredient milk = Ingredient.of(ForgeRegistries.ITEMS.getValues().stream()
                .map(ItemStack::new)
                .filter(SlopRecipe::isMilkBucket));
        return NonNullList.of(Ingredient.EMPTY, food, food, milk);
    }

    @Override
    public boolean isSpecial() {
        return false;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 3;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return AnimaniaRecipes.SLOP.get();
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingContainer container) {
        return NonNullList.withSize(container.getContainerSize(), ItemStack.EMPTY);
    }
}
