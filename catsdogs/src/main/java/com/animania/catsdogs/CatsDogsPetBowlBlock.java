package com.animania.catsdogs;

import com.animania.api.AnimaniaTags;
import com.animania.common.AnimaniaItems;
import com.animania.common.block.AnimaniaContainerBlock;
import com.animania.common.entity.AnimaniaAnimalEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import net.minecraftforge.fluids.FluidStack;
import net.minecraft.world.level.material.Fluids;

/** Modern server-authoritative pet bowl: solid food or one water container. */
public final class CatsDogsPetBowlBlock extends AnimaniaContainerBlock {
    private static final VoxelShape SHAPE = Block.box(4, 0, 4, 12, 4, 12);

    public CatsDogsPetBowlBlock(BlockBehaviour.Properties properties) {
        super(properties, CatsDogsPetBowlBlockEntity::new);
    }

    @Override
    public VoxelShape getShape(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof CatsDogsPetBowlBlockEntity bowl)) return InteractionResult.PASS;
        ItemStack held = player.getItemInHand(hand);
        if (isFoodItem(held)) {
            if (level.isClientSide) return InteractionResult.SUCCESS;
            if (bowl.tryInsertFood(held)) {
                if (!player.getAbilities().instabuild) held.shrink(1);
                level.playSound(null, pos, SoundEvents.GENERIC_EAT, net.minecraft.sounds.SoundSource.BLOCKS, 0.55F, 0.9F);
                return InteractionResult.CONSUME;
            }
        }
        if (isWaterContainer(held)) {
            if (level.isClientSide) return InteractionResult.SUCCESS;
            if (fillFromContainer(bowl, player, hand)) return InteractionResult.CONSUME;
        }
        if (held.isEmpty() && !bowl.getItem(0).isEmpty()) {
            if (level.isClientSide) return InteractionResult.SUCCESS;
            ItemStack result = bowl.removeItem(0, 1);
            if (!player.addItem(result)) player.drop(result, false);
            return InteractionResult.CONSUME;
        }
        if (!held.isEmpty() && isEmptyFluidContainer(held)) {
            if (level.isClientSide) return InteractionResult.SUCCESS;
            if (drainToContainer(bowl, player, hand)) return InteractionResult.CONSUME;
        }
        return super.use(state, level, pos, player, hand, hit);
    }

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (level.isClientSide || !(level.getBlockEntity(pos) instanceof CatsDogsPetBowlBlockEntity bowl)) return;
        if (entity instanceof ItemEntity itemEntity) {
            ItemStack stack = itemEntity.getItem();
            if (isFoodItem(stack) && bowl.tryInsertFood(stack)) {
                stack.shrink(1);
                itemEntity.setItem(stack);
            } else if (isWaterContainer(stack) && fillFromItemEntity(bowl, itemEntity)) {
                // fillFromItemEntity replaces the consumed container. Do not
                // overwrite that bucket/bottle with the now-empty old stack.
            }
        } else if (entity instanceof AnimaniaAnimalEntity animal && animal.getHunger() < 100 && !bowl.getItem(0).isEmpty()) {
            ItemStack food = bowl.getItem(0);
            if (animal.feed(food)) bowl.setItem(0, new ItemStack(food.getItem(), food.getCount() - 1));
        }
    }

    public static boolean isFoodItem(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        var registered = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (registered != null && CatsDogsConfig.matchesPetBowlFood(stack)) return true;
        return stack.is(AnimaniaTags.ANIMAL_FEED) || stack.is(AnimaniaTags.BREEDING_FOOD)
                || stack.is(Items.COD) || stack.is(Items.SALMON) || stack.is(Items.TROPICAL_FISH)
                || stack.is(Items.PUFFERFISH) || stack.is(Items.BEEF) || stack.is(Items.COOKED_BEEF)
                || stack.is(Items.CHICKEN) || stack.is(Items.COOKED_CHICKEN)
                || stack.is(Items.PORKCHOP) || stack.is(Items.COOKED_PORKCHOP)
                || stack.is(Items.RABBIT) || stack.is(Items.COOKED_RABBIT)
                || stack.is(Items.MUTTON) || stack.is(Items.COOKED_MUTTON)
                || stack.is(AnimaniaItems.HAY.get());
    }

    private static boolean isWaterContainer(ItemStack stack) {
        return stack.is(Items.WATER_BUCKET) || stack.is(AnimaniaItems.WATER_BOTTLE.get());
    }

    private static boolean isEmptyFluidContainer(ItemStack stack) {
        return stack.is(Items.BUCKET) || stack.is(Items.GLASS_BOTTLE);
    }

    private static boolean fillFromContainer(CatsDogsPetBowlBlockEntity bowl, Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (held.is(Items.WATER_BUCKET)) {
            if (!fillWater(bowl)) return false;
            replaceHeld(player, hand, new ItemStack(Items.BUCKET));
            return true;
        }
        if (held.is(AnimaniaItems.WATER_BOTTLE.get())) {
            if (!fillWater(bowl)) return false;
            replaceHeld(player, hand, new ItemStack(Items.GLASS_BOTTLE));
            return true;
        }
        IFluidHandlerItem handler = FluidUtil.getFluidHandler(held).orElse(null);
        if (handler == null) return false;
        FluidStack fluid = handler.getFluidInTank(0);
        if (fluid.isEmpty() || fluid.getFluid() != Fluids.WATER || fluid.getAmount() < FluidType.BUCKET_VOLUME || !fillWater(bowl)) return false;
        handler.drain(FluidType.BUCKET_VOLUME, IFluidHandler.FluidAction.EXECUTE);
        replaceHeld(player, hand, handler.getContainer());
        return true;
    }

    private static boolean fillFromItemEntity(CatsDogsPetBowlBlockEntity bowl, ItemEntity entity) {
        ItemStack stack = entity.getItem();
        if (stack.is(Items.WATER_BUCKET) || stack.is(AnimaniaItems.WATER_BOTTLE.get())) {
            boolean bucket = stack.is(Items.WATER_BUCKET);
            if (!fillWater(bowl)) return false;
            stack.shrink(1);
            if (stack.isEmpty()) stack = new ItemStack(bucket ? Items.BUCKET : Items.GLASS_BOTTLE);
            entity.setItem(stack);
            return true;
        }
        return false;
    }

    private static boolean fillWater(CatsDogsPetBowlBlockEntity bowl) {
        return bowl.getCapability(ForgeCapabilities.FLUID_HANDLER, Direction.UP).map(handler ->
                handler.fill(new FluidStack(Fluids.WATER, FluidType.BUCKET_VOLUME), IFluidHandler.FluidAction.EXECUTE)
                        == FluidType.BUCKET_VOLUME).orElse(false);
    }

    private static boolean drainToContainer(CatsDogsPetBowlBlockEntity bowl, Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        IFluidHandlerItem handler = FluidUtil.getFluidHandler(held).orElse(null);
        if (handler == null) return false;
        return bowl.getCapability(ForgeCapabilities.FLUID_HANDLER, Direction.UP).map(source -> {
            FluidStack drained = source.drain(FluidType.BUCKET_VOLUME, IFluidHandler.FluidAction.SIMULATE);
            if (drained.getAmount() < FluidType.BUCKET_VOLUME) return false;
            int accepted = handler.fill(drained, IFluidHandler.FluidAction.SIMULATE);
            if (accepted < drained.getAmount()) return false;
            source.drain(drained.getAmount(), IFluidHandler.FluidAction.EXECUTE);
            ItemStack filled = handler.getContainer();
            replaceHeld(player, hand, filled);
            return true;
        }).orElse(false);
    }

    private static void replaceHeld(Player player, InteractionHand hand, ItemStack replacement) {
        if (player.getAbilities().instabuild) return;
        ItemStack held = player.getItemInHand(hand);
        if (held.getCount() > 1) {
            held.shrink(1);
            if (!player.addItem(replacement)) player.drop(replacement, false);
        } else {
            player.setItemInHand(hand, replacement);
        }
    }
}
