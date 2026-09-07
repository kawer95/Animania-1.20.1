package com.animania.extra;

import com.animania.common.block.AnimaniaContainerBlock;
import com.animania.common.entity.AnimaniaAnimalEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.network.NetworkHooks;
import net.minecraft.server.level.ServerPlayer;

/** Functional hamster wheel block replacing the old legacy model tile entity. */
public final class ExtraHamsterWheelBlock extends AnimaniaContainerBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    public ExtraHamsterWheelBlock(BlockBehaviour.Properties properties) {
        super(properties, ExtraHamsterWheelBlockEntity::new);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
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
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(FACING);
    }

    /** The wheel mesh is rendered by its block entity, never by a cube model. */
    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    public VoxelShape getOcclusionShape(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos) {
        return Shapes.empty();
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        ExtraHamsterWheelBlockEntity wheel = level.getBlockEntity(pos) instanceof ExtraHamsterWheelBlockEntity found
                ? found : null;
        if (!level.isClientSide && hand == InteractionHand.MAIN_HAND
                && AnimaniaAnimalEntity.hasCarriedAnimal(player)
                && "animania_extra:hamster".equals(AnimaniaAnimalEntity.carriedAnimalType(player))
                && wheel != null
                && !wheel.isRunning()) {
            EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation("animania_extra", "hamster"));
            AnimaniaAnimalEntity hamster = type == null ? null : (AnimaniaAnimalEntity) type.create(level);
            if (hamster != null) {
                hamster.readAdditionalSaveData(AnimaniaAnimalEntity.carriedAnimalData(player));
                if (!hamster.isInBall() && hamster.isFed()) {
                    CompoundTag stored = new CompoundTag();
                    hamster.addAdditionalSaveData(stored);
                    if (!wheel.insertHamster(stored)) return InteractionResult.PASS;
                    AnimaniaAnimalEntity.clearCarriedAnimal(player);
                    level.playSound(null, pos, SoundEvents.ITEM_PICKUP, hamster.getSoundSource(), 1.0F, 1.0F);
                    player.swing(hand);
                    return InteractionResult.CONSUME;
                }
                hamster.discard();
            }
        }
        if (wheel != null && hand == InteractionHand.MAIN_HAND
                && player.getItemInHand(hand).is(ExtraContent.ITEM_ENTRIES.get("hamster_food").get())) {
            if (level.isClientSide) return InteractionResult.SUCCESS;
            ItemStack held = player.getItemInHand(hand);
            if (wheel.tryInsertFood(held)) {
                if (!player.getAbilities().instabuild) held.shrink(1);
                player.swing(hand);
                return InteractionResult.CONSUME;
            }
        }
        if (wheel != null && hand == InteractionHand.MAIN_HAND && wheel.hasHamster()
                && player.isShiftKeyDown() && player.getItemInHand(hand).isEmpty()) {
            if (level.isClientSide) return InteractionResult.SUCCESS;
            if (wheel.releaseHamster()) {
                level.playSound(null, pos, SoundEvents.ITEM_PICKUP, player.getSoundSource(), 1.0F, 1.0F);
                player.swing(hand);
            }
            return InteractionResult.CONSUME;
        }
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (wheel != null && player instanceof ServerPlayer serverPlayer) {
            NetworkHooks.openScreen(serverPlayer, wheel, pos);
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState replacement, boolean moving) {
        if (!state.is(replacement.getBlock()) && level.getBlockEntity(pos) instanceof ExtraHamsterWheelBlockEntity wheel) {
            wheel.ejectHamster();
        }
        super.onRemove(state, level, pos, replacement, moving);
    }
}
