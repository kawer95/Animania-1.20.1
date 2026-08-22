package com.animania.client.model;

import com.animania.common.entity.AnimaniaAnimalEntity;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.world.item.DyeColor;

import java.util.ArrayList;
import java.util.List;

/** Runtime wrapper for the breed-specific native layers converted from 1.12. */
public final class LegacyAnimalModel extends HierarchicalModel<AnimaniaAnimalEntity> {
    /** ModelPeacock.render() in 1.12 rendered every fan root with scale / 3. */
    private static final float LEGACY_PEACOCK_FAN_RENDER_SCALE = 1.0F / 3.0F;
    /** ModelPiglet.render() in 1.12 rendered Tail1 with scale * 0.8. */
    private static final float LEGACY_PIGLET_TAIL_RENDER_SCALE = 0.8F;
    /**
     * ModelDraftHorseMare/Stallion.render() in 1.12 rendered this complete
     * saddle assembly only while isHorseSaddled() was true.  The converted
     * 1.20 layers keep the parts as root children, so their visibility must be
     * restored explicitly instead of letting the generic root render draw
     * them for every horse.
     */
    private static final String[] LEGACY_HORSE_SADDLE_PARTS = {
            "saddle_base", "saddle_base2", "saddle_base3", "saddle",
            "saddle2", "saddle3", "saddle4", "saddle5", "saddle6",
            "footstrap", "foot1", "foot2", "foot3", "foot4",
            "footstrap2", "foot1a", "foot2a", "foot3a", "foot4a",
            "saddle7", "saddle_hump", "saddle_hump2", "strap1", "strap2", "strap3"
    };
    private final ModelPart root;
    private final List<ModelPart> heads;
    private final List<ModelPart> leftLegs;
    private final List<ModelPart> rightLegs;
    private final List<ModelPart> tails;
    private final List<ModelPart> wings;
    private final List<ModelPart> bodies;
    private final List<ModelPart> privateParts;
    private final List<ModelPart> coloredParts;
    private final List<ModelPart> fanNodes;
    private final ModelPart pigletTail;
    private final List<ModelPart> saddleParts;
    private final List<ResolvedPose> sittingPose;
    private final List<ResolvedPose> sleepingPose;
    private final ModelPart petLookPart;
    private final LegacyPetAnimationDefinition petAnimation;
    private float woolRed = 1.0F;
    private float woolGreen = 1.0F;
    private float woolBlue = 1.0F;
    private boolean renderPigletTailAtLegacyScale;

    public LegacyAnimalModel(ModelPart root, LegacyAnimationProfile profile) {
        this(root, profile, LegacyPoseDefinition.EMPTY, LegacyPetAnimationDefinition.EMPTY);
    }

    public LegacyAnimalModel(ModelPart root, LegacyAnimationProfile profile, LegacyPoseDefinition sittingPose) {
        this(root, profile, sittingPose, LegacyPetAnimationDefinition.EMPTY);
    }

    public LegacyAnimalModel(ModelPart root, LegacyAnimationProfile profile, LegacyPoseDefinition sittingPose,
                             LegacyPetAnimationDefinition petAnimation) {
        this.root = root;
        this.heads = resolve(root, profile.heads());
        this.leftLegs = resolve(root, profile.leftLegs());
        this.rightLegs = resolve(root, profile.rightLegs());
        this.tails = resolve(root, profile.tails());
        this.wings = resolve(root, profile.wings());
        this.bodies = resolve(root, profile.bodies());
        this.privateParts = resolve(root, profile.privateParts());
        this.coloredParts = resolve(root, profile.coloredParts());
        // Only the male peacock layer owns these four roots.  Resolving the
        // paths here lets the shared model preserve the old renderer's local
        // scale without changing peahen/peachick geometry.
        this.fanNodes = resolve(root, new String[]{
                "fan_node_a", "fan_node_b", "fan_node_c", "fan_node_d"});
        List<ModelPart> resolvedPigletTail = resolve(root, new String[]{"tail1"});
        this.pigletTail = resolvedPigletTail.isEmpty() ? null : resolvedPigletTail.get(0);
        this.saddleParts = resolve(root, LEGACY_HORSE_SADDLE_PARTS);
        this.sittingPose = resolvePose(root, sittingPose);
        this.sleepingPose = resolvePose(root, petAnimation.sleepingPose());
        List<ModelPart> look = resolve(root, new String[]{petAnimation.lookPart()});
        this.petLookPart = look.isEmpty() ? null : look.get(0);
        this.petAnimation = petAnimation;
    }

