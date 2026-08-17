package com.animania.catsdogs.gametest;

import net.minecraft.core.Direction;
import com.animania.catsdogs.AnimaniaCatsDogs;
import com.animania.catsdogs.CatsDogsConfig;
import com.animania.catsdogs.CatsDogsContent;
import com.animania.catsdogs.CatsDogsPetSeller;
import com.animania.catsdogs.CatsDogsPetBowlBlockEntity;
import com.animania.catsdogs.CatsDogsPetBowlBlock;
import com.animania.catsdogs.CatsDogsPetFacilityBlockEntity;
import com.animania.catsdogs.CatsDogsLegacyIds;
import com.animania.api.data.AnimalGender;
import com.animania.common.entity.AnimaniaAnimalEntity;
import com.animania.common.entity.goal.AnimaniaTemptGoal;
import com.animania.common.entity.goal.AnimaniaPlayGoal;
import com.animania.common.entity.goal.AnimaniaFollowOwnerGoal;
import com.animania.common.entity.goal.AnimaniaSitGoal;
import com.animania.common.entity.goal.AnimaniaOwnerHurtByTargetGoal;
import com.animania.common.entity.goal.AnimaniaOwnerHurtTargetGoal;
import com.animania.common.entity.goal.AnimaniaTargetNonTamedGoal;
import com.animania.gametest.AnimaniaGameTestEvidence;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.InteractionHand;
import com.animania.common.item.AnimaniaEntityEggItem;
import com.animania.common.AnimaniaItems;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.FluidType;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraftforge.event.village.VillagerTradesEvent;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.AABB;

