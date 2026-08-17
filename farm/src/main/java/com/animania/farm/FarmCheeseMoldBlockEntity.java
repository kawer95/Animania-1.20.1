package com.animania.farm;

import com.animania.common.block.AnimaniaStorageBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

/**
 * Server-authoritative cheese processing. Modern Forge fluid containers are
 * accepted, and the output retains the legacy milk
 * family so automation does not collapse all cheese variants to one item.
 */
public final class FarmCheeseMoldBlockEntity extends AnimaniaStorageBlockEntity implements com.animania.api.IAnimaniaProbeBlock {
    private int processTicks;

    public FarmCheeseMoldBlockEntity(BlockPos pos, BlockState state) {
        super(FarmContent.CHEESE_MOLD_BE.get(), pos, state, 1, 1000);
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    /** Legacy automation could extract the finished product but not insert arbitrary items. */
    @Override
    protected boolean isItemValid(int slot, ItemStack stack) {
        return false;
    }

    @Override
    public void serverTick() {
        syncVisualVariant();
        ItemStack input = getItem(0);
        if (input.isEmpty() && fluidCapability.getFluidAmount() >= 1000) {
            String fluidOutput = outputForFluid(fluidCapability.getFluid());
            if (fluidOutput != null) {
                if (++processTicks >= maturityTicks()) {
                    processTicks = 0;
                    fluidCapability.drain(1000, IFluidHandler.FluidAction.EXECUTE);
                    Item output = ForgeRegistries.ITEMS.getValue(new ResourceLocation(AnimaniaFarm.MOD_ID, fluidOutput));
                    if (output != null) {
                        int amount = fluidOutput.equals("salt") ? Math.max(1, FarmConfig.SALT_CREATION_AMOUNT.get()) : 1;
                        setItem(0, new ItemStack(output, amount));
                    }
                }
                return;
            }
        }
        processTicks = 0;
    }

    private void syncVisualVariant() {
        if (level == null || level.isClientSide || !getBlockState().hasProperty(FarmCheeseMoldBlock.VARIANT)) return;
        FarmCheeseMoldBlock.Variant expected = visualVariant();
        if (getBlockState().getValue(FarmCheeseMoldBlock.VARIANT) != expected) {
            level.setBlock(worldPosition, getBlockState().setValue(FarmCheeseMoldBlock.VARIANT, expected), 3);
        }
    }

    private FarmCheeseMoldBlock.Variant visualVariant() {
        ItemStack stack = getItem(0);
        if (!stack.isEmpty()) {
            ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
            if (id != null) {
                String path = id.getPath();
                if (path.equals("salt")) return FarmCheeseMoldBlock.Variant.SALT;
                FarmCheeseMoldBlock.Variant cheese = familyVariant(path, true);
                if (cheese != null) return cheese;
            }
        }
        FluidStack fluid = fluidCapability.getFluid();
        if (!fluid.isEmpty()) {
            ResourceLocation id = ForgeRegistries.FLUIDS.getKey(fluid.getFluid());
            if (id != null) {
                if (id.getNamespace().equals("minecraft") && id.getPath().equals("water")) {
                    return FarmCheeseMoldBlock.Variant.WATER;
                }
                FarmCheeseMoldBlock.Variant milk = familyVariant(id.getPath(), false);
                if (milk != null) return milk;
            }
        }
        return FarmCheeseMoldBlock.Variant.EMPTY;
    }

    private static FarmCheeseMoldBlock.Variant familyVariant(String path, boolean cheese) {
        if (path.contains("holstein")) return cheese ? FarmCheeseMoldBlock.Variant.HOLSTEIN_CHEESE : FarmCheeseMoldBlock.Variant.HOLSTEIN_MILK;
        if (path.contains("friesian")) return cheese ? FarmCheeseMoldBlock.Variant.FRIESIAN_CHEESE : FarmCheeseMoldBlock.Variant.FRIESIAN_MILK;
        if (path.contains("jersey")) return cheese ? FarmCheeseMoldBlock.Variant.JERSEY_CHEESE : FarmCheeseMoldBlock.Variant.JERSEY_MILK;
        if (path.contains("goat")) return cheese ? FarmCheeseMoldBlock.Variant.GOAT_CHEESE : FarmCheeseMoldBlock.Variant.GOAT_MILK;
        if (path.contains("sheep")) return cheese ? FarmCheeseMoldBlock.Variant.SHEEP_CHEESE : FarmCheeseMoldBlock.Variant.SHEEP_MILK;
        return null;
    }

    private static int maturityTicks() {
        try {
            return Math.max(20, FarmConfig.CHEESE_MATURITY_TIME.get());
        } catch (RuntimeException ignored) {
            return 24000;
        }
    }

    public int processTicks() {
        return Math.max(0, processTicks);
    }

    @Override
    public java.util.List<net.minecraft.network.chat.Component> getAnimaniaProbeInfo() {
        java.util.List<net.minecraft.network.chat.Component> lines = new java.util.ArrayList<>();
        if (processTicks > 0 || !getItem(0).isEmpty() || fluidCapability.getFluidAmount() > 0) {
            int percent = Math.min(100, Math.round(processTicks * 100.0F / maturityTicks()));
            lines.add(net.minecraft.network.chat.Component.translatable("jade.animania.aging", percent));
        }
        if (!getItem(0).isEmpty()) {
            lines.add(net.minecraft.network.chat.Component.translatable("jade.animania.item_count",
                    getItem(0).getCount(), getItem(0).getHoverName()));
        }
        if (!fluidCapability.getFluid().isEmpty()) {
            lines.add(net.minecraft.network.chat.Component.translatable("jade.animania.fluid_amount",
                    fluidCapability.getFluid().getDisplayName(), fluidCapability.getFluidAmount()));
        }
        return java.util.List.copyOf(lines);
    }

    private static String outputForFluid(FluidStack fluid) {
        if (fluid == null || fluid.isEmpty()) return null;
        ResourceLocation id = ForgeRegistries.FLUIDS.getKey(fluid.getFluid());
        if (id == null) return null;
        if (id.getNamespace().equals("minecraft") && id.getPath().equals("water") && !disabledSaltCreation()) return "salt";
        if (!AnimaniaFarm.MOD_ID.equals(id.getNamespace())) return null;
        return switch (id.getPath()) {
            case "milk_holstein" -> "holstein_cheese_wheel";
            case "milk_friesian" -> "friesian_cheese_wheel";
            case "milk_jersey" -> "jersey_cheese_wheel";
            case "milk_goat" -> "goat_cheese_wheel";
            case "milk_sheep" -> "sheep_cheese_wheel";
            default -> null;
        };
    }

    private static boolean disabledSaltCreation() {
        try {
            return FarmConfig.DISABLE_SALT_CREATION.get();
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    @Override
    protected boolean isFluidValid(FluidStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        ResourceLocation id = ForgeRegistries.FLUIDS.getKey(stack.getFluid());
        if (id == null) return false;
        if (AnimaniaFarm.MOD_ID.equals(id.getNamespace()) && id.getPath().startsWith("milk_")) return true;
        return id.getNamespace().equals("minecraft") && id.getPath().equals("water") && !disabledSaltCreation();
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("ProcessTicks", processTicks);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        processTicks = Math.max(0, tag.getInt("ProcessTicks"));
    }
}
