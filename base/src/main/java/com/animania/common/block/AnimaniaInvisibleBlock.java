package com.animania.common.block;

import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlockContainer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Interactive and collidable second half of the legacy two-block trough. */
public final class AnimaniaInvisibleBlock extends BaseEntityBlock implements LiquidBlockContainer {
    public static final DirectionProperty CONTROLLER = DirectionProperty.create("controller", Direction.Plane.HORIZONTAL);
    private static final VoxelShape NORTH_SOUTH = Block.box(1.6, 0, 0, 14.4, 4.8, 16);
    private static final VoxelShape EAST_WEST = Block.box(0, 0, 1.6, 16, 4.8, 14.4);

    public AnimaniaInvisibleBlock(Properties properties) {
        super(properties.noOcclusion().noLootTable().strength(-1.0F, 3600000.0F));
        registerDefaultState(stateDefinition.any().setValue(CONTROLLER, Direction.NORTH));
    }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(CONTROLLER); }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new com.animania.common.AnimaniaBlocks.InvisibleTroughProxyEntity(pos, state);
    }
    public BlockPos controllerPos(BlockPos pos, BlockState state) { return pos.relative(state.getValue(CONTROLLER)); }
    private static VoxelShape shape(BlockState state) {
        return state.getValue(CONTROLLER).getAxis() == Direction.Axis.Z ? NORTH_SOUTH : EAST_WEST;
    }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return shape(state); }
    @Override public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return shape(state); }
    /** The collidable second half must be as resistant to flowing fluid as its controller. */
    @Override public boolean canBeReplaced(BlockState state, Fluid fluid) { return false; }
    @Override public boolean canPlaceLiquid(BlockGetter level, BlockPos pos, BlockState state, Fluid fluid) { return false; }
    @Override public boolean placeLiquid(LevelAccessor level, BlockPos pos, BlockState state, FluidState fluid) { return false; }
    @Override public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        BlockPos controller = controllerPos(pos, state);
        BlockState controllerState = level.getBlockState(controller);
        return controllerState.getBlock() instanceof AnimaniaTroughBlock trough ? trough.use(controllerState, level, controller, player, hand, hit) : InteractionResult.PASS;
    }
    @Override public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        BlockPos controller = controllerPos(pos, state); BlockState controllerState = level.getBlockState(controller);
        if (controllerState.getBlock() instanceof AnimaniaTroughBlock trough) trough.entityInside(controllerState, level, controller, entity);
    }
    @Override public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        BlockPos controller = controllerPos(pos, state);
        if (level.getBlockState(controller).getBlock() instanceof AnimaniaTroughBlock) level.destroyBlock(controller, !player.getAbilities().instabuild, player);
        super.playerWillDestroy(level, pos, state, player);
    }
    @Override public boolean hasAnalogOutputSignal(BlockState state) { return true; }
    @Override public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        BlockPos controller = controllerPos(pos, state); BlockState controllerState = level.getBlockState(controller);
        return controllerState.getBlock() instanceof AnimaniaTroughBlock trough ? trough.getAnalogOutputSignal(controllerState, level, controller) : 0;
    }
}