    @Override
    public ModelPart root() {
        return root;
    }

    public void translatePrimaryHead(PoseStack poseStack) {
        if (!heads.isEmpty()) heads.get(0).translateAndRotate(poseStack);
    }

    @Override
    public void setupAnim(AnimaniaAnimalEntity entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        root.getAllParts().forEach(ModelPart::resetPose);
        renderPigletTailAtLegacyScale = entity.registryPath().startsWith("piglet_");
        float[] wool = DyeColor.byId(entity.getWoolColor()).getTextureDiffuseColors();
        woolRed = wool[0];
        woolGreen = wool[1];
        woolBlue = wool[2];
        boolean showPrivateParts;
        try {
            showPrivateParts = com.animania.common.config.AnimaniaConfig.SHOW_PARTS.get();
        } catch (IllegalStateException ignored) {
            showPrivateParts = false;
        }
        for (ModelPart part : privateParts) part.visible = showPrivateParts;
        // The 1.12 horse models guarded every saddle/stirrup part with
        // isHorseSaddled().  Without this gate the converted root pass draws
        // a saddle on every wild horse even though its synced saddle state is
        // false and riding is correctly rejected.
        for (ModelPart part : saddleParts) part.visible = entity.isSaddled();
        hideGoatHornBudArtifacts(entity);
        if (entity.isPigAnimal()) applyPigRestPose(entity.registryPath(), showPrivateParts);
        if (isChickenId(entity.registryPath())) applyChickenRestPose(entity.registryPath());
        if (isRabbitId(entity.registryPath())) {
            // The 1.12 rabbit models reset Neck1 in setRotationAngles after
            // their constructor pose is written.  The generated 1.20 layer
            // keeps that constructor pose (-0.7740535 radians), which makes
            // every rabbit look upward unless the runtime reset is restored.
            // The old models also put the look yaw on Neck1, not on the two
            // head children, and did this even while the renderer was laying
            // the rabbit down to sleep.
            ModelPart neck = child("neck1");
            if (neck != null) {
                neck.xRot = 0.0F;
                neck.yRot = netHeadYaw * Mth.DEG_TO_RAD;
            }
        } else if (petAnimation.active() && petLookPart != null && !entity.isSleeping()
                && (petAnimation.lookWhileSitting() || !entity.isSitting())) {
            petLookPart.xRot = headPitch * petAnimation.pitchScale() + petAnimation.pitchOffset();
            petLookPart.yRot = netHeadYaw * petAnimation.yawScale();
        } else if (!petAnimation.active() && !entity.isSleeping()) {
            float headX = headPitch * Mth.DEG_TO_RAD;
            float headY = netHeadYaw * Mth.DEG_TO_RAD;
            heads.forEach(part -> { part.xRot += headX; part.yRot += headY; });
        }

        if (!entity.isSleeping()) {
            // The standard 1.12 dog models assign a small standing offset to
            // every first leg segment in setRotationAngles, immediately
            // before adding the walking stride.  The converted layers keep
            // each breed's constructor pose, so relying on that baked value
            // leaves some breeds (notably Great Dane/Bloodhound) at 0 degrees
            // while others retain a different constructor angle.  Set the
            // runtime base first, then add the same 0.6 * 1.4 stride used by
            // the legacy models. Sitting poses are applied below and replace
            // this value just as the 1.12 code did.
            if (usesStandardDogGait(entity.registryPath())) {
                leftLegs.forEach(part -> part.xRot = 0.06981317F);
                rightLegs.forEach(part -> part.xRot = 0.06981317F);
            }
            float stride = Mth.cos(limbSwing * 0.6662F) * petAnimation.strideScale() * limbSwingAmount;
            leftLegs.forEach(part -> part.xRot += stride);
            rightLegs.forEach(part -> part.xRot -= stride);
            if (petAnimation.active()) {
                // Exact 1.12 Cats & Dogs tail cycle.  The two low-frequency
                // sine terms create the characteristic uneven wag; the old
                // generic approximation visibly changed both phase and arc.
                float tailYaw = Mth.sin(ageInTicks * 3.141593F * 0.05F)
                        * Mth.sin(ageInTicks * 3.141593F * 0.03F * 0.05F)
                        * 0.15F * 3.141593F;
                tails.forEach(part -> part.yRot += tailYaw);
            } else if (!entity.isPigAnimal()) {
                // The 1.12 pig/piglet models only reset tail1/tail1a to their
                // authored curled pose; they contain no idle sin/cos wag.
                // Keep the generic cycle off for pigs so the port matches the
                // original rather than inventing a new tail animation.
                tails.forEach(part -> part.yRot += Mth.sin(ageInTicks * 0.12F) * 0.18F);
            }
        }
        float flap = Mth.sin(ageInTicks * 0.55F) * (0.08F + limbSwingAmount * 0.45F);
        if (!entity.isSleeping()) {
            for (int i = 0; i < wings.size(); i++) wings.get(i).zRot += (i & 1) == 0 ? flap : -flap;
        }

        if (entity.isHamster()) setupHamsterPose(entity, ageInTicks);
        else if (entity.isSitting()) applyPose(sittingPose);
        else if (entity.isSleeping() && petAnimation.active()) applyPose(sleepingPose);

        if (entity.getEatingTicks() > 0) {
            heads.forEach(part -> part.xRot += 0.9F);
        } else if (entity.isSpooked()) {
            // Fainting goats collapse sideways for the one-second legacy
            // collision timer; the state is synchronized from the server.
            bodies.forEach(part -> part.zRot += 1.25F);
            heads.forEach(part -> part.zRot += 0.25F);
        } else if (entity.isFighting()) {
            heads.forEach(part -> part.xRot -= 0.35F);
            bodies.forEach(part -> part.xRot += 0.08F);
        } else if (entity.isSleeping() && !petAnimation.active()) {
            applyLegacySleepingPose(entity);
        } else if (entity.getPlayGoal() != null && entity.isPlaying()) {
            bodies.forEach(part -> part.y += Mth.sin(ageInTicks * 0.7F) * 0.8F);
            tails.forEach(part -> part.yRot += Mth.sin(ageInTicks * 0.8F) * 0.45F);
        } else if (entity.isInLove()) {
            tails.forEach(part -> part.yRot += Mth.sin(ageInTicks * 0.9F) * 0.55F);
        } else if (entity.getThirst() < 25) {
            heads.forEach(part -> part.xRot += 0.75F + Mth.sin(ageInTicks * 0.18F) * 0.08F);
        } else if (entity.getHunger() < 25) {
            heads.forEach(part -> part.xRot += 0.35F + Mth.sin(ageInTicks * 0.45F) * 0.12F);
        }
        if (entity.getCrowDuration() > 0 && entity.registryPath().startsWith("rooster_")) {
            ModelPart neck = child("neck");
            if (neck != null) {
                int duration = entity.getCrowDuration();
                neck.xRot = duration < 10 ? -(duration * 0.005F)
                        : duration >= 40 ? -0.5742105F + duration * 0.005F : -0.5742105F;
            }
        }
    }

