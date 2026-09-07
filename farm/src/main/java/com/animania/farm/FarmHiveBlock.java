package com.animania.farm;

import com.animania.common.block.AnimaniaContainerBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;

/** Modern bee-hive interaction replacing the old tile entity/legacy model path. */
public final class FarmHiveBlock extends AnimaniaContainerBlock {
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    private static final VoxelShape SHAPE = Block.box(2, 1, 2, 14, 15, 14);

    public FarmHiveBlock(BlockBehaviour.Properties properties, boolean wild) {
        super(properties, (pos, state) -> wild
                ? new FarmHiveBlockEntity(FarmContent.WILD_HIVE_BE.get(), pos, state)
                : new FarmHiveBlockEntity(FarmContent.HIVE_BE.get(), pos, state));
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
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
    public VoxelShape getShape(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    /** The visible hive is rendered exclusively by {@link FarmHiveRenderer}. */
    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    public VoxelShape getOcclusionShape(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos) {
        return Shapes.empty();
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof FarmHiveBlockEntity hive)) return InteractionResult.PASS;
        ItemStack held = player.getItemInHand(hand);
        if (held.is(Items.GLASS_BOTTLE) && hive.honeyAmount() >= FluidType.BUCKET_VOLUME) {
            if (level.isClientSide) return InteractionResult.SUCCESS;
            hive.honeyTank().drain(FluidType.BUCKET_VOLUME, IFluidHandler.FluidAction.EXECUTE);
            // The 1.12 Farm addon registered the filled container as
            // animania:honey_bottle and rendered it with bottle_honey.png.
            // honey_jar is retained only as a save-compatibility alias.
            ItemStack honey = new ItemStack(FarmContent.ITEM_ENTRIES.get("honey_bottle").get());
            replaceHeld(player, hand, honey);
            level.playSound(null, pos, SoundEvents.BOTTLE_FILL, net.minecraft.sounds.SoundSource.BLOCKS, 0.8F, 1.0F);
            return InteractionResult.CONSUME;
        }
        if (!held.isEmpty() && isEmptyFluidContainer(held) && hive.honeyAmount() >= FluidType.BUCKET_VOLUME) {
            if (level.isClientSide) return InteractionResult.SUCCESS;
            IFluidHandlerItem handler = FluidUtil.getFluidHandler(held).orElse(null);
            if (handler == null) return InteractionResult.PASS;
            FluidStack honey = hive.honeyTank().drain(FluidType.BUCKET_VOLUME, IFluidHandler.FluidAction.SIMULATE);
            if (honey.getAmount() < FluidType.BUCKET_VOLUME || handler.fill(honey, IFluidHandler.FluidAction.SIMULATE) < honey.getAmount()) return InteractionResult.PASS;
            hive.honeyTank().drain(honey.getAmount(), IFluidHandler.FluidAction.EXECUTE);
            handler.fill(honey, IFluidHandler.FluidAction.EXECUTE);
            replaceHeld(player, hand, handler.getContainer());
            return InteractionResult.CONSUME;
        }
        if (player.isShiftKeyDown() && held.isEmpty()) {
            if (!level.isClientSide) player.displayClientMessage(Component.translatable("message.animania.hive_honey", hive.honeyAmount()), true);
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        // Hives expose their fluid interactions directly and never had an
        // inventory screen. Do not leak the generic storage menu here.
        return InteractionResult.PASS;
    }

    private static boolean isEmptyFluidContainer(ItemStack stack) {
        return stack.is(Items.BUCKET) || stack.getItem() instanceof net.minecraft.world.item.BucketItem;
    }

    private static void replaceHeld(Player player, InteractionHand hand, ItemStack replacement) {
        if (player.getAbilities().instabuild) return;
        ItemStack held = player.getItemInHand(hand);
        if (held.getCount() > 1) {
            held.shrink(1);
            if (!player.addItem(replacement)) player.drop(replacement, false);
        } else player.setItemInHand(hand, replacement);
    }
}
