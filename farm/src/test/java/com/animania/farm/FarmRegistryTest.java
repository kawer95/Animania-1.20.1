package com.animania.farm;

import com.animania.common.item.LegacyEggColors;
import com.animania.farm.client.model.FarmLegacyModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.*;

class FarmRegistryTest {
    @Test
    void allLegacySoundEventsUseLegalModernIds() {
        assertEquals(96, FarmSoundCatalog.IDS.size());
        assertEquals(96, new HashSet<>(FarmSoundCatalog.IDS).size());
        assertTrue(FarmSoundCatalog.IDS.stream().allMatch(id -> id.equals(id.toLowerCase(java.util.Locale.ROOT))));
        assertTrue(FarmSoundCatalog.IDS.containsAll(java.util.List.of("crow1", "bullmoo8", "sheepliving7", "hitch", "unhitch")));
    }

    @Test
    void allPinnedAnimalIdsAreUniqueAndContentHasModernEntries() {
        assertFalse(FarmLegacyIds.ALL.isEmpty());
        assertEquals(FarmLegacyIds.ALL.size(), new HashSet<>(FarmLegacyIds.ALL).size());
        assertTrue(FarmLegacyIds.ALL.stream().anyMatch(id -> id.startsWith("cow_")));
    }

    @Test
    void everyFarmEntityHasItsPinnedLegacyPhysicalProfile() {
        assertEquals(102, FarmLegacyIds.ALL.size());
        for (String id : FarmLegacyIds.ALL) {
            FarmAnimalProfile profile = assertDoesNotThrow(() -> FarmAnimalProfile.forId(id), id);
            assertTrue(profile.width() > 0.0F && profile.height() > 0.0F, id);
            assertTrue(profile.maxHealth() > 0.0D && profile.movementSpeed() > 0.0D, id);
        }
        assertEquals(new FarmAnimalProfile(24.0D, 0.20D, 4.0D, 1.6F, 1.8F),
                FarmAnimalProfile.forId("bull_angus"));
        assertEquals(new FarmAnimalProfile(6.0D, 0.29D, 1.5D, 0.5F, 0.7F),
                FarmAnimalProfile.forId("hen_leghorn"));
        assertEquals(new FarmAnimalProfile(15.0D, 0.265D, 0.0D, 1.6F, 1.3F),
                FarmAnimalProfile.forId("doe_alpine"));
        assertEquals(new FarmAnimalProfile(15.0D, 0.265D, 0.0D, 1.1F, 1.2F),
                FarmAnimalProfile.forId("doe_nigerian_dwarf"));
        assertEquals(2.5F, FarmAnimalProfile.forId("wagon").width());
    }

    @Test
    void everyAnimalEggHasItsExactLegacyTintPair() {
        FarmLegacyIds.ALL.stream().filter(id -> !FarmLegacyIds.isVehicle(id))
                .forEach(id -> assertNotNull(LegacyEggColors.forEntity(id), id));
        assertEquals(new LegacyEggColors.Colors(3028024, 2304560), LegacyEggColors.forEntity("bull_angus"));
        assertEquals(new LegacyEggColors.Colors(15987699, 1776411), LegacyEggColors.forEntity("lamb_dorper"));
        assertEquals(new LegacyEggColors.Colors(15987699, 3944229), LegacyEggColors.forEntity("calf_friesian"));
    }

    @Test
    void convertedModelProfilesNeverAnimateOneBoneAsBothLeftAndRight() {
        FarmLegacyIds.ALL.stream().filter(id -> !FarmLegacyIds.isVehicle(id)).forEach(id -> {
            var profile = FarmLegacyModelLayers.profile(id);
            var left = new HashSet<>(java.util.List.of(profile.leftLegs()));
            var right = new HashSet<>(java.util.List.of(profile.rightLegs()));
            left.retainAll(right);
            assertTrue(left.isEmpty(), id + " overlapping limbs " + left);
        });
        assertArrayEquals(new String[]{"back_leg__l", "front_leg__r"},
                FarmLegacyModelLayers.profile("buck_alpine").leftLegs());
        assertArrayEquals(new String[]{"leg0", "leg2"},
                FarmLegacyModelLayers.profile("bull_angus").leftLegs());
        assertArrayEquals(new String[]{"sac", "penis"},
                FarmLegacyModelLayers.profile("bull_angus").privateParts());
    }

