package com.animania.farm;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;

/** Durable crafting tool replacing the old non-repairable carving knife. */
public final class FarmCarvingKnifeItem extends SwordItem {
    public FarmCarvingKnifeItem() {
        super(Tiers.IRON, 2, -2.4F, new Item.Properties().durability(100));
    }

    @Override
    public boolean hasCraftingRemainingItem(ItemStack stack) { return true; }

    @Override
    public ItemStack getCraftingRemainingItem(ItemStack stack) {
        ItemStack remaining = stack.copy();
        remaining.setDamageValue(Math.min(remaining.getMaxDamage(), remaining.getDamageValue() + 1));
        return remaining.getDamageValue() >= remaining.getMaxDamage() ? ItemStack.EMPTY : remaining;
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target,
                                                   InteractionHand hand) {
        if (!(target instanceof com.animania.common.entity.AnimaniaAnimalEntity animal)
                || !animal.isLegacySterilizableFarmMale() || animal.isSterilized()) {
            return InteractionResult.PASS;
        }
        if (!target.level().isClientSide) {
            animal.setSterilized(true);
            animal.markInteracted();
            if (target.level() instanceof ServerLevel server) {
                server.sendParticles(ParticleTypes.EXPLOSION, target.getX(), target.getY() + target.getBbHeight() * 0.5D,
                        target.getZ(), 1, 0.0D, 0.0D, 0.0D, 0.0D);
            }
            target.level().playSound(null, target.blockPosition(), SoundEvents.MOOSHROOM_SHEAR,
                    target.getSoundSource(), 1.0F, 1.0F);
            if (!player.getAbilities().instabuild) {
                stack.hurtAndBreak(1, player, broken -> player.broadcastBreakEvent(hand));
            }
        }
        return InteractionResult.sidedSuccess(target.level().isClientSide);
    }
}