    /** Applies the final positions written by ModelHamster#setLivingAnimations in 1.12. */
    private void setupHamsterPose(AnimaniaAnimalEntity entity, float ageInTicks) {
        // The Java model is deliberately centred at X=-1.5: its body cube is
        // -4.02..0.98. The generated layer lost the matching head offset,
        // which made an otherwise symmetric head look displaced.
        heads.forEach(part -> part.x = -1.5F);
        for (int i = 0; i < 5; i++) {
            if (root.hasChild("hamster_cheek_right" + i))
                root.getChild("hamster_cheek_right" + i).visible = i < entity.getHamsterFoodStack();
            if (root.hasChild("hamster_cheek_left" + i))
                root.getChild("hamster_cheek_left" + i).visible = i < entity.getHamsterFoodStack();
        }
        ModelPart body = child("hamster_body");
        ModelPart tail = child("hamster_tail");
        ModelPart backRight = child("hamster_leg_back_right");
        ModelPart backLeft = child("hamster_leg_back_left");
        ModelPart frontRight = child("hamster_leg_front_right");
        ModelPart frontLeft = child("hamster_leg_front_left");
        if (entity.isSitting()) {
            if (body != null) body.xRot = 1.0F;
            heads.forEach(part -> { part.y = 16.0F; part.z = -1.5F; });
            if (backRight != null) backRight.loadPose(net.minecraft.client.model.geom.PartPose.offsetAndRotation(-3.5F, 24.5F, 2.0F, -1.570796F, 0.8F, 0.0F));
            if (backLeft != null) backLeft.loadPose(net.minecraft.client.model.geom.PartPose.offsetAndRotation(2.5F, 24.5F, 3.5F, -1.570796F, -0.8F, 0.0F));
            if (frontRight != null) frontRight.setPos(-2.0F, 21.0F, -0.5F);
            if (frontLeft != null) frontLeft.setPos(2.0F, 21.0F, -0.5F);
            if (tail != null) tail.setPos(0.0F, 17.0F, 2.0F);
        } else if (entity.isHamsterStanding()) {
            heads.forEach(part -> { part.y = 9.6F; part.z = 4.5F; });
            if (body != null) { body.setPos(0.0F, 15.1F, 4.5F); body.xRot = Mth.cos(80.0F * Mth.DEG_TO_RAD); }
            if (backRight != null) backRight.setPos(-2.0F, 20.6F, 6.0F);
            if (backLeft != null) backLeft.setPos(2.0F, 20.6F, 6.0F);
            if (frontRight != null) frontRight.loadPose(net.minecraft.client.model.geom.PartPose.offsetAndRotation(-2.0F, 14.6F, 3.0F, Mth.cos(150.0F * Mth.DEG_TO_RAD), Mth.sin(-10.0F * Mth.DEG_TO_RAD), 0.0F));
            if (frontLeft != null) frontLeft.loadPose(net.minecraft.client.model.geom.PartPose.offsetAndRotation(2.0F, 14.6F, 3.0F, Mth.cos(150.0F * Mth.DEG_TO_RAD), Mth.sin(10.0F * Mth.DEG_TO_RAD), 0.0F));
            if (tail != null) tail.setPos(0.0F, 14.6F, 2.0F);
        }
        if (tail != null) {
            tail.xRot = 1.570796F;
            tail.zRot = entity.isTamed()
                    ? Mth.sin(ageInTicks * 3.141593F * 0.05F) * Mth.sin(ageInTicks * 3.141593F * 11.0F * 0.05F) * 0.15F * 3.141593F
                    : 0.0F;
        }
    }

