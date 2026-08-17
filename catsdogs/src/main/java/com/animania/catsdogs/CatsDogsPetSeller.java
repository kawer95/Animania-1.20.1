package com.animania.catsdogs;

import com.google.common.collect.ImmutableSet;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraftforge.event.village.VillagerTradesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.village.poi.PoiType;

import java.util.ArrayList;
import java.util.List;

/** Modern Forge profession and trade table replacing the 1.12 pet seller. */
public final class CatsDogsPetSeller {
    public static final DeferredRegister<VillagerProfession> PROFESSIONS =
            DeferredRegister.create(ForgeRegistries.VILLAGER_PROFESSIONS, AnimaniaCatsDogs.MOD_ID);
    public static final DeferredRegister<PoiType> POI_TYPES =
            DeferredRegister.create(ForgeRegistries.POI_TYPES, AnimaniaCatsDogs.MOD_ID);
    public static final ResourceKey<PoiType> PET_SELLER_POI_KEY = ResourceKey.create(Registries.POINT_OF_INTEREST_TYPE,
            new ResourceLocation(AnimaniaCatsDogs.MOD_ID, "pet_seller"));
    public static final RegistryObject<PoiType> PET_SELLER_POI = POI_TYPES.register("pet_seller", () ->
            new PoiType(java.util.Set.copyOf(CatsDogsContent.PET_BOWL.get().getStateDefinition().getPossibleStates()), 1, 1));
    public static final RegistryObject<VillagerProfession> PET_SELLER = PROFESSIONS.register("pet_seller", () ->
            new VillagerProfession("pet_seller", holder -> holder.is(PET_SELLER_POI_KEY),
                    holder -> holder.is(PET_SELLER_POI_KEY), ImmutableSet.of(), ImmutableSet.of(),
                    SoundEvents.VILLAGER_WORK_LEATHERWORKER));

    private CatsDogsPetSeller() { }

    @SubscribeEvent
    public static void addTrades(VillagerTradesEvent event) {
        if (event.getType() != PET_SELLER.get()) return;
        Int2ObjectMap<List<VillagerTrades.ItemListing>> trades = event.getTrades();
        add(trades, 1, "american_shorthair", true, 10, 20);
        add(trades, 2, "ragdoll", true, 10, 20);
        add(trades, 3, "norwegian", true, 10, 20);
        add(trades, 2, "asiatic", true, 15, 25);
        add(trades, 3, "exotic", true, 15, 25);
        add(trades, 2, "tabby", true, 15, 25);
        add(trades, 3, "siamese", true, 25, 35);
        add(trades, 1, "blood_hound", false, 15, 30);
        add(trades, 2, "chihuahua", false, 20, 30);
        add(trades, 3, "collie", false, 20, 30);
        add(trades, 1, "corgi", false, 15, 25);
        add(trades, 2, "dachshund", false, 10, 20);
        add(trades, 3, "german_shepherd", false, 20, 30);
        add(trades, 1, "great_dane", false, 20, 30);
        add(trades, 2, "greyhound", false, 20, 30);
        add(trades, 3, "husky", false, 20, 25);
        add(trades, 1, "labrador", false, 20, 30);
        add(trades, 2, "pomeranian", false, 15, 25);
        add(trades, 3, "poodle", false, 15, 25);
        add(trades, 1, "pug", false, 15, 25);
    }

    private static void add(Int2ObjectMap<List<VillagerTrades.ItemListing>> trades, int level,
                             String family, boolean cat, int min, int max) {
        String adultPrefix = cat ? "queen_" : "female_";
        String malePrefix = cat ? "tom_" : "male_";
        String childPrefix = cat ? "kitten_" : "puppy_";
        trades.computeIfAbsent(level, ignored -> new ArrayList<>()).add(listing(malePrefix + family, min, max));
        trades.get(level).add(listing(adultPrefix + family, min, max));
        trades.get(level).add(listing(childPrefix + family, min + min / 2, max + max / 2));
    }

    private static VillagerTrades.ItemListing listing(String id, int min, int max) {
        return (Entity trader, RandomSource random) -> {
            var item = CatsDogsContent.ITEM_ENTRIES.get("entity_egg_" + id);
            if (item == null) return null;
            int price = min + random.nextInt(Math.max(1, max - min + 1));
            return new MerchantOffer(new ItemStack(Items.EMERALD, price),
                    new ItemStack(item.get()), 12, 2, 0.05F);
        };
    }
}