    @Test
    void adultCattleUseDiagonalGaitsRatherThanTheLegacySameSidePace() {
        FarmLegacyIds.ALL.stream().filter(id -> id.startsWith("bull_")).forEach(id -> {
            var profile = FarmLegacyModelLayers.profile(id);
            assertArrayEquals(new String[]{"leg0", "leg2"}, profile.leftLegs(), id);
            assertArrayEquals(new String[]{"leg1", "leg3"}, profile.rightLegs(), id);
        });
        FarmLegacyIds.ALL.stream().filter(id -> id.startsWith("cow_")).forEach(id -> {
            var profile = FarmLegacyModelLayers.profile(id);
            assertArrayEquals(new String[]{"leg1", "leg3"}, profile.leftLegs(), id);
            assertArrayEquals(new String[]{"leg2", "leg4"}, profile.rightLegs(), id);
        });
        // Calf leg numbers are laid out differently and the original phase
        // pairs are already diagonal: front-left/rear-right vs. front-right/rear-left.
        FarmLegacyIds.ALL.stream().filter(id -> id.startsWith("calf_")).forEach(id -> {
            var profile = FarmLegacyModelLayers.profile(id);
            assertArrayEquals(new String[]{"leg0", "leg3"}, profile.leftLegs(), id);
            assertArrayEquals(new String[]{"leg1", "leg2"}, profile.rightLegs(), id);
        });
    }

    @Test
    void everyAnimalModelBakesGeometryAndEveryAnimationPathResolves() {
        FarmLegacyIds.ALL.stream().filter(id -> !FarmLegacyIds.isVehicle(id)).forEach(id -> {
            ModelPart root = FarmLegacyModelLayers.create(id).bakeRoot();
            assertTrue(root.getAllParts().anyMatch(part -> !part.isEmpty()), id + " baked with no cubes");
            assertProfilePaths(root, FarmLegacyModelLayers.profile(id), id);
        });
    }

    @Test
    void everyGoatMuzzleIsAttachedToTheAnimatedHeadNode() {
        FarmLegacyIds.ALL.stream()
                .filter(id -> id.startsWith("buck_") || id.startsWith("doe_") || id.startsWith("kid_"))
                .forEach(id -> {
                    ModelPart root = FarmLegacyModelLayers.create(id).bakeRoot();
                    assertTrue(root.hasChild("head_node"), id + " lost its head node");
                    assertTrue(root.getChild("head_node").hasChild("nose"),
                            id + " nose is not attached to the animated head");
                    assertFalse(root.hasChild("nose"), id + " retained a root-level floating nose");
                });
    }

    @Test
    void chickenLookAnimationRotatesTheWholeNeckAssembly() {
        FarmLegacyIds.ALL.stream()
                .filter(id -> id.startsWith("chick_") || id.startsWith("hen_") || id.startsWith("rooster_"))
                .forEach(id -> {
                    assertArrayEquals(new String[]{"neck"}, FarmLegacyModelLayers.profile(id).heads(), id);
                    ModelPart neck = FarmLegacyModelLayers.create(id).bakeRoot().getChild("neck");
                    assertTrue(neck.hasChild("head"), id + " head is not attached to its animated neck");
                    assertTrue(neck.hasChild("beak_top"), id + " upper beak is not attached to its animated neck");
                    if (!id.startsWith("chick_")) {
                        assertTrue(neck.hasChild("beak_bottom"), id + " lower beak is not attached to its animated neck");
                        assertTrue(neck.hasChild("crest"), id + " comb is not attached to its animated neck");
                        assertTrue(neck.hasChild("crest_bottom"), id + " wattle is not attached to its animated neck");
                    }
                });
        assertArrayEquals(new String[]{"wing1", "wing2", "wing3", "wing4"},
                FarmLegacyModelLayers.profile("chick_leghorn").wings(),
                "all four legacy chick wing layers must flap");
    }

    @Test
    void bothPigletModelsUseTheirOriginal32PixelTextureWidth() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/com/animania/farm/client/model/FarmLegacyModelLayers.java"));
        int commonStart = source.indexOf("private static LayerDefinition model_piglet()");
        int hampshireStart = source.indexOf("private static LayerDefinition model_piglet_hampshire()");
        int nextModel = source.indexOf("private static LayerDefinition model_rooster()", hampshireStart);
        assertTrue(commonStart >= 0 && hampshireStart > commonStart && nextModel > hampshireStart);