    /**
     * Restores the constant pose that the 1.12 pig Java models assigned from
     * {@code setRotationAngles}.  It is deliberately applied after resetPose:
     * those models did not store this pose in their constructors, so baking
     * the geometry alone leaves every pig body upright.
     */
    private void applyPigRestPose(String id, boolean showPrivateParts) {
        boolean piglet = id.startsWith("piglet_");
        boolean largeBlack = id.endsWith("large_black");
        setRotation(child("body"), Mth.HALF_PI, 0.0F, 0.0F);

        float earX = largeBlack ? 0.5235987F : -0.2617994F;
        float earY = largeBlack ? 0.5235987F : 0.3490658F;
        float earZ = largeBlack ? 0.8726646F : 0.6981317F;
        for (String name : new String[]{"ear1", "ear1a", "ear1b"})
            setRotation(child("head/" + name), earX, earY, earZ);
        for (String name : new String[]{"ear2", "ear2a", "ear2b"})
            setRotation(child("head/" + name), earX, -earY, -earZ);

        if (piglet) {
            setRotation(child("tail1/tail1a"), 1.5F, 1.5F, 0.0F);
            return;
        }

        setRotation(child("tail1"), 0.1409582F, 0.2046205F, 0.0F);
        setRotation(child("tail1/tail1a"), 1.429837F, -2.936972F, -Mth.PI);
        for (int number = 1; number <= 6; number++)
            setRotation(child("nipple" + number), Mth.HALF_PI, 0.0F, 0.0F);
        if (showPrivateParts) setRotation(child("block_a"), 0.2617994F, 0.0F, 0.0F);
    }

