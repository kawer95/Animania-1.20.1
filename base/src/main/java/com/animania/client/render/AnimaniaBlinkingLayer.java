package com.animania.client.render;

import com.animania.client.model.LegacyAnimalModel;
import com.animania.common.entity.AnimaniaAnimalEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Native 1.20.1 replacement for the 1.12 LayerBlinking renderer.
 *
 * The old implementation rendered the already-posed model twice with
 * transparent left/right eyelid textures.  Keeping that contract is
 * important: the blink images contain only the eyelid pixels and must not
 * replace the breed coat texture.  The legacy layer also multiplied the
 * white opaque pixels in those images by a breed-specific coat colour.  The
 * colour multiplication is not optional: without it the one-pixel eyelids
 * render as white eyes during a blink.  The timer is server state and is
 * synced through SynchedEntityData by {@link AnimaniaAnimalEntity}.
 */
public final class AnimaniaBlinkingLayer extends RenderLayer<AnimaniaAnimalEntity, LegacyAnimalModel> {
    public AnimaniaBlinkingLayer(RenderLayerParent<AnimaniaAnimalEntity, LegacyAnimalModel> parent) {
        super(parent);
    }

    /** Returns the two transparent eyelid textures for a registered ID. */
    public static ResourceLocation[] texturesFor(ResourceLocation id) {
        if (id == null) return null;
        String namespace = id.getNamespace();
        String path = id.getPath();
        if ("animania_farm".equals(namespace)) {
            String base;
            if (path.startsWith("cow_")) base = "cows/cow_blink";
            else if (path.startsWith("bull_")) base = "cows/bull_blink";
            else if (path.startsWith("calf_")) base = "cows/calf_blink";
            else if (path.startsWith("doe_angora") || path.startsWith("buck_angora") || path.startsWith("kid_angora")) base = "goats/angora_blink";
            else if (path.startsWith("doe_") || path.startsWith("buck_") || path.startsWith("kid_")) base = "goats/goats_blink";
            else if (path.startsWith("mare_") || path.startsWith("stallion_") || path.startsWith("foal_")) base = "horses/horse_blink";
            else if (path.startsWith("piglet_")) base = "pigs/piglet_blink";
            else if (path.startsWith("sow_") || path.startsWith("hog_")) base = path.endsWith("_hampshire") ? "pigs/hampshire_blink" : "pigs/pig_blink";
            else if (path.startsWith("hen_") || path.startsWith("rooster_")) base = "chickens/chicken_blink";
            else if (path.startsWith("chick_")) base = "chickens/chick_blink";
            else if (path.startsWith("ewe_") || path.startsWith("ram_") || path.startsWith("lamb_")) base = "sheep/sheep_blink";
            else return null;
            return pair(namespace, base);
        }
        if ("animania_extra".equals(namespace)) {
            String base;
            if (path.equals("hamster")) base = "rodents/hamster_blink";
            else if (path.startsWith("ferret_")) base = "rodents/ferret_blink";
            else if (path.startsWith("hedgehog")) base = "rodents/hedgehog_blink";
            else if (path.startsWith("doe_") || path.startsWith("buck_") || path.startsWith("kit_")) base = "rabbits/rabbit_blink";
            else if (path.startsWith("peachick_")) base = "peacocks/peachick_blink";
            else if (path.startsWith("peacock_")) base = "peacocks/peacock_blink";
            else if (path.startsWith("peahen_")) base = "peacocks/peafowl_blink";
            else return null;
            return pair(namespace, base);
        }
        if ("animania_catsdogs".equals(namespace)) {
            if (path.startsWith("tom_") || path.startsWith("queen_") || path.startsWith("kitten_")) {
                String breed = path.substring(path.indexOf('_') + 1);
                String base = (breed.equals("ragdoll") || breed.equals("norwegian")) ? "cats/blink_2" : "cats/blink_1";
                return pair(namespace, base);
            }
            if (path.startsWith("male_") || path.startsWith("female_") || path.startsWith("puppy_")) {
                String breed = path.substring(path.indexOf('_') + 1);
                String base = switch (breed) {
                    case "blood_hound" -> "dogs/blink_blood_hound";
                    case "chihuahua" -> "dogs/blink_chihuahua";
                    case "corgi" -> "dogs/blink_corgi";
                    case "dachshund" -> "dogs/blink_dachshund";
                    case "fox" -> "dogs/blink_fox";
                    case "greyhound" -> "dogs/blink_greyhound";
                    case "pomeranian" -> "dogs/blink_pomeranian";
                    case "poodle" -> "dogs/blink_poodle";
                    case "pug" -> "dogs/blink_pug";
                    default -> "dogs/blink_collie";
                };
                // Cat eyelids were supplied as two transparent layers. The
                // original dog renderers instead use one full-size overlay
                // (for example blink_collie.png), so appending _left/_right
                // requested files that have never existed and flashed the
                // missing-texture checkerboard for the blink duration.
                return single(namespace, base);
            }
        }
        return null;
    }

