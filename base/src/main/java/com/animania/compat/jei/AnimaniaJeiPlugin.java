package com.animania.compat.jei;

import com.animania.Animania;
import com.animania.common.AnimaniaItems;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/** JEI's optional plugin; vanilla data-pack recipes are indexed automatically. */
@JeiPlugin
@OnlyIn(Dist.CLIENT)
public final class AnimaniaJeiPlugin implements IModPlugin {
    private static final ResourceLocation UID = new ResourceLocation(Animania.MOD_ID, "jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return UID;
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        registration.addItemStackInfo(AnimaniaItems.MANUAL.get().getDefaultInstance(),
                Component.translatable("jei.animania.manual_info"));
        // Vanilla data-pack recipes are discovered by JEI itself. Add concise
        // descriptions for the legacy/custom processing endpoints (hives,
        // cheese moulds, pet facilities, vehicles and all addon items) so
        // optional JEI users can still discover interactions without a hard
        // dependency on addon classes.
        ForgeRegistries.ITEMS.forEach(item -> {
            ResourceLocation id = ForgeRegistries.ITEMS.getKey(item);
            if (id == null || !(id.getNamespace().equals("animania") || id.getNamespace().startsWith("animania_"))) return;
            String legacyDescription = legacyDescription(id);
            registration.addItemStackInfo(new ItemStack(item),
                    legacyDescription == null
                            ? Component.translatable("jei.animania.registry_info", Component.literal(id.toString()))
                            : Component.translatable(legacyDescription));
        });
    }

    private static String legacyDescription(ResourceLocation id) {
        if ("animania".equals(id.getNamespace())
                && ("slop_bucket".equals(id.getPath()) || "bucket_slop".equals(id.getPath()))) {
            return "text.jei.slop";
        }
        if (!"animania_farm".equals(id.getNamespace())) return null;
        return switch (id.getPath()) {
            case "truffle" -> "text.jei.truffle";
            case "salt" -> "text.jei.salt";
            case "milk_holstein_bucket" -> "text.jei.milkholstein";
            case "milk_friesian_bucket" -> "text.jei.milkfriesian";
            case "milk_jersey_bucket" -> "text.jei.milkjersey";
            case "milk_goat_bucket" -> "text.jei.milkgoat";
            case "milk_sheep_bucket" -> "text.jei.milksheep";
            default -> null;
        };
    }
}