    /**
     * The 1.12 goat models used two tiny dark horn-bud cubes as editor aids.
     * In a ModelPart layer their 3x1x3 faces become visible as a floating
     * black pixel above every goat's head.  Horns remain separate geometry;
     * only these shared artefact nodes are suppressed.
     */
    private void hideGoatHornBudArtifacts(AnimaniaAnimalEntity entity) {
        String id = entity.registryPath();
        if (!id.startsWith("buck_") && !id.startsWith("doe_") && !id.startsWith("kid_")) return;
        hide(child("head_node/bud__r"));
        hide(child("head_node/bud__l"));
    }

    private ModelPart child(String path) {
        ModelPart current = root;
        for (String segment : path.split("/")) {
            if (!current.hasChild(segment)) return null;
            current = current.getChild(segment);
        }
        return current;
    }

    private static void setRotation(ModelPart part, float xRot, float yRot, float zRot) {
        if (part == null) return;
        part.xRot = xRot;
        part.yRot = yRot;
        part.zRot = zRot;
    }

    private static void hide(ModelPart part) {
        if (part != null) part.visible = false;
    }

    /**
     * Recreates the part rotations that the 1.12 Java models applied while
     * their renderer was laying the animal down.  The modern port originally
     * kept only a small generic tilt, so the synchronized sleeping flag was
     * visible in tooltips but cattle, sheep, goats and horses remained
     * standing.  The generated layers retain the original part names, which
     * lets this shared model cover every breed without duplicating models.
     */
    private void applyLegacySleepingPose(AnimaniaAnimalEntity entity) {
        String id = entity.registryPath();
        float timer = Mth.clamp(entity.getSleepTimer(), -0.55F, 0.0F);

        if (id.startsWith("bull_") || id.startsWith("cow_") || id.startsWith("calf_")) {
            String[] legs = id.startsWith("cow_")
                    ? new String[]{"leg1", "leg2", "leg3", "leg4"}
                    : new String[]{"leg0", "leg1", "leg2", "leg3"};
            setXRotation(child(legs[0]), timer * -1.8F);
            setXRotation(child(legs[1]), timer * -1.8F);
            setXRotation(child(legs[2]), timer * 1.7F);
            setXRotation(child(legs[3]), timer * 1.75F);
            float body = Mth.HALF_PI + (timer > -0.28F ? -timer / 3.0F : timer / 3.0F);
            bodies.forEach(part -> part.xRot = body);
            float headFactor = id.startsWith("cow_") ? -2.8F : 2.8F;
            heads.forEach(part -> part.yRot = timer * headFactor);
            return;
        }

        if (id.startsWith("ewe_") || id.startsWith("lamb_") || id.startsWith("ram_")) {
            rotateSleepParts(timer * -1.8F,
                    "left_front_leg", "right_front_leg", "left_front_leg_wool", "right_front_leg_wool");
            rotateSleepParts(timer * 1.7F,
                    "left_back_leg", "right_back_leg", "left_back_leg_wool", "right_back_leg_wool");
            float headFactor = id.contains("dorper") ? -2.8F : id.startsWith("ram_") ? 4.0F : -4.5F;
            heads.forEach(part -> part.yRot = timer * headFactor);
            float body = timer > -0.28F ? -timer / 3.0F : timer / 3.0F;
            bodies.forEach(part -> part.xRot = body);
            return;
        }

        if (isGoatId(id)) {
            rotateSleepParts(timer * -1.8F,
                    "front_leg__l", "front_leg__r", "front_leg_wool__l", "front_leg_wool__r");
            rotateSleepParts(timer * 1.7F,
                    "back_leg__l", "back_leg__r", "back_leg_wool__l", "back_leg_wool__r");
            float headFactor = id.startsWith("doe_") && !id.equals("doe_angora") ? 2.8F : -2.8F;
            heads.forEach(part -> part.yRot = timer * headFactor);
            float body = timer > -0.28F ? -timer / 3.0F : timer / 3.0F;
            final float bodyRotation = id.equals("kid_kinder") ? body + Mth.HALF_PI : body;
            bodies.forEach(part -> part.xRot = bodyRotation);
            return;
        }

        if (id.startsWith("foal_") || id.startsWith("mare_") || id.startsWith("stallion_")) {
            rotateSleepParts(timer * -1.8F, "front_left_muscle", "front_right_muscle");
            rotateSleepParts(timer * 1.7F, "back_left_muscle");
            rotateSleepParts(timer * 1.75F, "back_right_muscle");
            heads.forEach(part -> part.yRot = timer * (id.startsWith("stallion_") ? -2.8F : 2.8F));
            float body = timer > -0.28F ? -timer / 3.0F : timer / 3.0F;
            bodies.forEach(part -> part.xRot = body);
            return;
        }

        // Pigs, birds and the extra-addon small animals use renderer-only
        // sleeping transforms in the original code; their model parts must
        // not receive the old generic tilt or wing flapping.
        if (id.startsWith("piglet_") || id.startsWith("hog_") || id.startsWith("sow_")
                || id.startsWith("chick_") || id.startsWith("hen_") || id.startsWith("rooster_")
                || isRabbitId(id)
                || id.startsWith("peachick_") || id.startsWith("peacock_") || id.startsWith("peahen_")
                || id.startsWith("ferret_") || id.startsWith("hamster") || id.startsWith("hedgehog")) {
            if (id.startsWith("peacock_")) {
                rotateSleepParts(-1.5F, "fan_node_a", "fan_node_b", "fan_node_c", "fan_node_d");
            }
            return;
        }

        // Preserve a visible pose for third-party legacy profiles that do not
        // yet have a dedicated renderer mapping.
        bodies.forEach(part -> part.zRot += 0.12F);
        heads.forEach(part -> part.xRot += 0.35F);
    }

