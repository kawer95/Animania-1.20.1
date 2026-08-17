package com.animania.extra.gametest;

import com.animania.extra.AnimaniaExtra;
import com.animania.extra.ExtraContent;
import com.animania.extra.ExtraHamsterWheelBlockEntity;
import com.animania.extra.ExtraHamsterWheelMenu;
import com.animania.extra.ExtraLegacyIds;
import com.animania.extra.ExtraSounds;
import com.animania.api.data.AnimalGender;
import com.animania.common.entity.AnimaniaAnimalEntity;
import com.animania.common.entity.goal.AnimaniaTemptGoal;
import com.animania.common.entity.goal.AnimaniaSleepGoal;
import com.animania.common.entity.goal.AnimaniaFollowOwnerGoal;
import com.animania.common.entity.goal.AnimaniaTargetNonTamedGoal;
import com.animania.common.entity.goal.AnimaniaAvoidEntityGoal;
import com.animania.common.entity.goal.AnimaniaFindNestFoodGoal;
import com.animania.gametest.AnimaniaGameTestEvidence;
import com.animania.common.entity.AnimaniaSleepProfiles;
import com.animania.common.entity.goal.AnimaniaSmallCreatureFloatGoal;
import com.animania.common.AnimaniaBlocks;
import com.animania.extra.ExtraConfig;
import com.animania.extra.AnimaniaHamsterBallItem;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.animal.Rabbit;
import net.minecraft.world.entity.monster.Silverfish;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.Direction;
import com.animania.common.item.AnimaniaEntityEggItem;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.UUID;

