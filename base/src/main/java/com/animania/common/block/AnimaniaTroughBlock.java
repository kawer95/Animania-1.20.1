package com.animania.common.block;

import com.animania.common.AnimaniaBlocks;
import com.animania.common.config.AnimaniaConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.LiquidBlockContainer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;

/** Native two-block trough retaining the legacy controller/companion layout. */
public final class AnimaniaTroughBlock extends BaseEntityBlock implements LiquidBlockContainer {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    private static final VoxelShape NORTH_SOUTH = Block.box(1.6, 0, 0, 14.4, 4.8, 16);
    private static final VoxelShape EAST_WEST = Block.box(0, 0, 1.6, 16, 4.8, 14.4);

    public AnimaniaTroughBlock(Properties properties) {
        super(properties.noOcclusion().randomTicks());
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    public BlockPos companionPos(BlockPos controller, BlockState state) { return controller.relative(state.getValue(FACING)); }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(FACING); }
    @Override public BlockState rotate(BlockState state, Rotation rotation) { return state.setValue(FACING, rotation.rotate(state.getValue(FACING))); }
    @Override public BlockState mirror(BlockState state, Mirror mirror) { return rotate(state, mirror.getRotation(state.getValue(FACING))); }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(FACING).getAxis() == Direction.Axis.Z ? NORTH_SOUTH : EAST_WEST;
    }
    @Override public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShape(state, level, pos, context);
    }

    /** Treat the wooden trough as an ordinary solid block when fluid spreads. */
    @Override public boolean canBeReplaced(BlockState state, Fluid fluid) { return false; }

    /**
     * FlowingFluid ignores canBeReplaced for non-solid partial blocks in
     * 1.20.1.  Implementing LiquidBlockContainer makes it consult this method
     * before replacing the trough with the incoming fluid.
     */
    @Override public boolean canPlaceLiquid(BlockGetter level, BlockPos pos, BlockState state, Fluid fluid) { return false; }
    @Override public boolean placeLiquid(LevelAccessor level, BlockPos pos, BlockState state, FluidState fluid) { return false; }

    @Override public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection();
        BlockPos companion = context.getClickedPos().relative(facing);
        return context.getLevel().getBlockState(companion).canBeReplaced(context)
                ? defaultBlockState().setValue(FACING, facing) : null;
    }

    @Override public void setPlacedBy(Level level, BlockPos pos, BlockState state, net.minecraft.world.entity.LivingEntity placer, ItemStack stack) {
        ensureCompanion(level, pos, state);
    }

    /** Restore the companion block for old saves without overwriting a real block. */
    public boolean ensureCompanion(Level level, BlockPos pos, BlockState state) {
        if (level.isClientSide) return false;
        BlockPos companion = companionPos(pos, state);
        BlockState existing = level.getBlockState(companion);
        if (existing.is(AnimaniaBlocks.INVISIBLE_BLOCK.get())) return false;
        if (!existing.isAir()) return false;
        return level.setBlock(companion, AnimaniaBlocks.INVISIBLE_BLOCK.get().defaultBlockState()
                .setValue(AnimaniaInvisibleBlock.CONTROLLER, state.getValue(FACING).getOpposite()), 3);
    }

    @Override public void onRemove(BlockState state, Level level, BlockPos pos, BlockState replacement, boolean moving) {
        if (!state.is(replacement.getBlock())) {
            if (level.getBlockEntity(pos) instanceof AnimaniaStorageBlockEntity storage) net.minecraft.world.Containers.dropContents(level, pos, storage);
            BlockPos companion = companionPos(pos, state);
            if (level.getBlockState(companion).is(AnimaniaBlocks.INVISIBLE_BLOCK.get())) level.removeBlock(companion, false);
        }
        super.onRemove(state, level, pos, replacement, moving);
    }

    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new AnimaniaBlocks.TroughEntity(pos, state); }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, AnimaniaBlocks.TROUGH_BE.get(), (l, p, s, be) -> {
            ensureCompanion(l, p, s);
            be.serverTick();
        });
    }

    @Override public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (level.isRainingAt(pos.above())
                && level.getBiome(pos).value().getBaseTemperature() >= 0.15F
                && level.getBlockEntity(pos) instanceof AnimaniaBlocks.TroughEntity trough) {
            collectRain(trough);
        }
    }

    /** Applies the 1.12 rain increment without allowing food/fluid mixing. */
    public static int collectRain(AnimaniaBlocks.TroughEntity trough) {
        if (trough == null || !trough.getItem(0).isEmpty()) return 0;
        FluidStack stored = trough.fluidSnapshot();
        if (!stored.isEmpty() && stored.getFluid() != Fluids.WATER) return 0;
        if (stored.getAmount() >= 1000) return 0;
        return trough.fillFluid(new FluidStack(Fluids.WATER, Math.min(100, 1000 - stored.getAmount())),
                net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
    }

    @Override public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (!level.isClientSide && entity instanceof ItemEntity dropped && !dropped.getItem().isEmpty()
                && AnimaniaConfig.matchesTroughFood(dropped.getItem()) && level.getBlockEntity(pos) instanceof AnimaniaBlocks.TroughEntity trough
                && trough.fluidSnapshot().isEmpty()) {
            ItemStack held = dropped.getItem();
            ItemStack existing = trough.getItem(0);
            if ((existing.isEmpty() || ItemStack.isSameItemSameTags(existing, held)) && existing.getCount() < 3) {
                int moved = Math.min(3 - existing.getCount(), held.getCount());
                ItemStack inserted = held.copy(); inserted.setCount(existing.getCount() + moved); trough.setItem(0, inserted);
                held.shrink(moved); if (held.isEmpty()) dropped.discard();
            }
        }
    }

    @Override public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof AnimaniaBlocks.TroughEntity trough)) return InteractionResult.PASS;
        boolean fluidInteraction = trough.getCapability(ForgeCapabilities.FLUID_HANDLER).resolve()
                .map(handler -> FluidUtil.interactWithFluidHandler(player, hand, handler)).orElse(false);
        if (fluidInteraction) return InteractionResult.sidedSuccess(level.isClientSide);
        ItemStack held = player.getItemInHand(hand);
        if (!held.isEmpty() && AnimaniaConfig.matchesTroughFood(held) && trough.fluidSnapshot().isEmpty()) {
            if (!level.isClientSide) {
                ItemStack existing = trough.getItem(0);
                if (existing.isEmpty() || ItemStack.isSameItemSameTags(existing, held)) {
                    int moved = Math.min(3 - existing.getCount(), held.getCount());
                    if (moved > 0) { ItemStack inserted = held.copy(); inserted.setCount(existing.getCount() + moved); trough.setItem(0, inserted); if (!player.getAbilities().instabuild) held.shrink(moved); }
                }
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (held.isEmpty() && !trough.getItem(0).isEmpty()) {
            if (!level.isClientSide) { ItemStack out = trough.removeItem(0, 1); if (!player.addItem(out)) player.drop(out, false); }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.PASS;
    }

    @Override public boolean hasAnalogOutputSignal(BlockState state) { return true; }
    @Override public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        if (!(level.getBlockEntity(pos) instanceof AnimaniaBlocks.TroughEntity trough)) return 0;
        if (!trough.getItem(0).isEmpty()) return Math.min(15, trough.getItem(0).getCount() * 5);
        return Math.min(15, trough.fluidSnapshot().getAmount() / 66);
    }
}