    private void rotateSleepParts(float value, String... paths) {
        for (String path : paths) setXRotation(child(path), value);
    }

    private static boolean isGoatId(String id) {
        if (!(id.startsWith("buck_") || id.startsWith("doe_") || id.startsWith("kid_"))) return false;
        return id.endsWith("_alpine") || id.endsWith("_angora") || id.endsWith("_fainting")
                || id.endsWith("_kiko") || id.endsWith("_kinder") || id.endsWith("_nigerian_dwarf")
                || id.endsWith("_pygmy");
    }

    private static boolean isRabbitId(String id) {
        return id.startsWith("kit_")
                || ((id.startsWith("buck_") || id.startsWith("doe_")) && !isGoatId(id));
    }

    private static void setXRotation(ModelPart part, float value) {
        if (part != null) part.xRot = value;
    }

    /**
     * IDs whose 1.12 models used the shared standard-dog gait.  The smaller
     * Chihuahua/Corgi/Dachshund/Pomeranian/Pug models have different leg
     * formulas and must retain their breed-specific animation profiles.
     */
    private static boolean usesStandardDogGait(String id) {
        if (!(id.startsWith("female_") || id.startsWith("male_") || id.startsWith("puppy_"))) return false;
        return id.endsWith("_blood_hound") || id.endsWith("_collie") || id.endsWith("_fox")
                || id.endsWith("_german_shepherd") || id.endsWith("_great_dane")
                || id.endsWith("_greyhound") || id.endsWith("_husky") || id.endsWith("_labrador")
                || id.endsWith("_poodle") || id.endsWith("_wolf");
    }