        String common = source.substring(commonStart, hampshireStart);
        String hampshire = source.substring(hampshireStart, nextModel);
        assertTrue(common.contains("LayerDefinition.create(mesh, 32, 32)"));
        assertTrue(hampshire.contains("LayerDefinition.create(mesh, 32, 32)"));
        assertFalse(common.contains("LayerDefinition.create(mesh, 64, 32)"));
        assertFalse(hampshire.contains("LayerDefinition.create(mesh, 64, 32)"));
    }

    @Test
    void legacyColoredWoolPartsRemainDedicatedTintPasses() {
        for (String id : java.util.List.of("ewe_dorset", "ram_merino", "ewe_suffolk", "ram_friesian")) {
            var profile = FarmLegacyModelLayers.profile(id);
            assertTrue(profile.coloredParts().length >= 8, id + " lost ModelRendererColored wool parts");
            ModelPart root = FarmLegacyModelLayers.create(id).bakeRoot();
            for (String path : profile.coloredParts()) {
                assertTrue(hasPath(root, path), id + " colored part does not resolve: " + path);
            }
        }
    }

    @Test
    void legacySleepBedDefaultsMapBlockStrawToBaseStraw() {
        for (String family : java.util.List.of("chicken", "cow", "goat", "horse", "pig", "sheep")) {
            assertEquals("animania:straw", FarmConfig.BED_BLOCKS.get(family + "Bed").getDefault(), family);
            assertEquals("minecraft:grass_block", FarmConfig.BED_BLOCKS.get(family + "Bed2").getDefault(), family);
        }
    }

    @Test
    void legacyOptionalModFoodsRemainInExactDefaultOrder() {
        assertEquals(java.util.List.of("minecraft:wheat_seeds", "minecraft:melon_seeds", "minecraft:beetroot_seeds", "minecraft:pumpkin_seeds", "simplecorn:corncob", "biomesoplenty:turnip_seeds", "harvestcraft:cornitem"), FarmConfig.CHICKEN_FOOD.getDefault());
        assertEquals(java.util.List.of("minecraft:wheat", "simplecorn:corncob", "harvestcraft:barleyitem", "harvestcraft:oatsitem", "harvestcraft:ryeitem", "harvestcraft:cornitem"), FarmConfig.COW_FOOD.getDefault());
        assertEquals(java.util.List.of("minecraft:wheat", "minecraft:string", "minecraft:stick", "minecraft:apple", "simplecorn:corncob", "harvestcraft:barleyitem", "harvestcraft:oatsitem", "harvestcraft:ryeitem", "harvestcraft:cornitem"), FarmConfig.GOAT_FOOD.getDefault());
        assertEquals(java.util.List.of("minecraft:wheat", "harvestcraft:barleyitem", "harvestcraft:oatsitem", "harvestcraft:ryeitem", "minecraft:apple", "minecraft:carrot"), FarmConfig.HORSE_FOOD.getDefault());
        assertEquals(java.util.List.of("minecraft:wheat", "harvestcraft:barleyitem", "harvestcraft:oatsitem", "harvestcraft:ryeitem"), FarmConfig.SHEEP_FOOD.getDefault());
    }

    @Test
    void everyLegacyRawMeatRetainsExactFoodAndNauseaValues() {
        for (String id : java.util.List.of("raw_prime_steak", "raw_prime_beef", "raw_horse", "raw_prime_pork",
                "raw_prime_bacon", "raw_prime_chicken", "raw_chevon", "raw_prime_chevon", "raw_prime_mutton")) {
            assertSame(com.animania.common.item.LegacyRawFoodProfile.RAW,
                    com.animania.common.item.LegacyRawFoodProfile.forItemId(id), id);
        }
        var profile = com.animania.common.item.LegacyRawFoodProfile.RAW;
        assertEquals(1, profile.nutrition());
        assertEquals(1.0F, profile.saturation());
        assertEquals(200, profile.nauseaTicks());
        assertEquals(3, profile.nauseaAmplifier());
        assertEquals(1.0F, profile.effectProbability());
        assertNull(com.animania.common.item.LegacyRawFoodProfile.forItemId("cooked_prime_beef"));
    }

    @Test
    void adultHorseLayersRetainTheCompleteLegacySaddleAssembly() {
        String[] saddleParts = {
                "saddle_base", "saddle_base2", "saddle_base3", "saddle",
                "saddle2", "saddle3", "saddle4", "saddle5", "saddle6",
                "footstrap", "foot1", "foot2", "foot3", "foot4",
                "footstrap2", "foot1a", "foot2a", "foot3a", "foot4a",
                "saddle7", "saddle_hump", "saddle_hump2", "strap1", "strap2", "strap3"
        };
        for (String id : java.util.List.of("mare_draft", "stallion_draft")) {
            ModelPart root = FarmLegacyModelLayers.create(id).bakeRoot();
            for (String part : saddleParts) {
                assertTrue(root.hasChild(part), id + " lost saddle part " + part);
            }
        }
        assertFalse(FarmLegacyModelLayers.create("foal_draft").bakeRoot().hasChild("saddle_base"),
                "foal model should not expose the adult saddle assembly");
    }

    private static void assertProfilePaths(ModelPart root, com.animania.client.model.LegacyAnimationProfile profile,
                                           String id) {
        java.util.stream.Stream.of(profile.heads(), profile.leftLegs(), profile.rightLegs(), profile.tails(),
                        profile.wings(), profile.bodies(), profile.privateParts(), profile.coloredParts())
                .flatMap(java.util.Arrays::stream)
                .forEach(path -> assertTrue(hasPath(root, path), id + " has missing animation bone " + path));
    }

    private static boolean hasPath(ModelPart root, String path) {
        ModelPart current = root;
        for (String segment : path.split("/")) {
            if (!current.hasChild(segment)) return false;
            current = current.getChild(segment);
        }
        return true;
    }
    @Test
    void draftHorseRenderScaleMatchesLegacyAdultRenderers() {
        assertEquals(0.72F, FarmLegacyModelLayers.scale("mare_draft"));
        assertEquals(0.85F, FarmLegacyModelLayers.scale("stallion_draft"));
    }

}
