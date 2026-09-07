package com.animania.common.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.function.BiFunction;

/** One-slot, three-egg legacy nest with direct empty-hand extraction. */
public final class AnimaniaNestBlock extends AnimaniaContainerBlock {
    private static final VoxelShape SHAPE = box(0, 0, 0, 16, 4.8, 16);

    public AnimaniaNestBlock(Properties properties, BiFunction<BlockPos, BlockState, net.minecraft.world.level.block.entity.BlockEntity> factory) {
        super(properties.noOcclusion(), factory);
    }

    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return SHAPE; }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (level.getBlockEntity(pos) instanceof com.animania.common.AnimaniaBlocks.NestEntity nest
                && !nest.getItem(0).isEmpty()) {
            com.animania.common.NestHatchHooks.tryHatch(level, pos, nest, random);
        }
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!player.getItemInHand(hand).isEmpty() || player.isShiftKeyDown()) return InteractionResult.PASS;
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof AnimaniaStorageBlockEntity storage) {
            ItemStack extracted = storage.removeItem(0, 1);
            if (!extracted.isEmpty() && !player.addItem(extracted)) player.drop(extracted, false);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
