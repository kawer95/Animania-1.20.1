package com.animania.common.entity;

import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.FarmBlock;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BooleanSupplier;


/**
 * Lightweight native replacement for the 1.12 cart, wagon and tiller
 * entities.  It keeps the inventory/passenger semantics server-side without
 * inheriting animal AI or pretending a vehicle is a living mob.
 */
public class AnimaniaVehicleEntity extends Entity implements Container, MenuProvider {
    private static final net.minecraft.network.syncher.EntityDataAccessor<Optional<UUID>> PULLER =
            net.minecraft.network.syncher.SynchedEntityData.defineId(AnimaniaVehicleEntity.class,
                    net.minecraft.network.syncher.EntityDataSerializers.OPTIONAL_UUID);
    private static BooleanSupplier wagonSleepRule = () -> true;
    private final NonNullList<ItemStack> items = NonNullList.withSize(27, ItemStack.EMPTY);
    private boolean dropsSpawned;
    private int boostTicks;
    @Nullable
    private Entity puller;
    @Nullable
    private UUID pullerUuid;
    private BlockPos lastTillOrigin;

    public AnimaniaVehicleEntity(EntityType<? extends AnimaniaVehicleEntity> type, Level level) {
        super(type, level);
        this.noPhysics = false;
    }

    /** Farm (or another addon) may provide its common-config sleep rule. */
    public static void setWagonSleepRule(BooleanSupplier rule) {
        wagonSleepRule = rule == null ? () -> true : rule;
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(PULLER, Optional.empty());
    }

    @Override
    public void tick() {
        super.tick();
        resolvePuller();
        if (puller != null) {
            if (!puller.isAlive() || !(puller instanceof AnimaniaAnimalEntity animal) || !animal.canPullVehicles()) {
                detachPuller();
            } else {
                // Keep a stable two-block hitch behind the draft animal.  The
                // server owns the movement and clients receive normal entity
                // tracking updates, so chunk unload/reload cannot duplicate
                // the vehicle or leave a phantom attachment.
                Vec3 look = puller.getLookAngle();
                Vec3 forward = new Vec3(look.x, 0.0D, look.z);
                if (forward.lengthSqr() < 1.0E-5D) forward = new Vec3(0.0D, 0.0D, 1.0D);
                forward = forward.normalize();
                Vec3 target = puller.position().subtract(forward.scale(2.0D));
                setYRot(puller.getYRot());
                if (distanceToSqr(target) > 9.0D) {
                    moveTo(target.x, target.y, target.z, getYRot(), getXRot());
                }
                setDeltaMovement(Vec3.ZERO);
                if (isTiller()) tillAtCurrentPosition();
            }
        } else {
            Entity rider = getFirstPassenger();
            if (rider instanceof Player player) {
                setYRot(player.getYRot());
                Vec3 look = player.getLookAngle();
                Vec3 forward = new Vec3(look.x, 0.0D, look.z);
                if (forward.lengthSqr() > 1.0E-5D) forward = forward.normalize();
                Vec3 right = new Vec3(-forward.z, 0.0D, forward.x);
                Vec3 desired = forward.scale(player.zza).add(right.scale(player.xxa * 0.5D));
                if (desired.lengthSqr() > 1.0E-5D) {
                    double speed = (boostTicks > 0 ? 0.24D : 0.12D);
                    setDeltaMovement(desired.normalize().scale(speed));
                }
            }
            if (!onGround()) setDeltaMovement(getDeltaMovement().x, getDeltaMovement().y - 0.04D, getDeltaMovement().z);
            move(MoverType.SELF, getDeltaMovement());
            setDeltaMovement(getDeltaMovement().scale(onGround() ? 0.82D : 0.98D));
        }
        if (boostTicks > 0) boostTicks--;
    }