    private static ResourceLocation[] pair(String namespace, String base) {
        return new ResourceLocation[]{
                ResourceLocation.fromNamespaceAndPath(namespace, "textures/entity/" + base + "_left.png"),
                ResourceLocation.fromNamespaceAndPath(namespace, "textures/entity/" + base + "_right.png")};
    }

    private static ResourceLocation[] single(String namespace, String base) {
        return new ResourceLocation[]{
                ResourceLocation.fromNamespaceAndPath(namespace, "textures/entity/" + base + ".png")};
    }

    /**
     * Legacy LayerBlinking's RGB tint for each overlay.  The source 1.12
     * renderer passed these values from each breed renderer; the modern
     * renderer has one shared layer, so we recover the same values from the
     * registered legacy entity id instead.
     */
    public static int[] colorsFor(ResourceLocation id) {
        if (id == null) return null;
        String namespace = id.getNamespace();
        String path = id.getPath();
        if ("animania_farm".equals(namespace)) {
            if (path.startsWith("piglet_") || path.startsWith("sow_") || path.startsWith("hog_")) {
                return new int[]{pigColor(path)};
            }
            if (path.startsWith("cow_") || path.startsWith("bull_") || path.startsWith("calf_")) {
                return cowColor(path);
            }
            if (path.startsWith("doe_") || path.startsWith("buck_") || path.startsWith("kid_")) {
                return new int[]{goatColor(path)};
            }
            if (path.startsWith("ewe_") || path.startsWith("ram_") || path.startsWith("lamb_")) {
                return new int[]{sheepColor(path)};
            }
            // Chickens and draft horses used a black tint in the legacy
            // renderers; their blink images are otherwise the same white
            // pixel overlays as the pig/cow images.
            if (path.startsWith("hen_") || path.startsWith("rooster_") || path.startsWith("chick_")
                    || path.startsWith("mare_") || path.startsWith("stallion_") || path.startsWith("foal_")) {
                return new int[]{0x000000};
            }
        }
        if ("animania_extra".equals(namespace)) {
            if (path.startsWith("doe_") || path.startsWith("buck_") || path.startsWith("kit_")) {
                return new int[]{rabbitColor(path)};
            }
            if (path.startsWith("ferret_")) {
                return new int[]{path.endsWith("_grey") ? 0x58372D : 0xC9C8B7};
            }
            if (path.equals("hedgehog_albino")) return new int[]{0xF6F0C7};
            if (path.equals("hedgehog")) return new int[]{0xD3CDAB};
            if (path.equals("hamster") || path.startsWith("peachick_") || path.startsWith("peacock_")
                    || path.startsWith("peahen_")) return new int[]{0x000000};
        }
        if ("animania_catsdogs".equals(namespace)) {
            String breed = path.substring(path.indexOf('_') + 1);
            if (path.startsWith("tom_") || path.startsWith("queen_") || path.startsWith("kitten_")) {
                return new int[]{catColor(breed)};
            }
            if (path.startsWith("male_") || path.startsWith("female_") || path.startsWith("puppy_")) {
                return new int[]{dogColor(breed)};
            }
        }
        return null;
    }

    private static int pigColor(String path) {
        if (path.endsWith("_duroc")) return 0x421006;
        if (path.endsWith("_hampshire") || path.endsWith("_large_black")) return 0x3A3333;
        if (path.endsWith("_large_white")) return 0xC4A8A8;
        if (path.endsWith("_old_spot")) return 0x514B4B;
        return 0xE07F7D; // Yorkshire and the legacy default
    }

    private static int[] cowColor(String path) {
        if (path.endsWith("_angus")) return new int[]{0x333333};
        if (path.endsWith("_friesian")) return new int[]{0x463930, 0xDEDEDE};
        if (path.endsWith("_holstein")) return new int[]{0x1C242B, 0xDEDEDE};
        if (path.endsWith("_highland")) return new int[]{path.startsWith("calf_") ? 0x5B2F1B : 0x130D0A};
        if (path.endsWith("_jersey")) return new int[]{path.startsWith("calf_") ? 0x7C632D : 0x3B2603};
        if (path.endsWith("_mooshroom")) return new int[]{0xAB0F0F};
        return new int[]{0xDEDEDE}; // Hereford, Longhorn and the legacy default
    }

