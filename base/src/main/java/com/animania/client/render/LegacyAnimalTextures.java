package com.animania.client.render;

import com.animania.common.entity.AnimaniaAnimalEntity;
import net.minecraft.resources.ResourceLocation;

/** Deterministic resolver for the original LGPL Animania texture layout. */
public final class LegacyAnimalTextures {
    private LegacyAnimalTextures() { }

    public static ResourceLocation resolve(ResourceLocation id, AnimaniaAnimalEntity animal) {
        return resolve(id, animal.getVariantName(), animal.isSheared());
    }

    public static ResourceLocation resolve(ResourceLocation id, String variant, boolean sheared) {
        String path = id.getPath();
        return switch (id.getNamespace()) {
            case "animania_farm", "animania_extra", "animania_catsdogs" ->
                    legacy(id.getNamespace(), switch (id.getNamespace()) {
                        case "animania_farm" -> farm(path, variant, sheared);
                        case "animania_extra" -> extra(path, variant);
                        default -> catsDogs(path, variant);
                    });
            default -> new ResourceLocation(id.getNamespace(), "textures/entity/" + path + ".png");
        };
    }

    private static ResourceLocation legacy(String namespace, String path) {
        return new ResourceLocation(namespace, "textures/entity/" + path + ".png");
    }

    private static String farm(String id, String variant, boolean sheared) {
        if (id.equals("cart") || id.equals("wagon") || id.equals("tiller")) return "props/" + id;
        if (startsWithAny(id, "cow_", "bull_", "calf_")) return "cows/" + id;
        if (startsWithAny(id, "doe_", "buck_", "kid_")) {
            String normalized = id.replace("nigerian_dwarf", "nigerian");
            // The 1.12 doe deliberately shared the buck Angora coat; only
            // adult buck/doe Angoras could be sheared.  Constructing
            // doe_angora_sheared (or kid_angora_sheared) points at files that
            // never existed and renders the entity purple/black.
            if (id.equals("doe_angora")) normalized = "buck_angora";
            if (sheared && (id.equals("buck_angora") || id.equals("doe_angora"))) {
                normalized = "buck_angora_sheared";
            }
            return "goats/" + normalized;
        }
        if (startsWithAny(id, "sow_", "hog_", "piglet_")) return "pigs/" + id;
        if (startsWithAny(id, "mare_draft", "stallion_draft", "foal_draft")) {
            return "horses/draft_horse_" + safe(variant, "black");
        }
        if (startsWithAny(id, "hen_", "rooster_", "chick_")) {
            String role = id.substring(0, id.indexOf('_'));
            String breed = id.substring(id.indexOf('_') + 1);
            String colour = switch (breed) {
                case "leghorn" -> "white";
                case "orpington" -> "golden";
                case "plymouth_rock" -> "specked";
                case "rhode_island_red" -> "red";
                case "wyandotte" -> "brown";
                default -> "white";
            };
            return "chickens/" + role + "_" + colour;
        }
        if (startsWithAny(id, "ewe_", "ram_", "lamb_")) return sheep(id, variant, sheared);
        return id;
    }

    private static String sheep(String id, String variant, boolean sheared) {
        String role = id.substring(0, id.indexOf('_'));
        String breed = id.substring(id.indexOf('_') + 1);
        String texture;
        if (breed.equals("dorper")) texture = "sheep_dorper";
        else if (breed.equals("jacob")) texture = role.equals("lamb") ? "sheep_jacob_lamb" : "sheep_jacob";
        else {
            String colour = safe(variant, "white");
            if (breed.equals("friesian")) {
                texture = "sheep_friesian_" + colour + (role.equals("ram") ? "_ram" : "");
            } else {
                String sex = role.equals("ram") ? "ram" : "ewe";
                texture = "sheep_" + breed + "_" + colour + "_" + sex;
            }
        }
        if (sheared && texture.equals("sheep_jacob_lamb")) texture = "sheep_jacob";
        return "sheep/" + texture + (sheared ? "_sheared" : "");
    }

    private static String extra(String id, String variant) {
        if (id.equals("dartfrog")) return "amphibians/dartfrogs/" + safe(variant, "blue") + "_dart_frog";
        if (id.equals("frog")) return "amphibians/frogs/" + safe(variant, "default") + "_frog";
        if (id.equals("toad")) return "amphibians/toads/toad";
        if (id.equals("hamster")) return "rodents/hamster_" + safe(variant, "black");
        if (id.equals("hedgehog")) return "rodents/hedgehog";
        if (id.equals("hedgehog_albino")) return "rodents/hedgehog_white";
        if (id.startsWith("ferret_")) return "rodents/" + id;
        if (startsWithAny(id, "doe_", "buck_", "kit_")) {
            String breed = id.substring(id.indexOf('_') + 1);
            return "rabbits/rabbit_" + (breed.equals("lop") ? "lop_" + safe(variant, "black") : breed);
        }
        if (startsWithAny(id, "peacock_", "peahen_", "peachick_")) {
            String colour = id.substring(id.indexOf('_') + 1);
            String role = id.startsWith("peacock_") ? "peacock" : id.startsWith("peachick_") ? "peachick" : "peafowl";
            return "peacocks/" + role + "_" + colour;
        }
        return id;
    }

    private static String catsDogs(String id, String variant) {
        if (startsWithAny(id, "tom_", "queen_", "kitten_")) {
            return "cats/" + id.substring(id.indexOf('_') + 1);
        }
        String breed = id.substring(id.indexOf('_') + 1);
        return "dogs/" + switch (breed) {
            case "chihuahua", "collie" -> breed + safe(variant, "0");
            case "labrador", "poodle", "wolf" -> breed + safe(variant, "0");
            default -> breed;
        };
    }

    private static boolean startsWithAny(String value, String... prefixes) {
        for (String prefix : prefixes) if (value.startsWith(prefix)) return true;
        return false;
    }

    private static String safe(String variant, String fallback) {
        return variant == null || variant.isBlank() || variant.equals("default") ? fallback : variant;
    }
}