    /**
     * Renders the model body normally and restores local render scales used
     * by the 1.12 Java models. ModelPart has no equivalent of the old
     * ModelRenderer.render(customScale), so those roots must be isolated from
     * the normal root pass and rendered under a temporary PoseStack scale.
     */
    private void renderRootWithLegacyFanScale(PoseStack pose, VertexConsumer consumer,
                                              int packedLight, int packedOverlay,
                                              float red, float green, float blue, float alpha) {
        boolean scaledPigletTail = renderPigletTailAtLegacyScale && pigletTail != null;
        if (fanNodes.isEmpty() && !scaledPigletTail) {
            root.render(pose, consumer, packedLight, packedOverlay, red, green, blue, alpha);
            return;
        }

        boolean[] visible = new boolean[fanNodes.size()];
        boolean[] skipDraw = new boolean[fanNodes.size()];
        for (int i = 0; i < fanNodes.size(); i++) {
            ModelPart fan = fanNodes.get(i);
            visible[i] = fan.visible;
            skipDraw[i] = fan.skipDraw;
            // The ordinary root pass must not draw the fan roots.  They are
            // rendered below with the legacy scale, so this is only a
            // temporary visibility change.
            fan.visible = false;
        }
        boolean pigletTailVisible = scaledPigletTail && pigletTail.visible;
        boolean pigletTailSkipDraw = scaledPigletTail && pigletTail.skipDraw;
        if (scaledPigletTail) pigletTail.visible = false;
        try {
            root.render(pose, consumer, packedLight, packedOverlay, red, green, blue, alpha);
            if (!fanNodes.isEmpty()) {
                pose.pushPose();
                try {
                    pose.scale(LEGACY_PEACOCK_FAN_RENDER_SCALE,
                            LEGACY_PEACOCK_FAN_RENDER_SCALE,
                            LEGACY_PEACOCK_FAN_RENDER_SCALE);
                    for (int i = 0; i < fanNodes.size(); i++) {
                        if (!visible[i]) continue;
                        ModelPart fan = fanNodes.get(i);
                        // ModelPart.render() checks both flags. Restore them for
                        // this explicit pass after hiding the ordinary pass.
                        fan.visible = true;
                        fan.skipDraw = false;
                        fan.render(pose, consumer, packedLight, packedOverlay,
                                red, green, blue, alpha);
                        fan.visible = false;
                    }
                } finally {
                    pose.popPose();
                }
            }
            if (pigletTailVisible && !pigletTailSkipDraw) {
                pose.pushPose();
                try {
                    // Scaling before ModelPart.render reproduces the old
                    // Tail1.render(scale * .8F): pivot, cubes and children all
                    // shrink together around the model origin.
                    pose.scale(LEGACY_PIGLET_TAIL_RENDER_SCALE,
                            LEGACY_PIGLET_TAIL_RENDER_SCALE,
                            LEGACY_PIGLET_TAIL_RENDER_SCALE);
                    pigletTail.visible = true;
                    pigletTail.skipDraw = false;
                    pigletTail.render(pose, consumer, packedLight, packedOverlay,
                            red, green, blue, alpha);
                    pigletTail.visible = false;
                } finally {
                    pose.popPose();
                }
            }
        } finally {
            for (int i = 0; i < fanNodes.size(); i++) {
                fanNodes.get(i).visible = visible[i];
                fanNodes.get(i).skipDraw = skipDraw[i];
            }
            if (scaledPigletTail) {
                pigletTail.visible = pigletTailVisible;
                pigletTail.skipDraw = pigletTailSkipDraw;
            }
        }
    }

    /**
     * Restores the constant rotations assigned by the 1.12 chicken models in
     * {@code setRotationAngles}.  The generated layers contain the constructor
     * offsets but not these per-frame rotations, leaving tails, beaks, combs
     * and lower legs in their editor pose after the port.
     */
    private void applyChickenRestPose(String id) {
        boolean chick = id.startsWith("chick_");
        setRotation(child(chick ? "body" : "body1"), Mth.HALF_PI, 0.0F, 0.0F);

        if (chick) {
            setRotation(child("tail1"), 0.3593722F, 0.0F, 0.0F);
            setRotation(child("tail1/tail2"), 0.6340498F, 0.0F, 0.0F);
            for (String wing : new String[]{"wing1", "wing2", "wing3", "wing4"})
                setRotation(child(wing), 0.1139416F, 0.0F, 0.0F);
            setRotation(child("neck/head"), -0.0213736F, 0.0F, 0.0F);
            setRotation(child("neck/beak_top"), 0.7268012F, 0.0F, 0.0F);
            for (String leg : new String[]{"leg1_top/leg1", "leg2_top/leg2"})
                setRotation(child(leg), -0.2617994F, 0.0F, 0.0F);
            for (String foot : new String[]{"leg1_top/foot1", "leg2_top/foot2"})
                setRotation(child(foot), Mth.HALF_PI, 0.0F, 0.0F);
            return;
        }

        setRotation(child("tail1"), 0.2144478F, 0.0F, 0.0F);
        setRotation(child("tail1/tail2"), 0.5295422F, 0.0F, 0.0F);
        for (String leg : new String[]{"leg1_pivot/leg1_top", "leg2_pivot/leg2_top"})
            setRotation(child(leg), 0.2617994F, 0.0F, 0.0F);
        for (String leg : new String[]{"leg1_pivot/leg1", "leg2_pivot/leg2"})
            setRotation(child(leg), -0.2617995F, 0.0F, 0.0F);
        setRotation(child("neck/neck2"), -0.7360098F, 0.0F, 0.0F);
        setRotation(child("neck/head"), 0.05872217F, 0.0F, 0.0F);
        setRotation(child("neck/crest"), 0.3490659F, 0.0F, 0.0F);
        setRotation(child("neck/crest_bottom"), 0.0F, 0.0F, 0.0F);
        setRotation(child("neck/beak_bottom"), 0.05872219F, 0.0F, 0.0F);
        setRotation(child("neck/beak_top"), 0.3169494F, 0.0F, 0.0F);

        if (id.startsWith("rooster_")) {
            setRotation(child("feather1"), 0.5097123F, -0.3010362F, -0.1503443F);
            setRotation(child("feather2"), 0.5097123F, 0.3010362F, 0.1503443F);
            setRotation(child("feather3"), 0.5295422F, 0.0F, 0.0F);
        }
    }

