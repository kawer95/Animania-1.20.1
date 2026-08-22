package com.animania.gametest;

import com.animania.common.AnimaniaBlocks;
import com.animania.common.AnimaniaSounds;
import com.animania.common.AnimaniaFluids;
import com.animania.common.block.AnimaniaStorageBlockEntity;
import com.animania.common.block.AnimaniaSaltLickBlock;
import com.animania.common.block.AnimaniaSaltLickBlockEntity;
import com.animania.common.item.AnimaniaSaltLickItem;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import com.animania.common.AnimaniaItems;
import com.animania.common.config.AnimaniaConfig;
import com.animania.common.item.AnimaniaEntityEggItem;
import com.animania.common.entity.AnimaniaAnimalEntity;
import com.animania.common.entity.goal.AnimaniaHerdedByGermanShepherdGoal;
import com.animania.common.recipe.SlopRecipe;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import com.animania.common.block.AnimaniaInvisibleBlock;
import com.animania.common.block.AnimaniaTroughBlock;
import com.animania.common.block.AnimaniaThinBlock;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;

/** Smoke GameTests run by the dedicated Forge GameTest server. */
@GameTestHolder("animania")
@PrefixGameTestTemplate(false)
public final class AnimaniaBaseGameTests {
    @GameTest(template = "empty")
    public static void germanShepherdHerdsFarmRuminantsWhenAllAddonsAreInstalled(GameTestHelper helper) {
        AnimaniaGameTestEvidence.mark("animania:germanShepherdHerdsFarmRuminantsWhenAllAddonsAreInstalled");
        var sheepType = net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getValue(
                new net.minecraft.resources.ResourceLocation("animania_farm", "ewe_dorper"));
        var shepherdType = net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getValue(
                new net.minecraft.resources.ResourceLocation("animania_catsdogs", "male_german_shepherd"));
        helper.assertTrue(sheepType != null && shepherdType != null,
                "full-addon fixture is missing the farm sheep or German shepherd registry entry");
        if (sheepType == null || shepherdType == null) return;
        if (!(sheepType.create(helper.getLevel()) instanceof AnimaniaAnimalEntity sheep)
                || !(shepherdType.create(helper.getLevel()) instanceof AnimaniaAnimalEntity shepherd)) {
            helper.fail("cross-addon herd fixture did not construct Animania animal entities");
            return;
        }
        sheep.moveTo(helper.absolutePos(new BlockPos(1, 1, 1)), 0.0F, 0.0F);
        shepherd.moveTo(helper.absolutePos(new BlockPos(3, 1, 1)), 0.0F, 0.0F);
        shepherd.setTamed(true);
        shepherd.setSitting(false);
        helper.getLevel().addFreshEntity(sheep);
        helper.getLevel().addFreshEntity(shepherd);
        var goal = new AnimaniaHerdedByGermanShepherdGoal(sheep);
        helper.assertTrue(AnimaniaHerdedByGermanShepherdGoal.supports(sheep) && goal.canUse()
                        && goal.shepherd() == shepherd,
                "farm ruminant did not select a nearby tamed German shepherd");
        goal.stop();
        sheep.discard();
        shepherd.discard();
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void legacyOreDictionaryCategoriesResolveThroughModernTags(GameTestHelper helper) {
        AnimaniaGameTestEvidence.mark("animania:legacyOreDictionaryCategoriesResolveThroughModernTags");
        helper.assertTrue(Items.CARROT.builtInRegistryHolder().is(net.minecraftforge.common.Tags.Items.CROPS_CARROT)
                        && Items.POTATO.builtInRegistryHolder().is(net.minecraftforge.common.Tags.Items.CROPS_POTATO)
                        && Items.BEETROOT.builtInRegistryHolder().is(net.minecraftforge.common.Tags.Items.CROPS_BEETROOT),
                "Forge crop tags do not cover the three legacy crop dictionary entries");
        helper.assertTrue(Items.WHEAT_SEEDS.builtInRegistryHolder().is(net.minecraftforge.common.Tags.Items.SEEDS)
                        && Items.MELON_SEEDS.builtInRegistryHolder().is(net.minecraftforge.common.Tags.Items.SEEDS)
                        && Items.PUMPKIN_SEEDS.builtInRegistryHolder().is(net.minecraftforge.common.Tags.Items.SEEDS)
                        && Items.BEETROOT_SEEDS.builtInRegistryHolder().is(net.minecraftforge.common.Tags.Items.SEEDS),
                "Forge seed tag does not cover all four legacy seed dictionary entries");
        helper.assertTrue(Items.BLACK_DYE.builtInRegistryHolder().is(net.minecraftforge.common.Tags.Items.DYES_BLACK)
                        && Items.RED_DYE.builtInRegistryHolder().is(net.minecraftforge.common.Tags.Items.DYES_RED)
                        && Items.GREEN_DYE.builtInRegistryHolder().is(net.minecraftforge.common.Tags.Items.DYES_GREEN)
                        && Items.BROWN_DYE.builtInRegistryHolder().is(net.minecraftforge.common.Tags.Items.DYES_BROWN)
                        && Items.BLUE_DYE.builtInRegistryHolder().is(net.minecraftforge.common.Tags.Items.DYES_BLUE)
                        && Items.PURPLE_DYE.builtInRegistryHolder().is(net.minecraftforge.common.Tags.Items.DYES_PURPLE)
                        && Items.CYAN_DYE.builtInRegistryHolder().is(net.minecraftforge.common.Tags.Items.DYES_CYAN)
                        && Items.LIGHT_GRAY_DYE.builtInRegistryHolder().is(net.minecraftforge.common.Tags.Items.DYES_LIGHT_GRAY)
                        && Items.GRAY_DYE.builtInRegistryHolder().is(net.minecraftforge.common.Tags.Items.DYES_GRAY)
                        && Items.PINK_DYE.builtInRegistryHolder().is(net.minecraftforge.common.Tags.Items.DYES_PINK)
                        && Items.LIME_DYE.builtInRegistryHolder().is(net.minecraftforge.common.Tags.Items.DYES_LIME)
                        && Items.YELLOW_DYE.builtInRegistryHolder().is(net.minecraftforge.common.Tags.Items.DYES_YELLOW)
                        && Items.LIGHT_BLUE_DYE.builtInRegistryHolder().is(net.minecraftforge.common.Tags.Items.DYES_LIGHT_BLUE)
                        && Items.MAGENTA_DYE.builtInRegistryHolder().is(net.minecraftforge.common.Tags.Items.DYES_MAGENTA)
                        && Items.ORANGE_DYE.builtInRegistryHolder().is(net.minecraftforge.common.Tags.Items.DYES_ORANGE)
                        && Items.WHITE_DYE.builtInRegistryHolder().is(net.minecraftforge.common.Tags.Items.DYES_WHITE),
                "Forge color tags did not load the full legacy dye mapping");
        helper.assertTrue(new ItemStack(AnimaniaBlocks.MUD.get()).is(com.animania.api.AnimaniaLegacyTags.MUD_STORAGE)
                        && new ItemStack(Items.SUGAR).is(com.animania.api.AnimaniaLegacyTags.SUGAR)
                        && new ItemStack(Items.BREAD).is(com.animania.api.AnimaniaLegacyTags.BREAD),
                "modern mud/sugar/bread common tags did not resolve");
        helper.assertTrue(new ItemStack(Items.CHICKEN).is(com.animania.api.AnimaniaLegacyTags.RAW_CHICKEN)
                        && new ItemStack(Items.BEEF).is(com.animania.api.AnimaniaLegacyTags.RAW_BEEF)
                        && new ItemStack(Items.PORKCHOP).is(com.animania.api.AnimaniaLegacyTags.RAW_PORK)
                        && new ItemStack(Items.COOKED_CHICKEN).is(com.animania.api.AnimaniaLegacyTags.COOKED_CHICKEN)
                        && new ItemStack(Items.COOKED_BEEF).is(com.animania.api.AnimaniaLegacyTags.COOKED_BEEF)
                        && new ItemStack(Items.COOKED_PORKCHOP).is(com.animania.api.AnimaniaLegacyTags.COOKED_PORK),
                "modern meat common tags do not cover all six legacy meat categories");
        helper.assertTrue(new ItemStack(Items.WHITE_WOOL).is(net.minecraft.tags.ItemTags.WOOL)
                        && new ItemStack(Items.BLACK_WOOL).is(net.minecraft.tags.ItemTags.WOOL),
                "vanilla wool tag did not retain wildcard blockWool semantics");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void everyLegacyBaseSoundEventIsRegistered(GameTestHelper helper) {
        AnimaniaGameTestEvidence.mark("animania:everyLegacyBaseSoundEventIsRegistered");
        helper.assertTrue(AnimaniaSounds.ZAP.isPresent() && AnimaniaSounds.COMBO.isPresent(),
                "legacy Base sound RegistryObjects were not resolved");
        helper.assertTrue(net.minecraftforge.registries.ForgeRegistries.SOUND_EVENTS.containsKey(
                        new net.minecraft.resources.ResourceLocation("animania", "zap")),
                "animania:zap is absent from the live Forge sound registry");
        helper.assertTrue(net.minecraftforge.registries.ForgeRegistries.SOUND_EVENTS.containsKey(
                        new net.minecraft.resources.ResourceLocation("animania", "combo")),
                "animania:combo is absent from the live Forge sound registry");
        helper.succeed();
    }
    private AnimaniaBaseGameTests() {
    }

    @GameTest(template = "empty")
    public static void apiContractLoads(GameTestHelper helper) {
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void freshWaterSquidConfigOnlyGatesNaturalNonOceanSpawns(GameTestHelper helper) {
        BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
        var setting = AnimaniaConfig.SPAWN_FRESH_WATER_SQUIDS;
        boolean previous = setting.get();
        setting.set(false);
        var natural = new net.minecraftforge.event.entity.living.MobSpawnEvent.SpawnPlacementCheck(
                net.minecraft.world.entity.EntityType.SQUID, helper.getLevel(),
                net.minecraft.world.entity.MobSpawnType.NATURAL, pos, helper.getLevel().getRandom(), true);
        new com.animania.AnimaniaServerEvents().onSpawnPlacement(natural);
        boolean ocean = helper.getLevel().getBiome(pos).is(net.minecraft.tags.BiomeTags.IS_OCEAN);
        boolean naturalRuleCorrect = ocean
                ? natural.getResult() != net.minecraftforge.eventbus.api.Event.Result.DENY
                : natural.getResult() == net.minecraftforge.eventbus.api.Event.Result.DENY;
        var spawned = new net.minecraftforge.event.entity.living.MobSpawnEvent.SpawnPlacementCheck(
                net.minecraft.world.entity.EntityType.SQUID, helper.getLevel(),
                net.minecraft.world.entity.MobSpawnType.SPAWN_EGG, pos, helper.getLevel().getRandom(), true);
        new com.animania.AnimaniaServerEvents().onSpawnPlacement(spawned);
        setting.set(previous);
        helper.assertTrue(naturalRuleCorrect, "fresh-water squid rule disagreed with the current biome");
        helper.assertTrue(spawned.getResult() != net.minecraftforge.eventbus.api.Event.Result.DENY,
                "fresh-water squid config blocked a non-natural spawn");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void legacyBaseItemsAndRandomEggRetainRegistrySemantics(GameTestHelper helper) {
        helper.assertTrue(AnimaniaItems.LEGACY_MANUAL.get() instanceof com.animania.common.item.ManualItem,
                "animania_manual did not retain the native manual item");
        helper.assertTrue(AnimaniaItems.LEGACY_SLOP_BUCKET.get() instanceof net.minecraft.world.item.BucketItem,
                "bucket_slop did not retain a real Forge fluid bucket");
        helper.assertTrue(AnimaniaItems.ENTITY_EGG_RANDOM.get() instanceof AnimaniaEntityEggItem,
                "entity_egg_random was not registered as a server-side egg");
        helper.assertTrue(AnimaniaItems.ENTITY_EGG_RANDOM.get().getMaxStackSize() == 64,
                "legacy entity eggs no longer use the 64-item stack size");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void serverAuthoritySmoke(GameTestHelper helper) {
        if (helper.getLevel().isClientSide()) {
            helper.fail("Base GameTests must run on a server level");
            return;
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void storageCapabilitiesPersist(GameTestHelper helper) {
        AnimaniaGameTestEvidence.mark("animania:storageCapabilitiesPersist");
        BlockPos pos = helper.absolutePos(new BlockPos(0, 1, 0));
        helper.getLevel().setBlock(pos, AnimaniaBlocks.TROUGH.get().defaultBlockState(), 3);
        if (!(helper.getLevel().getBlockEntity(pos) instanceof AnimaniaStorageBlockEntity storage)) {
            helper.fail("trough did not create its storage block entity");
            return;
        }
        var items = storage.getCapability(ForgeCapabilities.ITEM_HANDLER).resolve().orElseThrow();
        ItemStack remainder = items.insertItem(0, new ItemStack(Items.WHEAT, 4), false);
        helper.assertTrue(storage.getItem(0).getCount() == 3 && remainder.getCount() == 1,
                "item capability did not sync the legacy three-portion limit to the container");
        items.extractItem(0, 2, false);
        helper.assertTrue(storage.getItem(0).getCount() == 1, "capability extraction did not sync to container");
        storage.clearContent();
        var fluids = storage.getCapability(ForgeCapabilities.FLUID_HANDLER).resolve().orElseThrow();
        int filled = fluids.fill(new FluidStack(AnimaniaFluids.SOURCE_SLOP.get(), 1000), net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
        helper.assertTrue(filled == 1000, "fluid capability rejected registered slop fluid");
        AnimaniaBlocks.TroughEntity loaded = new AnimaniaBlocks.TroughEntity(pos, helper.getLevel().getBlockState(pos));
        loaded.load(storage.saveWithoutMetadata());
        helper.assertTrue(loaded.fluidSnapshot().getAmount() == 1000, "trough fluid did not survive NBT round-trip");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void troughClientUpdateClearsConsumedFoodAndTracksPortions(GameTestHelper helper) {
        AnimaniaGameTestEvidence.mark("animania:troughClientUpdateClearsConsumedFoodAndTracksPortions");
        BlockPos pos = helper.absolutePos(new BlockPos(0, 1, 0));
        BlockState state = AnimaniaBlocks.TROUGH.get().defaultBlockState();
        AnimaniaBlocks.TroughEntity server = new AnimaniaBlocks.TroughEntity(pos, state);
        AnimaniaBlocks.TroughEntity client = new AnimaniaBlocks.TroughEntity(pos, state);
        server.setItem(0, new ItemStack(Items.CARROT, 3));
        client.load(server.getUpdateTag());
        helper.assertTrue(client.getItem(0).getCount() == 3,
                "client did not receive the full three-portion trough state");
        server.removeItem(0, 2);
        client.load(server.getUpdateTag());
        helper.assertTrue(client.getItem(0).getCount() == 1,
                "client did not receive the reduced one-portion trough state");
        server.removeItem(0, 1);
        client.load(server.getUpdateTag());
        helper.assertTrue(client.getItem(0).isEmpty()
                        && client.content() == AnimaniaBlocks.TroughEntity.TroughContent.EMPTY,
                "empty update left the final consumed food portion visible on the client");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void troughFoodConfigUsesModernRegistryMatching(GameTestHelper helper) {
        helper.assertTrue(AnimaniaConfig.matchesTroughFood(new ItemStack(Items.WHEAT)),
                "default troughFood did not accept minecraft:wheat");
        helper.assertFalse(AnimaniaConfig.matchesTroughFood(new ItemStack(Items.DIRT)),
                "troughFood accepted an unconfigured item");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void troughContentStateMatchesLegacyPriority(GameTestHelper helper) {
        AnimaniaGameTestEvidence.mark("animania:troughContentStateMatchesLegacyPriority");
        BlockPos pos = helper.absolutePos(new BlockPos(0, 1, 0));
        helper.getLevel().setBlock(pos, AnimaniaBlocks.TROUGH.get().defaultBlockState(), 3);
        if (!(helper.getLevel().getBlockEntity(pos) instanceof AnimaniaBlocks.TroughEntity trough)) {
            helper.fail("trough content test did not create its block entity");
            return;
        }
        helper.assertTrue(trough.content() == AnimaniaBlocks.TroughEntity.TroughContent.EMPTY,
                "new trough was not EMPTY");
        trough.fillFluid(new FluidStack(net.minecraft.world.level.material.Fluids.WATER, 1000),
                net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
        helper.assertTrue(trough.content() == AnimaniaBlocks.TroughEntity.TroughContent.LIQUID,
                "water-filled trough was not LIQUID");
        trough.getCapability(ForgeCapabilities.FLUID_HANDLER).resolve().orElseThrow().drain(1000,
                net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
        trough.setItem(0, new ItemStack(Items.WHEAT));
        helper.assertTrue(trough.content() == AnimaniaBlocks.TroughEntity.TroughContent.FOOD,
                "food-filled trough was not FOOD");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void slopRecipePreservesConfigAndBucketSemantics(GameTestHelper helper) {
        helper.assertTrue(SlopRecipe.matchesInputs(java.util.List.of(
                        new ItemStack(Items.CARROT), new ItemStack(Items.BREAD), new ItemStack(Items.MILK_BUCKET))),
                "two configured pig foods plus one milk bucket did not make slop");
        helper.assertFalse(SlopRecipe.matchesInputs(java.util.List.of(
                        new ItemStack(Items.WHEAT), new ItemStack(Items.WHEAT),
                        new ItemStack(Items.MILK_BUCKET), new ItemStack(Items.MILK_BUCKET))),
                "legacy four-slot wheat/milk fallback was still accepted");
        helper.assertFalse(SlopRecipe.matchesInputs(java.util.List.of(
                        new ItemStack(Items.CARROT), new ItemStack(Items.DIRT), new ItemStack(Items.MILK_BUCKET))),
                "unconfigured pig food was accepted");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void saltLickCareAndDurability(GameTestHelper helper) {
        AnimaniaGameTestEvidence.mark("animania:saltLickCareAndDurability");
        BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 0));
        helper.getLevel().setBlock(pos, AnimaniaBlocks.SALT_LICK.get().defaultBlockState(), 3);
        if (!(helper.getLevel().getBlockEntity(pos) instanceof AnimaniaSaltLickBlockEntity lick)) {
            helper.fail("salt lick did not create its block entity");
            return;
        }
        int before = lick.usesLeft();
        lick.serverTick();
        helper.assertTrue(before == lick.usesLeft(), "unused salt lick changed durability");
        int maximum = AnimaniaSaltLickItem.configuredMaxUses();
        ItemStack partlyUsed = new ItemStack(AnimaniaBlocks.SALT_LICK.get().asItem());
        // Six reproduces the client crash report's first-use value. This call
        // must not trigger a late helper-class load from the installed JAR.
        partlyUsed.setDamageValue(6);
        ((AnimaniaSaltLickBlock) AnimaniaBlocks.SALT_LICK.get()).setPlacedBy(
                helper.getLevel(), pos, helper.getLevel().getBlockState(pos), null, partlyUsed);
        helper.assertTrue(lick.usesLeft() == maximum - 6, "placed salt lick lost its item damage/remaining-use state");
        helper.assertTrue(partlyUsed.getItem() instanceof AnimaniaSaltLickItem item
                        && item.isBarVisible(partlyUsed)
                        && item.getBarWidth(partlyUsed) == Math.round(13.0F * (maximum - 6) / maximum),
                "salt lick durability bar did not use the configured maximum");
        lick.setUsesLeft(125);
        AnimaniaSaltLickBlockEntity loaded = new AnimaniaSaltLickBlockEntity(
                pos, helper.getLevel().getBlockState(pos));
        loaded.load(lick.saveWithoutMetadata());
        helper.assertTrue(loaded.usesLeft() == 125,
                "salt lick remaining uses did not survive NBT round-trip");
        ItemStack dropped = ((AnimaniaSaltLickBlock) AnimaniaBlocks.SALT_LICK.get()).stackForRemainingUses(lick.usesLeft());
        helper.assertTrue(dropped.getDamageValue() == maximum - 125
                        && AnimaniaSaltLickItem.remainingUses(dropped) == 125,
                "salt lick drop did not preserve remaining uses");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void troughRetainsTwoBlockStructureAndControllerCleanup(GameTestHelper helper) {
        AnimaniaGameTestEvidence.mark("animania:troughRetainsTwoBlockStructureAndControllerCleanup");
        BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
        BlockState state = AnimaniaBlocks.TROUGH.get().defaultBlockState().setValue(AnimaniaTroughBlock.FACING, Direction.EAST);
        helper.getLevel().setBlock(pos, state, 3);
        ((AnimaniaTroughBlock) AnimaniaBlocks.TROUGH.get()).setPlacedBy(helper.getLevel(), pos, state, null, ItemStack.EMPTY);
        BlockPos companion = pos.east();
        helper.assertTrue(helper.getLevel().getBlockState(companion).is(AnimaniaBlocks.INVISIBLE_BLOCK.get()),
                "trough did not create its persisted companion block");
        helper.assertTrue(helper.getLevel().getBlockState(companion).getValue(AnimaniaInvisibleBlock.CONTROLLER) == Direction.WEST,
                "companion block did not point back to the trough controller");
        helper.assertFalse(state.canBeReplaced(net.minecraft.world.level.material.Fluids.FLOWING_WATER),
                "flowing water could replace and break the trough controller");
        helper.assertFalse(helper.getLevel().getBlockState(companion)
                        .canBeReplaced(net.minecraft.world.level.material.Fluids.FLOWING_WATER),
                "flowing water could replace the trough companion half");
        var companionCollision = helper.getLevel().getBlockState(companion)
                .getCollisionShape(helper.getLevel(), companion);
        helper.assertTrue(!companionCollision.isEmpty(),
                "trough companion had no collision shape");
        var collisionBounds = companionCollision.bounds();
        helper.assertTrue(Math.abs(collisionBounds.getXsize() - 1.0D) < 0.0001D
                        && Math.abs(collisionBounds.getYsize() - 0.3D) < 0.0001D
                        && Math.abs(collisionBounds.getZsize() - 0.8D) < 0.0001D,
                "trough companion collision did not cover the full second half");
        helper.assertTrue(helper.getLevel().getBlockEntity(companion) instanceof AnimaniaBlocks.InvisibleTroughProxyEntity,
                "trough companion did not create its capability proxy block entity");
        var proxy = (AnimaniaBlocks.InvisibleTroughProxyEntity) helper.getLevel().getBlockEntity(companion);
        helper.assertTrue(proxy.getCapability(ForgeCapabilities.ITEM_HANDLER, Direction.UP).isPresent(),
                "trough companion did not proxy sided item automation to the controller");
        helper.assertTrue(proxy.getCapability(ForgeCapabilities.FLUID_HANDLER, Direction.UP).isPresent(),
                "trough companion did not proxy sided fluid automation to the controller");
        boolean previousAutomation = AnimaniaConfig.ALLOW_TROUGH_AUTOMATION.get();
        AnimaniaConfig.ALLOW_TROUGH_AUTOMATION.set(false);
        helper.assertTrue(!proxy.getCapability(ForgeCapabilities.ITEM_HANDLER, Direction.UP).isPresent()
                        && !proxy.getCapability(ForgeCapabilities.FLUID_HANDLER, Direction.UP).isPresent(),
                "trough companion ignored allowTroughAutomation=false");
        AnimaniaConfig.ALLOW_TROUGH_AUTOMATION.set(previousAutomation);
        helper.getLevel().removeBlock(companion, false);
        helper.assertTrue(((AnimaniaTroughBlock) AnimaniaBlocks.TROUGH.get())
                        .ensureCompanion(helper.getLevel(), pos, state)
                        && helper.getLevel().getBlockState(companion).is(AnimaniaBlocks.INVISIBLE_BLOCK.get()),
                "existing trough did not restore a missing companion collision block");
        helper.getLevel().destroyBlock(pos, false);
        helper.assertTrue(helper.getLevel().getBlockState(companion).isAir(), "breaking the trough left an orphan companion block");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void troughEnforcesLegacyFoodFluidCapacityAndComparator(GameTestHelper helper) {
        AnimaniaGameTestEvidence.mark("animania:troughEnforcesLegacyFoodFluidCapacityAndComparator");
        BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
        helper.getLevel().setBlock(pos, AnimaniaBlocks.TROUGH.get().defaultBlockState(), 3);
        AnimaniaBlocks.TroughEntity trough = (AnimaniaBlocks.TroughEntity) helper.getLevel().getBlockEntity(pos);
        var items = trough.getCapability(ForgeCapabilities.ITEM_HANDLER).resolve().orElseThrow();
        helper.assertTrue(items.insertItem(0, new ItemStack(Items.DIRT), false).getCount() == 1, "trough automation accepted invalid food");
        ItemStack remainder = items.insertItem(0, new ItemStack(Items.WHEAT, 5), false);
        helper.assertTrue(trough.getItem(0).getCount() == 3 && remainder.getCount() == 2, "trough did not retain its three-food limit");
        var fluids = trough.getCapability(ForgeCapabilities.FLUID_HANDLER).resolve().orElseThrow();
        helper.assertTrue(fluids.fill(new FluidStack(net.minecraft.world.level.material.Fluids.WATER, 1000), net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE) == 0,
                "trough mixed fluid with stored food");
        helper.assertTrue(((AnimaniaTroughBlock) AnimaniaBlocks.TROUGH.get()).getAnalogOutputSignal(helper.getLevel().getBlockState(pos), helper.getLevel(), pos) == 15,
                "three food portions did not produce full comparator output");
        trough.clearContent();
        helper.assertTrue(fluids.fill(new FluidStack(net.minecraft.world.level.material.Fluids.WATER, 2000), net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE) == 1000,
                "trough did not enforce its 1000 mB legacy tank capacity");
        helper.assertTrue(items.insertItem(0, new ItemStack(Items.WHEAT), false).getCount() == 1, "trough mixed food with stored fluid");
        boolean oldAutomation = AnimaniaConfig.ALLOW_TROUGH_AUTOMATION.get();
        AnimaniaConfig.ALLOW_TROUGH_AUTOMATION.set(false);
        boolean sidedItemsHidden = !trough.getCapability(ForgeCapabilities.ITEM_HANDLER, Direction.DOWN).isPresent();
        boolean sidedFluidsHidden = !trough.getCapability(ForgeCapabilities.FLUID_HANDLER, Direction.DOWN).isPresent();
        boolean directAccessRetained = trough.getCapability(ForgeCapabilities.ITEM_HANDLER).isPresent();
        AnimaniaConfig.ALLOW_TROUGH_AUTOMATION.set(oldAutomation);
        helper.assertTrue(sidedItemsHidden && sidedFluidsHidden,
                "allowTroughAutomation=false still exposed a sided Forge capability");
        helper.assertTrue(directAccessRetained, "automation switch disabled direct player access");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void troughRainCollectionPreservesLegacyMixingAndIncrementRules(GameTestHelper helper) {
        AnimaniaGameTestEvidence.mark("animania:troughRainCollectionPreservesLegacyMixingAndIncrementRules");
        BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
        helper.getLevel().setBlock(pos, AnimaniaBlocks.TROUGH.get().defaultBlockState(), 3);
        var trough = (AnimaniaBlocks.TroughEntity) helper.getLevel().getBlockEntity(pos);
        trough.setItem(0, new ItemStack(Items.WHEAT));
        helper.assertTrue(AnimaniaTroughBlock.collectRain(trough) == 0 && trough.fluidSnapshot().isEmpty(),
                "rain mixed water into a food-filled trough");
        trough.clearContent();
        helper.assertTrue(AnimaniaTroughBlock.collectRain(trough) == 100
                        && trough.fluidSnapshot().getAmount() == 100,
                "empty trough did not collect the first 100 mB rain increment");
        helper.assertTrue(AnimaniaTroughBlock.collectRain(trough) == 100
                        && trough.fluidSnapshot().getAmount() == 200,
                "partially filled water trough did not collect another 100 mB increment");
        trough.getCapability(ForgeCapabilities.FLUID_HANDLER).ifPresent(handler ->
                handler.drain(Integer.MAX_VALUE,
                        net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE));
        trough.fillFluid(new FluidStack(AnimaniaFluids.SOURCE_SLOP.get(), 500),
                net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
        helper.assertTrue(AnimaniaTroughBlock.collectRain(trough) == 0
                        && trough.fluidSnapshot().getAmount() == 500,
                "rain mixed water into a slop-filled trough");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void nestAndFloorPilesRetainLegacyInteractionRules(GameTestHelper helper) {
        AnimaniaGameTestEvidence.mark("animania:nestAndFloorPilesRetainLegacyInteractionRules");
        BlockPos floor = helper.absolutePos(new BlockPos(1, 1, 1));
        helper.getLevel().setBlock(floor, Blocks.STONE.defaultBlockState(), 3);
        BlockPos nestPos = floor.above();
        helper.getLevel().setBlock(nestPos, AnimaniaBlocks.NEST.get().defaultBlockState(), 3);
        AnimaniaBlocks.NestEntity nest = (AnimaniaBlocks.NestEntity) helper.getLevel().getBlockEntity(nestPos);
        var handler = nest.getCapability(ForgeCapabilities.ITEM_HANDLER).resolve().orElseThrow();
        helper.assertTrue(handler.insertItem(0, new ItemStack(Items.DIRT), false).getCount() == 1, "nest accepted a non-egg item");
        helper.assertTrue(!nest.getCapability(ForgeCapabilities.ITEM_HANDLER, Direction.UP).isPresent(),
                "nest exposed sided automation despite the legacy insert rejection rule");
        handler.insertItem(0, new ItemStack(Items.EGG, 3), false);
        var player = helper.makeMockPlayer();
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        AnimaniaBlocks.NEST.get().use(helper.getLevel().getBlockState(nestPos), helper.getLevel(), nestPos, player,
                InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(nestPos), Direction.UP, nestPos, false));
        helper.assertTrue(nest.getItem(0).getCount() == 2, "empty-hand nest interaction did not extract exactly one egg");
        AnimaniaBlocks.NestEntity savedNest = new AnimaniaBlocks.NestEntity(
                nestPos, helper.getLevel().getBlockState(nestPos));
        helper.assertTrue(savedNest.insertEgg(new ItemStack(Items.EGG), "leghorn"),
                "nest rejected a valid egg/variant pair");
        AnimaniaBlocks.NestEntity loadedNest = new AnimaniaBlocks.NestEntity(
                nestPos, helper.getLevel().getBlockState(nestPos));
        loadedNest.load(savedNest.saveWithoutMetadata());
        helper.assertTrue(loadedNest.getItem(0).getCount() == 1
                        && loadedNest.birdVariant().equals("leghorn"),
                "nest egg inventory or bird variant did not survive NBT round-trip");
        net.minecraft.nbt.CompoundTag legacyNestTag = new net.minecraft.nbt.CompoundTag();
        legacyNestTag.putString("birdType", "legacy_leghorn");
        loadedNest.load(legacyNestTag);
        helper.assertTrue(loadedNest.birdVariant().equals("legacy_leghorn"),
                "nest did not migrate the legacy birdType save field");
        BlockPos seedsPos = floor.east().above();
        helper.getLevel().setBlock(floor.east(), Blocks.STONE.defaultBlockState(), 3);
        BlockState seeds = AnimaniaBlocks.SEEDS.get().defaultBlockState().setValue(AnimaniaThinBlock.VARIANT, AnimaniaThinBlock.SeedVariant.MELON);
        helper.getLevel().setBlock(seedsPos, seeds, 3);
        helper.assertTrue(AnimaniaBlocks.SEEDS.get().getCloneItemStack(helper.getLevel(), seedsPos, seeds).is(Items.MELON_SEEDS),
                "seed pile variant did not preserve its vanilla seed identity");
        helper.assertTrue(Math.abs(AnimaniaBlocks.SEEDS.get().getShape(seeds, helper.getLevel(), seedsPos,
                        net.minecraft.world.phys.shapes.CollisionContext.empty()).bounds().maxY - 0.0002D) < 0.000001D,
                "seed pile did not retain the legacy 0.0002-block outline height");
        helper.getLevel().removeBlock(floor.east(), false);
        helper.assertTrue(helper.getLevel().getBlockState(seedsPos).isAir(), "unsupported seed pile did not remove itself");
        BlockPos strawFloor = floor.south();
        BlockPos strawPos = strawFloor.above();
        helper.getLevel().setBlock(strawFloor, Blocks.STONE.defaultBlockState(), 3);
        BlockState straw = AnimaniaBlocks.STRAW.get().defaultBlockState();
        helper.getLevel().setBlock(strawPos, straw, 3);
        helper.assertTrue(Math.abs(AnimaniaBlocks.STRAW.get().getShape(straw, helper.getLevel(), strawPos,
                        net.minecraft.world.phys.shapes.CollisionContext.empty()).bounds().maxY - 0.002D) < 0.000001D,
                "straw pile did not retain the legacy 0.002-block outline height");
        helper.assertTrue(AnimaniaBlocks.STRAW.get().getCollisionShape(straw, helper.getLevel(), strawPos,
                        net.minecraft.world.phys.shapes.CollisionContext.empty()).isEmpty()
                        && AnimaniaBlocks.STRAW.get().isFlammable(straw, helper.getLevel(), strawPos, Direction.UP),
                "straw pile lost its non-colliding flammable behavior");
        helper.getLevel().removeBlock(strawFloor, false);
        helper.assertTrue(helper.getLevel().getBlockState(strawPos).isAir(),
                "unsupported straw pile did not remove itself");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void mudRetainsLegacyShapeSoundFrictionAndMovementDamping(GameTestHelper helper) {
        AnimaniaGameTestEvidence.mark("animania:mudRetainsLegacyShapeSoundFrictionAndMovementDamping");
        BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
        BlockState mud = AnimaniaBlocks.MUD.get().defaultBlockState();
        helper.getLevel().setBlock(pos, mud, 3);
        helper.assertTrue(Math.abs(AnimaniaBlocks.MUD.get().getShape(mud, helper.getLevel(), pos,
                        net.minecraft.world.phys.shapes.CollisionContext.empty()).bounds().maxY - 0.88D) < 0.000001D,
                "mud did not retain the legacy 0.88-block collision height");
        helper.assertTrue(Math.abs(AnimaniaBlocks.MUD.get().getFriction() - 0.6F) < 0.0001F
                        && AnimaniaBlocks.MUD.get().getSoundType(mud, helper.getLevel(), pos, null)
                        == net.minecraft.world.level.block.SoundType.SLIME_BLOCK
                        && mud.getMapColor(helper.getLevel(), pos) == net.minecraft.world.level.material.MapColor.COLOR_BROWN,
                "mud did not retain legacy friction, slime sound type, or brown map color");
        var player = helper.makeMockPlayer();
        player.setDeltaMovement(1.0D, 0.5D, -1.0D);
        AnimaniaBlocks.MUD.get().entityInside(mud, helper.getLevel(), pos, player);
        helper.assertTrue(Math.abs(player.getDeltaMovement().x - 0.2D) < 0.000001D
                        && Math.abs(player.getDeltaMovement().y - 0.5D) < 0.000001D
                        && Math.abs(player.getDeltaMovement().z + 0.2D) < 0.000001D,
                "mud did not damp horizontal movement by the legacy 0.2 multiplier");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void dispenserPlacesConfiguredSeedPileServerSide(GameTestHelper helper) {
        AnimaniaGameTestEvidence.mark("animania:dispenserPlacesConfiguredSeedPileServerSide");
        BlockPos dispenserPos = helper.absolutePos(new BlockPos(1, 2, 1));
        BlockPos target = dispenserPos.east();
        helper.getLevel().setBlock(target.below(), Blocks.STONE.defaultBlockState(), 3);
        helper.getLevel().setBlock(dispenserPos, Blocks.DISPENSER.defaultBlockState().setValue(DispenserBlock.FACING, Direction.EAST), 3);
        DispenserBlockEntity dispenser = (DispenserBlockEntity) helper.getLevel().getBlockEntity(dispenserPos);
        dispenser.setItem(0, new ItemStack(Items.PUMPKIN_SEEDS, 2));
        boolean previous = AnimaniaConfig.ALLOW_SEED_DISPENSER_PLACEMENT.get();
        AnimaniaConfig.ALLOW_SEED_DISPENSER_PLACEMENT.set(true);
        helper.getLevel().setBlock(dispenserPos.above(), Blocks.REDSTONE_BLOCK.defaultBlockState(), 3);
        helper.runAfterDelay(8, () -> {
            try {
                BlockState placed = helper.getLevel().getBlockState(target);
                helper.assertTrue(placed.is(AnimaniaBlocks.SEEDS.get())
                                && placed.getValue(AnimaniaThinBlock.VARIANT) == AnimaniaThinBlock.SeedVariant.PUMPKIN,
                        "registered dispenser behavior did not place the matching pumpkin seed pile");
                helper.assertTrue(dispenser.getItem(0).getCount() == 1, "seed dispenser did not consume exactly one seed");
                helper.succeed();
            } finally {
                AnimaniaConfig.ALLOW_SEED_DISPENSER_PLACEMENT.set(previous);
            }
        });
    }
}
