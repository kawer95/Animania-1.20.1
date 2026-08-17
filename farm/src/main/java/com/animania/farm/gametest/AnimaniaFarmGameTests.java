package com.animania.farm.gametest;

import com.animania.api.data.AnimalGender;
import com.animania.common.entity.AnimaniaAnimalEntity;
import com.animania.common.config.AnimaniaConfig;
import com.animania.common.entity.AnimaniaVehicleEntity;
import com.animania.common.entity.goal.AnimaniaTemptGoal;
import com.animania.common.entity.goal.AnimaniaFindMudGoal;
import com.animania.common.entity.goal.AnimaniaPigSnuffleGoal;
import com.animania.common.entity.goal.AnimaniaFindWaterGoal;
import com.animania.common.entity.goal.AnimaniaFindFoodGoal;
import com.animania.common.entity.goal.AnimaniaFindSaltLickGoal;
import com.animania.common.entity.goal.AnimaniaSleepGoal;
import com.animania.common.entity.goal.AnimaniaMateGoal;
import com.animania.common.entity.goal.AnimaniaFollowParentGoal;
import com.animania.common.entity.goal.AnimaniaPanicGoal;
import com.animania.common.entity.goal.AnimaniaWanderAvoidWaterGoal;
import com.animania.common.entity.goal.AnimaniaLookIdleGoal;
import com.animania.common.entity.goal.AnimaniaWatchClosestGoal;
import com.animania.common.entity.goal.AnimaniaEatGrassGoal;
import com.animania.common.entity.goal.AnimaniaRivalHeadbuttGoal;
import com.animania.gametest.AnimaniaGameTestEvidence;
import com.animania.common.AnimaniaBlocks;
import com.animania.common.advancement.FeedAnimalTrigger;
import com.animania.common.recipe.SlopRecipe;
import com.mojang.authlib.GameProfile;
import com.animania.farm.AnimaniaFarm;
import com.animania.farm.FarmCheeseMoldBlockEntity;
import com.animania.farm.FarmCheeseMoldBlock;
import com.animania.farm.FarmConfig;
import com.animania.farm.FarmHiveBlockEntity;
import com.animania.farm.FarmCheeseBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import com.animania.farm.FarmContent;
import com.animania.farm.FarmFluids;
import com.animania.farm.FarmMilkBottleItem;
import com.animania.farm.FarmHoneyJarItem;
import com.animania.farm.FarmBrownEggItem;
import com.animania.farm.FarmEggThrowHandler;
import com.animania.farm.FarmCarvingKnifeItem;
import com.animania.farm.FarmRidingCropItem;
import com.animania.farm.FarmWoolBlock;
import com.animania.farm.FarmWoolBlockItem;
import com.animania.farm.FarmMilkConversionRecipe;
import com.animania.farm.FarmLegacyIds;
import com.animania.farm.FarmSounds;
import com.animania.common.item.AnimaniaEntityEggItem;
import net.minecraftforge.registries.ForgeRegistries;
import java.util.UUID;

