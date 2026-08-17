package com.animania.extra;

import com.animania.common.block.AnimaniaStorageBlockEntity;
import com.animania.common.entity.AnimaniaAnimalEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.EnergyStorage;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * FE-capable hamster wheel.  A nearby Animania hamster runs server-side,
 * consumes hamster food from the inventory at the configured interval, and
 * generates FE through the standard Forge capability.
 */
public final class ExtraHamsterWheelBlockEntity extends AnimaniaStorageBlockEntity {
    private final PersistedEnergyStorage energy = new PersistedEnergyStorage(ExtraConfig.HAMSTER_WHEEL_CAPACITY.get());
    private final LazyOptional<EnergyStorage> energyOptional = LazyOptional.of(() -> energy);
    private CompoundTag hamsterData = new CompoundTag();
    private int useTicks;
    private boolean running;

    public ExtraHamsterWheelBlockEntity(BlockPos pos, BlockState state) {
        super(ExtraContent.HAMSTER_WHEEL_BE.get(), pos, state, 1, 0);
    }

    @Override
    public int getMaxStackSize() {
        return 16;
    }

    @Override
    protected boolean isItemValid(int slot, ItemStack stack) {
        return slot == 0 && isHamsterFood(stack);
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return isItemValid(slot, stack);
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new ExtraHamsterWheelMenu(id, inventory, this);
    }

    @Override
    public void serverTick() {
        running = hasHamster();
        if (!running) {
            useTicks = 0;
            return;
        }
        energy.receiveEnergy(ExtraConfig.HAMSTER_WHEEL_GENERATION.get(), false);
        pushEnergyToNeighbours();
        if (++useTicks >= ExtraConfig.HAMSTER_WHEEL_USE_TIME.get()) {
            useTicks = 0;
            ItemStack food = getItem(0);
            if (!food.isEmpty() && isHamsterFood(food)) {
                setItem(0, new ItemStack(food.getItem(), food.getCount() - 1));
            } else {
                ejectHamster();
                running = false;
            }
        }
        setChanged();
    }

    private void pushEnergyToNeighbours() {
        if (level == null || energy.getEnergyStored() <= 0) return;
        for (net.minecraft.core.Direction direction : net.minecraft.core.Direction.values()) {
            var neighbour = level.getBlockEntity(worldPosition.relative(direction));
            if (neighbour == null) continue;
            int available = energy.getEnergyStored();
            if (available <= 0) break;
            int accepted = neighbour.getCapability(ForgeCapabilities.ENERGY, direction.getOpposite())
                    .map(handler -> handler.canReceive() ? handler.receiveEnergy(available, false) : 0)
                    .orElse(0);
            if (accepted > 0) energy.extractEnergy(accepted, false);
        }
    }

    public boolean insertHamster(CompoundTag data) {
        if (hasHamster() || data == null || data.isEmpty()) return false;
        hamsterData = data.copy();
        running = true;
        useTicks = 0;
        setChanged();
        return true;
    }

    public boolean hasHamster() {
        return hamsterData != null && !hamsterData.isEmpty();
    }

    public String hamsterVariant() {
        String variant = hasHamster() ? hamsterData.getString("AnimaniaVariant") : "tarou";
        return switch (variant) {
            case "black", "brown", "darkbrown", "darkgray", "gray", "plum", "tarou", "white", "gold" -> variant;
            default -> "tarou";
        };
    }

    public boolean ejectHamster() {
        return releaseHamster(true);
    }

    /** Player-requested removal is not the legacy food-exhaustion path. */
    public boolean releaseHamster() {
        return releaseHamster(false);
    }

    private boolean releaseHamster(boolean markHungry) {
        if (!hasHamster() || level == null || level.isClientSide) return false;
        var type = ForgeRegistries.ENTITY_TYPES.getValue(
                new ResourceLocation(AnimaniaExtra.MOD_ID, "hamster"));
        if (type == null || !(type.create(level) instanceof AnimaniaAnimalEntity hamster)) return false;
        hamster.readAdditionalSaveData(hamsterData.copy());
        // The legacy wheel marks a runner unfed when its work cycle exhausts
        // the food supply (and when the wheel is broken). A manual removal did
        // not exist in 1.12, so it must not masquerade as food exhaustion.
        if (markHungry) hamster.setHunger(0);
        for (net.minecraft.core.Direction direction : net.minecraft.core.Direction.values()) {
            if (direction == net.minecraft.core.Direction.DOWN) continue;
            BlockPos target = worldPosition.relative(direction);
            hamster.moveTo(target.getX() + 0.5D, target.getY(), target.getZ() + 0.5D, 0.0F, 0.0F);
            if (!level.getWorldBorder().isWithinBounds(target) || !level.noCollision(hamster)) continue;
            hamster.setPersistenceRequired();
            if (!level.addFreshEntity(hamster)) return false;
            hamsterData = new CompoundTag();
            running = false;
            useTicks = 0;
            setChanged();
            return true;
        }
        hamster.discard();
        return false;
    }

    private static boolean isHamsterFood(ItemStack stack) {
        net.minecraft.world.item.Item food = ForgeRegistries.ITEMS.getValue(new ResourceLocation(AnimaniaExtra.MOD_ID, "hamster_food"));
        return food != null && stack.is(food);
    }

    public boolean tryInsertFood(ItemStack stack) {
        if (!isHamsterFood(stack)) return false;
        ItemStack current = getItem(0);
        if (!current.isEmpty() && !ItemStack.isSameItemSameTags(current, stack)) return false;
        if (current.getCount() >= getMaxStackSize()) return false;
        setItem(0, stack.copyWithCount(current.getCount() + 1));
        return true;
    }

    public boolean isRunning() {
        return running;
    }

    public int energyStored() {
        return energy.getEnergyStored();
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> capability, net.minecraft.core.Direction side) {
        if (capability == ForgeCapabilities.ENERGY) return energyOptional.cast();
        return super.getCapability(capability, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        energyOptional.invalidate();
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("Energy", energy.getEnergyStored());
        tag.putInt("UseTicks", useTicks);
        tag.putBoolean("Running", running);
        if (hasHamster()) tag.put("Hamster", hamsterData.copy());
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        ItemStack food = getItem(0);
        if (!food.isEmpty() && food.getCount() > getMaxStackSize()) {
            setItem(0, food.copyWithCount(getMaxStackSize()));
        }
        energy.restore(tag.getInt("Energy"));
        useTicks = Math.max(0, tag.getInt("UseTicks"));
        hamsterData = tag.contains("Hamster") ? tag.getCompound("Hamster").copy() : new CompoundTag();
        running = hasHamster() && tag.getBoolean("Running");
    }

    private static final class PersistedEnergyStorage extends EnergyStorage {
        private PersistedEnergyStorage(int capacity) {
            super(capacity, capacity, capacity);
        }

        private void restore(int stored) {
            energy = Math.max(0, Math.min(capacity, stored));
        }
    }
}
