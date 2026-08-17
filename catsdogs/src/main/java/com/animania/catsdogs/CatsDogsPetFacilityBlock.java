package com.animania.catsdogs;

import com.animania.common.entity.AnimaniaAnimalEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * Native pet facility block retaining the legacy shared pet_prop block entity.
 */
public final class CatsDogsPetFacilityBlock extends HorizontalDirectionalBlock implements EntityBlock {
    private static final VoxelShape CAT_BED_1_SHAPE = Block.box(2, 0, 2, 14, 1, 14);
    private static final VoxelShape CAT_BED_2_SHAPE = Block.box(2, 0, 2, 14, 2, 14);
    private static final VoxelShape CAT_TOWER_SHAPE = Block.box(0, 0, 0, 16, 24, 16);
    private static final VoxelShape DOG_PILLOW_SHAPE = Block.box(1, 0, 1, 15, 1, 15);
    private static final VoxelShape LITTER_BOX_SHAPE = Block.box(1, 0, 1, 15, 3, 15);
    private final String id;

    public CatsDogsPetFacilityBlock(String id, BlockBehaviour.Properties properties) {
        super(properties);
        this.id = id;
        registerDefaultState(stateDefinition.any().setValue(FACING, net.minecraft.core.Direction.NORTH));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide && player.isShiftKeyDown()) {
            player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.animania.pet_facility", id), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CatsDogsPetFacilityBlockEntity(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    public VoxelShape getShape(BlockState state, net.minecraft.world.level.BlockGetter level,
                               BlockPos pos, CollisionContext context) {
        return legacyShape();
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, net.minecraft.world.level.BlockGetter level,
                                        BlockPos pos, CollisionContext context) {
        return legacyShape();
    }

    @Override
    public VoxelShape getOcclusionShape(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos) {
        return Shapes.empty();
    }

    private VoxelShape legacyShape() {
        return switch (id) {
            case "cat_bed_1" -> CAT_BED_1_SHAPE;
            case "cat_bed_2" -> CAT_BED_2_SHAPE;
            case "cat_tower" -> CAT_TOWER_SHAPE;
            case "dog_pillow" -> DOG_PILLOW_SHAPE;
            case "litter_box" -> LITTER_BOX_SHAPE;
            default -> Shapes.block(); // The 1.12 dog house deliberately used a full-block AABB.
        };
    }

}
