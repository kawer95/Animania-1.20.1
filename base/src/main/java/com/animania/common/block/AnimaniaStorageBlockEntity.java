package com.animania.common.block;

import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import java.util.function.Predicate;

/** Small server-side inventory used by troughs, nests, bowls and cheese moulds. */
public abstract class AnimaniaStorageBlockEntity extends BlockEntity implements Container, MenuProvider {
    private final NonNullList<ItemStack> items;
    private boolean syncingCapability;
    private final ItemStackHandler itemCapability;
    protected final FluidTank fluidCapability;
    private final LazyOptional<ItemStackHandler> itemCapabilityOptional;
    private final LazyOptional<FluidTank> fluidCapabilityOptional;

    protected AnimaniaStorageBlockEntity(net.minecraft.world.level.block.entity.BlockEntityType<?> type, BlockPos pos, BlockState state) {
        this(type, pos, state, 9, 8000);
    }

    protected AnimaniaStorageBlockEntity(net.minecraft.world.level.block.entity.BlockEntityType<?> type, BlockPos pos,
                                         BlockState state, int slots, int fluidCapacity) {
        super(type, pos, state);
        this.items = NonNullList.withSize(slots, ItemStack.EMPTY);
        this.itemCapability = new ItemStackHandler(slots) {
            @Override
            public int getSlotLimit(int slot) {
                return AnimaniaStorageBlockEntity.this.getMaxStackSize();
            }

            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                return AnimaniaStorageBlockEntity.this.isItemValid(slot, stack);
            }

            @Override
            protected void onContentsChanged(int slot) {
                if (!syncingCapability) syncFromCapability();
            }
        };
        this.fluidCapability = new FluidTank(fluidCapacity) {
            @Override
            protected void onContentsChanged() {
                setChanged();
            }

            @Override
            public boolean isFluidValid(FluidStack stack) {
                return AnimaniaStorageBlockEntity.this.isFluidValid(stack);
            }
        };
        this.itemCapabilityOptional = LazyOptional.of(() -> itemCapability);
        this.fluidCapabilityOptional = LazyOptional.of(() -> fluidCapability);
    }

    /** Hook for server-only facility processing (nest laying, cheese moulds). */
    public void serverTick() {
    }

    /**
     * Subclasses can constrain automation to their supported fluid family.
     * The default intentionally accepts every Forge fluid for generic troughs
     * and addon facilities; specialised blocks override it server-side.
     */
    protected boolean isFluidValid(FluidStack stack) {
        return stack != null && !stack.isEmpty();
    }

    protected boolean isItemValid(int slot, ItemStack stack) {
        return stack != null && !stack.isEmpty();
    }

    /** Whether hoppers, pipes and other sided automation may see this store. */
    protected boolean allowsAutomation() {
        return true;
    }

    public int fluidAmount(Predicate<FluidStack> filter) {
        FluidStack stored = fluidCapability.getFluid();
        return stored.isEmpty() || !filter.test(stored) ? 0 : stored.getAmount();
    }

    public int drainFluid(int amount, Predicate<FluidStack> filter, IFluidHandler.FluidAction action) {
        FluidStack stored = fluidCapability.getFluid();
        if (amount <= 0 || stored.isEmpty() || !filter.test(stored)) return 0;
        return fluidCapability.drain(amount, action).getAmount();
    }

    public FluidStack fluidSnapshot() {
        return fluidCapability.getFluid().copy();
    }

    public int fillFluid(FluidStack stack, IFluidHandler.FluidAction action) {
        return fluidCapability.fill(stack, action);
    }

    @Override
    public CompoundTag getUpdateTag() {
        return saveWithoutMetadata();
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(Connection connection, ClientboundBlockEntityDataPacket packet) {
        CompoundTag tag = packet.getTag();
        if (tag != null) load(tag);
    }

    @Override
    public void setChanged() {
        super.setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    public int getContainerSize() {
        return items.size();
    }

    @Override
    public boolean isEmpty() {
        return items.stream().allMatch(ItemStack::isEmpty);
    }

    @Override
    public ItemStack getItem(int slot) {
        return items.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack result = net.minecraft.world.ContainerHelper.removeItem(items, slot, amount);
        if (!result.isEmpty()) {
            setCapabilityStack(slot, items.get(slot));
            setChanged();
        }
        return result;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        ItemStack result = net.minecraft.world.ContainerHelper.takeItem(items, slot);
        setCapabilityStack(slot, ItemStack.EMPTY);
        return result;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        items.set(slot, stack);
        stack.setCount(Math.min(stack.getCount(), getMaxStackSize()));
        setCapabilityStack(slot, stack);
        setChanged();
    }

    private void setCapabilityStack(int slot, ItemStack stack) {
        syncingCapability = true;
        try {
            itemCapability.setStackInSlot(slot, stack.copy());
        } finally {
            syncingCapability = false;
        }
    }

    private void syncFromCapability() {
        for (int slot = 0; slot < items.size(); slot++) items.set(slot, itemCapability.getStackInSlot(slot).copy());
        setChanged();
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> capability, Direction side) {
        // Unsided access is retained for the owning block's direct player
        // interaction. Forge automation is sided and follows the legacy
        // allowTroughAutomation switch in trough/bowl subclasses.
        if (side != null && !allowsAutomation()
                && (capability == ForgeCapabilities.ITEM_HANDLER || capability == ForgeCapabilities.FLUID_HANDLER)) {
            return LazyOptional.empty();
        }
        if (capability == ForgeCapabilities.ITEM_HANDLER) return itemCapabilityOptional.cast();
        if (capability == ForgeCapabilities.FLUID_HANDLER) return fluidCapabilityOptional.cast();
        return super.getCapability(capability, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        itemCapabilityOptional.invalidate();
        fluidCapabilityOptional.invalidate();
    }

    @Override
    public boolean stillValid(Player player) {
        return level != null && level.getBlockEntity(worldPosition) == this
                && player.distanceToSqr(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5) <= 64.0;
    }

    @Override
    public void clearContent() {
        for (int slot = 0; slot < items.size(); slot++) {
            items.set(slot, ItemStack.EMPTY);
            setCapabilityStack(slot, ItemStack.EMPTY);
        }
        setChanged();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable(getBlockState().getBlock().getDescriptionId());
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        if (getContainerSize() < 9) return null;
        return new ChestMenu(MenuType.GENERIC_9x1, id, inventory, this, 1);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        net.minecraft.world.ContainerHelper.saveAllItems(tag, items);
        tag.put("AnimaniaFluid", fluidCapability.writeToNBT(new CompoundTag()));
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        // ContainerHelper only overwrites slots present in the incoming list.
        // An empty update therefore used to leave the previous client-side
        // stack in place, making a consumed trough keep rendering its final
        // food layer. Clear every slot before applying the authoritative tag.
        for (int slot = 0; slot < items.size(); slot++) items.set(slot, ItemStack.EMPTY);
        net.minecraft.world.ContainerHelper.loadAllItems(tag, items);
        if (tag.contains("AnimaniaFluid")) fluidCapability.readFromNBT(tag.getCompound("AnimaniaFluid"));
        for (int slot = 0; slot < items.size(); slot++) setCapabilityStack(slot, items.get(slot));
    }
}