    private static int goatColor(String path) {
        if (path.endsWith("_angora")) return 0xCAC4B7;
        if (path.endsWith("_fainting")) return 0x6B6968;
        if (path.endsWith("_kiko")) return 0x694330;
        if (path.endsWith("_kinder")) return 0x6F4935;
        if (path.endsWith("_nigerian_dwarf")) return 0x404040;
        if (path.endsWith("_pygmy")) return 0x2B2E2E;
        return 0x83786D; // Alpine and the legacy default
    }

    private static int sheepColor(String path) {
        if (path.endsWith("_dorper")) return 0x222222;
        if (path.endsWith("_jacob")) return 0x353535;
        if (path.endsWith("_suffolk")) return 0x1D1D1D;
        return 0x000000; // Dorset, Friesian, Merino and the legacy default
    }

    private static int rabbitColor(String path) {
        if (path.endsWith("_chinchilla")) return 0x9E9E9E;
        if (path.endsWith("_cottontail")) return 0x896E58;
        if (path.endsWith("_dutch") || path.endsWith("_havana")) return 0x404040;
        if (path.endsWith("_jack")) return 0x938375;
        if (path.endsWith("_new_zealand")) return 0xF4F2F2;
        if (path.endsWith("_rex")) return 0x574133;
        return 0x000000; // Lop and the legacy default
    }

    private static int catColor(String breed) {
        return switch (breed) {
            case "ragdoll" -> 0x83786D;
            case "norwegian" -> 0x4E3C30;
            case "american_shorthair" -> 0x7D7D7D;
            case "asiatic" -> 0x836951;
            case "exotic" -> 0xA75823;
            case "ocelot" -> 0xA47947;
            case "siamese" -> 0x271D1B;
            default -> 0x594336; // Tabby and the legacy default
        };
    }

    private static int dogColor(String breed) {
        return switch (breed) {
            case "blood_hound" -> 0xA56234;
            case "chihuahua" -> 0xF6F1EC;
            case "collie" -> 0x403025;
            case "corgi" -> 0xFBFBFB;
            case "dachshund" -> 0x000000;
            case "fox" -> 0xAD5D3C;
            case "greyhound" -> 0x8C5C34;
            case "german_shepherd", "great_dane" -> 0x815940;
            case "husky" -> 0xC4C4C4;
            case "labrador" -> 0xC09D77;
            case "pomeranian" -> 0xFCFCFC;
            case "poodle" -> 0xF5F2ED;
            case "pug" -> 0xE8E3DF;
            case "wolf" -> 0xBCB6B0;
            default -> 0x403025;
        };
    }

    /**
     * The 1.12 LayerBlinking renderer also forced the eyelid overlay while an
     * animal was in its sleeping pose.  It used the same two transition
     * sentinels as the legacy sleep renderer: the overlay is visible when the
     * pose starts (timer 0), once the animal is fully down (-0.55), and for a
     * legacy save that still contains the old -100 sentinel.  Keep the
     * 23,250 daytime cutoff so the modern layer has the same wake transition.
     */
    static boolean shouldRenderSleepingEyes(boolean sleeping, float sleepTimer, long dayTime) {
        long currentTime = Math.floorMod(dayTime, 23999L);
        return sleeping && currentTime < 23250L
                && (sleepTimer == -100.0F || sleepTimer == 0.0F || sleepTimer <= -0.55F);
    }

    private static boolean shouldRenderSleepingEyes(AnimaniaAnimalEntity entity) {
        return shouldRenderSleepingEyes(entity.isSleeping(), entity.getSleepTimer(), entity.level().getDayTime());
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffers, int packedLight,
                       AnimaniaAnimalEntity entity, float limbSwing, float limbSwingAmount,
                       float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        int timer = entity.getBlinkTimer();
        if (!shouldRenderSleepingEyes(entity) && (timer < 0 || timer >= 7)) return;
        ResourceLocation[] textures = texturesFor(ForgeRegistries.ENTITY_TYPES.getKey(entity.getType()));
        if (textures == null) return;
        int[] colors = colorsFor(ForgeRegistries.ENTITY_TYPES.getKey(entity.getType()));
        if (colors == null || colors.length == 0) colors = new int[]{0xFFFFFF};
        for (int i = 0; i < textures.length; i++) {
            ResourceLocation texture = textures[i];
            int color = colors[Math.min(i, colors.length - 1)];
            VertexConsumer consumer = buffers.getBuffer(RenderType.entityTranslucent(texture));
            getParentModel().renderToBuffer(poseStack, consumer, packedLight, OverlayTexture.NO_OVERLAY,
                    ((color >> 16) & 0xFF) / 255.0F,
                    ((color >> 8) & 0xFF) / 255.0F,
                    (color & 0xFF) / 255.0F, 1.0F);
        }
    }
}
