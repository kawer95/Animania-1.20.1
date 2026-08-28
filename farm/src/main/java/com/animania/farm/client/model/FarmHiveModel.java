package com.animania.farm.client.model;

import net.minecraft.client.animation.AnimationDefinition;
import net.minecraft.client.animation.KeyframeAnimations;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.Entity;
import org.joml.Vector3f;

/** Applies the converted looping CraftStudio bee animation to a hive model. */
public final class FarmHiveModel extends HierarchicalModel<Entity> {
    private final ModelPart root;
    private final AnimationDefinition beeAnimation;
    private final Vector3f animationVectorCache = new Vector3f();

    public FarmHiveModel(ModelPart root, AnimationDefinition beeAnimation) {
        this.root = root;
        this.beeAnimation = beeAnimation;
    }

    @Override
    public ModelPart root() {
        return root;
    }

    public void applyBeeAnimation(long animationTimeMillis) {
        root.getAllParts().forEach(ModelPart::resetPose);
        KeyframeAnimations.animate(this, beeAnimation, animationTimeMillis, 1.0F, animationVectorCache);
    }

    @Override
    public void setupAnim(Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks,
                          float netHeadYaw, float headPitch) {
        applyBeeAnimation((long) (ageInTicks * 50.0F));
    }
}