    private static boolean isChickenId(String id) {
        return id.startsWith("chick_") || id.startsWith("hen_") || id.startsWith("rooster_");
    }

    @Override
    public void renderToBuffer(PoseStack pose, VertexConsumer consumer, int packedLight, int packedOverlay,
                               float red, float green, float blue, float alpha) {
        if (coloredParts.isEmpty()) {
            renderRootWithLegacyFanScale(pose, consumer, packedLight, packedOverlay,
                    red, green, blue, alpha);
            return;
        }
        try {
            coloredParts.forEach(part -> part.skipDraw = true);
            renderRootWithLegacyFanScale(pose, consumer, packedLight, packedOverlay,
                    red, green, blue, alpha);
            root.getAllParts().forEach(part -> part.skipDraw = true);
            coloredParts.forEach(part -> part.skipDraw = false);
            renderRootWithLegacyFanScale(pose, consumer, packedLight, packedOverlay,
                    red * woolRed, green * woolGreen, blue * woolBlue, alpha);
        } finally {
            root.getAllParts().forEach(part -> part.skipDraw = false);
        }
    }

    private static List<ModelPart> resolve(ModelPart root, String[] paths) {
        List<ModelPart> result = new ArrayList<>(paths.length);
        for (String path : paths) {
            ModelPart current = root;
            boolean valid = true;
            for (String segment : path.split("/")) {
                if (!current.hasChild(segment)) { valid = false; break; }
                current = current.getChild(segment);
            }
            if (valid) result.add(current);
        }
        return result;
    }

    private static List<ResolvedPose> resolvePose(ModelPart root, LegacyPoseDefinition definition) {
        LegacyPartPose[] definitions = definition.parts();
        List<ResolvedPose> result = new ArrayList<>(definitions.length);
        for (LegacyPartPose pose : definitions) {
            List<ModelPart> resolved = resolve(root, new String[]{pose.path()});
            if (!resolved.isEmpty()) result.add(new ResolvedPose(resolved.get(0), pose));
        }
        return result;
    }

    private static void applyPose(List<ResolvedPose> poses) {
        for (ResolvedPose resolved : poses) {
            ModelPart part = resolved.part();
            LegacyPartPose pose = resolved.pose();
            if (Float.isFinite(pose.x())) part.x = pose.x();
            if (Float.isFinite(pose.y())) part.y = pose.y();
            if (Float.isFinite(pose.z())) part.z = pose.z();
            if (Float.isFinite(pose.xRot())) part.xRot = pose.xRot();
            if (Float.isFinite(pose.yRot())) part.yRot = pose.yRot();
            if (Float.isFinite(pose.zRot())) part.zRot = pose.zRot();
        }
    }

    private record ResolvedPose(ModelPart part, LegacyPartPose pose) {}
}