@GameTestHolder("animania_farm")
@PrefixGameTestTemplate(false)
public final class AnimaniaFarmGameTests {
    @GameTest(template = "empty", timeoutTicks = 80)
    public static void faintingGoatSprintCollisionSetsAndExpiresSpookedState(GameTestHelper helper) {
        AnimaniaGameTestEvidence.mark("animania_farm:fainting_goat_sprint_collision");
        AnimaniaAnimalEntity goat = createAnimal(helper, "doe_fainting");
        goat.moveTo(helper.absolutePos(new BlockPos(1, 1, 1)), 0.0F, 0.0F);
        helper.getLevel().addFreshEntity(goat);
        var player = helper.makeMockPlayer();
        player.moveTo(helper.absolutePos(new BlockPos(1, 1, 2)), 0.0F, 0.0F);
        player.setSprinting(true);
        helper.getLevel().addFreshEntity(player);
        goat.push(player);
        helper.assertTrue(goat.isSpooked() && goat.getSpookedTimer() == 20,
                "sprinting-player collision did not start the legacy fainting-goat one-second state");
        helper.runAtTickTime(21, () -> {
            helper.assertFalse(goat.isSpooked(), "fainting-goat spooked state did not expire server-side");
            helper.assertTrue(goat.getSpookedTimer() == 0, "fainting-goat spooked timer did not reach zero");
            player.discard();
            goat.discard();
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 180)
    public static void buckRivalrySelectsMatchingBreedFamilyAndPersistsState(GameTestHelper helper) {
        AnimaniaGameTestEvidence.mark("animania_farm:buck_rivalry_selects_matching_family");
        AnimaniaAnimalEntity first = createAnimal(helper, "buck_alpine");
        AnimaniaAnimalEntity second = createAnimal(helper, "buck_kiko");
        first.moveTo(helper.absolutePos(new BlockPos(1, 1, 1)), 0.0F, 0.0F);
        second.moveTo(helper.absolutePos(new BlockPos(2, 1, 1)), 0.0F, 0.0F);
        first.setAge(0);
        second.setAge(0);
        helper.getLevel().addFreshEntity(first);
        helper.getLevel().addFreshEntity(second);
        AnimaniaRivalHeadbuttGoal goal = new AnimaniaRivalHeadbuttGoal(first);
        int previousDelay = AnimaniaConfig.AI_TICKS_BETWEEN_FIRINGS.get();
        AnimaniaConfig.AI_TICKS_BETWEEN_FIRINGS.set(1);
        boolean selected = false;
        for (int attempt = 0; attempt < 6000 && !selected; attempt++) selected = goal.canUse();
        AnimaniaConfig.AI_TICKS_BETWEEN_FIRINGS.set(previousDelay);
        helper.assertTrue(selected, "adult buck did not select a nearby same-family rival after the configured delay");
        helper.assertTrue(first.isFighting() && second.isFighting()
                        && second.getUUID().equals(first.getRivalUuid())
                        && first.getUUID().equals(second.getRivalUuid()),
                "buck rivalry did not synchronize both fighting flags and rival UUIDs");
        CompoundTag saved = new CompoundTag();
        first.addAdditionalSaveData(saved);
        AnimaniaAnimalEntity loaded = createAnimal(helper, "buck_alpine");
        loaded.readAdditionalSaveData(saved);
        helper.assertTrue(loaded.isFighting() && second.getUUID().equals(loaded.getRivalUuid()),
                "buck rivalry state lost its target UUID during NBT round-trip");
        goal.stop();
        helper.assertFalse(first.isFighting() || second.isFighting(),
                "stopping one rivalry goal left the reciprocal rival fighting");
        first.discard();
        second.discard();
        loaded.discard();
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 180)
    public static void ramRivalrySelectsMatchingBreedFamily(GameTestHelper helper) {
        AnimaniaGameTestEvidence.mark("animania_farm:ram_rivalry_selects_matching_family");
        AnimaniaAnimalEntity first = createAnimal(helper, "ram_dorper");
        AnimaniaAnimalEntity second = createAnimal(helper, "ram_merino");
        first.moveTo(helper.absolutePos(new BlockPos(1, 1, 1)), 0.0F, 0.0F);
        second.moveTo(helper.absolutePos(new BlockPos(2, 1, 1)), 0.0F, 0.0F);
        first.setAge(0);
        second.setAge(0);
        helper.getLevel().addFreshEntity(first);
        helper.getLevel().addFreshEntity(second);
        AnimaniaRivalHeadbuttGoal goal = new AnimaniaRivalHeadbuttGoal(first);
        int previousDelay = AnimaniaConfig.AI_TICKS_BETWEEN_FIRINGS.get();
        AnimaniaConfig.AI_TICKS_BETWEEN_FIRINGS.set(1);
        boolean selected = false;
        for (int attempt = 0; attempt < 6000 && !selected; attempt++) selected = goal.canUse();
        AnimaniaConfig.AI_TICKS_BETWEEN_FIRINGS.set(previousDelay);
        helper.assertTrue(selected && first.isFighting() && second.isFighting()
                        && second.getUUID().equals(first.getRivalUuid())
                        && first.getUUID().equals(second.getRivalUuid()),
                "adult ram did not select a nearby same-family rival");
        goal.stop();
        first.discard();
        second.discard();
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void nativeRecipeParsingAndCarvingKnifeRemainder(GameTestHelper helper) {
        ResourceLocation cuttingId = new ResourceLocation(AnimaniaFarm.MOD_ID, "beef_cutting_1");
        var cutting = helper.getLevel().getRecipeManager().byKey(cuttingId);
        helper.assertTrue(cutting.isPresent(), "vanilla parser did not load the migrated shapeless cutting recipe");
        helper.assertTrue(cutting.orElseThrow().getSerializer()
                        == net.minecraft.world.item.crafting.RecipeSerializer.SHAPELESS_RECIPE,
                "cutting recipe did not use the native shapeless serializer");

        FarmCarvingKnifeItem knife = (FarmCarvingKnifeItem) FarmContent.ITEM_ENTRIES.get("carving_knife").get();
        ItemStack fresh = new ItemStack(knife);
        ItemStack firstRemainder = knife.getCraftingRemainingItem(fresh);
        helper.assertTrue(firstRemainder.is(knife) && firstRemainder.getDamageValue() == 1,
                "carving knife was not returned with exactly one durability consumed");
        ItemStack lastUse = new ItemStack(knife);
        lastUse.setDamageValue(lastUse.getMaxDamage() - 1);
        helper.assertTrue(knife.getCraftingRemainingItem(lastUse).isEmpty(),
                "carving knife survived beyond its final durability use");
        helper.assertTrue(fresh.getDamageValue() == 0,
                "crafting remainder mutated the input stack instead of returning a copy");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void dispenserSpawnsNamedAnimaniaEggAndConsumesExactlyOne(GameTestHelper helper) {
        BlockPos dispenserPos = helper.absolutePos(new BlockPos(1, 2, 1));
        BlockPos target = dispenserPos.east();
        helper.getLevel().setBlock(dispenserPos,
                Blocks.DISPENSER.defaultBlockState().setValue(DispenserBlock.FACING, Direction.EAST), 3);
        DispenserBlockEntity dispenser = (DispenserBlockEntity) helper.getLevel().getBlockEntity(dispenserPos);
        ItemStack stack = new ItemStack(FarmContent.ITEM_ENTRIES.get("entity_egg_bull_angus").get(), 2);
        stack.setHoverName(net.minecraft.network.chat.Component.literal("Dispenser Angus"));
        dispenser.setItem(0, stack);
        helper.getLevel().setBlock(dispenserPos.above(), Blocks.REDSTONE_BLOCK.defaultBlockState(), 3);
        helper.runAfterDelay(8, () -> {
            var spawned = helper.getLevel().getEntitiesOfClass(AnimaniaAnimalEntity.class,
                    new AABB(target).inflate(1.0D), animal -> animal.getType() == AnimaniaFarm.ENTITIES.get("bull_angus").get());
            helper.assertTrue(spawned.size() == 1, "egg dispenser did not spawn exactly one registered Angus bull");
            helper.assertTrue(spawned.get(0).hasCustomName()
                            && "Dispenser Angus".equals(spawned.get(0).getCustomName().getString()),
                    "egg dispenser did not copy the stack custom name");
            helper.assertTrue(dispenser.getItem(0).getCount() == 1,
                    "egg dispenser did not consume exactly one egg after successful spawn");
            helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void wildHiveStingUsesLegacyDamageTypeAndAmount(GameTestHelper helper) {
        AnimaniaGameTestEvidence.mark("animania_farm:wildHiveStingUsesLegacyDamageTypeAndAmount");
        var player = helper.makeMockPlayer();
        player.setHealth(20.0F);
        helper.assertTrue(FarmHiveBlockEntity.sting(player), "wild hive sting was rejected");
        helper.assertTrue(Math.abs(player.getHealth() - 17.5F) < 0.001F,
                "wild hive sting did not deal the legacy 2.5 damage");
        helper.assertTrue("animania_bee".equals(com.animania.common.AnimaniaDamageSources.bee(helper.getLevel()).getMsgId()),
                "wild hive damage type lost its legacy death-message id");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void naturalFemaleBootstrapCreatesLinkedMaleOrChild(GameTestHelper helper) {
        AnimaniaAnimalEntity cow = createAnimal(helper, "cow_angus");
        BlockPos pos = helper.absolutePos(new BlockPos(2, 2, 2));
        cow.moveTo(pos.getX(), pos.getY(), pos.getZ(), 0.0F, 0.0F);
        helper.getLevel().addFreshEntity(cow);
        AnimaniaAnimalEntity bull = cow.trySpawnNaturalFamilyCompanion(0);
        helper.assertTrue(bull != null && bull.getType() == AnimaniaFarm.ENTITIES.get("bull_angus").get(),
                "natural cow family did not create the breed-matched bull");
        helper.assertTrue(bull != null && cow.mateUuid() != null && cow.mateUuid().equals(bull.getUUID())
                        && bull.mateUuid() != null && bull.mateUuid().equals(cow.getUUID()),
                "natural male companion was not linked symmetrically");
        AnimaniaAnimalEntity calf = cow.trySpawnNaturalFamilyCompanion(1);
        helper.assertTrue(calf != null && calf.getType() == AnimaniaFarm.ENTITIES.get("calf_angus").get(),
                "natural cow family did not create the breed-matched calf");
        helper.assertTrue(calf != null && cow.getUUID().equals(calf.parentUuid()) && calf.isBaby(),
                "natural child companion lost parent UUID or baby state");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void farmFamilySpawnLimitOnlyRejectsNaturalPopulationGrowth(GameTestHelper helper) {
        AnimaniaAnimalEntity existing = createAnimal(helper, "cow_angus");
        AnimaniaAnimalEntity candidate = createAnimal(helper, "bull_hereford");
        BlockPos pos = helper.absolutePos(new BlockPos(2, 2, 2));
        existing.moveTo(pos.getX(), pos.getY(), pos.getZ(), 0.0F, 0.0F);
        candidate.moveTo(pos.getX() + 1.0D, pos.getY(), pos.getZ(), 0.0F, 0.0F);
        helper.getLevel().addFreshEntity(existing);
        int previous = FarmConfig.SPAWN_LIMIT_COWS.get();
        FarmConfig.SPAWN_LIMIT_COWS.set(1);
        var natural = new net.minecraftforge.event.entity.living.MobSpawnEvent.PositionCheck(
                candidate, helper.getLevel(), net.minecraft.world.entity.MobSpawnType.NATURAL, null);
        AnimaniaFarm.limitNaturalFarmSpawns(natural);
        var egg = new net.minecraftforge.event.entity.living.MobSpawnEvent.PositionCheck(
                candidate, helper.getLevel(), net.minecraft.world.entity.MobSpawnType.SPAWN_EGG, null);
        AnimaniaFarm.limitNaturalFarmSpawns(egg);
        FarmConfig.SPAWN_LIMIT_COWS.set(previous);
        helper.assertTrue(natural.getResult() == net.minecraftforge.eventbus.api.Event.Result.DENY,
                "cow natural spawn ignored spawnLimitCows at the exact boundary");
        helper.assertTrue(egg.getResult() != net.minecraftforge.eventbus.api.Event.Result.DENY,
                "spawnLimitCows incorrectly blocked a spawn egg");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void legacyCareTimersResetExpireAndSurviveReload(GameTestHelper helper) {
        AnimaniaAnimalEntity cow = createAnimal(helper, "cow_angus");
        cow.setHunger(10);
        cow.setThirst(10);
        helper.assertTrue(cow.feed(new ItemStack(Items.WHEAT)), "valid feed did not reset the legacy fed timer");
        helper.assertTrue(cow.drink(new ItemStack(Items.WATER_BUCKET)), "valid drink did not reset the legacy water timer");
        int fed = cow.getFedTimer();
        int watered = cow.getWateredTimer();
        helper.assertTrue(fed >= com.animania.common.config.AnimaniaConfig.FEED_TIMER.get() && fed < com.animania.common.config.AnimaniaConfig.FEED_TIMER.get() + 100,
                "fed timer did not use feedTimer plus the legacy random offset");
        helper.assertTrue(watered >= com.animania.common.config.AnimaniaConfig.WATER_TIMER.get() && watered < com.animania.common.config.AnimaniaConfig.WATER_TIMER.get() + 100,
                "water timer did not use waterTimer plus the legacy random offset");
        CompoundTag saved = new CompoundTag();
        cow.addAdditionalSaveData(saved);
        AnimaniaAnimalEntity loaded = createAnimal(helper, "cow_angus");
        loaded.readAdditionalSaveData(saved);
        helper.assertTrue(loaded.getFedTimer() == fed && loaded.getWateredTimer() == watered,
                "care timers changed during NBT round-trip");
        saved.putInt("AnimaniaFedTimer", 1);
        saved.putInt("AnimaniaWateredTimer", 1);
        saved.putInt("AnimaniaHunger", 100);
        saved.putInt("AnimaniaThirst", 100);
        saved.putBoolean("AnimaniaInteracted", true);
        loaded.readAdditionalSaveData(saved);
        loaded.tick();
        helper.assertTrue(loaded.getHunger() == 0 && loaded.getThirst() == 0,
                "expired legacy care timers did not clear fed/watered state");
        helper.assertTrue(loaded.hasEffect(net.minecraft.world.effect.MobEffects.WEAKNESS)
                        && loaded.getEffect(net.minecraft.world.effect.MobEffects.WEAKNESS).getAmplifier() == 1,
                "fully hungry/thirsty animal did not receive legacy Weakness II");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void baseCareInteractionsRejectInvalidAndSleepingConsumption(GameTestHelper helper) {
        AnimaniaGameTestEvidence.mark("animania_farm:baseCareInteractionsRejectInvalidAndSleepingConsumption");
        AnimaniaAnimalEntity cow = createAnimal(helper, "cow_angus");
        cow.moveTo(helper.absolutePos(new BlockPos(1, 1, 1)), 0.0F, 0.0F);
        helper.getLevel().addFreshEntity(cow);
        var player = helper.makeMockPlayer();

        cow.setThirst(0);
        ItemStack poison = net.minecraft.world.item.alchemy.PotionUtils.setPotion(
                new ItemStack(Items.POTION), net.minecraft.world.item.alchemy.Potions.POISON);
        player.setItemInHand(InteractionHand.MAIN_HAND, poison);
        helper.assertTrue(cow.mobInteract(player, InteractionHand.MAIN_HAND) == net.minecraft.world.InteractionResult.PASS,
                "non-water potion was accepted as animal drinking water");
        helper.assertTrue(cow.getThirst() == 0 && poison.getCount() == 1,
                "non-water potion changed thirst or was consumed");

        ItemStack water = net.minecraft.world.item.alchemy.PotionUtils.setPotion(
                new ItemStack(Items.POTION), net.minecraft.world.item.alchemy.Potions.WATER);
        player.setItemInHand(InteractionHand.MAIN_HAND, water);
        helper.assertTrue(cow.mobInteract(player, InteractionHand.MAIN_HAND).consumesAction(),
                "water potion was rejected");
        helper.assertTrue(cow.getThirst() == 100 && player.getMainHandItem().is(Items.GLASS_BOTTLE),
                "water potion did not hydrate or return its glass bottle");

        cow.setThirst(0);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WATER_BUCKET));
        helper.assertTrue(cow.mobInteract(player, InteractionHand.MAIN_HAND).consumesAction()
                        && player.getMainHandItem().is(Items.BUCKET),
                "water bucket did not hydrate and return an empty bucket");

        cow.setSleeping(true);
        cow.setHunger(0);
        ItemStack wheat = new ItemStack(Items.WHEAT, 2);
        player.setItemInHand(InteractionHand.MAIN_HAND, wheat);
        helper.assertTrue(cow.mobInteract(player, InteractionHand.MAIN_HAND) == net.minecraft.world.InteractionResult.PASS,
                "sleeping animal accepted food");
        helper.assertTrue(cow.getHunger() == 0 && wheat.getCount() == 2,
                "sleeping animal changed hunger or consumed food");

        int interval = AnimaniaConfig.HUNGER_INTERVAL.get();
        AnimaniaAnimalEntity untouched = createAnimal(helper, "cow_angus");
        untouched.setHunger(100);
        // A direct GameTest invocation does not advance Entity#tickCount the
        // way ServerLevel's entity ticker does, so place it on the exact
        // configured boundary before exercising Animania's server tick.
        untouched.tickCount = interval;
        untouched.tick();
        helper.assertTrue(untouched.getHunger() == 100,
                "requireAnimalInteractionForAI did not protect an untouched animal's care meter");
        untouched.markInteracted();
        untouched.tickCount = interval * 2;
        untouched.tick();
        helper.assertTrue(untouched.getHunger() < 100,
                "interacted animal did not resume configured hunger decay: hunger="
                        + untouched.getHunger() + ", tickCount=" + untouched.tickCount
                        + ", interacted=" + untouched.hasInteracted());
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void baseRandomEggRosterContainsOnlySpeciesEntities(GameTestHelper helper) {
        AnimaniaGameTestEvidence.mark("animania_farm:baseRandomEggRosterContainsOnlySpeciesEntities");
        var projectile = FarmContent.BROWN_EGG_PROJECTILE.get();
        var roster = com.animania.common.AnimaniaItems.registeredAnimalTypes();
        helper.assertFalse(roster.contains(projectile), "random egg roster still contains brown_egg_projectile");
        helper.assertTrue(!roster.isEmpty(), "random egg roster is empty with all addons installed");
        for (var type : roster) {
            var created = type.create(helper.getLevel());
            helper.assertTrue(created instanceof AnimaniaAnimalEntity,
                    "random egg roster contains a non-animal type: " + ForgeRegistries.ENTITY_TYPES.getKey(type));
            if (created != null) created.discard();
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void basePregnancyRecoveryLactationAndProductionTimersMatchLegacyUnits(GameTestHelper helper) {
        AnimaniaGameTestEvidence.mark("animania_farm:basePregnancyRecoveryLactationAndProductionTimersMatchLegacyUnits");
        int configuredGestation = AnimaniaConfig.GESTATION_TICKS.get();
        AnimaniaAnimalEntity cow = createAnimal(helper, "cow_angus");
        cow.setPregnant(true);
        helper.assertTrue(cow.gestationTicks() >= configuredGestation
                        && cow.gestationTicks() < configuredGestation + 200,
                "pregnancy ignored configured duration or legacy random spread: " + cow.gestationTicks());
        int pregnancyBeforeTick = cow.pregnancyTicks();
        cow.tick();
        helper.assertTrue(cow.pregnancyTicks() == pregnancyBeforeTick + 1,
                "pregnancy counter did not advance on a server tick: " + cow.pregnancyTicks());

        CompoundTag recovery = new CompoundTag();
        cow.addAdditionalSaveData(recovery);
        recovery.putBoolean("AnimaniaPregnant", false);
        recovery.putBoolean("Fertile", false);
        recovery.putInt("AnimaniaFertilityCooldown", 1);
        recovery.putBoolean("AnimaniaMilkReady", true);
        recovery.putInt("AnimaniaLactationTicks", 1);
        cow.readAdditionalSaveData(recovery);
        cow.tick();
        helper.assertTrue(cow.isFertile() && !cow.isMilkReady(),
                "post-birth fertility/lactation timers did not expire in server ticks");

        AnimaniaAnimalEntity sheep = createAnimal(helper, "ewe_dorper");
        CompoundTag wool = new CompoundTag();
        sheep.addAdditionalSaveData(wool);
        wool.putBoolean("AnimaniaSheared", true);
        wool.putInt("AnimaniaWoolRegrowthTicks", 2);
        sheep.readAdditionalSaveData(wool);
        sheep.tick();
        helper.assertTrue(sheep.isSheared() && sheep.woolRegrowthTicks() == 1,
                "wool timer did not decrement once per server tick");
        sheep.tick();
        helper.assertFalse(sheep.isSheared(), "wool did not regrow when the tick counter reached zero");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void baseFallAndTransactionalConversionRules(GameTestHelper helper) {
        AnimaniaGameTestEvidence.mark("animania_farm:baseFallAndTransactionalConversionRules");
        helper.assertTrue(AnimaniaAnimalEntity.legacyFallDamage(10, false, 0.45D) == 10,
                "unleashed animal incorrectly received fall reduction");
        helper.assertTrue(AnimaniaAnimalEntity.legacyFallDamage(10, true, 0.45D) == 4,
                "leashed animal fall reduction did not apply to final damage");

        AnimaniaAnimalEntity source = createAnimal(helper, "cow_angus");
        source.moveTo(helper.absolutePos(new BlockPos(2, 1, 2)), 0.0F, 0.0F);
        helper.getLevel().addFreshEntity(source);
        Cow rejected = EntityType.COW.create(helper.getLevel());
        helper.assertTrue(rejected != null, "vanilla replacement could not be constructed");
        if (rejected == null) return;
        rejected.setUUID(source.getUUID());
        helper.assertFalse(com.animania.common.command.AnimaniaCommand.replaceAfterSuccessfulSpawn(
                        helper.getLevel(), source, rejected),
                "duplicate-UUID replacement unexpectedly entered the world");
        helper.assertTrue(source.isAlive(), "failed vanilla conversion deleted the source animal");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void unhappyParticleRuleHonorsCareSleepInteractionAndConfig(GameTestHelper helper) {
        AnimaniaAnimalEntity cow = createAnimal(helper, "cow_angus");
        cow.setHunger(0);
        cow.setThirst(0);
        cow.markInteracted();
        cow.setSleeping(false);
        var particleSwitch = com.animania.common.config.AnimaniaConfig.SHOW_UNHAPPY_PARTICLES;
        boolean previous = particleSwitch.get();
        particleSwitch.set(true);
        boolean visibleWhenUnhappy = cow.shouldShowUnhappyParticles();
        cow.setSleeping(true);
        boolean hiddenWhileSleeping = !cow.shouldShowUnhappyParticles();
        cow.setSleeping(false);
        particleSwitch.set(false);
        boolean hiddenByConfig = !cow.shouldShowUnhappyParticles();
        particleSwitch.set(previous);
        helper.assertTrue(visibleWhenUnhappy, "fully hungry/thirsty interacted animal did not request unhappy smoke");
        helper.assertTrue(hiddenWhileSleeping, "sleeping animal requested unhappy smoke");
        helper.assertTrue(hiddenByConfig, "showUnhappyParticles=false did not suppress unhappy smoke");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void baseNaturalSpawnSwitchGatesAddonAnimalsAtRuntime(GameTestHelper helper) {
        @SuppressWarnings("unchecked")
        EntityType<? extends net.minecraft.world.entity.animal.Animal> type =
                (EntityType<? extends net.minecraft.world.entity.animal.Animal>) (EntityType<?>) AnimaniaFarm.ENTITIES.get("cow_angus").get();
        var switchValue = com.animania.common.config.AnimaniaConfig.ENABLE_NATURAL_SPAWNS;
        boolean previous = switchValue.get();
        switchValue.set(false);
        boolean allowedWhileDisabled = AnimaniaAnimalEntity.checkAnimalSpawnRules(type, helper.getLevel(),
                net.minecraft.world.entity.MobSpawnType.NATURAL, helper.absolutePos(new BlockPos(1, 1, 1)),
                helper.getLevel().getRandom());
        switchValue.set(previous);
        helper.assertFalse(allowedWhileDisabled, "Base enableNaturalSpawns=false allowed a Farm animal spawn");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void hungryRuminantsCompleteLegacyGrassEatingCycle(GameTestHelper helper) {
        AnimaniaGameTestEvidence.mark("animania_farm:generic_ai_eat_grass");
        AnimaniaAnimalEntity cow = createAnimal(helper, "cow_angus");
        BlockPos animalPos = helper.absolutePos(new BlockPos(0, 1, 0));
        BlockPos grassPos = helper.absolutePos(new BlockPos(0, 0, 0));
        cow.moveTo(animalPos.getX() + 0.5D, animalPos.getY(), animalPos.getZ() + 0.5D, 0.0F, 0.0F);
        cow.setHunger(10);
        helper.getLevel().setBlock(grassPos, net.minecraft.world.level.block.Blocks.GRASS_BLOCK.defaultBlockState(), 3);
        helper.getLevel().addFreshEntity(cow);
        cow.goalSelector.removeAllGoals(ignored -> true);
        cow.targetSelector.removeAllGoals(ignored -> true);
        AnimaniaEatGrassGoal goal = new AnimaniaEatGrassGoal(cow, grassPos::equals);
        boolean selected = goal.consumesGrass() && goal.findTargetNow() && grassPos.equals(goal.target());
        if (!selected) cow.discard();
        helper.assertTrue(selected, "hungry cow did not select the isolated reachable grass fixture; target=" + goal.target()
                + ", state=" + helper.getLevel().getBlockState(grassPos)
                + ", path=" + cow.registryPath() + ", consumes=" + goal.consumesGrass()
                + ", blockPos=" + cow.blockPosition() + ", grassPos=" + grassPos);
        if (!selected) return;
        goal.start();
        cow.moveTo(grassPos.getX() + 0.5D, grassPos.getY() + 1.0D, grassPos.getZ() + 0.5D, 0.0F, 0.0F);
        goal.tick();
        helper.assertTrue(cow.getEatingTicks() == 160, "grass arrival did not start the legacy 160-tick animation");
        for (int tick = 0; tick < 156; tick++) {
            cow.setEatingTicks(cow.getEatingTicks() - 1);
            goal.tick();
        }
        helper.assertTrue(cow.getHunger() == 100, "grass consumption did not restore the fed state");
        helper.assertTrue(helper.getLevel().getBlockState(grassPos).is(net.minecraft.world.level.block.Blocks.DIRT),
                "grass was not replaced with dirt at the legacy animation frame");
        cow.discard();
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void legacyPanicWanderAndLookGatesRemainSpeciesSpecific(GameTestHelper helper) {
        AnimaniaGameTestEvidence.mark("animania_farm:generic_ai_panic");
        AnimaniaGameTestEvidence.mark("animania_farm:generic_ai_wander_avoid_water");
        AnimaniaGameTestEvidence.mark("animania_farm:generic_ai_look_idle");
        AnimaniaGameTestEvidence.mark("animania_farm:generic_ai_watch_closest");
        AnimaniaAnimalEntity cow = createAnimal(helper, "cow_angus");
        AnimaniaAnimalEntity pig = createAnimal(helper, "sow_duroc");
        cow.moveTo(helper.absolutePos(new BlockPos(0, 1, 0)).getCenter());
        pig.moveTo(helper.absolutePos(new BlockPos(2, 1, 0)).getCenter());
        helper.getLevel().addFreshEntity(cow);
        helper.getLevel().addFreshEntity(pig);

        AnimaniaPanicGoal cowPanic = new AnimaniaPanicGoal(cow, AnimaniaPanicGoal.legacySpeed(cow));
        AnimaniaPanicGoal pigPanic = new AnimaniaPanicGoal(pig, AnimaniaPanicGoal.legacySpeed(pig));
        helper.assertTrue(cowPanic.legacySpeed() == 2.0D && pigPanic.legacySpeed() == 1.5D,
                "farm panic speeds differ from the 1.12 family values");
        cow.setLastHurtByMob(pig);
        helper.assertFalse(cowPanic.legacyGateAllows(), "attacked legacy cow incorrectly entered panic");
        helper.assertTrue(pigPanic.legacyGateAllows(), "non-cow panic was blocked by the cow-only exception");

        cow.setLastHurtByMob(null);
        cow.setSleeping(true);
        cow.setSecondsOnFire(2);
        cowPanic.canUse();
        helper.assertFalse(cow.isSleeping(), "burning animal was not awakened before panic evaluation");

        AnimaniaWanderAvoidWaterGoal wander = new AnimaniaWanderAvoidWaterGoal(pig,
                AnimaniaWanderAvoidWaterGoal.legacySpeed(pig));
        pig.setSleeping(true);
        helper.assertFalse(wander.legacyGateAllows(), "sleeping animal was allowed to start generic wandering");
        pig.setSleeping(false);
        helper.assertTrue(wander.legacyGateAllows(), "awake animal was blocked from generic wandering");

        helper.getLevel().setBlock(pig.blockPosition().below(), AnimaniaBlocks.MUD.get().defaultBlockState(), 3);
        AnimaniaLookIdleGoal idle = new AnimaniaLookIdleGoal(pig);
        AnimaniaWatchClosestGoal watch = new AnimaniaWatchClosestGoal(pig);
        helper.assertFalse(idle.legacyGateAllows() || watch.legacyGateAllows(),
                "pig standing on legacy mud was allowed to look/watch");
        helper.getLevel().setBlock(pig.blockPosition().below(), net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), 3);
        helper.assertTrue(idle.legacyGateAllows() && watch.legacyGateAllows(),
                "pig away from mud remained blocked from look/watch");

        cow.clearFire();
        cow.discard();
        pig.discard();
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void horseGoalsRespectDayRiderAndPullingGates(GameTestHelper helper) {
        AnimaniaGameTestEvidence.mark("animania_farm:horseGoalsRespectDayRiderAndPullingGates");
        ServerLevel level = (ServerLevel) helper.getLevel();
        long previousTime = level.getDayTime();
        for (int x = 2; x <= 5; x++) {
            helper.setBlock(new BlockPos(x, 0, 2), Blocks.GRASS_BLOCK);
            helper.setBlock(new BlockPos(x, 1, 2), Blocks.AIR);
            helper.setBlock(new BlockPos(x, 2, 2), Blocks.AIR);
        }
        AnimaniaAnimalEntity mare = createAnimal(helper, "mare_draft");
        mare.moveTo(helper.absolutePos(new BlockPos(2, 1, 2)).getCenter());
        mare.setAge(0);
        mare.setGender(AnimalGender.FEMALE);
        mare.setSaddled(true);
        level.addFreshEntity(mare);
        AnimaniaAnimalEntity stallion = createAnimal(helper, "stallion_draft");
        stallion.moveTo(helper.absolutePos(new BlockPos(5, 1, 2)).getCenter());
        stallion.setAge(0);
        stallion.setGender(AnimalGender.MALE);
        level.addFreshEntity(stallion);
        mare.goalSelector.removeAllGoals(ignored -> true);
        stallion.goalSelector.removeAllGoals(ignored -> true);
        mare.setHunger(100); mare.setThirst(100);
        stallion.setHunger(100); stallion.setThirst(100);
        helper.assertTrue(mare.feed(farmFoodFor("mare_draft"))
                        && stallion.feed(farmFoodFor("stallion_draft")),
                "draft pair rejected configured horse food");
        stallion.setMateUuid(mare.getUUID());
        AnimaniaWanderAvoidWaterGoal wander = new AnimaniaWanderAvoidWaterGoal(mare,
                AnimaniaWanderAvoidWaterGoal.legacySpeed(mare));
        AnimaniaLookIdleGoal look = new AnimaniaLookIdleGoal(mare);
        BlockPos grassRelative = new BlockPos(2, 0, 3);
        BlockPos grassAbsolute = helper.absolutePos(grassRelative);
        helper.setBlock(grassRelative, Blocks.GRASS_BLOCK);
        AnimaniaEatGrassGoal grass = new AnimaniaEatGrassGoal(mare, grassAbsolute::equals);
        AnimaniaMateGoal mate = new AnimaniaMateGoal(stallion, 1.0D, () -> true);
        var rider = helper.makeMockPlayer();
        level.addFreshEntity(rider);
        AnimaniaVehicleEntity cart = (AnimaniaVehicleEntity) AnimaniaFarm.ENTITIES.get("cart").get().create(level);
        try {
            level.setDayTime(1000L);
            helper.assertTrue(wander.legacyGateAllows() && look.legacyGateAllows()
                            && grass.legacyMountGateAllows() && mate.legacyTimeGateAllows(),
                    "idle daylight horse was blocked by a legacy horse-goal gate");
            helper.assertTrue(grass.findTargetNow(), "draft mare did not find adjacent grass");
            grass.start();
            grass.tick();
            helper.assertTrue(mare.getEatingTicks() == 160,
                    "draft mare did not start the legacy 160-tick chewing animation");
            mare.setEatingTicks(4);
            grass.tick();
            helper.assertBlockPresent(Blocks.DIRT, grassRelative);

            boolean selected = false;
            for (int attempt = 0; attempt < 300 && !selected; attempt++) selected = mate.canUse();
            helper.assertTrue(selected && mate.targetMate() == mare,
                    "draft stallion did not select and follow its reserved mare");
            mate.start();
            helper.assertTrue(mate.targetMate() == mare,
                    "draft stallion lost its selected mare when follow began");
            mate.stop();
            level.setDayTime(14000L);
            helper.assertFalse(wander.legacyGateAllows() || look.legacyGateAllows() || mate.legacyTimeGateAllows(),
                    "horse wander/look/mate goals ignored their legacy daylight gate");

            level.setDayTime(1000L);
            cart.moveTo(mare.getX(), mare.getY(), mare.getZ() + 2.0D);
            level.addFreshEntity(cart);
            helper.assertTrue(cart.tryAttachPuller(mare), "draft mare could not attach for horse-goal test");
            helper.assertFalse(wander.legacyGateAllows() || grass.legacyMountGateAllows(),
                    "pulling horse was allowed to wander or stop to eat grass");
            cart.detachPuller();
            helper.assertTrue(rider.startRiding(mare, true), "saddled mare rejected rider in horse-goal test");
            helper.assertFalse(grass.legacyMountGateAllows(),
                    "ridden horse was allowed to start the legacy grass-eating goal");
            rider.stopRiding();
        } finally {
            level.setDayTime(previousTime);
            rider.discard();
            cart.discard();
            stallion.discard();
            mare.discard();
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void farmAnimalsSeekConfiguredBedsAndWakeByDay(GameTestHelper helper) {
        AnimaniaGameTestEvidence.mark("animania_farm:generic_ai_sleep");
        ServerLevel level = (ServerLevel) helper.getLevel();
        var cowBedConfig = FarmConfig.BED_BLOCKS.get("cowBed");
        String previousCowBed = cowBedConfig.get();
        cowBedConfig.set("animania:straw");
        level.setWeatherParameters(6000, 0, false, false);
        level.setDayTime(14000L);
        BlockPos bed = new BlockPos(0, 0, 0);
        helper.setBlock(bed, AnimaniaBlocks.STRAW.get());
        AnimaniaAnimalEntity cow = createAnimal(helper, "cow_angus");
        cow.moveTo(helper.absolutePos(bed.above()).getX() + 0.5D,
                helper.absolutePos(bed.above()).getY(), helper.absolutePos(bed.above()).getZ() + 0.5D, 0.0F, 0.0F);
        level.addFreshEntity(cow);
        AnimaniaSleepGoal sleep = new AnimaniaSleepGoal(cow);
        boolean foundBed = false;
        for (int attempt = 0; attempt < 2000 && !foundBed; attempt++) foundBed = sleep.canUse();
        helper.assertTrue(foundBed, "cow did not find its configured legacy straw bed at night");
        if (!foundBed) return;
        sleep.start();
        sleep.tick();
        helper.assertTrue(cow.isSleeping(), "cow reached straw but did not enter synchronized sleep state");
        level.setDayTime(1000L);
        helper.assertFalse(sleep.canUse(), "cow tried to continue sleeping outside its legacy night schedule");
        helper.assertFalse(cow.isSleeping(), "cow did not wake when its legacy night schedule ended");
        cowBedConfig.set(previousCowBed);
        cow.discard();
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void advancementsRequireMatchingGameplayTrigger(GameTestHelper helper) {
        ServerLevel level = (ServerLevel) helper.getLevel();
        var player = new ServerPlayer(level.getServer(), level,
                new GameProfile(UUID.randomUUID(), "AnimaniaAdvancementTest"));
        var manager = level.getServer().getAdvancements();
        // The GameTest server has no network clients. Reloading mirrors the
        // advancement listener registration in PlayerList's login path.
        player.getAdvancements().reload(manager);
        var root = manager.getAdvancement(new ResourceLocation(AnimaniaFarm.MOD_ID, "animania/root"));
        var angus = manager.getAdvancement(new ResourceLocation(AnimaniaFarm.MOD_ID, "animania/feed_cow_angus"));
        var hereford = manager.getAdvancement(new ResourceLocation(AnimaniaFarm.MOD_ID, "animania/feed_cow_hereford"));
        helper.assertTrue(root != null && angus != null && hereford != null,
                "farm advancement tree did not load");
        if (root == null || angus == null || hereford == null) return;
        helper.assertTrue((Object) CriteriaTriggers.getCriterion(FeedAnimalTrigger.ID) == FeedAnimalTrigger.INSTANCE,
                "loaded feed trigger is not the registered Animania trigger instance");
        var bullCriterion = angus.getCriteria().get("angus_bull");
        helper.assertTrue(bullCriterion != null && bullCriterion.getTrigger() instanceof FeedAnimalTrigger.Instance,
                "Angus bull criterion did not deserialize as FeedAnimalTrigger.Instance");
        if (bullCriterion == null || !(bullCriterion.getTrigger() instanceof FeedAnimalTrigger.Instance instance)) return;
        helper.assertTrue(instance.matches(new ItemStack(Items.WHEAT),
                        new ResourceLocation(AnimaniaFarm.MOD_ID, "bull_angus")),
                "deserialized Angus criterion rejects its exact entity and food");
        helper.assertTrue(instance.matchesPlayer(player),
                "deserialized Angus criterion rejects the joining player predicate");

        helper.assertFalse(player.getAdvancements().getOrStartProgress(root).isDone(),
                "Animania root was granted when a new player joined");
        helper.assertFalse(player.getAdvancements().getOrStartProgress(angus).isDone(),
                "Angus feeding advancement was granted before gameplay");
        helper.assertFalse(player.getAdvancements().getOrStartProgress(hereford).isDone(),
                "Hereford feeding advancement was granted before gameplay");

        helper.assertFalse(FeedAnimalTrigger.INSTANCE.trigger(player, new ItemStack(Items.CARROT),
                new ResourceLocation(AnimaniaFarm.MOD_ID, "bull_angus")),
                "wrong food matched a feed listener");
        helper.assertFalse(player.getAdvancements().getOrStartProgress(angus).isDone(),
                "wrong food granted the Angus feeding advancement");
        helper.assertFalse(FeedAnimalTrigger.INSTANCE.trigger(player, new ItemStack(Items.WHEAT),
                new ResourceLocation("minecraft", "cow")),
                "unregistered vanilla cow matched an Animania feed listener");
        helper.assertFalse(player.getAdvancements().getOrStartProgress(angus).isDone(),
                "wrong breed granted the Angus feeding advancement");

        helper.assertTrue(FeedAnimalTrigger.INSTANCE.trigger(player, new ItemStack(Items.WHEAT),
                new ResourceLocation(AnimaniaFarm.MOD_ID, "bull_angus")),
                "matching Angus feed action found no advancement listener");
        var angusProgress = player.getAdvancements().getOrStartProgress(angus);
        helper.assertTrue(angusProgress.isDone(),
                "matching Angus feed action did not grant its advancement: " + angusProgress);
        helper.assertFalse(FeedAnimalTrigger.INSTANCE.trigger(player, new ItemStack(Items.WHEAT),
                        new ResourceLocation(AnimaniaFarm.MOD_ID, "bull_angus")),
                "completed Angus advancement left a stale feed listener attached");
        helper.assertFalse(player.getAdvancements().getOrStartProgress(hereford).isDone(),
                "matching Angus feed action granted an unrelated advancement");
        helper.assertFalse(player.getAdvancements().getOrStartProgress(root).isDone(),
                "feeding an animal unexpectedly auto-granted the impossible root");
        player.getAdvancements().stopListening();
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void optionalFeedCriterionMatchesOnlyItsInstalledAddonItem(GameTestHelper helper) {
        ResourceLocation animal = new ResourceLocation(AnimaniaFarm.MOD_ID, "bull_angus");
        ResourceLocation brownEgg = new ResourceLocation(AnimaniaFarm.MOD_ID, "brown_egg");
        var criterion = FeedAnimalTrigger.Instance.optional(animal, brownEgg);
        helper.assertTrue(criterion.isOptional(), "optional feed criterion lost its optional marker");
        helper.assertTrue(criterion.matches(new ItemStack(FarmContent.ITEM_ENTRIES.get("brown_egg").get()), animal),
                "installed optional Farm food did not match its exact criterion");
        helper.assertFalse(criterion.matches(new ItemStack(Items.WHEAT), animal),
                "optional criterion accepted the wrong installed item");
        helper.assertFalse(criterion.matches(new ItemStack(FarmContent.ITEM_ENTRIES.get("brown_egg").get()),
                        new ResourceLocation(AnimaniaFarm.MOD_ID, "bull_hereford")),
                "optional criterion accepted the wrong animal type");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void allFarmEntitiesHaveRegistryObjects(GameTestHelper helper) {
        helper.assertTrue(AnimaniaFarm.ENTITIES.size() == FarmLegacyIds.ALL.size(),
                "farm registry count differs from the source-derived legacy ID ledger");
        for (String id : FarmLegacyIds.ALL) {
            helper.assertTrue(AnimaniaFarm.ENTITIES.containsKey(id), "missing farm registry object: " + id);
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void everyFarmSoundEventIsRegistered(GameTestHelper helper) {
        AnimaniaGameTestEvidence.mark("animania_farm:everyFarmSoundEventIsRegistered");
        helper.assertTrue(FarmSounds.ALL.size() == 96, "Farm legacy sound ledger count changed");
        for (String id : FarmSounds.ALL.keySet()) {
            helper.assertTrue(ForgeRegistries.SOUND_EVENTS.containsKey(
                    new ResourceLocation(AnimaniaFarm.MOD_ID, id)),
                    "missing Farm sound registration: " + id);
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void legacyOreDictionaryMembershipUsesModernTags(GameTestHelper helper) {
        assertTagged(helper, "brown_egg", "animania", "legacy_oredict/egg");
        assertTagged(helper, "salt", "animania", "legacy_oredict/dustsalt");
        assertTagged(helper, "animania_wool", "minecraft", "wool");
        assertTagged(helper, "raw_prime_mutton", "forge", "raw_meats");
        assertTagged(helper, "cooked_prime_mutton", "forge", "cooked_meats");
        assertTagged(helper, "friesian_cheese_wedge", "forge", "foods/cheese");
        assertTagged(helper, "honey_jar", "forge", "foods/honey");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void everyProgrammaticLegacySmeltingRecipeLoadsWithExactValues(GameTestHelper helper) {
        assertSmelting(helper, "raw_prime_beef_smelting", FarmContent.ITEM_ENTRIES.get("raw_prime_beef").get(), FarmContent.ITEM_ENTRIES.get("cooked_prime_beef").get());
        assertSmelting(helper, "raw_prime_steak_smelting", FarmContent.ITEM_ENTRIES.get("raw_prime_steak").get(), FarmContent.ITEM_ENTRIES.get("cooked_prime_steak").get());
        assertSmelting(helper, "raw_prime_pork_smelting", FarmContent.ITEM_ENTRIES.get("raw_prime_pork").get(), FarmContent.ITEM_ENTRIES.get("cooked_prime_pork").get());
        assertSmelting(helper, "raw_prime_bacon_smelting", FarmContent.ITEM_ENTRIES.get("raw_prime_bacon").get(), FarmContent.ITEM_ENTRIES.get("cooked_prime_bacon").get());
        assertSmelting(helper, "raw_prime_chicken_smelting", FarmContent.ITEM_ENTRIES.get("raw_prime_chicken").get(), FarmContent.ITEM_ENTRIES.get("cooked_prime_chicken").get());
        assertSmelting(helper, "egg_smelting", Items.EGG, FarmContent.ITEM_ENTRIES.get("plain_omelette").get());
        assertSmelting(helper, "brown_egg_smelting", FarmContent.ITEM_ENTRIES.get("brown_egg").get(), FarmContent.ITEM_ENTRIES.get("plain_omelette").get());
        assertSmelting(helper, "raw_prime_mutton_smelting", FarmContent.ITEM_ENTRIES.get("raw_prime_mutton").get(), FarmContent.ITEM_ENTRIES.get("cooked_prime_mutton").get());
        assertSmelting(helper, "raw_chevon_smelting", FarmContent.ITEM_ENTRIES.get("raw_chevon").get(), FarmContent.ITEM_ENTRIES.get("cooked_chevon").get());
        assertSmelting(helper, "raw_prime_chevon_smelting", FarmContent.ITEM_ENTRIES.get("raw_prime_chevon").get(), FarmContent.ITEM_ENTRIES.get("cooked_prime_chevon").get());
        assertSmelting(helper, "raw_horse_smelting", FarmContent.ITEM_ENTRIES.get("raw_horse").get(), FarmContent.ITEM_ENTRIES.get("cooked_horse").get());
        helper.succeed();
    }

    private static void assertSmelting(GameTestHelper helper, String recipeId, net.minecraft.world.level.ItemLike input,
                                       net.minecraft.world.level.ItemLike output) {
        var found = helper.getLevel().getRecipeManager().byKey(
                new net.minecraft.resources.ResourceLocation(AnimaniaFarm.MOD_ID, recipeId)).orElse(null);
        helper.assertTrue(found instanceof net.minecraft.world.item.crafting.AbstractCookingRecipe,
                "missing legacy smelting recipe " + recipeId);
        if (!(found instanceof net.minecraft.world.item.crafting.AbstractCookingRecipe recipe)) return;
        var container = new net.minecraft.world.SimpleContainer(new ItemStack(input));
        helper.assertTrue(recipe.matches(container, helper.getLevel()), recipeId + " rejects its legacy input");
        helper.assertTrue(recipe.getResultItem(helper.getLevel().registryAccess()).is(output.asItem()), recipeId + " has the wrong output");
        helper.assertTrue(Math.abs(recipe.getExperience() - 0.3F) < 0.0001F && recipe.getCookingTime() == 200,
                recipeId + " changed legacy experience/cooking time");
    }

    private static void assertTagged(GameTestHelper helper, String itemId, String namespace, String path) {
        var item = FarmContent.ITEM_ENTRIES.get(itemId);
        helper.assertTrue(item != null && item.get().builtInRegistryHolder().is(net.minecraft.tags.TagKey.create(
                        net.minecraft.core.registries.Registries.ITEM,
                        new net.minecraft.resources.ResourceLocation(namespace, path))),
                itemId + " missing modern tag " + namespace + ":" + path);
    }

    @GameTest(template = "empty")
    public static void everyFarmAnimalConstructsAndPersistsCareState(GameTestHelper helper) {
        AnimaniaGameTestEvidence.mark("animania_farm:all_legacy_animals_construct_persist");
        for (String id : FarmLegacyIds.ALL) {
            if (FarmLegacyIds.isVehicle(id)) continue;
            EntityType<?> type = AnimaniaFarm.ENTITIES.get(id).get();
            var created = type.create(helper.getLevel());
            helper.assertTrue(created instanceof AnimaniaAnimalEntity,
                    "farm animal did not construct as AnimaniaAnimalEntity: " + id);
            if (!(created instanceof AnimaniaAnimalEntity animal)) return;
            helper.assertTrue(validFarmVariant(id, animal.getVariantName()),
                    "farm animal initialized an invalid visual variant: " + id + "=" + animal.getVariantName());

            animal.setAge(0);
            animal.setHunger(7);
            animal.setThirst(9);
            helper.assertTrue(animal.feed(farmFoodFor(id)), "farm animal rejected configured food: " + id);
            helper.assertTrue(animal.getHunger() == 27, "farm animal hunger did not increase by 20: " + id);
            helper.assertFalse(animal.feed(new ItemStack(Items.DIAMOND)),
                    "farm animal accepted an unrelated item as food: " + id);
            helper.assertTrue(animal.drink(new ItemStack(Items.WATER_BUCKET)),
                    "farm animal rejected water: " + id);

            String variant = "roundtrip_" + id;
            animal.setVariantName(variant);
            CompoundTag tag = new CompoundTag();
            animal.addAdditionalSaveData(tag);
            animal.setVariantName("mutated");
            animal.setHunger(1);
            animal.setThirst(1);
            animal.readAdditionalSaveData(tag);
            helper.assertTrue(variant.equals(animal.getVariantName()), "farm animal lost variant NBT: " + id);
            helper.assertTrue(animal.getHunger() == 27 && animal.getThirst() == 100,
                    "farm animal lost care-state NBT: " + id);
            animal.discard();
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void everyFarmBreedResolvesItsLegacyChildType(GameTestHelper helper) {
        for (String childId : FarmLegacyIds.ALL) {
            String[] adults = farmAdultsForChild(childId);
            if (adults == null) continue;
            AnimaniaGameTestEvidence.mark("animania_farm:breed_child:" + childId);
            AnimaniaAnimalEntity female = createAnimal(helper, adults[0]);
            AnimaniaAnimalEntity male = createAnimal(helper, adults[1]);
            female.setAge(0);
            male.setAge(0);
            female.setGender(AnimalGender.FEMALE);
            male.setGender(AnimalGender.MALE);
            helper.assertTrue(female.feed(farmFoodFor(adults[0])) && male.feed(farmFoodFor(adults[1])),
                    "farm breeding pair rejected configured food: " + childId);
            helper.assertTrue(female.canBreedWith(male), "farm pair did not recognize matching breed: " + childId);
            AgeableMob child = female.getBreedOffspring((ServerLevel) helper.getLevel(), male);
            helper.assertTrue(child != null && child.getType() == AnimaniaFarm.ENTITIES.get(childId).get(),
                    "farm pair resolved the wrong child registry type: " + childId);
            female.spawnChildFromBreeding((ServerLevel) helper.getLevel(), male);
            helper.assertTrue(female.isPregnant(), "farm female did not enter pregnancy: " + childId);
            female.discard();
            male.discard();
            if (child != null) child.discard();
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void everyFarmChildRegistryTypeGrowsIntoItsBreedAdult(GameTestHelper helper) {
        AnimaniaGameTestEvidence.mark("animania_farm:everyFarmChildRegistryTypeGrowsIntoItsBreedAdult");
        BlockPos spawn = helper.absolutePos(new BlockPos(2, 1, 2));
        int previousInterval = AnimaniaConfig.CHILD_GROWTH_TICK.get();
        try {
            AnimaniaConfig.CHILD_GROWTH_TICK.set(20);
            for (String childId : FarmLegacyIds.ALL) {
                String[] adults = farmAdultsForChild(childId);
                if (adults == null) continue;
                AnimaniaAnimalEntity child = createAnimal(helper, childId);
                child.moveTo(spawn.getX() + 0.5D, spawn.getY(), spawn.getZ() + 0.5D, 0.0F, 0.0F);
                child.setAge(-20);
                child.setGender(AnimalGender.CHILD);
                child.setHunger(100);
                child.setThirst(100);
                child.setNoAi(true);
                helper.getLevel().addFreshEntity(child);
                for (int tick = 0; tick < 20; tick++) child.tick();
                var nearby = helper.getLevel().getEntitiesOfClass(AnimaniaAnimalEntity.class,
                        new AABB(spawn).inflate(1.0D), entity -> entity != child);
                helper.assertTrue(child.isRemoved() && nearby.stream().anyMatch(entity -> {
                            ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
                            return id != null && (id.getPath().equals(adults[0]) || id.getPath().equals(adults[1]));
                        }),
                        childId + " did not replace itself with its matching male/female adult at growth completion");
                nearby.forEach(net.minecraft.world.entity.Entity::discard);
                if (!child.isRemoved()) child.discard();
            }
        } finally {
            AnimaniaConfig.CHILD_GROWTH_TICK.set(previousInterval);
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void eggThrowingHonorsGlobalToggleAndOptionalExtraRodents(GameTestHelper helper) {
        AnimaniaGameTestEvidence.mark("animania_farm:eggThrowingHonorsGlobalToggleAndOptionalExtraRodents");
        boolean previous = FarmConfig.ALLOW_EGG_THROWING.get();
        var player = helper.makeMockPlayer();
        helper.getLevel().addFreshEntity(player);
        try {
            FarmConfig.ALLOW_EGG_THROWING.set(false);
            helper.assertTrue(FarmEggThrowHandler.shouldCancelEggUse(helper.getLevel(), player),
                    "disabled legacy egg throwing did not cancel vanilla/brown egg use");
            FarmConfig.ALLOW_EGG_THROWING.set(true);
            helper.assertFalse(FarmEggThrowHandler.shouldCancelEggUse(helper.getLevel(), player),
                    "enabled egg throwing was canceled when Extra rodents were absent");
            helper.assertTrue(FarmEggThrowHandler.isEggProtectingRodent(
                            new ResourceLocation("animania_extra", "ferret_white"))
                            && FarmEggThrowHandler.isEggProtectingRodent(
                            new ResourceLocation("animania_extra", "ferret_grey"))
                            && FarmEggThrowHandler.isEggProtectingRodent(
                            new ResourceLocation("animania_extra", "hedgehog")),
                    "legacy Extra rodent protection IDs were not recognized without a hard dependency");
            helper.assertFalse(FarmEggThrowHandler.isEggProtectingRodent(
                            new ResourceLocation("animania_extra", "hedgehog_albino")),
                    "egg handler broadened the old normal-hedgehog rule to unrelated variants");
        } finally {
            FarmConfig.ALLOW_EGG_THROWING.set(previous);
            player.discard();
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void farmTemptationUsesLiveSpeciesFoodRules(GameTestHelper helper) {
        AnimaniaGameTestEvidence.mark("animania_farm:generic_ai_tempt");
        AnimaniaGameTestEvidence.mark("animania_farm:farmTemptationUsesLiveSpeciesFoodRules");
        var player = helper.makeMockPlayer();
        player.moveTo(helper.absolutePos(new BlockPos(1, 1, 0)), 0.0F, 0.0F);
        helper.getLevel().addFreshEntity(player);
        AnimaniaAnimalEntity pig = createAnimal(helper, "sow_duroc");
        pig.moveTo(helper.absolutePos(new BlockPos(0, 1, 0)), 0.0F, 0.0F);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WHEAT));
        helper.assertFalse(new AnimaniaTemptGoal(pig, 1.0D).canUse(), "pig followed cow/sheep wheat food");
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.CARROT));
        AnimaniaTemptGoal goal = new AnimaniaTemptGoal(pig, 1.0D);
        helper.assertTrue(goal.canUse(), "pig ignored configured carrot food");
        goal.start(); goal.tick();
        helper.assertTrue(pig.hasInteracted(), "tempted animal did not retain the legacy interacted state");
        goal.stop();
        helper.assertTrue(goal.calmDownTicks() == 100 && !goal.canUse() && goal.calmDownTicks() == 99,
                "temptation did not apply the legacy 100-tick restart cooldown");
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.CARROT_ON_A_STICK));
        helper.assertTrue(new AnimaniaTemptGoal(pig, 1.0D).canUse(), "pig ignored legacy carrot-on-a-stick temptation");

        AnimaniaAnimalEntity cow = createAnimal(helper, "cow_angus");
        cow.moveTo(helper.absolutePos(new BlockPos(0, 1, 1)), 0.0F, 0.0F);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DANDELION));
        helper.assertTrue(new AnimaniaTemptGoal(cow, 1.0D).canUse(), "cow ignored legacy flower temptation");
        helper.assertTrue(AnimaniaTemptGoal.legacySpeed(cow) == 1.25D,
                "cow did not retain its legacy 1.25 temptation speed");
        cow.discard();
        pig.discard();
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void legacyCourtshipCreatesAndPersistsExclusiveMatePregnancy(GameTestHelper helper) {
        AnimaniaGameTestEvidence.mark("animania_farm:generic_ai_mate");
        int previousLimit = com.animania.common.config.AnimaniaConfig.ENTITY_BREEDING_LIMIT.get();
        com.animania.common.config.AnimaniaConfig.ENTITY_BREEDING_LIMIT.set(1000);
        AnimaniaAnimalEntity bull = createAnimal(helper, "bull_angus");
        AnimaniaAnimalEntity cow = createAnimal(helper, "cow_angus");
        BlockPos bullPos = helper.absolutePos(new BlockPos(0, 1, 0));
        BlockPos cowPos = helper.absolutePos(new BlockPos(4, 1, 0));
        bull.moveTo(bullPos.getX() + 0.5D, bullPos.getY(), bullPos.getZ() + 0.5D, 0.0F, 0.0F);
        cow.moveTo(cowPos.getX() + 0.5D, cowPos.getY(), cowPos.getZ() + 0.5D, 0.0F, 0.0F);
        bull.setAge(0); cow.setAge(0);
        bull.setGender(AnimalGender.MALE); cow.setGender(AnimalGender.FEMALE);
        bull.setHunger(100); bull.setThirst(100); cow.setHunger(100); cow.setThirst(100);
        helper.getLevel().addFreshEntity(bull);
        helper.getLevel().addFreshEntity(cow);
        bull.goalSelector.removeAllGoals(ignored -> true);
        bull.targetSelector.removeAllGoals(ignored -> true);
        cow.goalSelector.removeAllGoals(ignored -> true);
        cow.targetSelector.removeAllGoals(ignored -> true);
        helper.assertTrue(bull.feed(new ItemStack(Items.WHEAT)) && cow.feed(new ItemStack(Items.WHEAT)),
                "courtship pair rejected legacy hand feeding");
        // Reserving a known mate makes this test independent from entities in
        // the persistent GameTest world and verifies exclusive UUID matching.
        bull.setMateUuid(cow.getUUID());
        AnimaniaMateGoal goal = new AnimaniaMateGoal(bull, 1.0D, () -> true);
        boolean selected = false;
        for (int attempt = 0; attempt < 300 && !selected; attempt++) selected = goal.canUse();
        helper.assertTrue(selected && goal.targetMate() == cow, "male did not select its reserved eligible female");
        if (!selected) return;
        goal.start();
        bull.moveTo(cow.getX() - 1.0D, cow.getY(), cow.getZ(), 0.0F, 0.0F);
        goal.tick();
        helper.assertTrue(cow.isPregnant(), "completed courtship did not begin female pregnancy");
        helper.assertTrue(cow.getUUID().equals(bull.mateUuid()) && bull.getUUID().equals(cow.mateUuid()),
                "courtship did not establish reciprocal legacy MateUUID state");

        CompoundTag saved = new CompoundTag();
        cow.addAdditionalSaveData(saved);
        helper.assertTrue(saved.hasUUID("MateUUID"), "female MateUUID was not written with the legacy save key");
        cow.setMateUuid(null);
        cow.setPregnant(false);
        cow.readAdditionalSaveData(saved);
        helper.assertTrue(bull.getUUID().equals(cow.mateUuid()) && cow.isPregnant(),
                "female lost MateUUID or pregnancy during save reload");
        com.animania.common.config.AnimaniaConfig.ENTITY_BREEDING_LIMIT.set(previousLimit);
        bull.discard(); cow.discard();
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void recordedChildrenFollowOnlyTheirActualMotherByDay(GameTestHelper helper) {
        AnimaniaGameTestEvidence.mark("animania_farm:generic_ai_follow_parents");
        helper.getLevel().setDayTime(1000L);
        AnimaniaAnimalEntity mother = createAnimal(helper, "cow_angus");
        AnimaniaAnimalEntity unrelated = createAnimal(helper, "cow_angus");
        AnimaniaAnimalEntity calf = createAnimal(helper, "calf_angus");
        BlockPos childPos = helper.absolutePos(new BlockPos(0, 5, 0));
        BlockPos unrelatedPos = helper.absolutePos(new BlockPos(3, 5, 3));
        BlockPos motherPos = helper.absolutePos(new BlockPos(4, 5, 4));
        for (int x = 0; x <= 4; x++) {
            for (int z = 0; z <= 4; z++) {
                helper.setBlock(new BlockPos(x, 4, z), net.minecraft.world.level.block.Blocks.GRASS_BLOCK);
                helper.setBlock(new BlockPos(x, 5, z), net.minecraft.world.level.block.Blocks.AIR);
                helper.setBlock(new BlockPos(x, 6, z), net.minecraft.world.level.block.Blocks.AIR);
            }
        }
        calf.moveTo(childPos.getX() + 0.5D, childPos.getY(), childPos.getZ() + 0.5D, 0.0F, 0.0F);
        unrelated.moveTo(unrelatedPos.getX() + 0.5D, unrelatedPos.getY(), unrelatedPos.getZ() + 0.5D, 0.0F, 0.0F);
        mother.moveTo(motherPos.getX() + 0.5D, motherPos.getY(), motherPos.getZ() + 0.5D, 0.0F, 0.0F);
        mother.setGender(AnimalGender.FEMALE); unrelated.setGender(AnimalGender.FEMALE);
        calf.setGender(AnimalGender.CHILD);
        calf.setParentUuid(mother.getUUID());
        helper.getLevel().addFreshEntity(mother);
        helper.getLevel().addFreshEntity(unrelated);
        helper.getLevel().addFreshEntity(calf);
        AnimaniaFollowParentGoal goal = new AnimaniaFollowParentGoal(calf, 1.1D);
        boolean selected = false;
        for (int attempt = 0; attempt < 300 && !selected; attempt++) selected = goal.canUse();
        helper.assertTrue(selected && goal.targetParent() == mother,
                "child followed a nearby adult instead of its recorded ParentUUID");
        if (!selected) return;
        goal.start(); goal.tick();
        helper.assertTrue(goal.canContinueToUse(), "child did not retain its mother while inside the legacy follow range");
        calf.moveTo(mother.getX() - 1.0D, mother.getY(), mother.getZ() - 1.0D, 0.0F, 0.0F);
        helper.assertFalse(goal.canContinueToUse(), "child continued following after reaching its mother's three-block stop radius");
        CompoundTag saved = new CompoundTag();
        calf.addAdditionalSaveData(saved);
        helper.assertTrue(saved.hasUUID("ParentUUID"), "child ParentUUID was not written with the legacy save key");
        calf.setParentUuid(null);
        calf.readAdditionalSaveData(saved);
        helper.assertTrue(mother.getUUID().equals(calf.parentUuid()), "child lost ParentUUID during save reload");
        helper.getLevel().setDayTime(14000L);
        AnimaniaFollowParentGoal nightGoal = new AnimaniaFollowParentGoal(calf, 1.1D);
        boolean followedAtNight = false;
        for (int attempt = 0; attempt < 300 && !followedAtNight; attempt++) followedAtNight = nightGoal.canUse();
        helper.assertFalse(followedAtNight, "child attempted parent following at night");
        mother.discard(); unrelated.discard(); calf.discard();
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void liveBirthAssignsMotherUuidToEveryChild(GameTestHelper helper) {
        double previousMultiple = com.animania.common.config.AnimaniaConfig.BIRTH_MULTIPLE_CHANCE.get();
        com.animania.common.config.AnimaniaConfig.BIRTH_MULTIPLE_CHANCE.set(0.0D);
        AnimaniaAnimalEntity mother = createAnimal(helper, "cow_angus");
        BlockPos position = helper.absolutePos(new BlockPos(0, 1, 0));
        mother.moveTo(position.getX() + 0.5D, position.getY(), position.getZ() + 0.5D, 0.0F, 0.0F);
        mother.setAge(0); mother.setGender(AnimalGender.FEMALE); mother.markInteracted();
        mother.setPregnant(true);
        CompoundTag nearBirth = new CompoundTag();
        mother.addAdditionalSaveData(nearBirth);
        nearBirth.putInt("AnimaniaPregnancyTicks", mother.gestationTicks() - 1);
        mother.readAdditionalSaveData(nearBirth);
        helper.getLevel().addFreshEntity(mother);
        mother.tick();
        var children = helper.getLevel().getEntitiesOfClass(AnimaniaAnimalEntity.class,
                mother.getBoundingBox().inflate(3.0D), animal -> animal != mother && mother.getUUID().equals(animal.parentUuid()));
        helper.assertTrue(children.size() == 1, "live birth did not create exactly one child bound to its mother UUID");
        if (!children.isEmpty()) {
            CompoundTag childTag = new CompoundTag();
            children.get(0).addAdditionalSaveData(childTag);
            helper.assertTrue(childTag.hasUUID("ParentUUID") && childTag.getUUID("ParentUUID").equals(mother.getUUID()),
                    "newborn did not persist its mother's ParentUUID");
            children.get(0).discard();
        }
        com.animania.common.config.AnimaniaConfig.BIRTH_MULTIPLE_CHANCE.set(previousMultiple);
        mother.discard();
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void pigsSeekMudAndPersistPlayedMuddyState(GameTestHelper helper) {
        AnimaniaAnimalEntity pig = createAnimal(helper, "sow_duroc");
        pig.moveTo(helper.absolutePos(new BlockPos(0, 1, 0)), 0.0F, 0.0F);
        helper.getLevel().addFreshEntity(pig);
        pig.setPlaying(false);
        pig.setMuddy(false);
        helper.getLevel().setDayTime(1000L);
        BlockPos mudPos = helper.absolutePos(new BlockPos(4, 0, 0));
        helper.getLevel().setBlock(mudPos, com.animania.common.AnimaniaBlocks.MUD.get().defaultBlockState(), 3);
        AnimaniaFindMudGoal goal = new AnimaniaFindMudGoal(pig);
        boolean found = false;
        for (int attempt = 0; attempt < 500 && !found; attempt++) found = goal.canUse();
        helper.assertTrue(found && mudPos.equals(goal.targetMud()), "pig did not select the nearest legacy mud block");
        if (!found) return;
        goal.start();
        pig.moveTo(mudPos.getX() + 0.5D, mudPos.getY() + 1.0D, mudPos.getZ() + 0.5D, 0.0F, 0.0F);
        goal.tick();
        helper.assertTrue(pig.isPlaying() && pig.isMuddy(), "pig entering mud did not gain played/muddy state");
        CompoundTag saved = new CompoundTag();
        pig.addAdditionalSaveData(saved);
        helper.assertTrue(saved.getInt("AnimaniaPlayingTicks") > 0, "pig played timer was not persisted");
        pig.setPlaying(false);
        pig.setMuddy(false);
        pig.readAdditionalSaveData(saved);
        helper.assertTrue(pig.isPlaying() && pig.isMuddy(), "pig lost played/muddy state after save reload");
        pig.discard();
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void leashedAdultPigsSnuffleForestTrufflesAndEatThem(GameTestHelper helper) {
        AnimaniaAnimalEntity pig = createAnimal(helper, "sow_duroc");
        BlockPos pigPos = helper.absolutePos(new BlockPos(0, 1, 0));
        helper.getLevel().setBlock(pigPos.below(), net.minecraft.world.level.block.Blocks.GRASS_BLOCK.defaultBlockState(), 3);
        pig.moveTo(pigPos.getX() + 0.5D, pigPos.getY(), pigPos.getZ() + 0.5D, 0.0F, 0.0F);
        pig.setAge(0);
        pig.setHunger(25);
        helper.getLevel().addFreshEntity(pig);
        var player = helper.makeMockPlayer();
        player.moveTo(pig.getX() + 1.0D, pig.getY(), pig.getZ(), 0.0F, 0.0F);
        helper.getLevel().addFreshEntity(player);
        pig.setLeashedTo(player, true);
        AnimaniaPigSnuffleGoal goal = new AnimaniaPigSnuffleGoal(pig, ignored -> true);
        boolean selected = false;
        for (int attempt = 0; attempt < 1000 && !selected; attempt++) selected = goal.canUse();
        helper.assertTrue(selected, "hungry pig did not begin its legacy 1/120 snuffle attempt");
        if (!selected) return;
        goal.start();
        goal.tick();
        var truffle = FarmContent.ITEM_ENTRIES.get("truffle").get();
        var drops = helper.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
                pig.getBoundingBox().inflate(3.0D), item -> item.getItem().is(truffle));
        helper.assertTrue(drops.size() == 1 && drops.get(0).getItem().getCount() >= 1
                        && drops.get(0).getItem().getCount() <= 2,
                "leashed adult forest pig did not unearth one or two truffles");
        for (int tick = 0; tick < 61; tick++) goal.tick();
        helper.assertTrue(pig.getHunger() == 100, "pig did not eat its nearby truffle and become fed");
        helper.assertTrue(helper.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
                pig.getBoundingBox().inflate(3.0D), item -> item.getItem().is(truffle)).isEmpty(),
                "consumed truffle item entity remained in the world");
        goal.stop();
        helper.assertTrue(pig.getEatingTicks() == 0, "snuffle animation timer did not reset");
        pig.discard();
        player.discard();
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void animalsPathToAndConsumeTroughWaterAndFood(GameTestHelper helper) {
        AnimaniaGameTestEvidence.mark("animania_farm:generic_ai_find_food");
        AnimaniaAnimalEntity cow = createAnimal(helper, "cow_angus");
        BlockPos cowPos = helper.absolutePos(new BlockPos(0, 1, 0));
        BlockPos troughPos = helper.absolutePos(new BlockPos(4, 1, 0));
        cow.moveTo(cowPos.getX() + 0.5D, cowPos.getY(), cowPos.getZ() + 0.5D, 0.0F, 0.0F);
        cow.setAge(0);
        cow.markInteracted();
        cow.setThirst(0);
        cow.setHunger(100);
        helper.getLevel().addFreshEntity(cow);
        helper.getLevel().setBlock(troughPos, com.animania.common.AnimaniaBlocks.TROUGH.get().defaultBlockState(), 3);
        var storage = (com.animania.common.block.AnimaniaStorageBlockEntity) helper.getLevel().getBlockEntity(troughPos);
        helper.assertTrue(storage != null, "trough block entity missing");
        if (storage == null) return;
        var fluidHandler = storage.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.FLUID_HANDLER).resolve().orElseThrow();
        helper.assertTrue(fluidHandler.fill(new net.minecraftforge.fluids.FluidStack(net.minecraft.world.level.material.Fluids.WATER, 1000),
                net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE) == 1000, "trough rejected water capability input");
        AnimaniaFindWaterGoal waterGoal = new AnimaniaFindWaterGoal(cow);
        boolean foundWater = false;
        for (int attempt = 0; attempt < 500 && !foundWater; attempt++) foundWater = waterGoal.canUse();
        helper.assertTrue(foundWater && waterGoal.targetsTrough() && troughPos.equals(waterGoal.target()),
                "thirsty cow did not select the filled trough");
        if (!foundWater) return;
        waterGoal.start();
        cow.moveTo(troughPos.getX() + 1.0D, troughPos.getY(), troughPos.getZ() + 0.5D, 0.0F, 0.0F);
        waterGoal.tick();
        helper.assertTrue(cow.getThirst() == 100, "cow did not become watered at the trough");
        helper.assertTrue(storage.fluidAmount(stack -> stack.getFluid().is(net.minecraft.tags.FluidTags.WATER)) == 900,
                "cow did not drain exactly 100 mB from the trough");

        storage.setItem(0, new ItemStack(Items.WHEAT, 3));
        cow.moveTo(cowPos.getX() + 0.5D, cowPos.getY(), cowPos.getZ() + 0.5D, 0.0F, 0.0F);
        cow.setHunger(0);
        AnimaniaFindFoodGoal foodGoal = new AnimaniaFindFoodGoal(cow);
        boolean foundFood = false;
        for (int attempt = 0; attempt < 500 && !foundFood; attempt++) foundFood = foodGoal.canUse();
        helper.assertTrue(foundFood && troughPos.equals(foodGoal.target()), "hungry cow did not select trough wheat");
        if (!foundFood) return;
        foodGoal.start();
        cow.moveTo(troughPos.getX() + 1.0D, troughPos.getY(), troughPos.getZ() + 0.5D, 0.0F, 0.0F);
        foodGoal.tick();
        helper.assertTrue(cow.getHunger() == 100, "trough food did not restore the fed state");
        helper.assertTrue(storage.getItem(0).getCount() == 2, "trough did not consume exactly one wheat");
        cow.discard();
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void fullSizeAndSmallAnimalsUseNaturalWaterWithLegacyAmounts(GameTestHelper helper) {
        AnimaniaGameTestEvidence.mark("animania_farm:generic_ai_find_water");
        boolean previousRemoveWater = com.animania.common.config.AnimaniaConfig.WATER_REMOVED_AFTER_DRINKING.get();
        com.animania.common.config.AnimaniaConfig.WATER_REMOVED_AFTER_DRINKING.set(true);
        AnimaniaAnimalEntity cow = createAnimal(helper, "cow_angus");
        BlockPos cowPos = helper.absolutePos(new BlockPos(0, 1, 0));
        BlockPos waterPos = helper.absolutePos(new BlockPos(3, 1, 0));
        cow.moveTo(cowPos.getX() + 0.5D, cowPos.getY(), cowPos.getZ() + 0.5D, 0.0F, 0.0F);
        cow.markInteracted(); cow.setThirst(0); cow.setHunger(100);
        helper.getLevel().addFreshEntity(cow);
        cow.goalSelector.removeAllGoals(ignored -> true);
        cow.targetSelector.removeAllGoals(ignored -> true);
        helper.getLevel().setBlock(waterPos, net.minecraft.world.level.block.Blocks.WATER.defaultBlockState(), 3);
        AnimaniaFindWaterGoal goal = new AnimaniaFindWaterGoal(cow, false, true, waterPos::equals);
        boolean found = false;
        for (int attempt = 0; attempt < 500 && !found; attempt++) found = goal.canUse();
        helper.assertTrue(found && waterPos.equals(goal.target()) && !goal.targetsTrough(), "cow did not find natural fresh water");
        if (!found) return;
        cow.moveTo(waterPos.getX() + 1.0D, waterPos.getY(), waterPos.getZ() + 0.5D, 0.0F, 0.0F);
        goal.tick();
        helper.assertTrue(cow.getThirst() == 100,
                "full-size animal did not become watered; thirst=" + cow.getThirst());
        helper.assertTrue(helper.getLevel().getBlockState(waterPos).isAir(),
                "full-size animal did not remove natural source; state=" + helper.getLevel().getBlockState(waterPos)
                        + ", fluid=" + helper.getLevel().getFluidState(waterPos)
                        + ", removeConfig=" + com.animania.common.config.AnimaniaConfig.WATER_REMOVED_AFTER_DRINKING.get());
        cow.discard();

        AnimaniaAnimalEntity chick = createAnimal(helper, "chick_leghorn");
        BlockPos chickPos = helper.absolutePos(new BlockPos(0, 1, 3));
        BlockPos smallWater = helper.absolutePos(new BlockPos(3, 1, 3));
        chick.moveTo(chickPos.getX() + 0.5D, chickPos.getY(), chickPos.getZ() + 0.5D, 0.0F, 0.0F);
        chick.markInteracted(); chick.setThirst(0); chick.setHunger(100);
        helper.getLevel().addFreshEntity(chick);
        chick.goalSelector.removeAllGoals(ignored -> true);
        chick.targetSelector.removeAllGoals(ignored -> true);
        helper.getLevel().setBlock(smallWater, net.minecraft.world.level.block.Blocks.WATER.defaultBlockState(), 3);
        AnimaniaFindWaterGoal smallGoal = new AnimaniaFindWaterGoal(chick, false, true, smallWater::equals);
        boolean smallFound = false;
        for (int attempt = 0; attempt < 500 && !smallFound; attempt++) smallFound = smallGoal.canUse();
        helper.assertTrue(smallFound && smallWater.equals(smallGoal.target()), "small animal did not find natural water");
        if (!smallFound) return;
        chick.moveTo(smallWater.getX() + 1.0D, smallWater.getY(), smallWater.getZ() + 0.5D, 0.0F, 0.0F);
        smallGoal.tick();
        helper.assertTrue(chick.getThirst() == 100 && helper.getLevel().getBlockState(smallWater).is(net.minecraft.world.level.block.Blocks.WATER),
                "small animal incorrectly removed its half-amount water source");
        chick.discard();
        com.animania.common.config.AnimaniaConfig.WATER_REMOVED_AFTER_DRINKING.set(previousRemoveWater);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void animalsDrinkWaterloggedBlocksWithoutBreakingHost(GameTestHelper helper) {
        AnimaniaGameTestEvidence.mark("animania_farm:generic_ai_drink_waterlogged_blocks");
        boolean previousRemoveWater = AnimaniaConfig.WATER_REMOVED_AFTER_DRINKING.get();
        AnimaniaConfig.WATER_REMOVED_AFTER_DRINKING.set(true);
        AnimaniaAnimalEntity cow = createAnimal(helper, "cow_angus");
        BlockPos cowPos = helper.absolutePos(new BlockPos(0, 1, 0));
        BlockPos waterloggedPos = helper.absolutePos(new BlockPos(3, 1, 0));
        cow.moveTo(cowPos.getX() + 0.5D, cowPos.getY(), cowPos.getZ() + 0.5D, 0.0F, 0.0F);
        cow.markInteracted();
        cow.setThirst(0);
        cow.setHunger(100);
        helper.getLevel().addFreshEntity(cow);
        BlockState waterlogged = Blocks.OAK_SLAB.defaultBlockState()
                .setValue(net.minecraft.world.level.block.SlabBlock.WATERLOGGED, true);
        helper.getLevel().setBlock(waterloggedPos, waterlogged, 3);
        try {
            AnimaniaFindWaterGoal goal = new AnimaniaFindWaterGoal(cow, false, true, waterloggedPos::equals);
            boolean found = false;
            for (int attempt = 0; attempt < 500 && !found; attempt++) found = goal.canUse();
            helper.assertTrue(found && waterloggedPos.equals(goal.target()),
                    "animal did not select a waterlogged slab as natural water");
            if (!found) return;
            cow.moveTo(waterloggedPos.getX() + 1.0D, waterloggedPos.getY(), waterloggedPos.getZ() + 0.5D,
                    0.0F, 0.0F);
            goal.tick();
            helper.assertTrue(cow.getThirst() == 100,
                    "animal did not drink from the waterlogged slab; thirst=" + cow.getThirst());
            BlockState remaining = helper.getLevel().getBlockState(waterloggedPos);
            helper.assertTrue(remaining.is(Blocks.OAK_SLAB)
                            && !remaining.getValue(net.minecraft.world.level.block.SlabBlock.WATERLOGGED)
                            && helper.getLevel().getFluidState(waterloggedPos).isEmpty(),
                    "bucket-style drinking did not preserve the dry slab: " + remaining
                            + ", fluid=" + helper.getLevel().getFluidState(waterloggedPos));
        } finally {
            cow.discard();
            AnimaniaConfig.WATER_REMOVED_AFTER_DRINKING.set(previousRemoveWater);
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void pigsDrinkTroughSlopAndHerbivoresConsumeFoodBlocks(GameTestHelper helper) {
        boolean previousRemovePlants = com.animania.common.config.AnimaniaConfig.PLANTS_REMOVED_AFTER_EATING.get();
        com.animania.common.config.AnimaniaConfig.PLANTS_REMOVED_AFTER_EATING.set(true);

        AnimaniaAnimalEntity pig = createAnimal(helper, "sow_duroc");
        BlockPos pigPos = helper.absolutePos(new BlockPos(0, 1, 0));
        BlockPos troughPos = helper.absolutePos(new BlockPos(4, 1, 0));
        pig.moveTo(pigPos.getX() + 0.5D, pigPos.getY(), pigPos.getZ() + 0.5D, 0.0F, 0.0F);
        pig.markInteracted();
        pig.setHunger(0);
        pig.setThirst(100);
        helper.getLevel().addFreshEntity(pig);
        helper.getLevel().setBlock(troughPos, AnimaniaBlocks.TROUGH.get().defaultBlockState(), 3);
        var storage = (com.animania.common.block.AnimaniaStorageBlockEntity) helper.getLevel().getBlockEntity(troughPos);
        helper.assertTrue(storage != null, "slop trough block entity missing");
        if (storage == null) return;
        var fluids = storage.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.FLUID_HANDLER).resolve().orElseThrow();
        helper.assertTrue(fluids.fill(new net.minecraftforge.fluids.FluidStack(
                        com.animania.common.AnimaniaFluids.SOURCE_SLOP.get(), 500),
                        net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE) == 500,
                "trough rejected legacy slop fluid");
        AnimaniaFindFoodGoal slopGoal = new AnimaniaFindFoodGoal(pig, true, false);
        boolean foundSlop = false;
        for (int attempt = 0; attempt < 500 && !foundSlop; attempt++) foundSlop = slopGoal.canUse();
        helper.assertTrue(foundSlop && troughPos.equals(slopGoal.target()), "hungry pig did not select trough slop");
        if (!foundSlop) return;
        pig.moveTo(troughPos.getX() + 1.0D, troughPos.getY(), troughPos.getZ() + 0.5D, 0.0F, 0.0F);
        slopGoal.tick();
        helper.assertTrue(pig.getHunger() == 100, "slop did not restore pig hunger");
        helper.assertTrue(storage.fluidAmount(stack -> stack.getFluid() == com.animania.common.AnimaniaFluids.SOURCE_SLOP.get()) == 400,
                "pig did not drain exactly 100 mB of slop");
        pig.discard();

        AnimaniaAnimalEntity cow = createAnimal(helper, "cow_angus");
        BlockPos cowPos = helper.absolutePos(new BlockPos(0, 1, 3));
        BlockPos cropPos = helper.absolutePos(new BlockPos(4, 1, 3));
        cow.moveTo(cowPos.getX() + 0.5D, cowPos.getY(), cowPos.getZ() + 0.5D, 0.0F, 0.0F);
        cow.markInteracted();
        cow.setHunger(0);
        cow.setThirst(100);
        helper.getLevel().addFreshEntity(cow);
        helper.getLevel().setBlock(cropPos, net.minecraft.world.level.block.Blocks.WHEAT.defaultBlockState()
                .setValue(net.minecraft.world.level.block.CropBlock.AGE, 7), 3);
        AnimaniaFindFoodGoal cropGoal = new AnimaniaFindFoodGoal(cow, false, true);
        boolean foundCrop = false;
        for (int attempt = 0; attempt < 500 && !foundCrop; attempt++) foundCrop = cropGoal.canUse();
        helper.assertTrue(foundCrop && cropPos.equals(cropGoal.target()), "hungry herbivore did not select crop food");
        if (!foundCrop) return;
        cow.moveTo(cropPos.getX() + 1.0D, cropPos.getY(), cropPos.getZ() + 0.5D, 0.0F, 0.0F);
        cropGoal.tick();
        helper.assertTrue(cow.getHunger() == 100, "crop did not restore herbivore hunger");
        helper.assertTrue(helper.getLevel().getBlockState(cropPos).isAir(), "configured crop was not consumed");
        cow.discard();
        com.animania.common.config.AnimaniaConfig.PLANTS_REMOVED_AFTER_EATING.set(previousRemovePlants);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void injuredAnimalsPathToSaltLicksWithoutChangingCareMeters(GameTestHelper helper) {
        AnimaniaGameTestEvidence.mark("animania_farm:generic_ai_find_salt_lick");
        AnimaniaAnimalEntity cow = createAnimal(helper, "cow_angus");
        BlockPos cowPos = helper.absolutePos(new BlockPos(0, 1, 0));
        BlockPos saltPos = helper.absolutePos(new BlockPos(4, 1, 0));
        cow.moveTo(cowPos.getX() + 0.5D, cowPos.getY(), cowPos.getZ() + 0.5D, 0.0F, 0.0F);
        cow.setAge(0); cow.setHunger(33); cow.setThirst(44);
        cow.setHealth(cow.getMaxHealth() - 4.0F);
        helper.getLevel().addFreshEntity(cow);
        helper.getLevel().setBlock(saltPos, com.animania.common.AnimaniaBlocks.SALT_LICK.get().defaultBlockState(), 3);
        var lick = (com.animania.common.block.AnimaniaSaltLickBlockEntity) helper.getLevel().getBlockEntity(saltPos);
        helper.assertTrue(lick != null, "salt lick block entity missing");
        if (lick == null) return;
        int uses = lick.usesLeft();
        float health = cow.getHealth();
        AnimaniaFindSaltLickGoal goal = new AnimaniaFindSaltLickGoal(cow);
        boolean found = false;
        for (int attempt = 0; attempt < 30000 && !found; attempt++) found = goal.canUse();
        helper.assertTrue(found && saltPos.equals(goal.target()), "injured cow did not select salt lick");
        if (!found) return;
        cow.moveTo(saltPos.getX() + 1.0D, saltPos.getY(), saltPos.getZ() + 0.5D, 0.0F, 0.0F);
        goal.tick();
        helper.assertTrue(cow.getHealth() == Math.min(cow.getMaxHealth(), health + 2.0F), "salt lick did not heal exactly two health");
        helper.assertTrue(lick.usesLeft() == uses - 1, "salt lick did not lose exactly one use");
        helper.assertTrue(cow.getHunger() == 33 && cow.getThirst() == 44,
                "salt lick incorrectly replaced food or water care states");
        cow.discard();
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void vanillaCowReplacementRetainsWorldBoundarySemantics(GameTestHelper helper) {
        Cow commandCow = EntityType.COW.create(helper.getLevel());
        if (commandCow == null) {
            helper.fail("vanilla command cow could not be constructed");
            return;
        }
        commandCow.setUUID(UUID.randomUUID());
        commandCow.moveTo(helper.absolutePos(new BlockPos(3, 1, 0)), 0.0F, 0.0F);
        helper.getLevel().addFreshEntity(commandCow);
        helper.assertFalse(commandCow.isRemoved(),
                "non-natural vanilla cow was incorrectly replaced at the world boundary");

        Cow cow = EntityType.COW.create(helper.getLevel());
        if (cow == null) {
            helper.fail("vanilla cow could not be constructed");
            return;
        }
        cow.setUUID(UUID.randomUUID());
        cow.moveTo(helper.absolutePos(new BlockPos(0, 1, 0)), 0.0F, 0.0F);
        cow.getPersistentData().putBoolean("AnimaniaNaturalFarmSpawn", true);
        helper.getLevel().addFreshEntity(cow);
        helper.runAtTickTime(2, () -> {
            var entities = helper.getLevel().getEntitiesOfClass(AnimaniaAnimalEntity.class,
                    new AABB(helper.absolutePos(new BlockPos(0, 1, 0))).inflate(2.0D));
            helper.assertTrue(entities.stream().anyMatch(entity -> {
                var id = net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
                return id != null && id.getNamespace().equals(AnimaniaFarm.MOD_ID)
                        && (id.getPath().startsWith("cow_") || id.getPath().startsWith("bull_"));
            }), "vanilla cow was not replaced by a registered Animania cow");
            helper.assertTrue(helper.getLevel().getEntitiesOfClass(Cow.class,
                    new AABB(helper.absolutePos(new BlockPos(0, 1, 0))).inflate(1.0D)).isEmpty(),
                    "vanilla cow remained after replacement");
            helper.assertFalse(commandCow.isRemoved(),
                    "unmarked vanilla cow was removed after the natural-spawn replacement");
            commandCow.discard();
            helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void animalCareBreedingAndPersistence(GameTestHelper helper) {
        @SuppressWarnings("unchecked")
        EntityType<? extends AnimaniaAnimalEntity> femaleType = (EntityType<? extends AnimaniaAnimalEntity>) (EntityType<?>) AnimaniaFarm.ENTITIES.get("cow_angus").get();
        @SuppressWarnings("unchecked")
        EntityType<? extends AnimaniaAnimalEntity> maleType = (EntityType<? extends AnimaniaAnimalEntity>) (EntityType<?>) AnimaniaFarm.ENTITIES.get("bull_angus").get();
        AnimaniaAnimalEntity female = spawn(helper, femaleType, 0);
        AnimaniaAnimalEntity male = spawn(helper, maleType, 2);
        female.setGender(AnimalGender.FEMALE);
        male.setGender(AnimalGender.MALE);
        female.setHunger(10);
        female.setThirst(10);
        helper.assertTrue(female.feed(new ItemStack(Items.WHEAT)), "animal rejected valid feed");
        helper.assertTrue(female.getHunger() > 10, "feed did not restore hunger");
        helper.assertTrue(female.drink(new ItemStack(Items.WATER_BUCKET)), "animal rejected valid drink");
        helper.assertTrue(female.getThirst() == 100, "drink did not restore thirst");
        female.setInLove(null);
        male.setInLove(null);
        helper.assertTrue(female.canBreedWith(male), "paired legacy male/female IDs were not recognised as one species");
        female.spawnChildFromBreeding((ServerLevel) helper.getLevel(), male);
        helper.assertTrue(female.isPregnant(), "breeding did not enter the server-side pregnancy state");
        AgeableMob offspring = female.getBreedOffspring((ServerLevel) helper.getLevel(), male);
        helper.assertTrue(offspring != null && offspring.getType() == AnimaniaFarm.ENTITIES.get("calf_angus").get(),
                "breeding did not resolve the legacy calf entity type");
        CompoundTag saved = new CompoundTag();
        female.setVariantName("regression");
        female.setSterilized(false);
        female.addAdditionalSaveData(saved);
        female.setVariantName("mutated");
        female.readAdditionalSaveData(saved);
        helper.assertTrue("regression".equals(female.getVariantName()), "entity NBT did not restore variant");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void pullableVehicleHasInventoryAndPassengerPath(GameTestHelper helper) {
        AnimaniaGameTestEvidence.mark("animania_farm:pullableVehicleHasInventoryAndPassengerPath");
        @SuppressWarnings("unchecked")
        EntityType<? extends AnimaniaVehicleEntity> type = (EntityType<? extends AnimaniaVehicleEntity>) (EntityType<?>) AnimaniaFarm.ENTITIES.get("cart").get();
        AnimaniaVehicleEntity vehicle = type.create(helper.getLevel());
        if (vehicle == null) {
            helper.fail("cart entity could not be constructed");
            return;
        }
        vehicle.moveTo(helper.absolutePos(new BlockPos(0, 1, 0)), 0.0F, 0.0F);
        helper.getLevel().addFreshEntity(vehicle);
        vehicle.setItem(0, new ItemStack(Items.WHEAT, 3));
        helper.assertTrue(vehicle.getItem(0).getCount() == 3, "vehicle inventory did not accept cargo");
        var menuPlayer = helper.makeMockPlayer();
        menuPlayer.moveTo(vehicle.getX(), vehicle.getY(), vehicle.getZ(), 0.0F, 0.0F);
        var menu = vehicle.createMenu(17, menuPlayer.getInventory(), menuPlayer);
        helper.assertTrue(menu instanceof net.minecraft.world.inventory.ChestMenu
                        && menu.containerId == 17 && menu.slots.size() == 63,
                "vehicle did not expose its 27 cargo plus 36 player slots through the native MenuProvider");
        helper.assertTrue(vehicle.stillValid(menuPlayer),
                "nearby player was rejected by the vehicle menu validity boundary");
        CompoundTag saved = new CompoundTag();
        vehicle.addAdditionalSaveData(saved);
        vehicle.setItem(0, ItemStack.EMPTY);
        vehicle.readAdditionalSaveData(saved);
        helper.assertTrue(vehicle.getItem(0).getCount() == 3, "vehicle cargo was not serialized");
        helper.assertTrue(vehicle.boost(), "vehicle did not accept a riding-crop boost");
        helper.assertTrue(!vehicle.boost(), "vehicle accepted a duplicate boost before cooldown");

        @SuppressWarnings("unchecked")
        EntityType<? extends AnimaniaAnimalEntity> horseType = (EntityType<? extends AnimaniaAnimalEntity>) (EntityType<?>) AnimaniaFarm.ENTITIES.get("mare_draft").get();
        AnimaniaAnimalEntity horse = horseType.create(helper.getLevel());
        helper.assertTrue(horse != null, "draft horse could not be constructed for hitch test");
        if (horse == null) return;
        horse.moveTo(helper.absolutePos(new BlockPos(0, 1, 3)), 0.0F, 0.0F);
        horse.setAge(0);
        helper.getLevel().addFreshEntity(horse);
        helper.assertTrue(vehicle.tryAttachPuller(horse), "vehicle rejected an adult draft horse hitch");
        helper.assertTrue(vehicle.isPulled() && vehicle.getPuller() == horse, "vehicle hitch did not synchronize");
        CompoundTag hitch = new CompoundTag();
        vehicle.addAdditionalSaveData(hitch);
        helper.assertTrue(hitch.hasUUID("AnimaniaPuller"), "vehicle hitch UUID was not persisted");
        vehicle.detachPuller();
        helper.assertTrue(!vehicle.isPulled(), "vehicle hitch did not detach");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void vehicleItemsSpawnNamedEntitiesAtAirAndBlockTargets(GameTestHelper helper) {
        AnimaniaGameTestEvidence.mark("animania_farm:vehicleItemsSpawnNamedEntitiesAtAirAndBlockTargets");
        boolean previousVehicles = AnimaniaConfig.ENABLE_VEHICLES.get();
        boolean previousDisabled = FarmConfig.DISABLE_ROLLING_VEHICLES.get();
        var player = helper.makeMockPlayer();
        helper.getLevel().addFreshEntity(player);
        try {
            AnimaniaConfig.ENABLE_VEHICLES.set(true);
            FarmConfig.DISABLE_ROLLING_VEHICLES.set(false);
            int offset = 0;
            for (String id : new String[]{"cart", "wagon"}) {
                player.moveTo(helper.absolutePos(new BlockPos(1 + offset, 1, 1)), 0.0F, 0.0F);
                ItemStack stack = new ItemStack(FarmContent.ITEM_ENTRIES.get(id).get());
                stack.setHoverName(net.minecraft.network.chat.Component.literal("Named " + id));
                player.setItemInHand(InteractionHand.MAIN_HAND, stack);
                helper.assertTrue(stack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND)
                                .getResult().consumesAction() && stack.isEmpty(),
                        id + " item did not spawn server-side and consume exactly one item");
                var spawned = helper.getLevel().getEntitiesOfClass(AnimaniaVehicleEntity.class,
                        player.getBoundingBox().inflate(0.75D), vehicle -> vehicle.getType() == AnimaniaFarm.ENTITIES.get(id).get());
                helper.assertTrue(spawned.size() == 1 && ("Named " + id).equals(spawned.get(0).getCustomName().getString()),
                        id + " item did not preserve its custom name on the spawned entity");
                offset += 2;
            }

            BlockPos support = helper.absolutePos(new BlockPos(6, 1, 1));
            helper.getLevel().setBlock(support, Blocks.STONE.defaultBlockState(), 3);
            ItemStack tillerStack = new ItemStack(FarmContent.ITEM_ENTRIES.get("tiller").get());
            player.setItemInHand(InteractionHand.MAIN_HAND, tillerStack);
            var hit = new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(support),
                    Direction.UP, support, false);
            helper.assertTrue(tillerStack.getItem().useOn(new net.minecraft.world.item.context.UseOnContext(
                            helper.getLevel(), player, InteractionHand.MAIN_HAND, tillerStack, hit)).consumesAction()
                            && tillerStack.isEmpty(),
                    "tiller item did not retain clicked-block placement and consumption");
            var tillers = helper.getLevel().getEntitiesOfClass(AnimaniaVehicleEntity.class,
                    new AABB(support.above()).inflate(0.25D),
                    vehicle -> vehicle.getType() == AnimaniaFarm.ENTITIES.get("tiller").get());
            helper.assertTrue(tillers.size() == 1, "tiller did not spawn centered above the clicked face");
        } finally {
            AnimaniaConfig.ENABLE_VEHICLES.set(previousVehicles);
            FarmConfig.DISABLE_ROLLING_VEHICLES.set(previousDisabled);
            player.discard();
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void vehicleDropsHonorModernDoEntityDropsRule(GameTestHelper helper) {
        AnimaniaGameTestEvidence.mark("animania_farm:vehicleDropsHonorModernDoEntityDropsRule");
        var rule = helper.getLevel().getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_DOENTITYDROPS);
        boolean previous = rule.get();
        try {
            rule.set(false, helper.getLevel().getServer());
            AnimaniaVehicleEntity suppressed = (AnimaniaVehicleEntity) AnimaniaFarm.ENTITIES.get("cart").get()
                    .create(helper.getLevel());
            suppressed.moveTo(helper.absolutePos(new BlockPos(1, 1, 1)), 0.0F, 0.0F);
            suppressed.setItem(0, new ItemStack(Items.WHEAT, 3));
            helper.getLevel().addFreshEntity(suppressed);
            suppressed.kill();
            helper.assertTrue(helper.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
                            new AABB(suppressed.position(), suppressed.position()).inflate(1.0D)).isEmpty(),
                    "vehicle dropped itself/cargo while doEntityDrops was false");

            rule.set(true, helper.getLevel().getServer());
            AnimaniaVehicleEntity dropping = (AnimaniaVehicleEntity) AnimaniaFarm.ENTITIES.get("cart").get()
                    .create(helper.getLevel());
            dropping.moveTo(helper.absolutePos(new BlockPos(4, 1, 1)), 0.0F, 0.0F);
            dropping.setItem(0, new ItemStack(Items.WHEAT, 3));
            helper.getLevel().addFreshEntity(dropping);
            dropping.kill();
            var drops = helper.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
                    new AABB(dropping.position(), dropping.position()).inflate(1.0D));
            helper.assertTrue(drops.stream().anyMatch(drop -> drop.getItem().is(FarmContent.ITEM_ENTRIES.get("cart").get()))
                            && drops.stream().anyMatch(drop -> drop.getItem().is(Items.WHEAT)
                            && drop.getItem().getCount() == 3),
                    "vehicle did not drop both itself and exact cargo while doEntityDrops was true");
        } finally {
            rule.set(previous, helper.getLevel().getServer());
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void pulledTillerCultivatesThreeRowsAndConsumesSeed(GameTestHelper helper) {
        AnimaniaGameTestEvidence.mark("animania_farm:pulledTillerCultivatesThreeRowsAndConsumesSeed");
        @SuppressWarnings("unchecked")
        EntityType<? extends AnimaniaVehicleEntity> tillerType = (EntityType<? extends AnimaniaVehicleEntity>) (EntityType<?>) AnimaniaFarm.ENTITIES.get("tiller").get();
        AnimaniaVehicleEntity tiller = tillerType.create(helper.getLevel());
        AnimaniaAnimalEntity horse = createAnimal(helper, "mare_draft");
        BlockPos horsePos = helper.absolutePos(new BlockPos(4, 2, 6));
        BlockPos center = helper.absolutePos(new BlockPos(4, 1, 4));
        horse.moveTo(horsePos.getX() + 0.5D, horsePos.getY(), horsePos.getZ() + 0.5D, 0.0F, 0.0F);
        horse.setAge(0);
        tiller.moveTo(helper.absolutePos(new BlockPos(1, 2, 1)), 0.0F, 0.0F);
        tiller.setItem(0, new ItemStack(Items.WHEAT_SEEDS, 3));
        helper.getLevel().setBlock(center, Blocks.DIRT.defaultBlockState(), 3);
        helper.getLevel().setBlock(center.east(), Blocks.GRASS_BLOCK.defaultBlockState(), 3);
        helper.getLevel().setBlock(center.west(), Blocks.COARSE_DIRT.defaultBlockState(), 3);
        helper.getLevel().setBlock(center.above(), Blocks.AIR.defaultBlockState(), 3);
        helper.getLevel().setBlock(center.east().above(), Blocks.AIR.defaultBlockState(), 3);
        helper.getLevel().setBlock(center.west().above(), Blocks.AIR.defaultBlockState(), 3);
        for (BlockPos row : new BlockPos[]{center, center.east(), center.west()}) {
            helper.getLevel().setBlock(row.above(2), Blocks.LIGHT.defaultBlockState()
                    .setValue(net.minecraft.world.level.block.LightBlock.LEVEL, 15), 3);
        }
        // Block-light propagation is queued, so let the underground GameTest
        // fixture settle before asking vanilla crops to validate survival.
        helper.runAfterDelay(2, () -> {
            helper.getLevel().addFreshEntity(horse);
            helper.getLevel().addFreshEntity(tiller);
            helper.assertTrue(tiller.tryAttachPuller(horse), "tiller rejected a valid draft horse");
            tiller.tick();
            for (BlockPos row : new BlockPos[]{center, center.east(), center.west()}) {
                helper.assertTrue(helper.getLevel().getBlockState(row).is(Blocks.FARMLAND), "tiller did not cultivate row " + row);
                helper.assertTrue(helper.getLevel().getBlockState(row.above()).is(Blocks.WHEAT),
                        "tiller did not sow wheat in row " + row + "; above=" + helper.getLevel().getBlockState(row.above())
                                + ", light=" + helper.getLevel().getRawBrightness(row.above(), 0)
                                + ", remaining=" + tiller.getItem(0));
            }
            helper.assertTrue(tiller.getItem(0).isEmpty(), "three cultivated rows did not consume exactly three seeds");
            tiller.tick();
            helper.assertTrue(tiller.getItem(0).isEmpty(), "stationary tiller repeated its seed operation");
            helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void farmSpecialItemsRetainLegacyUseSemantics(GameTestHelper helper) {
        AnimaniaGameTestEvidence.mark("animania_farm:farmSpecialItemsRetainLegacyUseSemantics");
        helper.assertTrue(FarmContent.ITEM_ENTRIES.get("milk_bottle").get() instanceof FarmMilkBottleItem,
                "milk bottle is not the drinkable modern item");
        helper.assertTrue(FarmContent.ITEM_ENTRIES.get("honey_jar").get() instanceof FarmHoneyJarItem,
                "honey jar is not the drinkable modern item");
        helper.assertTrue(FarmContent.ITEM_ENTRIES.get("brown_egg").get() instanceof FarmBrownEggItem,
                "brown egg is not throwable");
        helper.assertTrue(FarmContent.ITEM_ENTRIES.get("carving_knife").get() instanceof FarmCarvingKnifeItem,
                "carving knife did not retain durability semantics");
        helper.assertTrue(FarmContent.ITEM_ENTRIES.get("riding_crop").get() instanceof FarmRidingCropItem,
                "riding crop did not retain boost semantics");
        helper.assertTrue(FarmContent.BROWN_EGG_PROJECTILE.get().create(helper.getLevel()) instanceof com.animania.farm.FarmBrownEggProjectile,
                "brown egg projectile was not registered as a synchronized Forge entity");
        FarmContent.CHEESE_BLOCKS.forEach((family, block) -> {
            var wheel = FarmContent.ITEM_ENTRIES.get(family + "_cheese_wheel").get();
            helper.assertTrue(wheel instanceof net.minecraft.world.item.BlockItem blockItem
                            && blockItem.getBlock() == block.get(),
                    family + " cheese wheel is not the placeable item for its matching cheese block");
        });

        boolean previousEggThrowing = FarmConfig.ALLOW_EGG_THROWING.get();
        boolean previousBonusEffects = AnimaniaConfig.FOODS_GIVE_BONUS_EFFECTS.get();
        var player = helper.makeMockPlayer();
        helper.getLevel().addFreshEntity(player);
        try {
            FarmConfig.ALLOW_EGG_THROWING.set(true);
            ItemStack eggs = new ItemStack(FarmContent.ITEM_ENTRIES.get("brown_egg").get(), 2);
            player.setItemInHand(InteractionHand.MAIN_HAND, eggs);
            helper.assertTrue(eggs.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND)
                            .getResult().consumesAction(),
                    "enabled brown egg throwing did not consume the interaction");
            helper.assertTrue(eggs.getCount() == 1, "brown egg throwing did not consume exactly one egg");
            helper.assertTrue(!helper.getLevel().getEntitiesOfClass(com.animania.farm.FarmBrownEggProjectile.class,
                            player.getBoundingBox().inflate(8.0D)).isEmpty(),
                    "brown egg use did not spawn its synchronized projectile");

            player.getFoodData().setFoodLevel(10);
            player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    net.minecraft.world.effect.MobEffects.POISON, 200, 0));
            ItemStack milk = new ItemStack(FarmContent.ITEM_ENTRIES.get("milk_bottle").get());
            ItemStack milkResult = milk.getItem().finishUsingItem(milk, helper.getLevel(), player);
            helper.assertTrue(milkResult.is(Items.GLASS_BOTTLE),
                    "consumed milk bottle did not return a glass bottle");
            helper.assertTrue(player.getFoodData().getFoodLevel() == 14,
                    "milk bottle did not restore its legacy four hunger points");
            helper.assertTrue(player.getActiveEffects().isEmpty(),
                    "milk bottle did not clear all status effects");

            AnimaniaConfig.FOODS_GIVE_BONUS_EFFECTS.set(true);
            player.getFoodData().setFoodLevel(5);
            ItemStack fluidHoney = new ItemStack(FarmContent.ITEM_ENTRIES.get("honey_bottle").get());
            var honeyHandler = net.minecraftforge.fluids.FluidUtil.getFluidHandler(fluidHoney)
                    .orElseThrow(() -> new AssertionError("honey bottle lost its Forge item-fluid capability"));
            var storedHoney = honeyHandler.getFluidInTank(0);
            helper.assertTrue(honeyHandler.getTankCapacity(0) == 1000 && storedHoney.getAmount() == 1000
                            && storedHoney.getFluid() == FarmFluids.ALL.get("animania_honey").source.get(),
                    "honey bottle did not contain exactly 1000 mB of Animania honey");
            helper.assertTrue(honeyHandler.drain(1000,
                            net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE).getAmount() == 1000
                            && honeyHandler.getContainer().is(Items.GLASS_BOTTLE),
                    "automation drain did not swap an emptied honey bottle to glass");
            ItemStack honey = new ItemStack(FarmContent.ITEM_ENTRIES.get("honey_bottle").get());
            ItemStack honeyResult = honey.getItem().finishUsingItem(honey, helper.getLevel(), player);
            var honeyRegeneration = player.getEffect(net.minecraft.world.effect.MobEffects.REGENERATION);
            helper.assertTrue(honeyResult.is(Items.GLASS_BOTTLE),
                    "consumed honey bottle did not return a glass bottle");
            helper.assertTrue(player.getFoodData().getFoodLevel() == 15,
                    "honey bottle did not restore its legacy ten hunger points");
            helper.assertTrue(honeyRegeneration != null && honeyRegeneration.getDuration() == 100
                            && honeyRegeneration.getAmplifier() == 1,
                    "honey bottle did not grant Regeneration II for 100 ticks");

            player.removeAllEffects();
            player.getFoodData().setFoodLevel(0);
            ItemStack soup = new ItemStack(FarmContent.ITEM_ENTRIES.get("truffle_soup").get());
            ItemStack soupResult = soup.getItem().finishUsingItem(soup, helper.getLevel(), player);
            var soupRegeneration = player.getEffect(net.minecraft.world.effect.MobEffects.REGENERATION);
            helper.assertTrue(soupResult.is(Items.BOWL), "consumed truffle soup did not return its bowl");
            helper.assertTrue(player.getFoodData().getFoodLevel() == 10,
                    "truffle soup did not restore its legacy ten hunger points");
            helper.assertTrue(soupRegeneration != null && soupRegeneration.getDuration() == 1200
                            && soupRegeneration.getAmplifier() == 1,
                    "truffle soup did not grant Regeneration II for 1200 ticks");

            AnimaniaVehicleEntity cart = (AnimaniaVehicleEntity) AnimaniaFarm.ENTITIES.get("cart").get()
                    .create(helper.getLevel());
            helper.assertTrue(cart != null, "cart could not be created for riding-crop verification");
            cart.moveTo(player.position());
            helper.getLevel().addFreshEntity(cart);
            helper.assertTrue(player.startRiding(cart, true), "mock player could not mount the cart");
            ItemStack crop = new ItemStack(FarmContent.ITEM_ENTRIES.get("riding_crop").get());
            player.setItemInHand(InteractionHand.MAIN_HAND, crop);
            helper.assertTrue(crop.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND)
                            .getResult().consumesAction(),
                    "riding crop did not start a mounted boost");
            helper.assertTrue(crop.getDamageValue() == 1,
                    "riding crop boost did not consume exactly one durability");
            helper.assertFalse(cart.boost(), "riding crop did not leave the vehicle in its active boost window");
            player.stopRiding();
            cart.discard();
        } finally {
            FarmConfig.ALLOW_EGG_THROWING.set(previousEggThrowing);
            AnimaniaConfig.FOODS_GIVE_BONUS_EFFECTS.set(previousBonusEffects);
            player.discard();
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void sheepAndOnlyAngoraGoatsRetainLegacyShearingRules(GameTestHelper helper) {
        var player = helper.makeMockPlayer();
        helper.getLevel().addFreshEntity(player);
        verifyShearing(helper, player, "ewe_dorset", true);
        verifyShearing(helper, player, "ram_merino", true);
        verifyShearing(helper, player, "buck_angora", true);
        verifyShearing(helper, player, "doe_angora", true);
        verifyShearing(helper, player, "doe_alpine", false);
        verifyShearing(helper, player, "buck_pygmy", false);
        player.discard();
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void sheepDyeColorSynchronizesPersistsAndControlsWoolDrop(GameTestHelper helper) {
        var player = helper.makeMockPlayer();
        helper.getLevel().addFreshEntity(player);
        AnimaniaAnimalEntity sheep = createAnimal(helper, "ewe_dorset");
        sheep.moveTo(helper.absolutePos(new BlockPos(1, 1, 1)), 0.0F, 0.0F);
        sheep.setAge(0);
        helper.getLevel().addFreshEntity(sheep);

        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.RED_DYE));
        helper.assertTrue(sheep.mobInteract(player, InteractionHand.MAIN_HAND).consumesAction(),
                "adult sheep rejected a legacy dye interaction");
        helper.assertTrue(sheep.getWoolColor() == net.minecraft.world.item.DyeColor.RED.getId(),
                "dye color did not enter synchronized entity data");

        CompoundTag tag = new CompoundTag();
        sheep.addAdditionalSaveData(tag);
        helper.assertTrue(tag.getInt("DyeColor") == net.minecraft.world.item.DyeColor.RED.getId(),
                "legacy DyeColor NBT alias was not retained");
        sheep.setWoolColor(net.minecraft.world.item.DyeColor.WHITE.getId());
        sheep.readAdditionalSaveData(tag);
        helper.assertTrue(sheep.getWoolColor() == net.minecraft.world.item.DyeColor.RED.getId(),
                "dye color did not survive NBT reload");

        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.SHEARS));
        sheep.mobInteract(player, InteractionHand.MAIN_HAND);
        var drops = helper.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
                sheep.getBoundingBox().inflate(2.0D));
        helper.assertTrue(drops.stream().anyMatch(drop -> drop.getItem().is(Items.RED_WOOL)),
                "dyed sheep did not drop matching modern wool");
        player.discard();
        sheep.discard();
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void farmLactationAndEggLayStatePersists(GameTestHelper helper) {
        @SuppressWarnings("unchecked")
        EntityType<? extends AnimaniaAnimalEntity> cowType = (EntityType<? extends AnimaniaAnimalEntity>) (EntityType<?>) AnimaniaFarm.ENTITIES.get("cow_angus").get();
        AnimaniaAnimalEntity cow = spawn(helper, cowType, 0);
        cow.setGender(AnimalGender.FEMALE);
        helper.assertTrue(!cow.isMilkReady(), "cow was milkable at spawn while the legacy default is disabled");
        cow.setMilkReady(true);
        CompoundTag cowTag = new CompoundTag();
        cow.addAdditionalSaveData(cowTag);
        cow.setMilkReady(false);
        cow.readAdditionalSaveData(cowTag);
        helper.assertTrue(cow.isMilkReady(), "lactation state did not survive entity NBT");

        @SuppressWarnings("unchecked")
        EntityType<? extends AnimaniaAnimalEntity> henType = (EntityType<? extends AnimaniaAnimalEntity>) (EntityType<?>) AnimaniaFarm.ENTITIES.get("hen_leghorn").get();
        AnimaniaAnimalEntity hen = spawn(helper, henType, 3);
        hen.setGender(AnimalGender.FEMALE);
        hen.setAge(0);
        hen.setHunger(100);
        hen.setThirst(100);
        ((ServerLevel) helper.getLevel()).setDayTime(1000L);
        CompoundTag henTag = new CompoundTag();
        henTag.putInt("AnimaniaEggLayTicks", 1);
        hen.readAdditionalSaveData(henTag);
        hen.setGender(AnimalGender.FEMALE);
        hen.setAge(0);
        hen.setHunger(100);
        hen.setThirst(100);
        BlockPos nestPos = helper.absolutePos(new BlockPos(4, 1, 0));
        helper.getLevel().setBlock(nestPos, com.animania.common.AnimaniaBlocks.NEST.get().defaultBlockState(), 3);
        helper.assertTrue(hen.tryLayFarmEgg(false), "hen did not lay in a nearby nest when loose egg drops were disabled");
        var nest = (com.animania.common.AnimaniaBlocks.NestEntity) helper.getLevel().getBlockEntity(nestPos);
        helper.assertTrue(nest.getItem(0).is(Items.EGG) && nest.getItem(0).getCount() == 1,
                "Leghorn did not preserve its white-egg nest behavior");
        helper.assertTrue(nest.birdVariant().equals("leghorn"), "nest did not persist the laying hen variant");
        helper.getLevel().removeBlock(nestPos, false);
        henTag.putInt("AnimaniaEggLayTicks", 1);
        hen.readAdditionalSaveData(henTag);
        hen.setGender(AnimalGender.FEMALE);
        hen.setAge(0);
        hen.setHunger(100);
        hen.setThirst(100);
        helper.assertTrue(hen.tryLayFarmEgg(true), "enabled loose hen egg laying did not produce an egg without a nest");
        helper.assertTrue(!helper.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
                new AABB(helper.absolutePos(new BlockPos(3, 1, 0))).inflate(2.0D)).isEmpty(),
                "hen egg was not spawned as a server item entity");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void randomEggsAreRealServerItems(GameTestHelper helper) {
        for (String id : new String[]{"entity_egg_cow_random", "entity_egg_chicken_random", "entity_egg_pig_random",
                "entity_egg_goat_random", "entity_egg_sheep_random"}) {
            helper.assertTrue(FarmContent.ITEM_ENTRIES.get(id).get() instanceof AnimaniaEntityEggItem,
                    id + " is an inert placeholder instead of an entity egg");
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void entityEggSpawnsOnePersistentNamedInteractedAnimalServerSide(GameTestHelper helper) {
        BlockPos support = helper.absolutePos(new BlockPos(2, 1, 2));
        helper.getLevel().setBlock(support, Blocks.STONE.defaultBlockState(), 3);
        var player = helper.makeMockPlayer();
        ItemStack stack = new ItemStack(FarmContent.ITEM_ENTRIES.get("entity_egg_cow_angus").get(), 2);
        var eggItem = (AnimaniaEntityEggItem) stack.getItem();
        helper.assertTrue(eggItem.tintColor(0) == 0xFF2E3438
                        && eggItem.tintColor(1) == 0xFF232A30
                        && eggItem.tintColor(2) == 0xFFFFFFFF,
                "entity egg did not preserve the two opaque legacy tint layers");
        stack.setHoverName(net.minecraft.network.chat.Component.literal("Legacy Angus"));
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        var hit = new net.minecraft.world.phys.BlockHitResult(
                net.minecraft.world.phys.Vec3.atCenterOf(support), Direction.UP, support, false);
        var result = stack.getItem().useOn(new net.minecraft.world.item.context.UseOnContext(
                helper.getLevel(), player, InteractionHand.MAIN_HAND, stack, hit));
        helper.assertTrue(result.consumesAction(), "entity egg did not report a successful server-side use");
        helper.assertTrue(stack.getCount() == 1, "survival entity egg did not consume exactly one item");
        BlockPos spawn = support.above();
        var animals = helper.getLevel().getEntitiesOfClass(AnimaniaAnimalEntity.class,
                new AABB(spawn).inflate(0.25D));
        helper.assertTrue(animals.size() == 1, "entity egg did not add exactly one animal at the clicked face");
        var animal = animals.get(0);
        helper.assertTrue(animal.getType() == AnimaniaFarm.ENTITIES.get("cow_angus").get(),
                "specific entity egg spawned the wrong legacy animal type");
        helper.assertTrue(animal.hasCustomName()
                        && "Legacy Angus".equals(animal.getCustomName().getString()),
                "entity egg did not copy its custom display name to the animal");
        helper.assertTrue(animal.hasInteracted(),
                "entity egg did not preserve the legacy interacted state needed by care AI");
        helper.assertTrue(animal.isPersistenceRequired(),
                "entity egg animal was not marked persistent");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void baseRandomEggSelectsOnlyLoadedAnimalTypes(GameTestHelper helper) {
        BlockPos support = helper.absolutePos(new BlockPos(2, 1, 2));
        helper.getLevel().setBlock(support, Blocks.STONE.defaultBlockState(), 3);
        var player = helper.makeMockPlayer();
        ItemStack stack = new ItemStack(com.animania.common.AnimaniaItems.ENTITY_EGG_RANDOM.get(), 2);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        var hit = new net.minecraft.world.phys.BlockHitResult(
                net.minecraft.world.phys.Vec3.atCenterOf(support), Direction.UP, support, false);
        var result = stack.getItem().useOn(new net.minecraft.world.item.context.UseOnContext(
                helper.getLevel(), player, InteractionHand.MAIN_HAND, stack, hit));
        helper.assertTrue(result.consumesAction() && stack.getCount() == 1,
                "Base random egg did not spawn successfully and consume exactly one item");
        var animals = helper.getLevel().getEntitiesOfClass(AnimaniaAnimalEntity.class,
                new AABB(support.above()).inflate(0.25D));
        helper.assertTrue(animals.size() == 1, "Base random egg did not spawn exactly one animal");
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(animals.get(0).getType());
        helper.assertTrue(id != null && java.util.Set.of("animania_farm", "animania_extra", "animania_catsdogs")
                        .contains(id.getNamespace())
                        && !(AnimaniaFarm.MOD_ID.equals(id.getNamespace()) && FarmLegacyIds.VEHICLE_IDS.contains(id.getPath())),
                "Base random egg selected an unloaded, non-animal, or vehicle entity type: " + id);
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 260)
    public static void cheeseMoldAcceptsModernMilkFluid(GameTestHelper helper) {
        AnimaniaGameTestEvidence.mark("animania_farm:cheeseMoldAcceptsModernMilkFluid");
        BlockPos pos = helper.absolutePos(new BlockPos(4, 1, 0));
        helper.getLevel().setBlock(pos, FarmContent.CHEESE_MOLD.get().defaultBlockState(), 3);
        if (!(helper.getLevel().getBlockEntity(pos) instanceof FarmCheeseMoldBlockEntity mold)) {
            helper.fail("fluid cheese mold did not create its block entity");
            return;
        }
        int filled = mold.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.FLUID_HANDLER, null)
                .map(handler -> handler.fill(new net.minecraftforge.fluids.FluidStack(FarmFluids.MILK_HOLSTEIN.source.get(), 1000),
                        net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE)).orElse(0);
        helper.assertTrue(filled == 1000, "cheese mold rejected registered Holstein milk fluid");
        int overflow = mold.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.FLUID_HANDLER, null)
                .map(handler -> handler.fill(new net.minecraftforge.fluids.FluidStack(
                                FarmFluids.MILK_HOLSTEIN.source.get(), 1),
                        net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE)).orElse(-1);
        helper.assertTrue(overflow == 0, "cheese mold exceeded its legacy 1000 mB capacity");
        var moldItems = mold.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER,
                        Direction.UP).orElseThrow(() -> new AssertionError(
                        "cheese mold did not expose its modern sided item automation capability"));
        helper.assertTrue(moldItems.getSlots() == 1 && moldItems.getSlotLimit(0) == 1,
                "cheese mold did not retain its exact one-slot/one-item automation limit");
        ItemStack rejectedSalt = new ItemStack(FarmContent.ITEM_ENTRIES.get("salt").get());
        helper.assertTrue(moldItems.insertItem(0, rejectedSalt, false).getCount() == 1,
                "cheese mold automation accepted an item that legacy automation rejected");
        mold.serverTick();
        helper.assertTrue(helper.getLevel().getBlockState(pos).getValue(FarmCheeseMoldBlock.VARIANT)
                        == FarmCheeseMoldBlock.Variant.HOLSTEIN_MILK,
                "Holstein milk did not select its legacy visible mold state");
        helper.assertTrue(helper.getLevel().getBlockState(pos).getShape(helper.getLevel(), pos).max(Direction.Axis.Y) == 0.625D,
                "cheese mold lost its legacy ten-pixel height");
        CompoundTag moldTag = mold.saveWithFullMetadata();
        helper.assertTrue(moldTag.getInt("ProcessTicks") == 1 && moldTag.contains("AnimaniaFluid"),
                "cheese mold did not serialize progress and fluid after processing began");
        FarmCheeseMoldBlockEntity reloadedMold = new FarmCheeseMoldBlockEntity(pos,
                helper.getLevel().getBlockState(pos));
        reloadedMold.load(moldTag);
        CompoundTag reloadedMoldTag = reloadedMold.saveWithFullMetadata();
        helper.assertTrue(reloadedMoldTag.getInt("ProcessTicks") == 1
                        && reloadedMold.fluidSnapshot().getAmount() == 1000,
                "cheese mold progress/fluid did not survive NBT reload");
        int originalMaturity = FarmConfig.CHEESE_MATURITY_TIME.get();
        FarmConfig.CHEESE_MATURITY_TIME.set(200);
        helper.runAtTickTime(205, () -> {
            FarmConfig.CHEESE_MATURITY_TIME.set(originalMaturity);
            helper.assertTrue(mold.getItem(0).is(FarmContent.ITEM_ENTRIES.get("holstein_cheese_wheel").get()),
                    "Holstein milk did not produce the matching cheese wheel");
            mold.serverTick();
            helper.assertTrue(helper.getLevel().getBlockState(pos).getValue(FarmCheeseMoldBlock.VARIANT)
                            == FarmCheeseMoldBlock.Variant.HOLSTEIN_CHEESE,
                    "finished Holstein cheese did not select its visible mold state");
            helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void draftHorseSaddleAndBoostStatePersists(GameTestHelper helper) {
        @SuppressWarnings("unchecked")
        EntityType<? extends AnimaniaAnimalEntity> type = (EntityType<? extends AnimaniaAnimalEntity>) (EntityType<?>) AnimaniaFarm.ENTITIES.get("mare_draft").get();
        AnimaniaAnimalEntity horse = type.create(helper.getLevel());
        helper.assertTrue(horse != null, "draft horse entity could not be constructed");
        if (horse == null) return;
        horse.setAge(0);
        horse.setSaddled(true);
        helper.assertTrue(horse.isSaddled(), "horse saddle state did not synchronize");
        helper.assertTrue(horse.boost(), "saddled horse rejected a riding-crop boost");
        CompoundTag tag = new CompoundTag();
        horse.addAdditionalSaveData(tag);
        horse.setSaddled(false);
        horse.readAdditionalSaveData(tag);
        helper.assertTrue(horse.isSaddled(), "horse saddle state did not persist through NBT");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 460)
    public static void childGrowsIntoAdultRegistryType(GameTestHelper helper) {
        @SuppressWarnings("unchecked")
        EntityType<? extends AnimaniaAnimalEntity> childType = (EntityType<? extends AnimaniaAnimalEntity>) (EntityType<?>) AnimaniaFarm.ENTITIES.get("calf_angus").get();
        AnimaniaAnimalEntity child = childType.create(helper.getLevel());
        if (child == null) {
            helper.fail("calf entity could not be constructed");
            return;
        }
        child.moveTo(helper.absolutePos(new BlockPos(0, 1, 0)), 0.0F, 0.0F);
        int interval = AnimaniaConfig.CHILD_GROWTH_TICK.get();
        child.setAge(-interval);
        child.setHunger(0);
        child.setCustomName(net.minecraft.network.chat.Component.literal("Kept Name"));
        child.markInteracted();
        child.setNoAi(true);
        helper.getLevel().addFreshEntity(child);
        helper.runAtTickTime(interval + 5, () -> {
            helper.assertTrue(child.isAlive() && child.getAge() == -interval,
                    "legacy growth gate mismatch: alive=" + child.isAlive() + ", age=" + child.getAge()
                            + ", hunger=" + child.getHunger() + ", thirst=" + child.getThirst()
                            + ", sleeping=" + child.isSleeping());
            child.setHunger(100);
        });
        helper.runAtTickTime(interval * 2 + 10, () -> {
                var grown = helper.getLevel().getEntitiesOfClass(AnimaniaAnimalEntity.class,
                        new AABB(helper.absolutePos(new BlockPos(0, 1, 0))).inflate(10.0D));
                boolean preserved = grown.stream().anyMatch(entity -> {
                    var id = net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
                    return id != null && ("cow_angus".equals(id.getPath()) || "bull_angus".equals(id.getPath()))
                            && entity.isAdult() && entity.hasInteracted() && entity.hasCustomName()
                            && "Kept Name".equals(entity.getCustomName().getString());
                });
                String observed = grown.stream().map(entity -> {
                    var id = net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
                    return id + "[adult=" + entity.isAdult() + ",interacted=" + entity.hasInteracted()
                            + ",name=" + (entity.hasCustomName() ? entity.getCustomName().getString() : "<none>") + "]";
                }).collect(java.util.stream.Collectors.joining(", "));
                helper.assertTrue(preserved,
                        "cared-for calf did not preserve adult state; observed: " + observed);
                helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void foodOverridesAndBonusEffectSwitchApplyAtConsumption(GameTestHelper helper) {
        var overrides = AnimaniaConfig.FOOD_VALUE_OVERRIDES;
        var bonusEffects = AnimaniaConfig.FOODS_GIVE_BONUS_EFFECTS;
        var eatAnytime = AnimaniaConfig.EAT_FOOD_ANYTIME;
        java.util.List<? extends String> previousOverrides = overrides.get();
        boolean previousEffects = bonusEffects.get();
        boolean previousEatAnytime = eatAnytime.get();
        var player = helper.makeMockPlayer();
        try {
            overrides.set(java.util.List.of("animania_farm:truffle(3,0.5)"));
            player.getFoodData().setFoodLevel(10);
            player.getFoodData().setSaturation(0.0F);
            ItemStack truffle = new ItemStack(FarmContent.ITEM_ENTRIES.get("truffle").get());
            truffle.getItem().finishUsingItem(truffle, helper.getLevel(), player);
            helper.assertTrue(player.getFoodData().getFoodLevel() == 13,
                    "foodValueOverrides did not replace the registered nutrition value");
            helper.assertTrue(Math.abs(player.getFoodData().getSaturationLevel() - 3.0F) < 0.001F,
                    "foodValueOverrides did not replace the registered saturation modifier");

            bonusEffects.set(false);
            player.removeEffect(net.minecraft.world.effect.MobEffects.DAMAGE_BOOST);
            helper.assertFalse(player.hasEffect(net.minecraft.world.effect.MobEffects.DAMAGE_BOOST),
                    "food bonus-effect test did not start from an isolated player state");
            ItemStack omelette = new ItemStack(FarmContent.ITEM_ENTRIES.get("bacon_omelette").get());
            omelette.getItem().finishUsingItem(omelette, helper.getLevel(), player);
            helper.assertFalse(player.hasEffect(net.minecraft.world.effect.MobEffects.DAMAGE_BOOST),
                    "foodsGiveBonusEffects=false still applied an Animania food effect");

            player.getFoodData().setFoodLevel(20);
            eatAnytime.set(false);
            ItemStack fullFood = new ItemStack(FarmContent.ITEM_ENTRIES.get("truffle").get());
            player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, fullFood);
            helper.assertTrue(fullFood.getItem().use(helper.getLevel(), player,
                            net.minecraft.world.InteractionHand.MAIN_HAND).getResult()
                            == net.minecraft.world.InteractionResult.FAIL,
                    "eatFoodAnytime=false allowed a full player to start eating");
            eatAnytime.set(true);
            helper.assertTrue(fullFood.getItem().use(helper.getLevel(), player,
                            net.minecraft.world.InteractionHand.MAIN_HAND).getResult().consumesAction(),
                    "eatFoodAnytime=true did not allow a full player to start eating");
        } finally {
            overrides.set(previousOverrides);
            bonusEffects.set(previousEffects);
            eatAnytime.set(previousEatAnytime);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 260)
    public static void farmFluidsAndCheeseMoldProcess(GameTestHelper helper) {
        AnimaniaGameTestEvidence.mark("animania_farm:farmFluidsAndCheeseMoldProcess");
        helper.assertTrue(FarmFluids.ALL.size() == 6, "legacy milk and honey fluids were not all registered");
        FarmFluids.ALL.values().forEach(fluid -> {
            helper.assertTrue(fluid.source.isPresent() && fluid.flowing.isPresent(), "missing source/flowing fluid " + fluid.id);
            helper.assertTrue(fluid.block.isPresent() && fluid.bucket.isPresent(), "missing fluid block/bucket " + fluid.id);
            helper.assertTrue(fluid.block.get() instanceof com.animania.farm.FarmLegacyFluidBlock,
                    "fluid lost its legacy collision block " + fluid.id);
        });
        BlockPos honeyPos = helper.absolutePos(new BlockPos(0, 1, 0));
        BlockState honeyState = FarmFluids.HONEY.block.get().defaultBlockState();
        helper.getLevel().setBlock(honeyPos, honeyState, 3);
        var honeyTester = helper.makeMockPlayer();
        helper.getLevel().addFreshEntity(honeyTester);
        FarmFluids.HONEY.block.get().entityInside(honeyState, helper.getLevel(), honeyPos, honeyTester);
        helper.assertTrue(honeyTester.hasEffect(net.minecraft.world.effect.MobEffects.REGENERATION),
                "honey fluid did not apply the legacy one-tick regeneration effect");
        helper.assertTrue(honeyState.getMapColor(helper.getLevel(), honeyPos) == net.minecraft.world.level.material.MapColor.COLOR_YELLOW,
                "honey fluid lost its legacy yellow map color");
        BlockState milkState = FarmFluids.MILK_HOLSTEIN.block.get().defaultBlockState();
        helper.assertTrue(milkState.getMapColor(helper.getLevel(), honeyPos) == net.minecraft.world.level.material.MapColor.SNOW,
                "milk fluid lost its legacy snow map color");
        honeyTester.discard();
        for (String milk : new String[]{"milk_holstein", "milk_friesian", "milk_jersey", "milk_goat", "milk_sheep"}) {
            ItemStack milkBucket = new ItemStack(FarmFluids.ALL.get(milk).bucket.get());
            helper.assertTrue(FarmMilkConversionRecipe.isAnimaniaMilkBucket(milkBucket),
                    milk + " bucket was rejected by the vanilla-milk conversion recipe");
            helper.assertTrue(SlopRecipe.matchesInputs(java.util.List.of(
                            new ItemStack(Items.CARROT), new ItemStack(Items.BREAD), milkBucket)),
                    milk + " bucket was rejected by the addon-aware slop recipe");
        }
        helper.assertFalse(FarmMilkConversionRecipe.isAnimaniaMilkBucket(new ItemStack(Items.MILK_BUCKET)),
                "vanilla milk was accepted by an identity conversion recipe");
        BlockPos pos = helper.absolutePos(new BlockPos(2, 1, 0));
        helper.getLevel().setBlock(pos, FarmContent.CHEESE_MOLD.get().defaultBlockState(), 3);
        if (!(helper.getLevel().getBlockEntity(pos) instanceof FarmCheeseMoldBlockEntity mold)) {
            helper.fail("farm cheese mold did not create its block entity");
            return;
        }
        ItemStack rejectedBottle = new ItemStack(FarmContent.ITEM_ENTRIES.get("milk_bottle").get());
        mold.setItem(0, rejectedBottle);
        mold.serverTick();
        helper.assertTrue(mold.getItem(0).is(FarmContent.ITEM_ENTRIES.get("milk_bottle").get())
                        && mold.processTicks() == 0,
                "cheese mold incorrectly accepted the removed milk-bottle shortcut");
        mold.removeItemNoUpdate(0);
        int filled = mold.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.FLUID_HANDLER, null)
                .map(handler -> handler.fill(new net.minecraftforge.fluids.FluidStack(
                                FarmFluids.MILK_FRIESIAN.source.get(), 1000),
                        net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE)).orElse(0);
        helper.assertTrue(filled == 1000, "cheese mold rejected 1000 mB of Friesian milk fluid");
        int originalMaturity = FarmConfig.CHEESE_MATURITY_TIME.get();
        FarmConfig.CHEESE_MATURITY_TIME.set(200);
        helper.runAtTickTime(205, () -> {
            FarmConfig.CHEESE_MATURITY_TIME.set(originalMaturity);
            helper.assertTrue(mold.getItem(0).is(FarmContent.ITEM_ENTRIES.get("friesian_cheese_wheel").get()),
                    "Friesian milk fluid did not process into the legacy Friesian cheese wheel");
            helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void hiveFluidAndCheeseBlockState(GameTestHelper helper) {
        AnimaniaGameTestEvidence.mark("animania_farm:hiveFluidAndCheeseBlockState");
        BlockPos hivePos = helper.absolutePos(new BlockPos(1, 1, 1));
        helper.getLevel().setBlock(hivePos, FarmContent.HIVE.get().defaultBlockState()
                .setValue(com.animania.farm.FarmHiveBlock.FACING, Direction.EAST), 3);
        helper.assertTrue(helper.getLevel().getBlockState(hivePos).getValue(com.animania.farm.FarmHiveBlock.FACING)
                        == Direction.EAST
                        && FarmContent.HIVE.get().rotate(helper.getLevel().getBlockState(hivePos),
                        net.minecraft.world.level.block.Rotation.CLOCKWISE_90)
                        .getValue(com.animania.farm.FarmHiveBlock.FACING) == Direction.SOUTH,
                "hive lost its legacy horizontal facing/rotation state");
        helper.assertTrue(helper.getLevel().getBlockEntity(hivePos) instanceof FarmHiveBlockEntity, "hive block entity was not registered");
        FarmHiveBlockEntity hive = (FarmHiveBlockEntity) helper.getLevel().getBlockEntity(hivePos);
        helper.assertTrue(hive.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.FLUID_HANDLER, null).isPresent(), "hive fluid capability missing");
        helper.assertFalse(hive.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER,
                        Direction.UP).isPresent(),
                "hive exposed a fake item inventory that did not exist in 1.12");
        helper.assertTrue(hive.honeyTank().fill(new net.minecraftforge.fluids.FluidStack(FarmFluids.ALL.get("animania_honey").source.get(), 2000),
                net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE) == 2000, "hive rejected animania honey");
        CompoundTag hiveTag = hive.saveWithFullMetadata();
        helper.assertTrue(hiveTag.contains("Honey") && hiveTag.getInt("NextHoney") > 0,
                "hive did not serialize its honey tank and production timer");
        FarmHiveBlockEntity reloadedHive = FarmHiveBlockEntity.createHive(hivePos,
                helper.getLevel().getBlockState(hivePos));
        reloadedHive.load(hiveTag);
        helper.assertTrue(reloadedHive.honeyAmount() == 2000
                        && reloadedHive.saveWithFullMetadata().getInt("NextHoney") == hiveTag.getInt("NextHoney"),
                "hive honey/timer did not survive NBT reload");

        var player = helper.makeMockPlayer();
        helper.getLevel().addFreshEntity(player);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.GLASS_BOTTLE));
        var hiveHit = new net.minecraft.world.phys.BlockHitResult(
                net.minecraft.world.phys.Vec3.atCenterOf(hivePos), Direction.UP, hivePos, false);
        helper.assertTrue(FarmContent.HIVE.get().use(helper.getLevel().getBlockState(hivePos), helper.getLevel(),
                        hivePos, player, InteractionHand.MAIN_HAND, hiveHit).consumesAction(),
                "hive rejected a glass-bottle honey extraction interaction");
        helper.assertTrue(player.getMainHandItem().is(FarmContent.ITEM_ENTRIES.get("honey_jar").get())
                        && hive.honeyAmount() == 1000,
                "hive extraction did not exchange one bottle for one bucket of honey");
        BlockPos cheesePos = helper.absolutePos(new BlockPos(3, 1, 1));
        helper.getLevel().setBlock(cheesePos, FarmContent.CHEESE_FRIESIAN.get().defaultBlockState(), 3);
        helper.assertTrue(helper.getLevel().getBlockState(cheesePos).getValue(FarmCheeseBlock.BITES) == 0, "cheese did not start at zero bites");
        helper.assertTrue(FarmContent.CHEESE_FRIESIAN.get().getAnalogOutputSignal(helper.getLevel().getBlockState(cheesePos), helper.getLevel(), cheesePos) == 4,
                "cheese comparator level is incorrect");
        double previousVolume = Double.POSITIVE_INFINITY;
        for (int bites = 0; bites < 4; bites++) {
            var state = FarmContent.CHEESE_FRIESIAN.get().defaultBlockState().setValue(FarmCheeseBlock.BITES, bites);
            double volume = state.getShape(helper.getLevel(), cheesePos,
                            net.minecraft.world.phys.shapes.CollisionContext.empty())
                    .toAabbs().stream()
                    .mapToDouble(box -> box.getXsize() * box.getYsize() * box.getZsize())
                    .sum();
            helper.assertTrue(volume < previousVolume, "cheese collision did not shrink at bite stage " + bites);
            previousVolume = volume;
        }
        player.getFoodData().setFoodLevel(10);
        var cheeseHit = new net.minecraft.world.phys.BlockHitResult(
                net.minecraft.world.phys.Vec3.atCenterOf(cheesePos), Direction.UP, cheesePos, false);
        for (int bite = 0; bite < 4; bite++) {
            BlockState cheeseState = helper.getLevel().getBlockState(cheesePos);
            helper.assertTrue(FarmContent.CHEESE_FRIESIAN.get().use(cheeseState, helper.getLevel(), cheesePos,
                            player, InteractionHand.MAIN_HAND, cheeseHit).consumesAction(),
                    "cheese rejected edible bite " + bite);
        }
        helper.assertTrue(helper.getLevel().getBlockState(cheesePos).isAir()
                        && player.getFoodData().getFoodLevel() == 18,
                "four cheese bites did not consume the wheel and grant two hunger each");
        player.discard();
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void allSevenLegacyWoolVariantsPlaceAndDropTheirState(GameTestHelper helper) {
        AnimaniaGameTestEvidence.mark("animania_farm:allSevenLegacyWoolVariantsPlaceAndDropTheirState");
        java.util.Set<String> encoded = new java.util.HashSet<>();
        for (FarmWoolBlock.Variant variant : FarmWoolBlock.Variant.values()) {
            ItemStack stack = FarmWoolBlockItem.stack(variant);
            encoded.add(FarmWoolBlockItem.variant(stack).getSerializedName());
        }
        helper.assertTrue(encoded.size() == 7, "the 1.12 wool metadata variants were collapsed");

        BlockPos pos = helper.absolutePos(new BlockPos(5, 1, 0));
        helper.getLevel().setBlock(pos, FarmContent.BLOCK_ENTRIES.get("animania_wool").get().defaultBlockState()
                .setValue(FarmWoolBlock.VARIANT, FarmWoolBlock.Variant.MERINO_WHITE), 3);
        helper.getLevel().destroyBlock(pos, true);
        var drops = helper.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
                new AABB(pos).inflate(1.0D));
        helper.assertTrue(drops.stream().anyMatch(drop ->
                        FarmWoolBlockItem.variant(drop.getItem()) == FarmWoolBlock.Variant.MERINO_WHITE),
                "breaking a wool variant lost its block-state identity");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void farmProbeStatusCoversLegacyAnimalAndFacilityProviders(GameTestHelper helper) {
        AnimaniaAnimalEntity cow = createAnimal(helper, "cow_angus");
        cow.setGender(AnimalGender.FEMALE);
        cow.setAge(0);
        cow.setPregnant(true);
        cow.setMilkReady(true);
        cow.setMateUuid(UUID.randomUUID());
        java.util.Set<String> cowKeys = probeKeys(com.animania.compat.AnimaniaProbeComponents.animal(cow));
        helper.assertTrue(cowKeys.containsAll(java.util.Set.of("jade.animania.animal_state",
                        "text.waila.mated", "jade.animania.pregnancy_remaining", "text.waila.milkable")),
                "cow probe lost mating, pregnancy, milk, or care status");

        AnimaniaAnimalEntity ewe = createAnimal(helper, "ewe_dorper");
        ewe.setAge(0);
        ewe.setSheared(true);
        helper.assertTrue(probeKeys(com.animania.compat.AnimaniaProbeComponents.animal(ewe))
                        .contains("jade.animania.wool_remaining"),
                "sheep probe lost wool-regrowth status");

        AnimaniaAnimalEntity pig = createAnimal(helper, "sow_duroc");
        pig.setPlaying(false);
        helper.assertTrue(probeKeys(com.animania.compat.AnimaniaProbeComponents.animal(pig))
                        .contains("text.waila.bored"),
                "pig probe lost legacy played/bored status");

        AnimaniaAnimalEntity hen = createAnimal(helper, "hen_leghorn");
        helper.assertTrue(probeKeys(com.animania.compat.AnimaniaProbeComponents.animal(hen))
                        .contains("jade.animania.egg_remaining"),
                "hen probe lost egg timer status");

        BlockPos moldPos = helper.absolutePos(new BlockPos(1, 1, 0));
        helper.getLevel().setBlock(moldPos, FarmContent.CHEESE_MOLD.get().defaultBlockState(), 3);
        FarmCheeseMoldBlockEntity mold = (FarmCheeseMoldBlockEntity) helper.getLevel().getBlockEntity(moldPos);
        mold.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.FLUID_HANDLER, Direction.UP)
                .ifPresent(handler -> handler.fill(new net.minecraftforge.fluids.FluidStack(
                                FarmFluids.MILK_HOLSTEIN.source.get(), 1000),
                        net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE));
        mold.serverTick();
        helper.assertTrue(probeKeys(mold.getAnimaniaProbeInfo()).contains("jade.animania.aging"),
                "cheese mold probe lost fluid-aging progress");

        BlockPos hivePos = helper.absolutePos(new BlockPos(2, 1, 0));
        helper.getLevel().setBlock(hivePos, FarmContent.HIVE.get().defaultBlockState(), 3);
        FarmHiveBlockEntity hive = (FarmHiveBlockEntity) helper.getLevel().getBlockEntity(hivePos);
        hive.honeyTank().fill(new net.minecraftforge.fluids.FluidStack(
                FarmFluids.HONEY.source.get(), 250), net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
        helper.assertTrue(probeKeys(hive.getAnimaniaProbeInfo()).contains("jade.animania.fluid_amount"),
                "hive probe lost honey amount");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void allFarmAnimalProfilesApplyAtRuntime(GameTestHelper helper) {
        AnimaniaGameTestEvidence.mark("animania_farm:allFarmAnimalProfilesApplyAtRuntime");
        for (String id : FarmLegacyIds.ALL.stream().filter(value -> !FarmLegacyIds.VEHICLE_IDS.contains(value)).toList()) {
            var profile = com.animania.farm.FarmAnimalProfile.forId(id);
            var type = AnimaniaFarm.ENTITIES.get(id).get();
            helper.assertTrue(Math.abs(type.getWidth() - profile.width()) < 0.0001F
                            && Math.abs(type.getHeight() - profile.height()) < 0.0001F,
                    id + " did not retain its 1.12 collision dimensions");
            AnimaniaAnimalEntity animal = createAnimal(helper, id);
            helper.assertTrue(Math.abs(animal.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH)
                            - profile.maxHealth()) < 0.0001D,
                    id + " did not receive its 1.12 max health");
            helper.assertTrue(Math.abs(animal.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED)
                            - profile.movementSpeed()) < 0.0001D,
                    id + " did not receive its 1.12 movement speed");
            if (profile.attackDamage() > 0.0D) {
                helper.assertTrue(Math.abs(animal.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE)
                                - profile.attackDamage()) < 0.0001D,
                        id + " did not receive its 1.12 attack damage");
            }
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void farmPigRidingAndCarvingKnifeRestoreLegacyInteractions(GameTestHelper helper) {
        AnimaniaGameTestEvidence.mark("animania_farm:farmPigRidingAndCarvingKnifeRestoreLegacyInteractions");
        AnimaniaAnimalEntity pig = createAnimal(helper, "sow_duroc");
        pig.setAge(0);
        pig.setHunger(100);
        pig.setThirst(100);
        pig.moveTo(helper.absolutePos(new BlockPos(1, 1, 1)), 0.0F, 0.0F);
        helper.getLevel().addFreshEntity(pig);
        var rider = helper.makeMockPlayer();
        rider.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.SADDLE));
        helper.assertTrue(pig.mobInteract(rider, InteractionHand.MAIN_HAND).consumesAction() && pig.isSaddled(),
                "adult Farm pig rejected its legacy saddle interaction");
        rider.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.CARROT_ON_A_STICK));
        helper.assertTrue(pig.mobInteract(rider, InteractionHand.MAIN_HAND).consumesAction()
                        && rider.getVehicle() == pig && pig.boost(),
                "saddled Farm pig rejected carrot-on-a-stick riding or boost");
        CompoundTag pigTag = new CompoundTag();
        pig.addAdditionalSaveData(pigTag);
        pig.setSaddled(false);
        pig.readAdditionalSaveData(pigTag);
        helper.assertTrue(pig.isSaddled(), "Farm pig saddle state did not survive NBT reload");

        AnimaniaAnimalEntity bull = createAnimal(helper, "bull_angus");
        bull.setAge(0);
        helper.getLevel().addFreshEntity(bull);
        ItemStack knifeStack = new ItemStack(FarmContent.ITEM_ENTRIES.get("carving_knife").get());
        rider.setItemInHand(InteractionHand.MAIN_HAND, knifeStack);
        int damage = knifeStack.getDamageValue();
        helper.assertTrue(knifeStack.interactLivingEntity(rider, bull, InteractionHand.MAIN_HAND).consumesAction()
                        && bull.isSterilized() && knifeStack.getDamageValue() == damage + 1,
                "carving knife did not sterilize an adult Farm male and consume durability exactly once");
        rider.stopRiding();
        rider.discard();
        pig.discard();
        bull.discard();
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void saddledDraftHorseAcceptsPlayerControlInput(GameTestHelper helper) {
        AnimaniaGameTestEvidence.mark("animania_farm:saddled_draft_horse_accepts_player_control");
        AnimaniaAnimalEntity horse = createAnimal(helper, "mare_draft");
        horse.setAge(0);
        horse.setTamed(true);
        horse.setSaddled(true);
        horse.setHunger(100);
        horse.setThirst(100);
        helper.getLevel().setBlock(helper.absolutePos(new BlockPos(2, 0, 2)),
                Blocks.STONE.defaultBlockState(), 3);
        horse.moveTo(helper.absolutePos(new BlockPos(2, 1, 2)), 0.0F, 0.0F);
        helper.getLevel().addFreshEntity(horse);
        var rider = helper.makeMockPlayer();
        helper.getLevel().addFreshEntity(rider);
        rider.setYRot(0.0F);
        rider.zza = 1.0F;
        rider.xxa = 0.0F;
        helper.assertTrue(rider.startRiding(horse, true),
                "player could not mount a saddled draft horse");
        helper.assertTrue(horse.getControllingPassenger() == rider,
                "saddled draft horse did not expose the player as its controlling passenger");
        helper.assertTrue(Math.abs(horse.getPassengersRidingOffset() - horse.getBbHeight() * 0.60D) < 0.0001D,
                "mare draft horse did not retain the legacy 60 percent riding offset");
        helper.assertTrue(horse.maxUpStep() >= 1.2F,
                "draft horse did not retain the legacy one-block-plus step height");
        helper.assertTrue(horse.getItem(0).is(Items.SADDLE),
                "saddled draft horse did not expose its legacy saddle slot");
        horse.setOnGround(true);
        horse.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
        horse.onPlayerJump(90);
        horse.handleStartJump(90);
        helper.runAtTickTime(1, () -> {
            helper.assertTrue(horse.getDeltaMovement().y > 0.2D,
                    "saddled draft horse ignored the real riding jump pipeline");
            helper.assertTrue(horse.getDeltaMovement().horizontalDistanceSqr() > 0.000001D,
                    "saddled draft horse ignored forward rider input");
            helper.assertTrue(horse.walkAnimation.isMoving(),
                    "ridden draft horse moved without updating its walk animation");
        });
        helper.runAtTickTime(18, () -> {
            helper.assertTrue(horse.getY() < helper.absolutePos(new BlockPos(2, 3, 2)).getY(),
                    "ridden draft horse did not receive normal gravity after jumping");
            rider.stopRiding();
            rider.discard();
            horse.discard();
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 80)
    public static void saddledDraftHorseClimbsOneBlockWithNativeCollision(GameTestHelper helper) {
        AnimaniaGameTestEvidence.mark("animania_farm:saddled_draft_horse_one_block_step");
        AnimaniaAnimalEntity horse = createAnimal(helper, "mare_draft");
        horse.setAge(0);
        horse.setTamed(true);
        horse.setSaddled(true);
        horse.setHunger(100);
        horse.setThirst(100);
        BlockPos start = helper.absolutePos(new BlockPos(2, 1, 2));
        BlockPos step = helper.absolutePos(new BlockPos(2, 1, 3));
        for (int x = 1; x <= 3; x++) {
            for (int z = 1; z <= 8; z++) {
                helper.getLevel().setBlock(helper.absolutePos(new BlockPos(x, 0, z)),
                        Blocks.STONE.defaultBlockState(), 3);
            }
        }
        helper.getLevel().setBlock(step, Blocks.STONE.defaultBlockState(), 3);
        horse.moveTo(start.getX() + 0.5D, start.getY(), start.getZ() + 0.05D, 0.0F, 0.0F);
        horse.setOnGround(true);
        horse.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
        helper.getLevel().addFreshEntity(horse);
        var rider = helper.makeMockPlayer();
        helper.getLevel().addFreshEntity(rider);
        rider.setYRot(0.0F);
        rider.zza = 1.0F;
        helper.assertTrue(rider.startRiding(horse, true), "player could not mount step-test horse");
        double initialZ = horse.getZ();
        // A GameTest mock player is server-side and therefore cannot claim
        // LivingEntity.isControlledByLocalInstance() like a real client. Use
        // the same native Entity.move collision path once to exercise the
        // horse's maxUpStep without pretending the server owns client input.
        helper.runAtTickTime(1, () -> {
            horse.setOnGround(true);
            horse.move(net.minecraft.world.entity.MoverType.SELF,
                    new net.minecraft.world.phys.Vec3(0.0D, 0.0D, 1.0D));
            helper.assertTrue(horse.getZ() > initialZ + 0.50D,
                    "ridden draft horse never crossed the one-block obstacle: z=" + horse.getZ()
                            + ", initial=" + initialZ + ", y=" + horse.getY()
                            + ", delta=" + horse.getDeltaMovement() + ", step=" + horse.maxUpStep());
            helper.assertTrue(horse.getY() >= start.getY() + 0.9D,
                    "ridden draft horse did not climb onto the one-block obstacle: " + horse.getY());
            rider.stopRiding();
            rider.discard();
            horse.discard();
            helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void farmProductionConversionsAndCrowStateFollowLegacyRules(GameTestHelper helper) {
        AnimaniaGameTestEvidence.mark("animania_farm:farmProductionConversionsAndCrowStateFollowLegacyRules");
        var player = helper.makeMockPlayer();
        AnimaniaAnimalEntity cow = createAnimal(helper, "cow_angus");
        cow.setGender(AnimalGender.FEMALE);
        cow.setAge(0);
        cow.setHunger(100);
        cow.setThirst(100);
        cow.setMilkReady(true);
        helper.getLevel().addFreshEntity(cow);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BUCKET));
        helper.assertTrue(cow.mobInteract(player, InteractionHand.MAIN_HAND).consumesAction()
                        && cow.getThirst() == 0
                        && player.getInventory().contains(new ItemStack(Items.MILK_BUCKET)),
                "milking did not produce milk and consume the watered state");
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BUCKET));
        var secondMilk = cow.mobInteract(player, InteractionHand.MAIN_HAND);
        helper.assertTrue(!secondMilk.consumesAction()
                        && player.getMainHandItem().is(Items.BUCKET)
                        && player.getMainHandItem().getCount() == 1,
                "unwatered cow could be milked repeatedly");

        AnimaniaAnimalEntity mare = createAnimal(helper, "mare_draft");
        mare.setGender(AnimalGender.FEMALE);
        mare.setAge(0);
        mare.setHunger(100);
        mare.setThirst(100);
        mare.setMilkReady(true);
        helper.getLevel().addFreshEntity(mare);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BUCKET));
        helper.assertFalse(mare.mobInteract(player, InteractionHand.MAIN_HAND).consumesAction(),
                "mare incorrectly exposed the cow/goat/sheep milking interaction");

        AnimaniaAnimalEntity mooshroom = createAnimal(helper, "cow_mooshroom");
        mooshroom.setGender(AnimalGender.FEMALE);
        mooshroom.setAge(0);
        mooshroom.setHunger(100);
        mooshroom.setThirst(100);
        mooshroom.setMilkReady(true);
        mooshroom.moveTo(helper.absolutePos(new BlockPos(3, 1, 0)), 0.0F, 0.0F);
        helper.getLevel().addFreshEntity(mooshroom);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BOWL));
        helper.assertTrue(mooshroom.mobInteract(player, InteractionHand.MAIN_HAND).consumesAction()
                        && mooshroom.getThirst() == 0
                        && player.getInventory().contains(new ItemStack(Items.MUSHROOM_STEW)),
                "Mooshroom cow did not produce stew and consume watered state");

        AnimaniaAnimalEntity shearTarget = createAnimal(helper, "bull_mooshroom");
        shearTarget.setAge(0);
        shearTarget.moveTo(helper.absolutePos(new BlockPos(5, 1, 0)), 0.0F, 0.0F);
        helper.getLevel().addFreshEntity(shearTarget);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.SHEARS));
        helper.assertTrue(shearTarget.mobInteract(player, InteractionHand.MAIN_HAND).consumesAction()
                        && shearTarget.isRemoved()
                        && !helper.getLevel().getEntitiesOfClass(AnimaniaAnimalEntity.class,
                        shearTarget.getBoundingBox().inflate(1.0D), entity -> "bull_friesian".equals(entity.registryPath())).isEmpty(),
                "adult Mooshroom bull did not transactionally convert to Friesian");

        AnimaniaAnimalEntity pig = createAnimal(helper, "piglet_duroc");
        pig.setCustomName(net.minecraft.network.chat.Component.literal("Storm Pig"));
        pig.moveTo(helper.absolutePos(new BlockPos(7, 1, 0)), 0.0F, 0.0F);
        helper.getLevel().addFreshEntity(pig);
        pig.thunderHit((ServerLevel) helper.getLevel(), EntityType.LIGHTNING_BOLT.create(helper.getLevel()));
        helper.assertTrue(pig.isRemoved() && !helper.getLevel().getEntitiesOfClass(
                        net.minecraft.world.entity.monster.ZombifiedPiglin.class,
                        new AABB(helper.absolutePos(new BlockPos(7, 1, 0))).inflate(1.0D),
                        entity -> entity.isBaby() && entity.hasCustomName()
                                && "Storm Pig".equals(entity.getCustomName().getString())).isEmpty(),
                "lightning did not transactionally convert the Farm pig and preserve child/name state");

        AnimaniaAnimalEntity rooster = createAnimal(helper, "rooster_leghorn");
        CompoundTag crow = new CompoundTag();
        crow.putInt("CrowDuration", 50);
        rooster.readAdditionalSaveData(crow);
        CompoundTag crowSaved = new CompoundTag();
        rooster.addAdditionalSaveData(crowSaved);
        helper.assertTrue(rooster.getCrowDuration() == 50 && crowSaved.getInt("CrowDuration") == 50,
                "rooster crow animation duration did not synchronize and persist");
        player.discard();
        cow.discard();
        mare.discard();
        mooshroom.discard();
        rooster.discard();
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void farmHiveAndHenSchedulersHonorBiomeAndCareGates(GameTestHelper helper) {
        AnimaniaGameTestEvidence.mark("animania_farm:farmHiveAndHenSchedulersHonorBiomeAndCareGates");
        ServerLevel level = (ServerLevel) helper.getLevel();
        level.setDayTime(1000L);
        AnimaniaAnimalEntity hen = createAnimal(helper, "hen_leghorn");
        hen.setGender(AnimalGender.FEMALE);
        hen.setAge(0);
        hen.setHunger(0);
        hen.setThirst(100);
        CompoundTag due = new CompoundTag();
        due.putInt("AnimaniaEggLayTicks", 1);
        due.putBoolean("AnimaniaEggLayInitialized", true);
        hen.readAdditionalSaveData(due);
        hen.setGender(AnimalGender.FEMALE);
        hen.setAge(0);
        hen.setHunger(0);
        hen.setThirst(100);
        helper.getLevel().addFreshEntity(hen);
        helper.assertFalse(hen.tryLayFarmEgg(true), "hungry hen ignored the legacy egg-laying care gate");
        CompoundTag gated = new CompoundTag();
        hen.addAdditionalSaveData(gated);
        helper.assertTrue(gated.getInt("AnimaniaEggLayTicks") == 1,
                "egg timer advanced while the hen failed a care gate");
        hen.setHunger(100);
        level.setDayTime(14000L);
        helper.assertFalse(hen.tryLayFarmEgg(true), "hen laid an egg during the legacy night gate");
        level.setDayTime(1000L);
        helper.assertTrue(hen.tryLayFarmEgg(true), "fed/watered daytime hen failed its due egg lay");

        BlockPos hivePos = helper.absolutePos(new BlockPos(4, 1, 0));
        helper.getLevel().setBlock(hivePos, FarmContent.HIVE.get().defaultBlockState(), 3);
        FarmHiveBlockEntity hive = (FarmHiveBlockEntity) helper.getLevel().getBlockEntity(hivePos);
        var hiveBiomes = FarmConfig.BIOME_TYPES.get("hiveValidBiomeTypes");
        java.util.List<? extends String> previousHiveBiomes = java.util.List.copyOf(hiveBiomes.get());
        hiveBiomes.set(java.util.List.of("HOT", "COLD", "SPARSE", "DENSE", "WET", "DRY",
                "CONIFEROUS", "SPOOKY", "DEAD", "LUSH", "MUSHROOM", "MAGICAL", "RARE", "PLATEAU",
                "MODIFIED", "WATER", "DESERT", "PLAINS", "SWAMP", "SANDY", "SNOWY", "WASTELAND",
                "MOUNTAIN", "OCEAN", "BEACH", "RIVER", "HILLS", "TAIGA", "JUNGLE", "FOREST",
                "SAVANNA", "MESA"));
        CompoundTag hiveDue = hive.saveWithFullMetadata();
        hiveDue.putInt("NextHoney", 1);
        hive.load(hiveDue);
        int before = hive.honeyAmount();
        hive.serverTick();
        helper.assertTrue(hive.honeyAmount() == before + 25,
                "hive did not produce exactly 25 mB in a configured biome");
        CompoundTag reset = hive.saveWithFullMetadata();
        int baseRate = FarmConfig.HIVE_PLAYER_HONEY_RATE.get();
        helper.assertTrue(reset.getInt("NextHoney") >= baseRate && reset.getInt("NextHoney") < baseRate + 100,
                "hive production timer did not reset to configured rate plus 0..99 jitter");
        hiveBiomes.set(previousHiveBiomes);
        hen.discard();
        helper.succeed();
    }

    private static java.util.Set<String> probeKeys(java.util.List<net.minecraft.network.chat.Component> lines) {
        java.util.Set<String> keys = new java.util.HashSet<>();
        for (net.minecraft.network.chat.Component line : lines) {
            if (line.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents translated) {
                keys.add(translated.getKey());
            }
        }
        return keys;
    }

    private static AnimaniaAnimalEntity spawn(GameTestHelper helper, EntityType<? extends AnimaniaAnimalEntity> type, int x) {
        AnimaniaAnimalEntity entity = type.create(helper.getLevel());
        if (entity == null) throw new IllegalStateException("registered farm entity could not be constructed");
        entity.moveTo(helper.absolutePos(new BlockPos(x, 1, 0)), 0.0F, 0.0F);
        helper.getLevel().addFreshEntity(entity);
        entity.setAge(0);
        return entity;
    }

    private static ItemStack farmFoodFor(String id) {
        if (id.startsWith("hen_") || id.startsWith("rooster_") || id.startsWith("chick_")) {
            return new ItemStack(Items.WHEAT_SEEDS);
        }
        if (id.startsWith("sow_") || id.startsWith("hog_") || id.startsWith("piglet_")) {
            return new ItemStack(Items.CARROT);
        }
        return new ItemStack(Items.WHEAT);
    }

    private static AnimaniaAnimalEntity createAnimal(GameTestHelper helper, String id) {
        var created = AnimaniaFarm.ENTITIES.get(id).get().create(helper.getLevel());
        if (!(created instanceof AnimaniaAnimalEntity animal)) {
            throw new IllegalStateException("farm animal could not be constructed: " + id);
        }
        return animal;
    }

    private static AnimaniaAnimalEntity createExtraAnimal(GameTestHelper helper, String id) {
        EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(
                new ResourceLocation("animania_extra", id));
        if (type == null || !(type.create(helper.getLevel()) instanceof AnimaniaAnimalEntity animal)) {
            throw new IllegalStateException("extra animal could not be constructed: " + id);
        }
        animal.setAge(0);
        return animal;
    }

    private static String[] farmAdultsForChild(String childId) {
        String species;
        if (childId.startsWith("calf_")) {
            species = childId.substring("calf_".length());
            return new String[]{"cow_" + species, "bull_" + species};
        }
        if (childId.startsWith("chick_")) {
            species = childId.substring("chick_".length());
            return new String[]{"hen_" + species, "rooster_" + species};
        }
        if (childId.startsWith("kid_")) {
            species = childId.substring("kid_".length());
            return new String[]{"doe_" + species, "buck_" + species};
        }
        if (childId.startsWith("lamb_")) {
            species = childId.substring("lamb_".length());
            return new String[]{"ewe_" + species, "ram_" + species};
        }
        if (childId.startsWith("foal_")) {
            species = childId.substring("foal_".length());
            return new String[]{"mare_" + species, "stallion_" + species};
        }
        if (childId.startsWith("piglet_")) {
            species = childId.substring("piglet_".length());
            return new String[]{"sow_" + species, "hog_" + species};
        }
        return null;
    }

    private static void verifyShearing(GameTestHelper helper, net.minecraft.world.entity.player.Player player,
                                       String id, boolean expected) {
        AnimaniaAnimalEntity animal = createAnimal(helper, id);
        animal.moveTo(helper.absolutePos(new BlockPos(0, 1, 0)), 0.0F, 0.0F);
        animal.setAge(0);
        helper.getLevel().addFreshEntity(animal);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.SHEARS));
        var result = animal.mobInteract(player, InteractionHand.MAIN_HAND);
        helper.assertTrue(animal.isSheared() == expected,
                id + (expected ? " rejected legacy shearing" : " incorrectly accepted shearing"));
        if (expected) helper.assertTrue(result.consumesAction(), id + " did not consume the shearing action");
        animal.discard();
    }

    private static boolean validFarmVariant(String id, String variant) {
        if (id.endsWith("_draft")) return java.util.Set.of("black", "bw1", "bw2", "grey", "red", "white").contains(variant);
        if (id.endsWith("_dorset") || id.endsWith("_merino") || id.endsWith("_suffolk")) {
            return variant.equals("white") || variant.equals("brown");
        }
        if (id.endsWith("_friesian") && (id.startsWith("ewe_") || id.startsWith("ram_") || id.startsWith("lamb_"))) {
            return java.util.Set.of("white", "black", "brown").contains(variant);
        }
        return variant.equals("default");
    }

    private AnimaniaFarmGameTests() { }
}
