package com.animania.farm;

import com.animania.farm.client.model.FarmHiveModel;
import com.animania.farm.client.model.FarmNativeAnimations;
import com.animania.farm.client.model.FarmNativeModelLayers;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

final class FarmHiveAnimationTest {
    @Test
    void bothHiveVariantsApplyTheirLoopingBeeAnimation() {
        assertAnimationMovesBeeRing("model_bee_hive", "anim_bees");
        assertAnimationMovesBeeRing("model_wild_hive", "anim_bees_wild");
    }

    private static void assertAnimationMovesBeeRing(String modelId, String animationId) {
        var root = FarmNativeModelLayers.create(modelId).bakeRoot();
        var beeRing = root.getChild("bee_node1");
        var model = new FarmHiveModel(root, FarmNativeAnimations.ALL.get(animationId));

        model.applyBeeAnimation(0L);
        float startYRotation = beeRing.yRot;
        model.applyBeeAnimation(800L);
        float quarterCycleYRotation = beeRing.yRot;
        assertNotEquals(startYRotation, quarterCycleYRotation, animationId + " does not move the bee ring");

        model.applyBeeAnimation(800L);
        assertEquals(quarterCycleYRotation, beeRing.yRot, 0.000001F,
                animationId + " accumulates transforms instead of resetting its pose");
        model.applyBeeAnimation(3200L);
        assertEquals(startYRotation, beeRing.yRot, 0.000001F,
                animationId + " does not close its 3.2 second loop");
    }
}