    /** Start a short speed boost; returns false while a boost is active. */
    public boolean boost() {
        if (boostTicks > 0) return false;
        boostTicks = 40;
        return true;
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        // Sneak-right-clicking a vehicle while mounted on a draft horse is the
        // modern equivalent of the legacy wagon hitch interaction.  It is a
        // single server mutation and therefore safe under competing clients.
        if (player.isSecondaryUseActive() && player.getVehicle() instanceof AnimaniaAnimalEntity animal
                && animal.canPullVehicles()) {
            if (!level().isClientSide) {
                if (puller == animal) detachPuller();
                else tryAttachPuller(animal);
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        if (player.isSecondaryUseActive()) {
            if (!level().isClientSide) player.openMenu(this);
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        if (isWagon() && player.getMainHandItem().isEmpty() && !player.isShiftKeyDown()
                && wagonSleepRule.getAsBoolean() && level().isNight()) {
            if (!level().isClientSide) {
                player.startSleeping(blockPosition());
                if (player instanceof net.minecraft.server.level.ServerPlayer server) {
                    server.setRespawnPosition(level().dimension(), blockPosition(), getYRot(), true, false);
                }
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        if (!player.isPassenger() && !isVehicle()) {
            if (!level().isClientSide) player.startRiding(this);
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        return InteractionResult.PASS;
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return getPassengers().isEmpty();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        ContainerHelper.saveAllItems(tag, items);
        tag.putInt("AnimaniaBoostTicks", boostTicks);
        if (puller != null) tag.putUUID("AnimaniaPuller", puller.getUUID());
        else if (pullerUuid != null) tag.putUUID("AnimaniaPuller", pullerUuid);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        ContainerHelper.loadAllItems(tag, items);
        boostTicks = Math.max(0, tag.getInt("AnimaniaBoostTicks"));
        puller = null;
        pullerUuid = tag.hasUUID("AnimaniaPuller") ? tag.getUUID("AnimaniaPuller") : null;
        entityData.set(PULLER, pullerUuid == null ? Optional.empty() : Optional.of(pullerUuid));
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return new ClientboundAddEntityPacket(this);
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
        return ContainerHelper.removeItem(items, slot, amount);
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(items, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        items.set(slot, stack);
        setChanged();
    }

    @Override
    public void setChanged() {
    }

    /** Attach this vehicle to an adult draft animal on the authoritative side. */
    public boolean tryAttachPuller(Entity candidate) {
        if (level().isClientSide || puller != null || !(candidate instanceof AnimaniaAnimalEntity animal)
                || !animal.canPullVehicles() || candidate == this || candidate.isPassenger()) return false;
        puller = candidate;
        pullerUuid = candidate.getUUID();
        entityData.set(PULLER, Optional.of(pullerUuid));
        setDeltaMovement(Vec3.ZERO);
        setChanged();
        playHitchSound("hitch");
        return true;
    }

    public void detachPuller() {
        boolean attached = puller != null || pullerUuid != null;
        puller = null;
        pullerUuid = null;
        entityData.set(PULLER, Optional.empty());
        setChanged();
        if (attached) playHitchSound("unhitch");
    }

    private void playHitchSound(String id) {
        if (level().isClientSide) return;
        net.minecraft.sounds.SoundEvent sound = ForgeRegistries.SOUND_EVENTS.getValue(
                new ResourceLocation("animania_farm", id));
        if (sound != null) level().playSound(null, blockPosition(), sound,
                net.minecraft.sounds.SoundSource.NEUTRAL, 0.7F, 1.5F);
    }

    public boolean isPulled() {
        return entityData.get(PULLER).isPresent();
    }

    @Nullable
    public Entity getPuller() {
        resolvePuller();
        return puller;
    }

    private void resolvePuller() {
        if (puller == null && pullerUuid != null && level() instanceof ServerLevel server) {
            puller = server.getEntity(pullerUuid);
            if (puller != null) pullerUuid = puller.getUUID();
        }
    }

    private boolean isWagon() {
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(getType());
        return id != null && "wagon".equals(id.getPath());
    }

    private boolean isTiller() {
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(getType());
        return id != null && "tiller".equals(id.getPath());
    }

    /** Three-wide 1.12 tiller pass, with server-side inventory seed consumption. */
    private void tillAtCurrentPosition() {
        if (level().isClientSide) return;
        BlockPos origin = blockPosition().below();
        if (origin.equals(lastTillOrigin)) return;
        lastTillOrigin = origin.immutable();
        Direction side = Direction.fromYRot(getYRot()).getClockWise();
        tillGround(origin);
        tillGround(origin.relative(side));
        tillGround(origin.relative(side.getOpposite()));
    }

    private void tillGround(BlockPos pos) {
        var state = level().getBlockState(pos);
        if (!(state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.DIRT) || state.is(Blocks.COARSE_DIRT) || state.is(Blocks.FARMLAND))) return;
        level().setBlock(pos, Blocks.FARMLAND.defaultBlockState().setValue(FarmBlock.MOISTURE, 7), 3);
        BlockPos cropPos = pos.above();
        var above = level().getBlockState(cropPos);
        if (above.getBlock() instanceof CropBlock) return;
        if (!above.isAir() && above.canBeReplaced()) level().destroyBlock(cropPos, false);
        if (!level().getBlockState(cropPos).isAir()) return;
        for (int slot = 0; slot < Math.min(10, items.size()); slot++) {
            ItemStack seed = items.get(slot);
            var crop = cropFor(seed);
            if (crop == null) continue;
            // Never consume inventory when another mod or a world rule rejects
            // the placement.  This keeps the tiller transactional under event
            // cancellation and prevents silent seed loss on protected land.
            if (!crop.canSurvive(level(), cropPos) || !level().setBlock(cropPos, crop, 3)
                    || !level().getBlockState(cropPos).is(crop.getBlock())) continue;
            seed.shrink(1);
            if (seed.isEmpty()) items.set(slot, ItemStack.EMPTY);
            setChanged();
            break;
        }
    }

    @Nullable
    private static net.minecraft.world.level.block.state.BlockState cropFor(ItemStack stack) {
        if (stack.is(net.minecraft.world.item.Items.WHEAT_SEEDS)) return Blocks.WHEAT.defaultBlockState();
        if (stack.is(net.minecraft.world.item.Items.BEETROOT_SEEDS)) return Blocks.BEETROOTS.defaultBlockState();
        if (stack.is(net.minecraft.world.item.Items.MELON_SEEDS)) return Blocks.MELON_STEM.defaultBlockState();
        if (stack.is(net.minecraft.world.item.Items.PUMPKIN_SEEDS)) return Blocks.PUMPKIN_STEM.defaultBlockState();
        return null;
    }

    @Override
    public void remove(RemovalReason reason) {
        if (!level().isClientSide && reason == RemovalReason.KILLED && !dropsSpawned
                && level().getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_DOENTITYDROPS)) {
            dropsSpawned = true;
            ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(getType());
            if (id != null) {
                net.minecraft.world.item.Item item = ForgeRegistries.ITEMS.getValue(id);
                if (item != null && item != net.minecraft.world.item.Items.AIR) spawnAtLocation(new ItemStack(item));
            }
            for (ItemStack stack : items) {
                if (!stack.isEmpty()) spawnAtLocation(stack.copy());
            }
        }
        super.remove(reason);
    }

    @Override
    public void clearContent() {
        items.clear();
        setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        return !isRemoved() && player.distanceToSqr(this) <= 64.0D;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable(getType().getDescriptionId());
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return ChestMenu.threeRows(id, inventory, this);
    }

    private static final class ContainerHelper {
        private static void saveAllItems(CompoundTag tag, NonNullList<ItemStack> stacks) {
            net.minecraft.world.ContainerHelper.saveAllItems(tag, stacks);
        }

        private static void loadAllItems(CompoundTag tag, NonNullList<ItemStack> stacks) {
            net.minecraft.world.ContainerHelper.loadAllItems(tag, stacks);
        }

        private static ItemStack removeItem(NonNullList<ItemStack> stacks, int slot, int amount) {
            return net.minecraft.world.ContainerHelper.removeItem(stacks, slot, amount);
        }

        private static ItemStack takeItem(NonNullList<ItemStack> stacks, int slot) {
            return net.minecraft.world.ContainerHelper.takeItem(stacks, slot);
        }
    }
}