@GameTestHolder("animania_extra")
@PrefixGameTestTemplate(false)
public final class AnimaniaExtraGameTests {
    @GameTest(template = "empty")
    public static void legacyExtraAttributesSpecialInteractionsAndEnergyPersistence(GameTestHelper helper) {
        AnimaniaAnimalEntity frog = createAnimal(helper, "frog");
        AnimaniaAnimalEntity dart = createAnimal(helper, "dartfrog");
        AnimaniaAnimalEntity ferret = createAnimal(helper, "ferret_grey");
        AnimaniaAnimalEntity hedgehog = createAnimal(helper, "hedgehog");
        AnimaniaAnimalEntity peacock = createAnimal(helper, "peacock_blue");
        AnimaniaAnimalEntity doe = createAnimal(helper, "doe_rex");
        helper.assertTrue(frog.getGender() == AnimalGender.NONE && ferret.getGender() == AnimalGender.NONE
                        && hedgehog.getGender() == AnimalGender.NONE,
                "genderless Extra animals were exposed as male");
        helper.assertTrue(frog.getMaxHealth() == 3.0F && ferret.getMaxHealth() == 8.0F
                        && hedgehog.getMaxHealth() == 8.0F && peacock.getMaxHealth() == 7.0F
                        && doe.getMaxHealth() == 9.0F,
                "Extra family health table differs from 1.12");
        helper.assertTrue(Math.abs(ferret.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED) - 0.35D) < 0.0001D
                        && Math.abs(peacock.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE) - 1.5D) < 0.0001D,
                "Extra speed/attack table differs from 1.12");
        helper.assertTrue(frog.goalSelector.getAvailableGoals().stream().anyMatch(goal -> goal.getGoal() instanceof AnimaniaAvoidEntityGoal)
                        && frog.goalSelector.getAvailableGoals().stream().noneMatch(goal -> goal.getGoal() instanceof com.animania.common.entity.goal.AnimaniaMateGoal),
                "amphibian retained domestic breeding AI or lost avoidance AI");

        var player = helper.makeMockPlayer();
        dart.tick(); // legacy poisonTimer starts at two and becomes usable after its first living tick
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.ARROW));
        helper.assertTrue(dart.mobInteract(player, InteractionHand.MAIN_HAND).consumesAction()
                        && player.getMainHandItem().is(Items.TIPPED_ARROW)
                        && net.minecraft.world.item.alchemy.PotionUtils.getPotion(player.getMainHandItem())
                        == net.minecraft.world.item.alchemy.Potions.POISON,
                "dart frog did not convert an arrow into a poison arrow");
        dart.push(player);
        helper.assertTrue(player.hasEffect(net.minecraft.world.effect.MobEffects.POISON)
                        && player.getEffect(net.minecraft.world.effect.MobEffects.POISON).getAmplifier() == 1,
                "dart frog collision did not apply legacy poison II");
        CompoundTag dartTag = new CompoundTag();
        dart.addAdditionalSaveData(dartTag);
        AnimaniaAnimalEntity loadedDart = createAnimal(helper, "dartfrog");
        loadedDart.readAdditionalSaveData(dartTag);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.ARROW));
        helper.assertTrue(!loadedDart.mobInteract(player, InteractionHand.MAIN_HAND).consumesAction()
                        && player.getMainHandItem().is(Items.ARROW),
                "dart frog poison-arrow cooldown was not persisted");

        player.setShiftKeyDown(false);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.MUTTON));
        helper.assertTrue(ferret.mobInteract(player, InteractionHand.MAIN_HAND).consumesAction()
                        && ferret.isTamed() && player.getUUID().equals(ferret.getOwnerUUID()),
                "configured ferret food did not tame and assign its owner");
        player.setShiftKeyDown(true);
        player.setPose(net.minecraft.world.entity.Pose.CROUCHING);
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        helper.assertTrue(ferret.mobInteract(player, InteractionHand.MAIN_HAND).consumesAction()
                        && AnimaniaAnimalEntity.hasCarriedAnimal(player),
                "tamed ferret could not use the legacy shoulder-carry interaction");
        AnimaniaAnimalEntity.clearCarriedAnimal(player);

        hedgehog.setCustomName(net.minecraft.network.chat.Component.literal("Sanic"));
        hedgehog.tick();
        helper.assertTrue(hedgehog.hasEffect(net.minecraft.world.effect.MobEffects.GLOWING)
                        && hedgehog.getEffect(net.minecraft.world.effect.MobEffects.MOVEMENT_SPEED).getAmplifier() == 6,
                "Sanic did not receive legacy glowing IV and speed VII");
        float health = peacock.getHealth();
        peacock.causeFallDamage(20.0F, 1.0F, helper.getLevel().damageSources().fall());
        helper.assertTrue(peacock.getHealth() == health, "peafowl took fall damage despite the legacy no-fall override");

        BlockPos wheelPos = helper.absolutePos(new BlockPos(6, 1, 1));
        helper.getLevel().setBlock(wheelPos, ExtraContent.HAMSTER_WHEEL.get().defaultBlockState(), 3);
        ExtraHamsterWheelBlockEntity wheel = (ExtraHamsterWheelBlockEntity) helper.getLevel().getBlockEntity(wheelPos);
        var energy = wheel.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.ENERGY)
                .orElseThrow(() -> new IllegalStateException("wheel energy capability missing"));
        energy.receiveEnergy(400, false);
        CompoundTag wheelTag = wheel.saveWithoutMetadata();
        ExtraHamsterWheelBlockEntity loadedWheel = new ExtraHamsterWheelBlockEntity(wheelPos,
                ExtraContent.HAMSTER_WHEEL.get().defaultBlockState());
        loadedWheel.load(wheelTag);
        helper.assertTrue(loadedWheel.energyStored() == 400,
                "hamster wheel truncated persisted energy to one generation tick");

        CompoundTag runner = new CompoundTag();
        createAnimal(helper, "hamster").addAdditionalSaveData(runner);
        wheel.insertHamster(runner);
        wheel.tryInsertFood(new ItemStack(ExtraContent.ITEM_ENTRIES.get("hamster_food").get()));
        BlockPos receiverPos = wheelPos.east();
        helper.getLevel().setBlock(receiverPos, net.minecraft.world.level.block.Blocks.BARREL.defaultBlockState(), 3);
        TestEnergyReceiver receiver = new TestEnergyReceiver(receiverPos,
                net.minecraft.world.level.block.Blocks.BARREL.defaultBlockState());
        helper.getLevel().setBlockEntity(receiver);
        wheel.serverTick();
        helper.assertTrue(receiver.energy.getEnergyStored() > 0,
                "running hamster wheel did not actively push FE into an adjacent receiver");
        helper.succeed();
    }
    @GameTest(template = "empty")
    public static void supporterSneakFeedingUnlocksLegacyGoldenHamster(GameTestHelper helper) {
        AnimaniaAnimalEntity hamster = createAnimal(helper, "hamster");
        hamster.setVariantName("brown");
        hamster.setTamed(true);
        helper.getLevel().addFreshEntity(hamster);

        ServerPlayer ordinary = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(),
                new GameProfile(UUID.fromString("00000000-0000-0000-0000-000000000001"), "OrdinaryTester"));
        ordinary.setShiftKeyDown(true);
        ordinary.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.APPLE, 2));
        hamster.mobInteract(ordinary, InteractionHand.MAIN_HAND);
        helper.assertTrue("brown".equals(hamster.getVariantName()),
                "an unlisted UUID unlocked the supporter-only golden hamster");

        ServerPlayer supporter = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(),
                new GameProfile(UUID.fromString("3507ad5c-d868-453c-90a0-3b8092999d22"), "SupporterTester"));
        supporter.setShiftKeyDown(true);
        supporter.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.APPLE));
        helper.assertTrue(com.animania.common.AnimaniaSupporters.contains(supporter.getUUID())
                        && supporter.isShiftKeyDown(),
                "supporter GameTest fixture did not preserve its source UUID or sneak state");
        hamster.mobInteract(supporter, InteractionHand.MAIN_HAND);
        helper.assertTrue("gold".equals(hamster.getVariantName()),
                "listed supporter UUID did not unlock the legacy golden hamster while sneak-feeding");
        helper.assertTrue(com.animania.common.AnimaniaSupporters.size() == 19,
                "supporter UUID ledger differs from the 1.12 source list");
        hamster.discard();
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void legacyNamedCombatUsesNativeDamageTypesAndExactValues(GameTestHelper helper) {
        AnimaniaAnimalEntity frog = createAnimal(helper, "frog");
        frog.setCustomName(net.minecraft.network.chat.Component.literal("Pepe"));
        var frogTarget = helper.makeMockPlayer();
        frogTarget.setHealth(20.0F);
        helper.assertTrue(frog.doHurtTarget(frogTarget), "Pepe frog attack was rejected");
        helper.assertTrue(Math.abs(frogTarget.getHealth() - 18.0F) < 0.001F,
                "Pepe frog did not retain the legacy effective 2-point hit (the duplicate call is blocked by hurt immunity)");
        helper.assertTrue("pepe".equals(com.animania.common.AnimaniaDamageSources.pepe(helper.getLevel()).getMsgId()),
                "Pepe damage type lost its legacy death-message id");
        helper.assertTrue(Math.abs(frog.getMaxHealth() - 20.0F) < 0.001F
                        && frog.goalSelector.getAvailableGoals().stream().anyMatch(goal ->
                        goal.getGoal() instanceof net.minecraft.world.entity.ai.goal.MeleeAttackGoal),
                "naming a frog Pepe did not install its legacy health/combat profile");

        AnimaniaAnimalEntity rabbit = createAnimal(helper, "doe_rex");
        rabbit.setCustomName(net.minecraft.network.chat.Component.literal("Killer"));
        var rabbitTarget = helper.makeMockPlayer();
        rabbitTarget.setHealth(20.0F);
        helper.assertTrue(rabbit.doHurtTarget(rabbitTarget), "Killer rabbit attack was rejected");
        helper.assertTrue(Math.abs(rabbitTarget.getHealth() - 15.0F) < 0.001F,
                "Killer rabbit did not retain the legacy effective 5-point hit (the duplicate call is blocked by hurt immunity)");
        helper.assertTrue("killer_rabbit".equals(com.animania.common.AnimaniaDamageSources.killerRabbit(helper.getLevel()).getMsgId()),
                "Killer rabbit damage type lost its legacy death-message id");
        helper.assertTrue(Math.abs(rabbit.getMaxHealth() - 50.0F) < 0.001F
                        && rabbit.goalSelector.getAvailableGoals().stream().anyMatch(goal ->
                        goal.getGoal() instanceof net.minecraft.world.entity.ai.goal.MeleeAttackGoal),
                "naming a rabbit Killer did not install its legacy health/combat profile");
        frog.discard();
        rabbit.discard();
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void absentFarmOptionalFeedItemsLoadButNeverMatch(GameTestHelper helper) {
        ResourceLocation farmEgg = new ResourceLocation("animania_farm", "brown_egg");
        boolean farmLoaded = net.minecraftforge.fml.ModList.get().isLoaded("animania_farm");
        helper.assertTrue(ForgeRegistries.ITEMS.containsKey(farmEgg) == farmLoaded,
                "optional Farm registry visibility disagreed with the actual addon set");
        var advancement = helper.getLevel().getServer().getAdvancements().getAdvancement(
                new ResourceLocation(AnimaniaExtra.MOD_ID, "animania/feed_ferret_grey"));
        helper.assertTrue(advancement != null, "ferret advancement failed to load without Farm installed");
        if (advancement == null) return;
        var criterion = advancement.getCriteria().get("ferret4");
        helper.assertTrue(criterion != null
                        && criterion.getTrigger() instanceof com.animania.common.advancement.FeedAnimalTrigger.Instance,
                "optional ferret criterion did not deserialize as the Animania trigger");
        if (criterion == null || !(criterion.getTrigger() instanceof com.animania.common.advancement.FeedAnimalTrigger.Instance instance)) return;
        helper.assertTrue(instance.isOptional(), "deserialized optional criterion lost its marker");
        helper.assertFalse(instance.matches(new ItemStack(Items.EGG),
                        new ResourceLocation(AnimaniaExtra.MOD_ID, "ferret_grey")),
                "missing optional Farm food degraded into an any-food match");
        if (farmLoaded) {
            helper.assertTrue(instance.matches(new ItemStack(ForgeRegistries.ITEMS.getValue(farmEgg)),
                            new ResourceLocation(AnimaniaExtra.MOD_ID, "ferret_grey")),
                    "installed optional Farm food did not become an exact ferret criterion match");
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void extraFamilySpawnLimitOnlyRejectsNaturalPopulationGrowth(GameTestHelper helper) {
        AnimaniaGameTestEvidence.mark("animania_extra:extraFamilySpawnLimitOnlyRejectsNaturalPopulationGrowth");
        AnimaniaAnimalEntity existing = createAnimal(helper, "hedgehog");
        AnimaniaAnimalEntity candidate = createAnimal(helper, "hedgehog_albino");
        BlockPos pos = helper.absolutePos(new BlockPos(2, 2, 2));
        existing.moveTo(pos.getX(), pos.getY(), pos.getZ(), 0.0F, 0.0F);
        candidate.moveTo(pos.getX() + 1.0D, pos.getY(), pos.getZ(), 0.0F, 0.0F);
        helper.getLevel().addFreshEntity(existing);
        int previous = ExtraConfig.SPAWN_LIMIT_HEDGEHOGS.get();
        ExtraConfig.SPAWN_LIMIT_HEDGEHOGS.set(1);
        var natural = new net.minecraftforge.event.entity.living.MobSpawnEvent.PositionCheck(
                candidate, helper.getLevel(), net.minecraft.world.entity.MobSpawnType.NATURAL, null);
        AnimaniaExtra.limitNaturalExtraSpawns(natural);
        var egg = new net.minecraftforge.event.entity.living.MobSpawnEvent.PositionCheck(
                candidate, helper.getLevel(), net.minecraft.world.entity.MobSpawnType.SPAWN_EGG, null);
        AnimaniaExtra.limitNaturalExtraSpawns(egg);
        ExtraConfig.SPAWN_LIMIT_HEDGEHOGS.set(previous);
        helper.assertTrue(natural.getResult() == net.minecraftforge.eventbus.api.Event.Result.DENY,
                "hedgehog natural spawn ignored spawnLimitHedgehogs");
        helper.assertTrue(egg.getResult() != net.minecraftforge.eventbus.api.Event.Result.DENY,
                "hedgehog spawn limit blocked a spawn egg");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void vanillaRabbitReplacementHonorsConfigAndPreservesUuid(GameTestHelper helper) {
        AnimaniaGameTestEvidence.mark("animania_extra:vanillaRabbitReplacementHonorsConfigAndPreservesUuid");
        var level = helper.getLevel();
        var rabbit = net.minecraft.world.entity.EntityType.RABBIT.create(level);
        if (rabbit == null) {
            helper.fail("vanilla rabbit fixture could not be constructed");
            return;
        }
        rabbit.moveTo(helper.absolutePos(new BlockPos(0, 1, 0)), 0.0F, 0.0F);
        java.util.UUID originalUuid = rabbit.getUUID();
        boolean previous = ExtraConfig.REPLACE_VANILLA_RABBITS.get();
        try {
            ExtraConfig.REPLACE_VANILLA_RABBITS.set(false);
            var disabled = new net.minecraftforge.event.entity.EntityJoinLevelEvent(rabbit, level);
            boolean disabledCanceled = net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(disabled);
            helper.assertFalse(disabledCanceled || disabled.isCanceled(),
                    "disabled vanilla rabbit replacement still canceled the join event");
            ExtraConfig.REPLACE_VANILLA_RABBITS.set(true);
            var enabled = new net.minecraftforge.event.entity.EntityJoinLevelEvent(rabbit, level);
            boolean enabledCanceled = net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(enabled);
            helper.assertTrue(enabledCanceled || enabled.isCanceled(),
                    "enabled vanilla rabbit replacement did not cancel the original join");
            boolean replacementFound = level.getEntitiesOfClass(AnimaniaAnimalEntity.class,
                            rabbit.getBoundingBox().inflate(16.0D))
                    .stream().anyMatch(entity -> originalUuid.equals(entity.getUUID())
                            && "animania_extra".equals(net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES
                            .getKey(entity.getType()).getNamespace())
                            && (net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getKey(entity.getType()).getPath().startsWith("buck_")
                            || net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getKey(entity.getType()).getPath().startsWith("doe_")
                            || net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getKey(entity.getType()).getPath().startsWith("kit_")));
            helper.assertTrue(replacementFound, "vanilla rabbit replacement lost its UUID or rabbit family");
        } finally {
            ExtraConfig.REPLACE_VANILLA_RABBITS.set(previous);
            rabbit.discard();
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void rabbitsAvoidLegacyPredatorsButSleepersDoNotReact(GameTestHelper helper) {
        AnimaniaGameTestEvidence.mark("animania_extra:generic_ai_avoid_entity");
        AnimaniaAnimalEntity rabbit = createAnimal(helper, "doe_rex");
        rabbit.moveTo(helper.absolutePos(new BlockPos(0, 1, 0)), 0.0F, 0.0F);
        helper.getLevel().addFreshEntity(rabbit);
        rabbit.goalSelector.removeAllGoals(ignored -> true);
        Wolf wolf = EntityType.WOLF.create(helper.getLevel());
        if (wolf == null) {
            helper.fail("wolf target could not be constructed");
            return;
        }
        wolf.moveTo(helper.absolutePos(new BlockPos(2, 1, 0)), 0.0F, 0.0F);
        helper.getLevel().addFreshEntity(wolf);
        AnimaniaAvoidEntityGoal<Wolf> avoid = new AnimaniaAvoidEntityGoal<>(rabbit, Wolf.class, 24.0F, 3.0D, 3.5D);
        helper.assertTrue(avoid.legacyGateAllows() && avoid.distance() == 24.0F
                        && avoid.farSpeed() == 3.0D && avoid.nearSpeed() == 3.5D,
                "rabbit lost its legacy wolf-avoidance profile");
        rabbit.setSleeping(true);
        helper.assertFalse(new AnimaniaAvoidEntityGoal<>(rabbit, Wolf.class, 24.0F, 3.0D, 3.5D).legacyGateAllows(),
                "sleeping rabbit selected the avoid-predator goal");
        wolf.discard();
        rabbit.discard();
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void untamedRodentsSelectLegacyPreyTargets(GameTestHelper helper) {
        AnimaniaAnimalEntity ferret = createAnimal(helper, "ferret_grey");
        ferret.moveTo(helper.absolutePos(new BlockPos(0, 1, 0)), 0.0F, 0.0F);
        helper.getLevel().addFreshEntity(ferret);
        ferret.goalSelector.removeAllGoals(ignored -> true);
        ferret.targetSelector.removeAllGoals(ignored -> true);
        Silverfish silverfish = EntityType.SILVERFISH.create(helper.getLevel());
        if (silverfish == null) {
            helper.fail("silverfish target could not be constructed");
            return;
        }
        silverfish.moveTo(helper.absolutePos(new BlockPos(2, 1, 0)), 0.0F, 0.0F);
        helper.getLevel().addFreshEntity(silverfish);
        AnimaniaTargetNonTamedGoal<Silverfish> prey = new AnimaniaTargetNonTamedGoal<>(ferret, Silverfish.class, false, target -> true);
        boolean preySelected = false;
        for (int attempt = 0; attempt < 200 && !preySelected; attempt++) preySelected = prey.canUse();
        helper.assertTrue(preySelected, "untamed ferret did not select a legacy silverfish target");
        ferret.setTamed(true);
        helper.assertFalse(new AnimaniaTargetNonTamedGoal<>(ferret, Silverfish.class, false, target -> true).canUse(),
                "tamed ferret retained the non-tamed target goal");
        ferret.setSleeping(true);
        helper.assertFalse(new AnimaniaTargetNonTamedGoal<>(ferret, Silverfish.class, false, target -> true).canUse(),
                "sleeping ferret selected a prey target");
        silverfish.discard();
        ferret.discard();
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void rodentsForageChickenEggsAndHedgehogCrops(GameTestHelper helper) {
        AnimaniaGameTestEvidence.mark("animania_extra:ferret_forages_chicken_nest_egg");
        AnimaniaGameTestEvidence.mark("animania_extra:hedgehog_forages_mature_crop");
        BlockPos nestPos = helper.absolutePos(new BlockPos(0, 1, 0));
        helper.getLevel().setBlock(nestPos, AnimaniaBlocks.NEST.get().defaultBlockState(), 3);
        helper.assertTrue(helper.getLevel().getBlockEntity(nestPos) instanceof AnimaniaBlocks.NestEntity,
                "foraging fixture did not create a native nest block entity");
        AnimaniaBlocks.NestEntity nest = (AnimaniaBlocks.NestEntity) helper.getLevel().getBlockEntity(nestPos);
        helper.assertTrue(nest.insertEgg(new ItemStack(Items.EGG), "chicken"), "nest rejected a chicken egg");
        AnimaniaAnimalEntity ferret = createAnimal(helper, "ferret_grey");
        ferret.moveTo(nestPos.getX() + 0.5D, nestPos.getY(), nestPos.getZ() + 0.5D, 0.0F, 0.0F);
        ferret.setHunger(0); ferret.setThirst(0); ferret.markInteracted();
        helper.getLevel().addFreshEntity(ferret);
        AnimaniaFindNestFoodGoal ferretGoal = new AnimaniaFindNestFoodGoal(ferret);
        boolean selected = false;
        for (int attempt = 0; attempt < 200 && !selected; attempt++) selected = ferretGoal.canUse();
        helper.assertTrue(selected && !ferretGoal.targetsCrop(), "ferret did not select a nearby chicken nest");
        if (selected) {
            ferretGoal.start();
            ferretGoal.tick();
            helper.assertTrue(nest.getItem(0).isEmpty() && ferret.getHunger() == 100 && ferret.getThirst() == 100,
                    "ferret failed to consume the server-side nest egg");
        }
        BlockPos cropPos = helper.absolutePos(new BlockPos(4, 1, 0));
        helper.getLevel().setBlock(cropPos, net.minecraft.world.level.block.Blocks.CARROTS.defaultBlockState()
                .setValue(net.minecraft.world.level.block.CarrotBlock.AGE, 7), 3);
        AnimaniaAnimalEntity hedgehog = createAnimal(helper, "hedgehog");
        hedgehog.moveTo(cropPos.getX() + 0.5D, cropPos.getY(), cropPos.getZ() + 0.5D, 0.0F, 0.0F);
        hedgehog.setHunger(0); hedgehog.setThirst(0); hedgehog.markInteracted();
        helper.getLevel().addFreshEntity(hedgehog);
        AnimaniaFindNestFoodGoal cropGoal = new AnimaniaFindNestFoodGoal(hedgehog);
        selected = false;
        for (int attempt = 0; attempt < 200 && !selected; attempt++) selected = cropGoal.canUse();
        helper.assertTrue(selected && cropGoal.targetsCrop(), "hedgehog did not select a mature carrot crop");
        if (selected) { cropGoal.start(); cropGoal.tick(); }
        helper.assertTrue(helper.getLevel().getBlockState(cropPos).is(net.minecraft.world.level.block.Blocks.AIR)
                        && hedgehog.getHunger() == 100,
                "hedgehog failed to uproot the mature crop server-side");
        ferret.discard(); hedgehog.discard();
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void tameableRodentsFollowOwnersWithLegacyDistances(GameTestHelper helper) {
        var owner = helper.makeMockPlayer();
        BlockPos ownerPos = helper.absolutePos(new BlockPos(12, 1, 3));
        owner.moveTo(ownerPos.getX() + 0.5D, ownerPos.getY(), ownerPos.getZ() + 0.5D, 0.0F, 0.0F);
        helper.getLevel().addFreshEntity(owner);
        AnimaniaAnimalEntity hamster = createAnimal(helper, "hamster");
        BlockPos hamsterPos = helper.absolutePos(new BlockPos(0, 1, 3));
        hamster.moveTo(hamsterPos.getX() + 0.5D, hamsterPos.getY(), hamsterPos.getZ() + 0.5D, 0.0F, 0.0F);
        hamster.setTamed(true);
        hamster.setOwnerUUID(owner.getUUID());
        hamster.setSitting(false);
        helper.getLevel().addFreshEntity(hamster);
        hamster.goalSelector.removeAllGoals(ignored -> true);
        hamster.targetSelector.removeAllGoals(ignored -> true);

        AnimaniaFollowOwnerGoal follow = new AnimaniaFollowOwnerGoal(hamster);
        helper.assertTrue(follow.legacyGateAllows() && follow.canUse(),
                "tamed hamster did not acquire its mock owner");
        helper.assertTrue(follow.speed() == 1.0D && follow.minDistance() == 10.0F && follow.maxDistance() == 2.0F,
                "rodent lost its legacy follow-owner distances or speed");
        follow.start();
        helper.assertTrue(follow.owner() == owner, "rodent follow goal did not retain its owner");
        hamster.setSleeping(true);
        helper.assertFalse(new AnimaniaFollowOwnerGoal(hamster).canUse(),
                "sleeping rodent attempted to follow its owner");
        hamster.discard();
        owner.discard();
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void smallCreaturesAnticipateMudBeforeVanillaSwimming(GameTestHelper helper) {
        AnimaniaGameTestEvidence.mark("animania_extra:generic_ai_small_creature_float");
        AnimaniaAnimalEntity hamster = createAnimal(helper, "hamster");
        BlockPos start = helper.absolutePos(new BlockPos(0, 1, 0));
        hamster.moveTo(start.getX() + 0.5D, start.getY(), start.getZ() + 0.5D, 0.0F, 0.0F);
        hamster.setDeltaMovement(1.5D, 0.0D, 0.0D);
        helper.getLevel().addFreshEntity(hamster);
        AnimaniaSmallCreatureFloatGoal goal = new AnimaniaSmallCreatureFloatGoal(hamster);
        BlockPos predicted = goal.predictedPosition();
        helper.getLevel().setBlock(predicted, AnimaniaBlocks.MUD.get().defaultBlockState(), 3);
        helper.assertTrue(goal.isMudAhead() && goal.canUse(),
                "legacy small-creature swimmer did not anticipate mud along its movement vector");
        helper.getLevel().setBlock(predicted, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
        helper.assertFalse(goal.isMudAhead(), "small-creature mud probe stayed true after mud was removed");
        helper.assertTrue(AnimaniaSmallCreatureFloatGoal.legacyPriority(hamster) == 2,
                "hamster lost its legacy swimming goal priority");
        hamster.discard();
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void legacySleepSchedulesAndSecondaryBedsRemainSpeciesSpecific(GameTestHelper helper) {
        AnimaniaAnimalEntity hamster = createAnimal(helper, "hamster");
        AnimaniaAnimalEntity hedgehog = createAnimal(helper, "hedgehog");
        AnimaniaAnimalEntity rabbit = createAnimal(helper, "doe_rex");
        var hamsterProfile = AnimaniaSleepProfiles.resolve(hamster).orElseThrow();
        var hedgehogProfile = AnimaniaSleepProfiles.resolve(hedgehog).orElseThrow();
        var rabbitProfile = AnimaniaSleepProfiles.resolve(rabbit).orElseThrow();
        helper.assertTrue(hamsterProfile.shouldSleep(1000L) && !hamsterProfile.shouldSleep(14000L),
                "hamster lost its legacy daytime sleep schedule");
        helper.assertTrue(hedgehogProfile.shouldSleep(1000L) && !hedgehogProfile.shouldSleep(14000L),
                "hedgehog lost its legacy daytime sleep schedule");
        helper.assertTrue(rabbitProfile.shouldSleep(11000L) && rabbitProfile.shouldSleep(21000L)
                        && !rabbitProfile.shouldSleep(5000L) && !rabbitProfile.shouldSleep(16000L),
                "rabbit lost its two legacy sleep windows");

        var primaryConfig = ExtraConfig.BED_BLOCKS.get("hedgehogBed");
        var secondaryConfig = ExtraConfig.BED_BLOCKS.get("hedgehogBed2");
        String previousPrimary = primaryConfig.get();
        String previousSecondary = secondaryConfig.get();
        primaryConfig.set("minecraft:diamond_block");
        secondaryConfig.set("minecraft:emerald_block");
        helper.getLevel().setDayTime(1000L);
        BlockPos animalPos = helper.absolutePos(new BlockPos(0, 1, 0));
        BlockPos secondaryBed = helper.absolutePos(new BlockPos(3, 0, 0));
        hedgehog.moveTo(animalPos.getX() + 0.5D, animalPos.getY(), animalPos.getZ() + 0.5D, 0.0F, 0.0F);
        helper.getLevel().setBlock(secondaryBed, net.minecraft.world.level.block.Blocks.EMERALD_BLOCK.defaultBlockState(), 3);
        helper.getLevel().addFreshEntity(hedgehog);
        AnimaniaSleepGoal goal = new AnimaniaSleepGoal(hedgehog);
        boolean selected = false;
        for (int attempt = 0; attempt < 2000 && !selected; attempt++) selected = goal.canUse();
        helper.assertTrue(selected && secondaryBed.equals(goal.targetBed()),
                "hedgehog did not fall back to its configured secondary bed");
        if (!selected) return;
        hedgehog.moveTo(secondaryBed.getX() + 0.5D, secondaryBed.getY() + 1.0D,
                secondaryBed.getZ() + 0.5D, 0.0F, 0.0F);
        goal.tick();
        helper.assertTrue(hedgehog.isSleeping(), "hedgehog reached its bed without entering synchronized sleep");
        helper.getLevel().setDayTime(14000L);
        helper.assertFalse(goal.canUse(), "hedgehog attempted daytime-only sleep at night");
        helper.assertFalse(hedgehog.isSleeping(), "hedgehog did not wake outside its legacy sleep schedule");
        primaryConfig.set(previousPrimary);
        secondaryConfig.set(previousSecondary);
        hamster.discard(); hedgehog.discard(); rabbit.discard();
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void allExtraEntitiesHaveRegistryObjects(GameTestHelper helper) {
        helper.assertTrue(AnimaniaExtra.ENTITIES.size() == ExtraLegacyIds.ALL.size(),
                "extra registry count differs from the source-derived legacy ID ledger");
        for (String id : ExtraLegacyIds.ALL) {
            helper.assertTrue(AnimaniaExtra.ENTITIES.containsKey(id), "missing extra registry object: " + id);
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void everyExtraSoundEventIsRegistered(GameTestHelper helper) {
        AnimaniaGameTestEvidence.mark("animania_extra:everyExtraSoundEventIsRegistered");
        helper.assertTrue(ExtraSounds.ALL.size() == 52, "Extra legacy sound ledger count changed");
        for (String id : ExtraSounds.ALL.keySet()) {
            helper.assertTrue(ForgeRegistries.SOUND_EVENTS.containsKey(
                    new ResourceLocation(AnimaniaExtra.MOD_ID, id)),
                    "missing Extra sound registration: " + id);
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void legacyOreDictionaryMembershipUsesModernTags(GameTestHelper helper) {
        assertTagged(helper, "peacock_egg_blue", "animania", "legacy_oredict/egg");
        assertTagged(helper, "blue_peacock_feather", "forge", "feathers");
        assertTagged(helper, "raw_prime_rabbit", "forge", "raw_meats");
        assertTagged(helper, "cooked_prime_rabbit", "forge", "cooked_meats");
        assertTagged(helper, "raw_frog_legs", "animania", "legacy_oredict/listallmeatraw");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void everyNativeLegacySmeltingRecipeLoadsWithExactValues(GameTestHelper helper) {
        assertSmelting(helper, "raw_frog_legs_smelting", "raw_frog_legs", "cooked_frog_legs");
        assertSmelting(helper, "raw_prime_rabbit_smelting", "raw_prime_rabbit", "cooked_prime_rabbit");
        assertSmelting(helper, "raw_peacock_smelting", "raw_peacock", "cooked_peacock");
        assertSmelting(helper, "raw_prime_peacock_smelting", "raw_prime_peacock", "cooked_prime_peacock");
        var omelette = helper.getLevel().getRecipeManager().byKey(new net.minecraft.resources.ResourceLocation(
                AnimaniaExtra.MOD_ID, "peacock_egg_blue_smelting"));
        boolean farmLoaded = net.minecraftforge.fml.ModList.get().isLoaded("animania_farm");
        helper.assertTrue(omelette.isPresent() == farmLoaded,
                "Farm-conditional omelette recipe did not follow the actual addon set");
        if (omelette.orElse(null) instanceof net.minecraft.world.item.crafting.AbstractCookingRecipe recipe) {
            var input = new net.minecraft.world.SimpleContainer(new ItemStack(ExtraContent.ITEM_ENTRIES.get("peacock_egg_blue").get()));
            helper.assertTrue(recipe.matches(input, helper.getLevel())
                            && ForgeRegistries.ITEMS.getKey(recipe.getResultItem(helper.getLevel().registryAccess()).getItem())
                            .equals(new ResourceLocation("animania_farm", "plain_omelette"))
                            && Math.abs(recipe.getExperience() - 0.3F) < 0.0001F && recipe.getCookingTime() == 200,
                    "Farm-conditional peacock omelette recipe changed input/output/time/experience");
        }
        helper.succeed();
    }

    private static void assertSmelting(GameTestHelper helper, String recipeId, String inputId, String outputId) {
        var found = helper.getLevel().getRecipeManager().byKey(
                new net.minecraft.resources.ResourceLocation(AnimaniaExtra.MOD_ID, recipeId)).orElse(null);
        helper.assertTrue(found instanceof net.minecraft.world.item.crafting.AbstractCookingRecipe,
                "missing legacy smelting recipe " + recipeId);
        if (!(found instanceof net.minecraft.world.item.crafting.AbstractCookingRecipe recipe)) return;
        var container = new net.minecraft.world.SimpleContainer(new net.minecraft.world.item.ItemStack(ExtraContent.ITEM_ENTRIES.get(inputId).get()));
        helper.assertTrue(recipe.matches(container, helper.getLevel()), recipeId + " rejects its legacy input");
        helper.assertTrue(recipe.getResultItem(helper.getLevel().registryAccess()).is(ExtraContent.ITEM_ENTRIES.get(outputId).get()), recipeId + " has the wrong output");
        helper.assertTrue(Math.abs(recipe.getExperience() - 0.3F) < 0.0001F && recipe.getCookingTime() == 200,
                recipeId + " changed legacy experience/cooking time");
    }

    private static void assertTagged(GameTestHelper helper, String itemId, String namespace, String path) {
        var item = ExtraContent.ITEM_ENTRIES.get(itemId);
        helper.assertTrue(item != null && item.get().builtInRegistryHolder().is(net.minecraft.tags.TagKey.create(
                        net.minecraft.core.registries.Registries.ITEM,
                        new net.minecraft.resources.ResourceLocation(namespace, path))),
                itemId + " missing modern tag " + namespace + ":" + path);
    }

    @GameTest(template = "empty")
    public static void everyExtraAnimalConstructsAndPersistsCareState(GameTestHelper helper) {
        AnimaniaGameTestEvidence.mark("animania_extra:all_legacy_animals_construct_persist");
        for (String id : ExtraLegacyIds.ALL) {
            EntityType<?> type = AnimaniaExtra.ENTITIES.get(id).get();
            var created = type.create(helper.getLevel());
            helper.assertTrue(created instanceof AnimaniaAnimalEntity,
                    "extra animal did not construct as AnimaniaAnimalEntity: " + id);
            if (!(created instanceof AnimaniaAnimalEntity animal)) return;
            helper.assertTrue(validExtraVariant(id, animal.getVariantName()),
                    "extra animal initialized an invalid visual variant: " + id + "=" + animal.getVariantName());

            animal.setAge(0);
            animal.setHunger(11);
            animal.setThirst(13);
            ItemStack food = extraFoodFor(id);
            if (food.isEmpty()) {
                helper.assertFalse(animal.feed(new ItemStack(Items.WHEAT)),
                        "legacy non-feedable amphibian accepted farm feed: " + id);
            } else {
                helper.assertTrue(animal.feed(food), "extra animal rejected configured food: " + id);
                helper.assertTrue(animal.getHunger() == 31, "extra animal hunger did not increase by 20: " + id);
            }
            helper.assertFalse(animal.feed(new ItemStack(Items.DIAMOND)),
                    "extra animal accepted an unrelated item as food: " + id);
            helper.assertTrue(animal.drink(new ItemStack(Items.WATER_BUCKET)),
                    "extra animal rejected water: " + id);

            String variant = "roundtrip_" + id;
            animal.setVariantName(variant);
            CompoundTag tag = new CompoundTag();
            animal.addAdditionalSaveData(tag);
            animal.setVariantName("mutated");
            animal.setHunger(1);
            animal.setThirst(1);
            animal.readAdditionalSaveData(tag);
            helper.assertTrue(variant.equals(animal.getVariantName()), "extra animal lost variant NBT: " + id);
            helper.assertTrue(animal.getHunger() == (food.isEmpty() ? 11 : 31) && animal.getThirst() == 100,
                    "extra animal lost care-state NBT: " + id);
            animal.discard();
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void everyExtraBreedResolvesItsLegacyChildType(GameTestHelper helper) {
        for (String childId : ExtraLegacyIds.ALL) {
            String[] adults = extraAdultsForChild(childId);
            if (adults == null) continue;
            AnimaniaGameTestEvidence.mark("animania_extra:breed_child:" + childId);
            AnimaniaAnimalEntity female = createAnimal(helper, adults[0]);
            AnimaniaAnimalEntity male = createAnimal(helper, adults[1]);
            female.setAge(0);
            male.setAge(0);
            female.setGender(AnimalGender.FEMALE);
            male.setGender(AnimalGender.MALE);
            helper.assertTrue(female.feed(extraFoodFor(adults[0])) && male.feed(extraFoodFor(adults[1])),
                    "extra breeding pair rejected configured food: " + childId);
            helper.assertTrue(female.canBreedWith(male), "extra pair did not recognize matching breed: " + childId);
            AgeableMob child = female.getBreedOffspring((ServerLevel) helper.getLevel(), male);
            helper.assertTrue(child != null && child.getType() == AnimaniaExtra.ENTITIES.get(childId).get(),
                    "extra pair resolved the wrong child registry type: " + childId);
            female.spawnChildFromBreeding((ServerLevel) helper.getLevel(), male);
            helper.assertTrue(female.isPregnant(), "extra female did not enter pregnancy: " + childId);
            female.discard();
            male.discard();
            if (child != null) child.discard();
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void extraTemptationUsesLiveSpeciesFoodRules(GameTestHelper helper) {
        var player = helper.makeMockPlayer();
        player.moveTo(helper.absolutePos(new BlockPos(1, 1, 0)), 0.0F, 0.0F);
        helper.getLevel().addFreshEntity(player);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WHEAT));
        AnimaniaAnimalEntity frog = createAnimal(helper, "dartfrog");
        frog.moveTo(helper.absolutePos(new BlockPos(0, 1, 0)), 0.0F, 0.0F);
        helper.assertFalse(new AnimaniaTemptGoal(frog, 1.0D).canUse(), "dart frog followed farm feed");
        AnimaniaAnimalEntity rabbit = createAnimal(helper, "doe_chinchilla");
        rabbit.moveTo(helper.absolutePos(new BlockPos(0, 1, 0)), 0.0F, 0.0F);
        helper.assertTrue(new AnimaniaTemptGoal(rabbit, 1.0D).canUse(), "rabbit ignored configured wheat food");
        frog.discard();
        rabbit.discard();
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void vanillaRabbitReplacementRetainsWorldBoundarySemantics(GameTestHelper helper) {
        Rabbit rabbit = EntityType.RABBIT.create(helper.getLevel());
        if (rabbit == null) {
            helper.fail("vanilla rabbit could not be constructed");
            return;
        }
        rabbit.moveTo(helper.absolutePos(new BlockPos(0, 1, 0)), 0.0F, 0.0F);
        helper.getLevel().addFreshEntity(rabbit);
        helper.runAtTickTime(2, () -> {
            var entities = helper.getLevel().getEntitiesOfClass(AnimaniaAnimalEntity.class,
                    new net.minecraft.world.phys.AABB(helper.absolutePos(new BlockPos(0, 1, 0))).inflate(2.0D));
            helper.assertTrue(entities.stream().anyMatch(entity -> {
                var id = net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
                return id != null && id.getNamespace().equals(AnimaniaExtra.MOD_ID)
                        && (id.getPath().startsWith("doe_") || id.getPath().startsWith("buck_"));
            }), "vanilla rabbit was not replaced by a registered Animania rabbit");
            helper.assertTrue(helper.getLevel().getEntitiesOfClass(Rabbit.class,
                    new net.minecraft.world.phys.AABB(helper.absolutePos(new BlockPos(0, 1, 0))).inflate(2.0D)).isEmpty(),
                    "vanilla rabbit remained after replacement");
            helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void randomEggsAreRealServerItems(GameTestHelper helper) {
        for (String id : new String[]{"entity_egg_peacock_random", "entity_egg_rabbit_random", "entity_egg_dart_frog"}) {
            helper.assertTrue(ExtraContent.ITEM_ENTRIES.get(id).get() instanceof AnimaniaEntityEggItem,
                    id + " is an inert placeholder instead of an entity egg");
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void peahenLaysVariantEggInNestAndPersistsNestOwner(GameTestHelper helper) {
        AnimaniaAnimalEntity peahen = createAnimal(helper, "peahen_blue");
        BlockPos animalPos = helper.absolutePos(new BlockPos(1, 1, 1));
        BlockPos nestPos = helper.absolutePos(new BlockPos(3, 1, 1));
        peahen.moveTo(animalPos.getX() + 0.5D, animalPos.getY(), animalPos.getZ() + 0.5D, 0, 0);
        helper.getLevel().addFreshEntity(peahen);
        CompoundTag timer = new CompoundTag();
        timer.putInt("AnimaniaEggLayTicks", 1);
        peahen.readAdditionalSaveData(timer);
        peahen.setGender(AnimalGender.FEMALE);
        peahen.setAge(0);
        helper.getLevel().setBlock(nestPos, AnimaniaBlocks.NEST.get().defaultBlockState(), 3);
        helper.assertTrue(peahen.tryLayPeafowlEgg(), "ready blue peahen did not lay in a nearby nest");
        var nest = (AnimaniaBlocks.NestEntity) helper.getLevel().getBlockEntity(nestPos);
        helper.assertTrue(nest.getItem(0).is(ExtraContent.ITEM_ENTRIES.get("peacock_egg_blue").get()),
                "blue peahen did not preserve the blue peacock egg type");
        helper.assertTrue(nest.birdVariant().equals("blue"), "nest lost the laying peahen variant");
        AnimaniaBlocks.NestEntity loaded = new AnimaniaBlocks.NestEntity(nestPos, helper.getLevel().getBlockState(nestPos));
        loaded.load(nest.saveWithoutMetadata());
        helper.assertTrue(loaded.getItem(0).getCount() == 1 && loaded.birdVariant().equals("blue"),
                "peafowl nest contents or owner variant failed NBT round-trip");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void onlyMalePeacockDropsTimedFeather(GameTestHelper helper) {
        AnimaniaGameTestEvidence.mark("animania_extra:onlyMalePeacockDropsTimedFeather");
        AnimaniaAnimalEntity peahen = createAnimal(helper, "peahen_blue");
        CompoundTag femaleTag = new CompoundTag();
        peahen.addAdditionalSaveData(femaleTag);
        femaleTag.putInt("AnimaniaFeatherDropTicks", 1);
        peahen.readAdditionalSaveData(femaleTag);
        peahen.moveTo(helper.absolutePos(new BlockPos(1, 1, 1)), 0.0F, 0.0F);
        helper.getLevel().addFreshEntity(peahen);
        peahen.tick();

        AnimaniaAnimalEntity peacock = createAnimal(helper, "peacock_blue");
        CompoundTag maleTag = new CompoundTag();
        peacock.addAdditionalSaveData(maleTag);
        maleTag.putInt("AnimaniaFeatherDropTicks", 1);
        peacock.readAdditionalSaveData(maleTag);
        peacock.moveTo(helper.absolutePos(new BlockPos(3, 1, 1)), 0.0F, 0.0F);
        helper.getLevel().addFreshEntity(peacock);
        peacock.tick();

        var feathers = helper.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
                new net.minecraft.world.phys.AABB(helper.absolutePos(new BlockPos(2, 1, 1))).inflate(3.0D),
                item -> net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(item.getItem().getItem()) != null
                        && net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(item.getItem().getItem())
                        .getPath().endsWith("peacock_feather"));
        helper.assertTrue(feathers.size() == 1,
                "only the male peacock should drop one feather; found " + feathers.size());
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void extraAnimalCareAndSaveRoundTrip(GameTestHelper helper) {
        @SuppressWarnings("unchecked")
        EntityType<? extends AnimaniaAnimalEntity> type = (EntityType<? extends AnimaniaAnimalEntity>) (EntityType<?>) AnimaniaExtra.ENTITIES.values().iterator().next().get();
        AnimaniaAnimalEntity animal = type.create(helper.getLevel());
        if (animal == null) {
            helper.fail("registered extra entity could not be constructed");
            return;
        }
        animal.moveTo(helper.absolutePos(new BlockPos(0, 1, 0)), 0.0F, 0.0F);
        helper.getLevel().addFreshEntity(animal);
        animal.setAge(0);
        animal.setGender(AnimalGender.FEMALE);
        animal.setHunger(5);
        helper.assertTrue(animal.feed(new ItemStack(Items.WHEAT)), "extra animal rejected feed");
        helper.assertTrue(animal.getHunger() > 5, "extra animal hunger did not recover");
        helper.assertFalse(animal.play(new ItemStack(Items.STICK)),
                "extra animal accepted an invented stick-play interaction absent from 1.12");
        helper.assertFalse(animal.isPlaying(), "unrelated extra animal entered a cats/dogs or pig-only play state");
        CompoundTag tag = new CompoundTag();
        animal.setVariantName("extra_regression");
        animal.addAdditionalSaveData(tag);
        animal.setVariantName("mutated");
        animal.readAdditionalSaveData(tag);
        helper.assertTrue("extra_regression".equals(animal.getVariantName()), "extra entity NBT did not restore");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void hamsterWheelGeneratesForgeEnergy(GameTestHelper helper) {
        AnimaniaGameTestEvidence.mark("animania_extra:hamsterWheelGeneratesForgeEnergy");
        BlockPos pos = helper.absolutePos(new BlockPos(2, 1, 0));
        helper.getLevel().setBlock(pos, ExtraContent.HAMSTER_WHEEL.get().defaultBlockState(), 3);
        helper.assertFalse(helper.getLevel().getBlockState(pos).canOcclude(),
                "hamster wheel incorrectly occludes the supporting block and exposes the underground void");
        if (!(helper.getLevel().getBlockEntity(pos) instanceof ExtraHamsterWheelBlockEntity wheel)) {
            helper.fail("hamster wheel did not create its block entity");
            return;
        }
        var itemHandler = wheel.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER, null)
                .orElse(null);
        helper.assertTrue(wheel.getContainerSize() == 1 && wheel.getMaxStackSize() == 16
                        && itemHandler != null && itemHandler.getSlots() == 1 && itemHandler.getSlotLimit(0) == 16,
                "hamster wheel did not retain its legacy one-slot, sixteen-food capacity");
        ItemStack rejected = itemHandler.insertItem(0, new ItemStack(net.minecraft.world.item.Items.STONE, 1), false);
        ItemStack overflow = itemHandler.insertItem(0,
                new ItemStack(ExtraContent.ITEM_ENTRIES.get("hamster_food").get(), 64), false);
        helper.assertTrue(rejected.getCount() == 1 && wheel.getItem(0).getCount() == 16
                        && overflow.getCount() == 48,
                "hamster wheel automation accepted invalid food or exceeded sixteen items");
        wheel.clearContent();
        helper.assertTrue(wheel.tryInsertFood(new ItemStack(ExtraContent.ITEM_ENTRIES.get("hamster_food").get()))
                        && wheel.getItem(0).getCount() == 1,
                "hamster wheel rejected its legacy right-click food insertion path");
        @SuppressWarnings("unchecked")
        EntityType<? extends AnimaniaAnimalEntity> type = (EntityType<? extends AnimaniaAnimalEntity>) (EntityType<?>) AnimaniaExtra.ENTITIES.get("hamster").get();
        AnimaniaAnimalEntity hamster = type.create(helper.getLevel());
        if (hamster == null) {
            helper.fail("hamster entity could not be constructed");
            return;
        }
        hamster.setHunger(100);
        CompoundTag wheelHamster = new CompoundTag();
        hamster.addAdditionalSaveData(wheelHamster);
        helper.assertTrue(wheel.insertHamster(wheelHamster), "hamster wheel rejected its stored hamster NBT");
        wheel.serverTick();
        helper.assertTrue(wheel.isRunning() && wheel.hasHamster(), "stored hamster did not start the wheel");
        helper.assertTrue(helper.getLevel().getEntitiesOfClass(AnimaniaAnimalEntity.class,
                        new net.minecraft.world.phys.AABB(pos).inflate(1.5D)).isEmpty(),
                "hamster wheel duplicated its stored hamster as a live nearby entity");
        helper.assertTrue(wheel.energyStored() > 0, "hamster wheel did not generate Forge energy");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void hamsterBallRollsInsteadOfTeleporting(GameTestHelper helper) {
        AnimaniaGameTestEvidence.mark("animania_extra:hamster_ball_continuous_movement");
        var owner = helper.makeMockPlayer();
        BlockPos ownerPos = helper.absolutePos(new BlockPos(18, 1, 1));
        owner.moveTo(ownerPos.getX() + 0.5D, ownerPos.getY(), ownerPos.getZ() + 0.5D, 0.0F, 0.0F);
        helper.getLevel().addFreshEntity(owner);

        AnimaniaAnimalEntity hamster = createAnimal(helper, "hamster");
        BlockPos start = helper.absolutePos(new BlockPos(1, 1, 1));
        hamster.moveTo(start.getX() + 0.5D, start.getY(), start.getZ() + 0.5D, 0.0F, 0.0F);
        hamster.setTamed(true);
        hamster.setOwnerUUID(owner.getUUID());
        hamster.setInBall(true);
        helper.getLevel().addFreshEntity(hamster);

        double initialX = hamster.getX();
        double initialZ = hamster.getZ();
        AnimaniaFollowOwnerGoal follow = new AnimaniaFollowOwnerGoal(hamster);
        helper.assertTrue(follow.canUse(), "ball hamster did not acquire its distant owner");
        follow.start();
        helper.assertTrue(hamster.distanceToSqr(initialX, hamster.getY(), initialZ) < 0.0001D,
                "hamster ball teleported when the follow goal started");

        helper.runAfterDelay(40, () -> {
            double travelled = hamster.distanceToSqr(initialX, hamster.getY(), initialZ);
            helper.assertTrue(travelled > 0.04D,
                    "hamster ball remained frozen instead of rolling along its navigation path");
            helper.assertTrue(travelled < 100.0D,
                    "hamster ball crossed a teleport-sized distance instead of rolling continuously");
            hamster.discard();
            owner.discard();
            helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void hamsterInteractionMenuAndLegacyStateRoundTrip(GameTestHelper helper) {
        AnimaniaGameTestEvidence.mark("animania_extra:hamster_interaction_menu_and_state");
        var player = helper.makeMockPlayer();
        player.moveTo(helper.absolutePos(new BlockPos(1, 1, 1)), 0.0F, 0.0F);
        helper.getLevel().addFreshEntity(player);
        BlockPos wheelPos = helper.absolutePos(new BlockPos(1, 1, 2));
        helper.getLevel().setBlock(wheelPos, ExtraContent.HAMSTER_WHEEL.get().defaultBlockState(), 3);
        ExtraHamsterWheelBlockEntity wheel = (ExtraHamsterWheelBlockEntity) helper.getLevel().getBlockEntity(wheelPos);
        helper.assertTrue(wheel != null, "wheel block entity was absent");
        var menu = wheel.createMenu(1, player.getInventory(), player);
        helper.assertTrue(menu instanceof ExtraHamsterWheelMenu && menu.slots.size() == 37,
                "wheel did not expose one real slot plus the 36 player inventory slots");
        helper.assertFalse(menu.getSlot(0).mayPlace(new ItemStack(Items.STONE)),
                "wheel menu accepted a non-food item");
        helper.assertTrue(menu.getSlot(0).mayPlace(new ItemStack(ExtraContent.ITEM_ENTRIES.get("hamster_food").get())),
                "wheel menu rejected hamster food");

        AnimaniaAnimalEntity hamster = createAnimal(helper, "hamster");
        hamster.moveTo(helper.absolutePos(new BlockPos(3, 1, 1)), 0.0F, 0.0F);
        helper.getLevel().addFreshEntity(hamster);
        player.setItemInHand(InteractionHand.MAIN_HAND,
                new ItemStack(ExtraContent.ITEM_ENTRIES.get("hamster_food").get(), 2));
        helper.assertTrue(hamster.mobInteract(player, InteractionHand.MAIN_HAND).consumesAction(),
                "hamster feeding interaction was not consumed");
        helper.assertTrue(hamster.isTamed() && player.getUUID().equals(hamster.getOwnerUUID()),
                "legacy first feeding did not tame the hamster to the feeding player");
        helper.assertTrue(hamster.getHamsterFoodStack() == 1 && hamster.isHamsterStanding(),
                "feeding did not fill one cheek-pouch unit and start the alert pose");

        CompoundTag saved = new CompoundTag();
        hamster.addAdditionalSaveData(saved);
        hamster.setHamsterFoodStack(0);
        hamster.setHamsterStanding(false, 0);
        hamster.readAdditionalSaveData(saved);
        helper.assertTrue(hamster.getHamsterFoodStack() == 1 && hamster.isHamsterStanding(),
                "hamster cheek-pouch or standing state was lost on save/reload");

        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        player.setShiftKeyDown(false);
        helper.assertTrue(hamster.mobInteract(player, InteractionHand.MAIN_HAND).consumesAction() && hamster.isSitting(),
                "normal empty-hand interaction did not sit the tamed hamster");
        helper.assertTrue(hamster.mobInteract(player, InteractionHand.MAIN_HAND).consumesAction() && !hamster.isSitting(),
                "second normal empty-hand interaction did not release the tamed hamster");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void hamsterBallAndWheelCarryRoundTripIsServerAuthoritative(GameTestHelper helper) {
        AnimaniaGameTestEvidence.mark("animania_extra:hamster_carry_server_round_trip");
        var player = helper.makeMockPlayer();
        player.moveTo(helper.absolutePos(new BlockPos(0, 1, 0)), 0.0F, 0.0F);
        helper.getLevel().addFreshEntity(player);
        AnimaniaAnimalEntity hamster = createAnimal(helper, "hamster");
        hamster.moveTo(helper.absolutePos(new BlockPos(0, 1, 1)), 0.5F, 0.0F);
        hamster.setTamed(true);
        hamster.setOwnerUUID(player.getUUID());
        helper.getLevel().addFreshEntity(hamster);

        ItemStack colored = AnimaniaHamsterBallItem.stackForColor(
                ExtraContent.ITEM_ENTRIES.get("hamster_ball_colored").get(), 5);
        net.minecraft.world.item.Item coloredItem = colored.getItem();
        helper.assertTrue(colored.hasTag() && colored.getTag().getInt(AnimaniaHamsterBallItem.COLOR_TAG) == 5,
                "coloured hamster ball factory did not write its NBT colour");
        player.setItemInHand(InteractionHand.MAIN_HAND, colored);
        helper.assertTrue(hamster.mobInteract(player, InteractionHand.MAIN_HAND).consumesAction(),
                "hamster ball interaction was not consumed on the server");
        helper.assertTrue(hamster.isInBall() && hamster.getBallColor() == 5,
                "coloured hamster ball state did not synchronize");
        helper.assertTrue(player.getItemInHand(InteractionHand.MAIN_HAND).isEmpty(),
                "hamster ball was not consumed for a non-creative player");

        CompoundTag saved = new CompoundTag();
        hamster.addAdditionalSaveData(saved);
        hamster.setInBall(false);
        hamster.setBallColor(0);
        hamster.readAdditionalSaveData(saved);
        helper.assertTrue(hamster.isInBall() && hamster.getBallColor() == 5,
                "hamster ball state was not restored from NBT");

        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        helper.assertTrue(hamster.mobInteract(player, InteractionHand.MAIN_HAND).consumesAction(),
                "empty-hand hamster ball removal was not consumed");
        helper.assertFalse(hamster.isInBall(), "hamster remained in its ball after removal");
        boolean returned = player.getInventory().items.stream().anyMatch(stack -> stack.is(coloredItem)
                && stack.hasTag() && stack.getTag().getInt(AnimaniaHamsterBallItem.COLOR_TAG) == 5);
        helper.assertTrue(returned, "coloured hamster ball was not returned to the player");

        AnimaniaAnimalEntity carried = createAnimal(helper, "hamster");
        carried.moveTo(helper.absolutePos(new BlockPos(4, 1, 1)), 0.0F, 0.0F);
        carried.setTamed(true);
        carried.setOwnerUUID(player.getUUID());
        carried.setVariantName("wheel_round_trip");
        helper.getLevel().addFreshEntity(carried);
        player.setShiftKeyDown(true);
        player.setPose(net.minecraft.world.entity.Pose.CROUCHING);
        helper.assertTrue(player.isCrouching(), "mock player did not enter the crouching pose");
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        helper.assertTrue(carried.mobInteract(player, InteractionHand.MAIN_HAND).consumesAction(),
                "sneak-pickup of a tamed hamster was not consumed");
        helper.assertTrue(AnimaniaAnimalEntity.hasCarriedAnimal(player) && carried.isRemoved(),
                "hamster carrier state did not replace the live entity");

        BlockPos wheelPos = helper.absolutePos(new BlockPos(4, 1, 3));
        helper.getLevel().setBlock(wheelPos, ExtraContent.HAMSTER_WHEEL.get().defaultBlockState(), 3);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(wheelPos), Direction.UP, wheelPos, false);
        net.minecraft.world.phys.AABB wheelArea = new net.minecraft.world.phys.AABB(wheelPos).inflate(1.5D);
        java.util.Set<java.util.UUID> preexistingWheelHamsters = helper.getLevel()
                .getEntitiesOfClass(AnimaniaAnimalEntity.class, wheelArea).stream()
                .filter(animal -> "wheel_round_trip".equals(animal.getVariantName()))
                .map(net.minecraft.world.entity.Entity::getUUID)
                .collect(java.util.stream.Collectors.toSet());
        ExtraContent.HAMSTER_WHEEL.get().use(helper.getLevel().getBlockState(wheelPos), helper.getLevel(),
                wheelPos, player, InteractionHand.MAIN_HAND, hit);
        helper.assertFalse(AnimaniaAnimalEntity.hasCarriedAnimal(player),
                "hamster carrier state was not cleared when inserted into the wheel");
        ExtraHamsterWheelBlockEntity wheel = (ExtraHamsterWheelBlockEntity) helper.getLevel().getBlockEntity(wheelPos);
        helper.assertTrue(wheel != null && wheel.hasHamster() && wheel.isRunning(),
                "carried hamster was not stored inside the wheel");
        helper.assertTrue(helper.getLevel().getEntitiesOfClass(AnimaniaAnimalEntity.class,
                        wheelArea).stream()
                        .filter(animal -> "wheel_round_trip".equals(animal.getVariantName()))
                        .map(net.minecraft.world.entity.Entity::getUUID)
                        .allMatch(preexistingWheelHamsters::contains),
                "wheel insertion left a duplicate live hamster outside");
        InteractionResult releaseResult = ExtraContent.HAMSTER_WHEEL.get().use(
                helper.getLevel().getBlockState(wheelPos), helper.getLevel(), wheelPos,
                player, InteractionHand.MAIN_HAND, hit);
        helper.assertTrue(releaseResult.consumesAction() && !wheel.hasHamster(),
                "empty-hand sneak-use could not release the stored hamster from the wheel");
        var released = helper.getLevel().getEntitiesOfClass(AnimaniaAnimalEntity.class, wheelArea).stream()
                .filter(animal -> "wheel_round_trip".equals(animal.getVariantName()))
                .filter(animal -> !preexistingWheelHamsters.contains(animal.getUUID()))
                .toList();
        helper.assertTrue(released.size() == 1 && "wheel_round_trip".equals(released.get(0).getVariantName())
                        && player.getUUID().equals(released.get(0).getOwnerUUID())
                        && released.get(0).getHunger() > 0,
                "wheel release did not restore exactly the same hamster state");
        helper.assertTrue(released.get(0).mobInteract(player, InteractionHand.MAIN_HAND).consumesAction(),
                "manually released hamster could not be carried again");
        helper.assertTrue(AnimaniaAnimalEntity.hasCarriedAnimal(player),
                "released hamster did not return to the player's shoulder");
        InteractionResult reinsertResult = ExtraContent.HAMSTER_WHEEL.get().use(
                helper.getLevel().getBlockState(wheelPos), helper.getLevel(), wheelPos,
                player, InteractionHand.MAIN_HAND, hit);
        helper.assertTrue(reinsertResult.consumesAction() && wheel.hasHamster()
                        && !AnimaniaAnimalEntity.hasCarriedAnimal(player),
                "manually released hamster could not be inserted into the wheel again");

        java.util.Set<java.util.UUID> beforeSecondRelease = helper.getLevel()
                .getEntitiesOfClass(AnimaniaAnimalEntity.class, wheelArea).stream()
                .map(net.minecraft.world.entity.Entity::getUUID)
                .collect(java.util.stream.Collectors.toSet());
        helper.assertTrue(ExtraContent.HAMSTER_WHEEL.get().use(
                        helper.getLevel().getBlockState(wheelPos), helper.getLevel(), wheelPos,
                        player, InteractionHand.MAIN_HAND, hit).consumesAction() && !wheel.hasHamster(),
                "reinserted hamster could not be removed for shoulder-release verification");
        AnimaniaAnimalEntity secondRelease = helper.getLevel()
                .getEntitiesOfClass(AnimaniaAnimalEntity.class, wheelArea).stream()
                .filter(animal -> "wheel_round_trip".equals(animal.getVariantName()))
                .filter(animal -> !beforeSecondRelease.contains(animal.getUUID()))
                .filter(animal -> !animal.isRemoved())
                .findFirst().orElseThrow();
        helper.assertTrue(secondRelease.mobInteract(player, InteractionHand.MAIN_HAND).consumesAction()
                        && AnimaniaAnimalEntity.hasCarriedAnimal(player),
                "hamster could not return to the shoulder before normal placement");
        BlockPos releaseTarget = helper.absolutePos(new BlockPos(6, 1, 1));
        helper.assertTrue(com.animania.AnimaniaServerEvents.releaseCarriedAnimal(player, releaseTarget)
                        && !AnimaniaAnimalEntity.hasCarriedAnimal(player),
                "sneak-use placement could not release the hamster from the player's shoulder");
        helper.assertTrue(helper.getLevel().getEntitiesOfClass(AnimaniaAnimalEntity.class,
                        new net.minecraft.world.phys.AABB(releaseTarget).inflate(0.75D)).stream()
                        .anyMatch(animal -> "wheel_round_trip".equals(animal.getVariantName())),
                "shoulder release did not spawn the same hamster at the selected position");
        player.setShiftKeyDown(false);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void hamsterDeathReturnsExactlyOneColourPreservingBall(GameTestHelper helper) {
        AnimaniaGameTestEvidence.mark("animania_extra:hamsterDeathReturnsExactlyOneColourPreservingBall");
        AnimaniaAnimalEntity hamster = createAnimal(helper, "hamster");
        BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
        hamster.moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, 0.0F, 0.0F);
        hamster.setInBall(true);
        hamster.setBallColor(7);
        helper.getLevel().addFreshEntity(hamster);

        helper.assertTrue(hamster.hurt(helper.getLevel().damageSources().generic(), Float.MAX_VALUE),
                "hamster did not accept lethal server damage");
        var balls = helper.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
                        new net.minecraft.world.phys.AABB(pos).inflate(2.0D)).stream()
                .map(net.minecraft.world.entity.item.ItemEntity::getItem)
                .filter(stack -> stack.is(ExtraContent.ITEM_ENTRIES.get("hamster_ball_colored").get()))
                .toList();
        helper.assertTrue(balls.size() == 1, "hamster death returned " + balls.size() + " coloured balls instead of one");
        helper.assertTrue(balls.get(0).hasTag()
                        && balls.get(0).getTag().getInt(AnimaniaHamsterBallItem.COLOR_TAG) == 7,
                "hamster death lost the stored ball colour");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void extraFoodsUseSharedLiveValueOverrides(GameTestHelper helper) {
        var setting = com.animania.common.config.AnimaniaConfig.FOOD_VALUE_OVERRIDES;
        java.util.List<? extends String> previous = setting.get();
        var player = helper.makeMockPlayer();
        try {
            setting.set(java.util.List.of("animania_extra:cooked_frog_legs(2,0.25)"));
            player.getFoodData().setFoodLevel(5);
            player.getFoodData().setSaturation(0.0F);
            ItemStack food = new ItemStack(ExtraContent.ITEM_ENTRIES.get("cooked_frog_legs").get());
            helper.assertTrue(food.getItem() instanceof com.animania.common.item.AnimaniaFoodItem,
                    "Extra edible item bypassed the shared configurable food implementation");
            food.getItem().finishUsingItem(food, helper.getLevel(), player);
            helper.assertTrue(player.getFoodData().getFoodLevel() == 7,
                    "Extra food ignored the live Base foodValueOverrides setting");
            helper.assertTrue(Math.abs(player.getFoodData().getSaturationLevel() - 1.0F) < 0.001F,
                    "Extra food override used the wrong saturation calculation");
        } finally {
            setting.set(previous);
        }
        helper.succeed();
    }

    private static ItemStack extraFoodFor(String id) {
        if (id.equals("dartfrog") || id.equals("frog") || id.equals("toad")) return ItemStack.EMPTY;
        if (id.equals("hamster") || id.startsWith("peacock_") || id.startsWith("peahen_")
                || id.startsWith("peachick_")) return new ItemStack(Items.WHEAT_SEEDS);
        if (id.startsWith("ferret_")) return new ItemStack(Items.MUTTON);
        if (id.startsWith("hedgehog")) return new ItemStack(Items.CARROT);
        return new ItemStack(Items.WHEAT);
    }

    private static AnimaniaAnimalEntity createAnimal(GameTestHelper helper, String id) {
        var created = AnimaniaExtra.ENTITIES.get(id).get().create(helper.getLevel());
        if (!(created instanceof AnimaniaAnimalEntity animal)) {
            throw new IllegalStateException("extra animal could not be constructed: " + id);
        }
        return animal;
    }

    private static String[] extraAdultsForChild(String childId) {
        if (childId.startsWith("kit_")) {
            String species = childId.substring("kit_".length());
            return new String[]{"doe_" + species, "buck_" + species};
        }
        if (childId.startsWith("peachick_")) {
            String species = childId.substring("peachick_".length());
            return new String[]{"peahen_" + species, "peacock_" + species};
        }
        return null;
    }

    private static boolean validExtraVariant(String id, String variant) {
        if (id.equals("hamster")) return java.util.Set.of("black", "brown", "darkbrown", "darkgray", "gray", "plum", "tarou", "white", "gold").contains(variant);
        if (id.equals("dartfrog")) return java.util.Set.of("blue", "red", "yellow").contains(variant);
        if (id.equals("frog")) return variant.equals("default") || variant.equals("green");
        if (id.endsWith("_lop")) return java.util.Set.of("black", "brown", "golden", "olive", "patch_black", "patch_brown", "patch_grey").contains(variant);
        return variant.equals("default");
    }

    private static final class TestEnergyReceiver extends net.minecraft.world.level.block.entity.BlockEntity {
        private final net.minecraftforge.energy.EnergyStorage energy = new net.minecraftforge.energy.EnergyStorage(1000);
        private final net.minecraftforge.common.util.LazyOptional<net.minecraftforge.energy.IEnergyStorage> capability =
                net.minecraftforge.common.util.LazyOptional.of(() -> energy);

        private TestEnergyReceiver(BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
            super(net.minecraft.world.level.block.entity.BlockEntityType.BARREL, pos, state);
        }

        @Override
        public <T> net.minecraftforge.common.util.LazyOptional<T> getCapability(
                net.minecraftforge.common.capabilities.Capability<T> requested, Direction side) {
            return requested == net.minecraftforge.common.capabilities.ForgeCapabilities.ENERGY
                    ? capability.cast() : super.getCapability(requested, side);
        }

        @Override
        public void invalidateCaps() {
            super.invalidateCaps();
            capability.invalidate();
        }
    }

    private AnimaniaExtraGameTests() { }
}
