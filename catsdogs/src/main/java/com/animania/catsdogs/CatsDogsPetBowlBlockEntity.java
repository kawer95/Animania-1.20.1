package com.animania.catsdogs;

import com.animania.common.block.AnimaniaStorageBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.fluids.FluidStack;
import net.minecraft.world.level.material.Fluids;

/**
 * Pet bowl inventory with the same Forge item/fluid capabilities as the
 * shared trough, kept in the Cats&Dogs namespace to preserve the addon ID.
 */
public final class CatsDogsPetBowlBlockEntity extends AnimaniaStorageBlockEntity {
    public CatsDogsPetBowlBlockEntity(BlockPos pos, BlockState state) {
        super(CatsDogsContent.PET_BOWL_BE.get(), pos, state, 1, 1000);
    }

    @Override
    public int getMaxStackSize() {
        return 3;
    }

    @Override
    protected boolean isItemValid(int slot, ItemStack stack) {
        return slot == 0 && CatsDogsPetBowlBlock.isFoodItem(stack);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        ItemStack food = getItem(0);
        if (!food.isEmpty() && food.getCount() > getMaxStackSize()) {
            setItem(0, food.copyWithCount(getMaxStackSize()));
        }
    }

    @Override
    protected boolean allowsAutomation() {
        try { return com.animania.common.config.AnimaniaConfig.ALLOW_TROUGH_AUTOMATION.get(); }
        catch (IllegalStateException ignored) { return true; }
    }

    @Override public boolean providesAnimalFood() { return true; }
    @Override public boolean providesAnimalWater() { return true; }

    /** Pet facilities are water-only; reject lava, milk and other automation fluids. */
    @Override
    protected boolean isFluidValid(FluidStack stack) {
        return stack != null && !stack.isEmpty() && stack.getFluid() == Fluids.WATER;
    }

    /** Insert one food item, enforcing the legacy three-item bowl limit. */
    public boolean tryInsertFood(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        ItemStack current = getItem(0);
        if (!CatsDogsPetBowlBlock.isFoodItem(stack) || (!current.isEmpty() && !ItemStack.isSameItemSameTags(current, stack))
                || current.getCount() >= 3) return false;
        setItem(0, new ItemStack(stack.getItem(), Math.min(3, current.getCount() + 1)));
        return true;
    }
}