@GameTestHolder("animania_catsdogs")
@PrefixGameTestTemplate(false)
public final class AnimaniaCatsDogsGameTests {
    @GameTest(template = "empty")
    public static void petSellerProfessionPublishesExecutableEggTrades(GameTestHelper helper) {
        AnimaniaGameTestEvidence.mark("animania_catsdogs:pet_seller_publishes_executable_egg_trades");
        VillagerProfession profession = CatsDogsPetSeller.PET_SELLER.get();
        helper.assertTrue(profession != null, "pet seller profession was not registered");
        Int2ObjectOpenHashMap<java.util.List<VillagerTrades.ItemListing>> trades = new Int2ObjectOpenHashMap<>();
        VillagerTradesEvent event = new VillagerTradesEvent(trades, profession);
        CatsDogsPetSeller.addTrades(event);
        helper.assertTrue(trades.containsKey(1) && trades.containsKey(2) && trades.containsKey(3),
                "pet seller did not populate all three legacy trade tiers");
        var trader = helper.makeMockPlayer();
        int executable = 0;
        for (var listings : trades.values()) {
            for (VillagerTrades.ItemListing listing : listings) {
                var offer = listing.getOffer(trader, RandomSource.create(17L));
                helper.assertTrue(offer != null && offer.getResult().getItem() instanceof AnimaniaEntityEggItem,
                        "pet seller emitted a null or non-entity-egg offer");
                if (offer != null) executable++;
            }
        }
        helper.assertTrue(executable >= 20, "pet seller trade table lost legacy cat/dog family coverage");
        helper.assertTrue(trades.get(1).size() == 18, "Pug did not retain its legacy level-one trade tier");
        var poi = CatsDogsPetSeller.PET_SELLER_POI.getHolder().orElseThrow();
        helper.assertTrue(profession.heldJobSite().test(poi) && profession.acquirableJobSite().test(poi),
                "pet seller profession cannot acquire or retain its registered bowl POI");
        trader.discard();
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void legacyPetAttributesFeedingBreedingAndFacilities(GameTestHelper helper) {
        AnimaniaAnimalEntity male = createPet(helper, "male_labrador");
        AnimaniaAnimalEntity female = createPet(helper, "female_labrador");
        AnimaniaAnimalEntity puppy = createPet(helper, "puppy_labrador");
        helper.assertTrue(male.getMaxHealth() == 20.0F && female.getMaxHealth() == 18.0F
                        && puppy.getMaxHealth() == 12.0F,
                "adult/child dog health attributes differ from 1.12");
        helper.assertTrue(Math.abs(male.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED) - 0.3D) < 0.0001D
                        && Math.abs(puppy.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED) - 0.315D) < 0.0001D
                        && Math.abs(male.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE) - 2.5D) < 0.0001D,
                "dog movement/attack attributes differ from 1.12");

        var owner = helper.makeMockPlayer();
        male.setHunger(10);
        owner.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BEEF, 2));
        helper.assertTrue(male.mobInteract(owner, InteractionHand.MAIN_HAND).consumesAction()
                        && male.isTamed() && owner.getUUID().equals(male.getOwnerUUID())
                        && male.getHunger() == 30 && owner.getMainHandItem().getCount() == 1,
                "configured dog food did not atomically tame, feed and consume once");

        female.setHunger(10);
        female.feed(new ItemStack(Items.BEEF));
        female.setTamed(false);
        male.setTamed(false);
        helper.assertTrue(male.canBreedWith(female), "default config incorrectly requires pets to be tamed for breeding");

        BlockPos litterPos = helper.absolutePos(new BlockPos(2, 1, 2));
        var litter = CatsDogsContent.BLOCK_ENTRIES.get("litter_box").get();
        helper.getLevel().setBlock(litterPos, litter.defaultBlockState(), 3);
        AnimaniaAnimalEntity cat = createPet(helper, "queen_tabby");
        cat.setThirst(9);
        litter.entityInside(helper.getLevel().getBlockState(litterPos), helper.getLevel(), litterPos, cat);
        helper.assertTrue(cat.getThirst() == 9 && !cat.isSleeping(),
                "pet facility retained invented collision hydration/sleep behavior");

        BlockPos bowlPos = helper.absolutePos(new BlockPos(4, 1, 2));
        helper.getLevel().setBlock(bowlPos, CatsDogsContent.PET_BOWL.get().defaultBlockState(), 3);
        var bucket = new net.minecraft.world.entity.item.ItemEntity(helper.getLevel(), bowlPos.getX() + 0.5D,
                bowlPos.getY() + 0.2D, bowlPos.getZ() + 0.5D, new ItemStack(Items.WATER_BUCKET));
        CatsDogsContent.PET_BOWL.get().entityInside(helper.getLevel().getBlockState(bowlPos), helper.getLevel(), bowlPos, bucket);
        var bowl = (CatsDogsPetBowlBlockEntity) helper.getLevel().getBlockEntity(bowlPos);
        helper.assertTrue(bucket.getItem().is(Items.BUCKET) && bowl.fluidAmount(stack -> stack.getFluid() == Fluids.WATER) == 1000,
                "dropped water bucket did not return one empty bucket after filling the bowl");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void companionSpawnLimitOnlyRejectsNaturalPopulationGrowth(GameTestHelper helper) {
        AnimaniaAnimalEntity existing = createPet(helper, "queen_ragdoll");
        AnimaniaAnimalEntity candidate = createPet(helper, "tom_siamese");
        BlockPos pos = helper.absolutePos(new BlockPos(2, 2, 2));
        existing.moveTo(pos.getX(), pos.getY(), pos.getZ(), 0.0F, 0.0F);
        candidate.moveTo(pos.getX() + 1.0D, pos.getY(), pos.getZ(), 0.0F, 0.0F);
        helper.getLevel().addFreshEntity(existing);
        int previous = CatsDogsConfig.SPAWN_LIMIT_CATS.get();
        CatsDogsConfig.SPAWN_LIMIT_CATS.set(1);
        var natural = new net.minecraftforge.event.entity.living.MobSpawnEvent.PositionCheck(
                candidate, helper.getLevel(), net.minecraft.world.entity.MobSpawnType.NATURAL, null);
        AnimaniaCatsDogs.limitNaturalCompanionSpawns(natural);
        var egg = new net.minecraftforge.event.entity.living.MobSpawnEvent.PositionCheck(
                candidate, helper.getLevel(), net.minecraft.world.entity.MobSpawnType.SPAWN_EGG, null);
        AnimaniaCatsDogs.limitNaturalCompanionSpawns(egg);
        CatsDogsConfig.SPAWN_LIMIT_CATS.set(previous);
        helper.assertTrue(natural.getResult() == net.minecraftforge.eventbus.api.Event.Result.DENY,
                "cat natural spawn ignored spawnLimitCats");
        helper.assertTrue(egg.getResult() != net.minecraftforge.eventbus.api.Event.Result.DENY,
                "cat spawn limit blocked a spawn egg");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void companionCombatGoalsHonorOwnerAndSleepGates(GameTestHelper helper) {
        AnimaniaGameTestEvidence.mark("animania_catsdogs:generic_ai_owner_hurt_by_target");
        AnimaniaGameTestEvidence.mark("animania_catsdogs:generic_ai_owner_hurt_target");
        AnimaniaGameTestEvidence.mark("animania_catsdogs:generic_ai_target_non_tamed");
        var owner = helper.makeMockPlayer();
        owner.moveTo(helper.absolutePos(new BlockPos(8, 1, 0)), 0.0F, 0.0F);
        helper.getLevel().addFreshEntity(owner);
        AnimaniaAnimalEntity dog = createPet(helper, "male_labrador");
        dog.moveTo(helper.absolutePos(new BlockPos(0, 1, 0)), 0.0F, 0.0F);
        dog.setTamed(true);
        dog.setOwnerUUID(owner.getUUID());
        helper.getLevel().addFreshEntity(dog);
        dog.goalSelector.removeAllGoals(ignored -> true);
        dog.targetSelector.removeAllGoals(ignored -> true);

        Zombie zombie = EntityType.ZOMBIE.create(helper.getLevel());
        if (zombie == null) {
            helper.fail("zombie target could not be constructed");
            return;
        }
        zombie.moveTo(helper.absolutePos(new BlockPos(2, 1, 0)), 0.0F, 0.0F);
        helper.getLevel().addFreshEntity(zombie);

        owner.setLastHurtByMob(zombie);
        AnimaniaOwnerHurtByTargetGoal ownerHurtBy = new AnimaniaOwnerHurtByTargetGoal(dog);
        helper.assertTrue(ownerHurtBy.canUse(), "dog did not defend its owner against the attacker");
        ownerHurtBy.start();
        helper.assertTrue(dog.getTarget() == zombie, "owner-hurt-by goal did not synchronize the target");

        dog.setSleeping(true);
        helper.assertFalse(new AnimaniaOwnerHurtByTargetGoal(dog).canUse(),
                "sleeping dog reacted to owner-hurt-by state");
        dog.setSleeping(false);
        owner.setLastHurtMob(zombie);
        AnimaniaOwnerHurtTargetGoal ownerHurtTarget = new AnimaniaOwnerHurtTargetGoal(dog);
        helper.assertTrue(ownerHurtTarget.canUse(), "dog did not attack the entity its owner attacked");
        ownerHurtTarget.start();
        helper.assertTrue(dog.getTarget() == zombie, "owner-hurt-target goal did not synchronize the target");

        AnimaniaAnimalEntity untamed = createPet(helper, "queen_tabby");
        untamed.moveTo(helper.absolutePos(new BlockPos(0, 1, 2)), 0.0F, 0.0F);
        helper.getLevel().addFreshEntity(untamed);
        zombie.moveTo(helper.absolutePos(new BlockPos(1, 1, 2)), 0.0F, 0.0F);
        untamed.goalSelector.removeAllGoals(ignored -> true);
        untamed.targetSelector.removeAllGoals(ignored -> true);
        AnimaniaTargetNonTamedGoal<Zombie> prey = new AnimaniaTargetNonTamedGoal<>(untamed, Zombie.class, false, target -> true);
        boolean preySelected = false;
        for (int attempt = 0; attempt < 200 && !preySelected; attempt++) preySelected = prey.canUse();
        helper.assertTrue(preySelected, "untamed cat did not select a nearby hostile target; tamed="
                + untamed.isTamed() + ", sleeping=" + untamed.isSleeping() + ", zombies="
                + helper.getLevel().getEntitiesOfClass(Zombie.class, untamed.getBoundingBox().inflate(16.0D), z -> true).size()
                + ", alive=" + zombie.isAlive() + ", canAttack=" + untamed.canAttack(zombie));
        untamed.setTamed(true);
        helper.assertFalse(new AnimaniaTargetNonTamedGoal<>(untamed, Zombie.class, false, target -> true).canUse(),
                "tamed cat retained the non-tamed prey target goal");

        untamed.discard();
        zombie.discard();
        dog.discard();
        owner.discard();
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void tamedPetsFollowOwnersAndSleepingPetsStaySeated(GameTestHelper helper) {
        AnimaniaGameTestEvidence.mark("animania_catsdogs:generic_ai_follow_owner");
        AnimaniaGameTestEvidence.mark("animania_catsdogs:generic_ai_sit");
        var owner = helper.makeMockPlayer();
        BlockPos ownerPos = helper.absolutePos(new BlockPos(12, 1, 0));
        owner.moveTo(ownerPos.getX() + 0.5D, ownerPos.getY(), ownerPos.getZ() + 0.5D, 0.0F, 0.0F);
        helper.getLevel().addFreshEntity(owner);
        AnimaniaAnimalEntity dog = createPet(helper, "male_labrador");
        BlockPos dogPos = helper.absolutePos(new BlockPos(0, 1, 0));
        dog.moveTo(dogPos.getX() + 0.5D, dogPos.getY(), dogPos.getZ() + 0.5D, 0.0F, 0.0F);
        dog.setTamed(true);
        dog.setOwnerUUID(owner.getUUID());
        dog.setSitting(false);
        helper.getLevel().addFreshEntity(dog);
        dog.goalSelector.removeAllGoals(ignored -> true);
        dog.targetSelector.removeAllGoals(ignored -> true);

        AnimaniaFollowOwnerGoal follow = new AnimaniaFollowOwnerGoal(dog);
        helper.assertTrue(dog.isTamed(), "dog taming flag was not set");
        helper.assertTrue(dog.getOwnerUUID() != null && dog.getOwnerUUID().equals(owner.getUUID()),
                "dog owner UUID was not synchronized: " + dog.getOwnerUUID() + " != " + owner.getUUID());
        helper.assertTrue(follow.legacyGateAllows(), "tamed dog failed follow gate: sleeping=" + dog.isSleeping()
                + ", sitting=" + dog.isSitting() + ", leashed=" + dog.isLeashed() + ", passenger=" + dog.isPassenger()
                + ", water=" + dog.isInWater());
        helper.assertTrue(follow.canUse(), "tamed dog did not acquire its owner; owner entity count="
                + helper.getLevel().getEntitiesOfClass(net.minecraft.world.entity.player.Player.class,
                dog.getBoundingBox().inflate(128.0D), p -> true).size());
        helper.assertTrue(follow.speed() == 1.5D && follow.minDistance() == 5.0F && follow.maxDistance() == 30.0F,
                "dog lost its legacy follow-owner distances or speed");
        follow.start();
        helper.assertTrue(follow.owner() == owner,
                "dog follow goal did not retain its resolved owner after start");

        dog.setSitting(true);
        AnimaniaSitGoal sit = new AnimaniaSitGoal(dog);
        helper.assertTrue(sit.legacyGateAllows(), "tamed dog sitting state was not accepted by GenericAISit");
        dog.setSleeping(true);
        helper.assertFalse(sit.legacyGateAllows(), "sleeping dog was allowed to enter the active sit goal");
        helper.assertFalse(new AnimaniaFollowOwnerGoal(dog).canUse(), "sleeping dog attempted to follow its owner");

        dog.discard();
        owner.discard();
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void allPetEntitiesHaveRegistryObjects(GameTestHelper helper) {
        helper.assertTrue(AnimaniaCatsDogs.ENTITIES.size() == CatsDogsLegacyIds.ALL.size(),
                "cats/dogs registry count differs from the source-derived legacy ID ledger");
        for (String id : CatsDogsLegacyIds.ALL) {
            helper.assertTrue(AnimaniaCatsDogs.ENTITIES.containsKey(id), "missing pet registry object: " + id);
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void everyPetConstructsAndPersistsCareState(GameTestHelper helper) {
        AnimaniaGameTestEvidence.mark("animania_catsdogs:all_legacy_animals_construct_persist");
        for (String id : CatsDogsLegacyIds.ALL) {
            EntityType<?> type = AnimaniaCatsDogs.ENTITIES.get(id).get();
            var created = type.create(helper.getLevel());
            helper.assertTrue(created instanceof AnimaniaAnimalEntity,
                    "pet did not construct as AnimaniaAnimalEntity: " + id);
            if (!(created instanceof AnimaniaAnimalEntity animal)) return;
            helper.assertTrue(validPetVariant(id, animal.getVariantName()),
                    "pet initialized an invalid visual variant: " + id + "=" + animal.getVariantName());

            animal.setAge(0);
            animal.setHunger(17);
            animal.setThirst(19);
            ItemStack food = new ItemStack(isCat(id) ? Items.COD : Items.BEEF);
            helper.assertTrue(animal.feed(food), "pet rejected configured food: " + id);
            helper.assertTrue(animal.getHunger() == 37, "pet hunger did not increase by 20: " + id);
            helper.assertFalse(animal.feed(new ItemStack(Items.DIAMOND)),
                    "pet accepted an unrelated item as food: " + id);
            helper.assertTrue(animal.drink(new ItemStack(Items.WATER_BUCKET)), "pet rejected water: " + id);

            java.util.UUID owner = java.util.UUID.randomUUID();
            String variant = "roundtrip_" + id;
            animal.setTamed(true);
            animal.setOwnerUUID(owner);
            animal.setSitting(true);
            animal.setVariantName(variant);
            CompoundTag tag = new CompoundTag();
            animal.addAdditionalSaveData(tag);
            animal.setTamed(false);
            animal.setOwnerUUID(null);
            animal.setSitting(false);
            animal.setVariantName("mutated");
            animal.setHunger(1);
            animal.setThirst(1);
            animal.readAdditionalSaveData(tag);
            helper.assertTrue(variant.equals(animal.getVariantName()), "pet lost variant NBT: " + id);
            helper.assertTrue(animal.getHunger() == 37 && animal.getThirst() == 100,
                    "pet lost care-state NBT: " + id);
            helper.assertTrue(animal.isTamed() && animal.isSitting() && owner.equals(animal.getOwnerUUID()),
                    "pet lost ownership NBT: " + id);
            animal.discard();
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void everyPetBreedResolvesItsLegacyChildType(GameTestHelper helper) {
        for (String childId : CatsDogsLegacyIds.ALL) {
            String[] adults = petAdultsForChild(childId);
            if (adults == null) continue;
            AnimaniaGameTestEvidence.mark("animania_catsdogs:breed_child:" + childId);
            AnimaniaAnimalEntity female = createPet(helper, adults[0]);
            AnimaniaAnimalEntity male = createPet(helper, adults[1]);
            female.setAge(0);
            male.setAge(0);
            female.setGender(AnimalGender.FEMALE);
            male.setGender(AnimalGender.MALE);
            female.setTamed(true);
            male.setTamed(true);
            ItemStack food = new ItemStack(childId.startsWith("kitten_") ? Items.COD : Items.BEEF);
            helper.assertTrue(female.feed(food) && male.feed(food),
                    "pet breeding pair rejected configured food: " + childId);
            helper.assertTrue(female.canBreedWith(male), "pet pair did not recognize matching breed: " + childId);
            AgeableMob child = female.getBreedOffspring((ServerLevel) helper.getLevel(), male);
            helper.assertTrue(child != null && child.getType() == AnimaniaCatsDogs.ENTITIES.get(childId).get(),
                    "pet pair resolved the wrong child registry type: " + childId);
            female.spawnChildFromBreeding((ServerLevel) helper.getLevel(), male);
            helper.assertTrue(female.isPregnant(), "pet female did not enter pregnancy: " + childId);
            female.discard();
            male.discard();
            if (child != null) child.discard();
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void petTemptationUsesLiveSpeciesFoodRules(GameTestHelper helper) {
        var player = helper.makeMockPlayer();
        player.moveTo(helper.absolutePos(new BlockPos(1, 1, 0)), 0.0F, 0.0F);
        helper.getLevel().addFreshEntity(player);
        AnimaniaAnimalEntity dog = createPet(helper, "female_labrador");
        dog.moveTo(helper.absolutePos(new BlockPos(0, 1, 0)), 0.0F, 0.0F);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.COD));
        helper.assertFalse(new AnimaniaTemptGoal(dog, 1.0D).canUse(), "dog followed cat fish food");
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BEEF));
        helper.assertTrue(new AnimaniaTemptGoal(dog, 1.0D).canUse(), "dog ignored configured raw beef food");

        dog.setTamed(true);
        dog.setSitting(true);
        helper.assertFalse(new AnimaniaTemptGoal(dog, 1.0D).canUse(), "sitting tamed dog followed temptation food");
        dog.setSitting(false);
        AnimaniaAnimalEntity cat = createPet(helper, "queen_tabby");
        cat.moveTo(helper.absolutePos(new BlockPos(0, 1, 1)), 0.0F, 0.0F);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.COD));
        AnimaniaTemptGoal scared = new AnimaniaTemptGoal(cat, AnimaniaTemptGoal.legacySpeed(cat),
                AnimaniaTemptGoal.legacyScaredByMovement(cat));
        helper.assertTrue(scared.canUse() && AnimaniaTemptGoal.legacySpeed(cat) == 0.6D,
                "cat did not start its legacy slow temptation behavior");
        scared.start();
        helper.assertTrue(scared.canContinueToUse(), "stationary nearby player incorrectly frightened tempted cat");
        player.moveTo(player.getX() + 0.2D, player.getY(), player.getZ(), player.getYRot(), player.getXRot());
        helper.assertFalse(scared.canContinueToUse(), "nearby player movement did not frighten tempted cat");
        cat.setSleeping(true);
        helper.assertFalse(new AnimaniaTemptGoal(cat, 0.6D, true).canUse(), "sleeping cat followed temptation food");
        cat.discard();
        dog.discard();
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void randomEggsAreRealServerItems(GameTestHelper helper) {
        helper.assertTrue(CatsDogsContent.ITEM_ENTRIES.get("entity_egg_cat_random").get() instanceof AnimaniaEntityEggItem,
                "cat random egg is an inert placeholder instead of an entity egg");
        helper.assertTrue(CatsDogsContent.ITEM_ENTRIES.get("entity_egg_dog_random").get() instanceof AnimaniaEntityEggItem,
                "dog random egg is an inert placeholder instead of an entity egg");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void petCareSterilizationAndSaveRoundTrip(GameTestHelper helper) {
        @SuppressWarnings("unchecked")
        EntityType<? extends AnimaniaAnimalEntity> type = (EntityType<? extends AnimaniaAnimalEntity>) (EntityType<?>) AnimaniaCatsDogs.ENTITIES.values().iterator().next().get();
        AnimaniaAnimalEntity animal = type.create(helper.getLevel());
        if (animal == null) {
            helper.fail("registered pet entity could not be constructed");
            return;
        }
        animal.moveTo(helper.absolutePos(new BlockPos(0, 1, 0)), 0.0F, 0.0F);
        helper.getLevel().addFreshEntity(animal);
        animal.setAge(0);
        animal.setGender(AnimalGender.FEMALE);
        java.util.UUID owner = java.util.UUID.randomUUID();
        animal.setTamed(true);
        animal.setOwnerUUID(owner);
        animal.setSitting(true);
        helper.assertTrue(animal.isTamed() && animal.isSitting() && owner.equals(animal.getOwnerUUID()), "pet taming state was not synchronized");
        animal.setSterilized(true);
        helper.assertTrue(animal.isSterilized() && !animal.isPregnant(), "sterilization did not block pregnancy");
        helper.assertTrue(animal.play(new ItemStack(Items.STRING)), "pet rejected play item");
        CompoundTag tag = new CompoundTag();
        animal.setVariantName("pet_regression");
        animal.addAdditionalSaveData(tag);
        animal.setVariantName("mutated");
        animal.readAdditionalSaveData(tag);
        helper.assertTrue("pet_regression".equals(animal.getVariantName()), "pet entity NBT did not restore");
        helper.assertTrue(animal.isTamed() && owner.equals(animal.getOwnerUUID()) && animal.isSitting(), "pet taming state did not persist");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void kittensAndPuppiesPlayByChasingEachOther(GameTestHelper helper) {
        AnimaniaGameTestEvidence.mark("animania_catsdogs:generic_ai_play");
        verifyPlayPair(helper, "kitten_tabby", "kitten_siamese");
        verifyPlayPair(helper, "puppy_labrador", "puppy_collie");
        AnimaniaAnimalEntity adult = createPet(helper, "queen_tabby");
        helper.assertTrue(adult.getPlayGoal() == null, "adult cat incorrectly received child play AI");
        adult.discard();
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void petBowlFoodAndWaterCapabilities(GameTestHelper helper) {
        AnimaniaGameTestEvidence.mark("animania_catsdogs:petBowlFoodAndWaterCapabilities");
        BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
        helper.getLevel().setBlock(pos, CatsDogsContent.PET_BOWL.get().defaultBlockState(), 3);
        BlockEntity raw = helper.getLevel().getBlockEntity(pos);
        helper.assertTrue(helper.getLevel().getBlockState(pos).is(CatsDogsContent.PET_BOWL.get()), "pet bowl block state was not placed: " + helper.getLevel().getBlockState(pos));
        helper.assertTrue(helper.getLevel().getBlockState(pos).getRenderShape() == net.minecraft.world.level.block.RenderShape.INVISIBLE,
                "pet bowl base model would overlap its native block-entity renderer");
        helper.assertTrue(raw instanceof CatsDogsPetBowlBlockEntity, "pet bowl block entity was not registered: " + raw);
        CatsDogsPetBowlBlockEntity bowl = (CatsDogsPetBowlBlockEntity) raw;
        helper.assertTrue(bowl.getContainerSize() == 1 && bowl.getMaxStackSize() == 3,
                "pet bowl did not retain its legacy one-slot, three-food capacity");
        helper.assertTrue(bowl.tryInsertFood(new ItemStack(Items.COD)), "pet bowl rejected fish food");
        helper.assertTrue(bowl.getItem(0).getCount() == 1, "pet bowl food count is not one");
        helper.assertTrue(bowl.getCapability(ForgeCapabilities.FLUID_HANDLER, null).map(handler ->
                handler.fill(new FluidStack(Fluids.WATER, FluidType.BUCKET_VOLUME), IFluidHandler.FluidAction.EXECUTE)
                        == FluidType.BUCKET_VOLUME).orElse(false), "pet bowl did not accept water capability");
        helper.assertTrue(!CatsDogsPetBowlBlock.isFoodItem(new ItemStack(AnimaniaItems.WATER_BOTTLE.get())),
                "water bottle was incorrectly treated as solid pet food");
        helper.assertTrue(bowl.getCapability(ForgeCapabilities.FLUID_HANDLER, null).map(handler ->
                handler.fill(new FluidStack(Fluids.LAVA, FluidType.BUCKET_VOLUME), IFluidHandler.FluidAction.SIMULATE) == 0)
                .orElse(false), "pet bowl accepted a non-water automation fluid");
        var itemHandler = bowl.getCapability(ForgeCapabilities.ITEM_HANDLER, null).orElse(null);
        helper.assertTrue(itemHandler != null, "pet bowl item capability missing");
        bowl.clearContent();
        ItemStack overflow = itemHandler.insertItem(0, new ItemStack(Items.COD, 64), false);
        helper.assertTrue(itemHandler.getSlots() == 1 && itemHandler.getSlotLimit(0) == 3
                        && bowl.getItem(0).getCount() == 3 && overflow.getCount() == 61,
                "pet bowl automation exceeded its legacy three-food capacity");
        int facilityOffset = 2;
        for (String id : CatsDogsContent.BLOCK_IDS) {
            BlockPos facilityPos = helper.absolutePos(new BlockPos(facilityOffset++, 1, 1));
            helper.getLevel().setBlock(facilityPos, CatsDogsContent.BLOCK_ENTRIES.get(id).get().defaultBlockState(), 3);
            helper.assertTrue(helper.getLevel().getBlockState(facilityPos).getRenderShape()
                            == net.minecraft.world.level.block.RenderShape.INVISIBLE,
                    id + " base model would overlap its native block-entity renderer");
            helper.assertTrue(helper.getLevel().getBlockEntity(facilityPos) instanceof CatsDogsPetFacilityBlockEntity,
                    id + " did not create the native-rendered pet facility block entity");
            AABB expected = switch (id) {
                case "cat_bed_1" -> new AABB(2 / 16.0, 0, 2 / 16.0, 14 / 16.0, 1 / 16.0, 14 / 16.0);
                case "cat_bed_2" -> new AABB(2 / 16.0, 0, 2 / 16.0, 14 / 16.0, 2 / 16.0, 14 / 16.0);
                case "cat_tower" -> new AABB(0, 0, 0, 1, 1.5, 1);
                case "dog_pillow" -> new AABB(1 / 16.0, 0, 1 / 16.0, 15 / 16.0, 1 / 16.0, 15 / 16.0);
                case "litter_box" -> new AABB(1 / 16.0, 0, 1 / 16.0, 15 / 16.0, 3 / 16.0, 15 / 16.0);
                default -> new AABB(0, 0, 0, 1, 1, 1);
            };
            AABB selection = helper.getLevel().getBlockState(facilityPos)
                    .getShape(helper.getLevel(), facilityPos).bounds();
            AABB collision = helper.getLevel().getBlockState(facilityPos)
                    .getCollisionShape(helper.getLevel(), facilityPos).bounds();
            helper.assertTrue(expected.equals(selection), id + " selection shape differs from 1.12: " + selection);
            helper.assertTrue(expected.equals(collision), id + " collision shape differs from 1.12: " + collision);
        }
        boolean oldAutomation = com.animania.common.config.AnimaniaConfig.ALLOW_TROUGH_AUTOMATION.get();
        com.animania.common.config.AnimaniaConfig.ALLOW_TROUGH_AUTOMATION.set(false);
        boolean sidedAutomationHidden = !bowl.getCapability(ForgeCapabilities.ITEM_HANDLER, Direction.DOWN).isPresent()
                && !bowl.getCapability(ForgeCapabilities.FLUID_HANDLER, Direction.DOWN).isPresent();
        com.animania.common.config.AnimaniaConfig.ALLOW_TROUGH_AUTOMATION.set(oldAutomation);
        helper.assertTrue(sidedAutomationHidden, "pet bowl ignored allowTroughAutomation=false");
        helper.succeed();
    }

    private static boolean isCat(String id) {
        return id.startsWith("tom_") || id.startsWith("queen_") || id.startsWith("kitten_");
    }

    private static AnimaniaAnimalEntity createPet(GameTestHelper helper, String id) {
        var created = AnimaniaCatsDogs.ENTITIES.get(id).get().create(helper.getLevel());
        if (!(created instanceof AnimaniaAnimalEntity animal)) {
            throw new IllegalStateException("pet could not be constructed: " + id);
        }
        return animal;
    }

    private static void verifyPlayPair(GameTestHelper helper, String firstId, String secondId) {
        AnimaniaAnimalEntity first = createPet(helper, firstId);
        AnimaniaAnimalEntity second = createPet(helper, secondId);
        first.moveTo(helper.absolutePos(new BlockPos(0, 1, 0)), 0.0F, 0.0F);
        second.moveTo(helper.absolutePos(new BlockPos(1, 1, 0)), 0.0F, 0.0F);
        helper.getLevel().addFreshEntity(first);
        helper.getLevel().addFreshEntity(second);
        AnimaniaPlayGoal firstGoal = first.getPlayGoal();
        AnimaniaPlayGoal secondGoal = second.getPlayGoal();
        helper.assertTrue(firstGoal != null && secondGoal != null, "child pet has no native play goal: " + firstId);
        if (firstGoal == null || secondGoal == null) return;
        boolean selected = false;
        for (int attempt = 0; attempt < 200 && !selected; attempt++) selected = firstGoal.canUse();
        helper.assertTrue(selected, "child pet did not select a nearby same-kind playmate: " + firstId);
        if (!selected) return;
        firstGoal.start();
        helper.assertTrue(firstGoal.isRunning() && secondGoal.isRunning(), "play pair did not start on both animals");
        helper.assertTrue(firstGoal.playmate() == second && secondGoal.playmate() == first,
                "play pair did not link symmetrically");
        helper.assertTrue(first.isPlaying() && second.isPlaying(), "play state was not synchronized");
        helper.assertTrue(firstGoal.isChaser() && !secondGoal.isChaser(), "initial chaser/runner roles are wrong");
        second.moveTo(first.getX(), first.getY(), first.getZ(), 0.0F, 0.0F);
        firstGoal.tick();
        helper.assertTrue(!firstGoal.isChaser() && secondGoal.isChaser(), "touching did not swap play roles");
        firstGoal.stop();
        helper.assertTrue(!firstGoal.isRunning() && !secondGoal.isRunning(), "stopping one pet left its mate running");
        helper.assertFalse(first.isPlaying() || second.isPlaying(), "play state remained set after pair cleanup");
        first.discard();
        second.discard();
    }

    private static String[] petAdultsForChild(String childId) {
        if (childId.startsWith("kitten_")) {
            String species = childId.substring("kitten_".length());
            return new String[]{"queen_" + species, "tom_" + species};
        }
        if (childId.startsWith("puppy_")) {
            String species = childId.substring("puppy_".length());
            return new String[]{"female_" + species, "male_" + species};
        }
        return null;
    }

    private static boolean validPetVariant(String id, String variant) {
        if (id.endsWith("_chihuahua") || id.endsWith("_collie")) return variant.equals("0") || variant.equals("1");
        if (id.endsWith("_labrador") || id.endsWith("_poodle")) return java.util.Set.of("0", "1", "2").contains(variant);
        if (id.endsWith("_wolf")) return java.util.Set.of("0", "1", "2", "3", "4", "5", "6", "7").contains(variant);
        return variant.equals("default");
    }

    private AnimaniaCatsDogsGameTests() { }
}
