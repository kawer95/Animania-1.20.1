package com.animania.common.entity.goal;

import com.animania.common.entity.AnimaniaAnimalEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.EnumSet;
import java.util.function.Predicate;

/** Native 1.20.1 port of the leashed adult pig truffle-snuffling goal. */
public final class AnimaniaPigSnuffleGoal extends Goal {
    private static final ResourceLocation TRUFFLE = new ResourceLocation("animania_farm", "truffle");
    private final AnimaniaAnimalEntity pig;
    private final Predicate<BlockPos> forestCheck;
    private boolean spawned;
    private boolean eaten;

    public AnimaniaPigSnuffleGoal(AnimaniaAnimalEntity pig) {
        this(pig, pos -> pig.level().getBiome(pos).is(BiomeTags.IS_FOREST));
    }

    /** Injectable biome check keeps the server behavior deterministic in GameTests. */
    public AnimaniaPigSnuffleGoal(AnimaniaAnimalEntity pig, Predicate<BlockPos> forestCheck) {
        this.pig = pig;
        this.forestCheck = forestCheck;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        BlockPos ground = pig.blockPosition().below();
        return AnimaniaFindMudGoal.supports(pig) && !isMud(ground) && !pig.isSleeping()
                && pig.getHunger() < 100 && pig.getRandom().nextInt(120) == 50;
    }

    @Override
    public void start() {
        spawned = false;
        eaten = false;
        pig.setEatingTicks(160);
        pig.getNavigation().stop();
    }

    @Override
    public boolean canContinueToUse() {
        return pig.getEatingTicks() > 0;
    }

    @Override
    public void tick() {
        int timer = Math.max(0, pig.getEatingTicks() - 1);
        pig.setEatingTicks(timer);
        BlockPos ground = pig.blockPosition().below();
        if (!pig.level().getBlockState(ground).is(Blocks.GRASS_BLOCK)) {
            stop();
            return;
        }
        if (timer > 80 && !spawned && forestCheck.test(ground) && pig.isAdult()
                && pig.isLeashed() && pig.getLeashHolder() instanceof Player) {
            pig.level().levelEvent(2001, ground, Block.getId(pig.level().getBlockState(ground)));
            Item truffle = ForgeRegistries.ITEMS.getValue(TRUFFLE);
            if (truffle != null) {
                pig.spawnAtLocation(new ItemStack(truffle, pig.getRandom().nextInt(2) + 1));
                spawned = true;
            }
        }
        if (timer < 100 && !eaten) {
            Item truffle = ForgeRegistries.ITEMS.getValue(TRUFFLE);
            if (truffle == null) return;
            for (ItemEntity itemEntity : pig.level().getEntitiesOfClass(ItemEntity.class,
                    pig.getBoundingBox().inflate(3.0D), item -> item.getItem().is(truffle))) {
                itemEntity.getItem().shrink(64);
                if (itemEntity.getItem().isEmpty()) itemEntity.discard();
                pig.setHunger(100);
                eaten = true;
            }
        }
    }

    @Override
    public void stop() {
        pig.setEatingTicks(0);
        spawned = false;
        eaten = false;
    }

    private boolean isMud(BlockPos pos) {
        ResourceLocation id = ForgeRegistries.BLOCKS.getKey(pig.level().getBlockState(pos).getBlock());
        return id != null && id.getPath().equals("mud");
    }
}
