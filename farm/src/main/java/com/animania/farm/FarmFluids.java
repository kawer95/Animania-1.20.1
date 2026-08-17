package com.animania.farm;

import com.animania.Animania;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fluids.ForgeFlowingFluid;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Modern Forge fluid registrations for the five legacy milk fluids and
 * Animania honey.  The 1.12 universal-bucket/NBT path is intentionally
 * replaced by normal source/flowing fluids and Forge fluid capabilities.
 */
public final class FarmFluids {
    public static final DeferredRegister<FluidType> FLUID_TYPES =
            DeferredRegister.create(ForgeRegistries.Keys.FLUID_TYPES, AnimaniaFarm.MOD_ID);
    public static final DeferredRegister<net.minecraft.world.level.material.Fluid> FLUIDS =
            DeferredRegister.create(ForgeRegistries.FLUIDS, AnimaniaFarm.MOD_ID);
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, AnimaniaFarm.MOD_ID);
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, AnimaniaFarm.MOD_ID);

    public static final Map<String, FluidRegistration> ALL = new LinkedHashMap<>();
    public static final FluidRegistration MILK_HOLSTEIN = register("milk_holstein", 500, 1000);
    public static final FluidRegistration MILK_FRIESIAN = register("milk_friesian", 500, 1000);
    public static final FluidRegistration MILK_JERSEY = register("milk_jersey", 500, 1000);
    public static final FluidRegistration MILK_GOAT = register("milk_goat", 500, 1000);
    public static final FluidRegistration MILK_SHEEP = register("milk_sheep", 500, 1000);
    public static final FluidRegistration HONEY = register("animania_honey", 3000, 7000);

    private static FluidRegistration register(String id, int density, int viscosity) {
        FluidRegistration registration = new FluidRegistration(id);
        registration.type = FLUID_TYPES.register(id,
                () -> new FluidType(FluidType.Properties.create().density(density).viscosity(viscosity).canSwim(false)) {
                    @Override
                    public void initializeClient(Consumer<IClientFluidTypeExtensions> consumer) {
                        consumer.accept(new IClientFluidTypeExtensions() {
                            @Override public ResourceLocation getStillTexture() { return stillTexture(id); }
                            @Override public ResourceLocation getFlowingTexture() { return flowingTexture(id); }
                        });
                    }
                });
        registration.source = FLUIDS.register(id, () -> new ForgeFlowingFluid.Source(registration.properties()));
        registration.flowing = FLUIDS.register("flowing_" + id,
                () -> new ForgeFlowingFluid.Flowing(registration.properties()));
        boolean honey = id.equals("animania_honey");
        registration.block = BLOCKS.register(id, () -> new FarmLegacyFluidBlock(registration.source,
                BlockBehaviour.Properties.copy(Blocks.WATER)
                        .mapColor(honey ? MapColor.COLOR_YELLOW : MapColor.SNOW).noLootTable(), honey));
        registration.bucket = ITEMS.register(id + "_bucket",
                () -> new BucketItem(registration.source,
                        new Item.Properties().stacksTo(1).craftRemainder(net.minecraft.world.item.Items.BUCKET)));
        ALL.put(id, registration);
        return registration;
    }

    public static FluidRegistration byId(String id) {
        return ALL.get(id);
    }

    public static ResourceLocation stillTexture(String id) {
        return new ResourceLocation(AnimaniaFarm.MOD_ID, "fluids/" + id + "_still");
    }

    public static ResourceLocation flowingTexture(String id) {
        return new ResourceLocation(AnimaniaFarm.MOD_ID, "fluids/" + id + "_flow");
    }

    public static final class FluidRegistration {
        public final String id;
        public RegistryObject<FluidType> type;
        public RegistryObject<FlowingFluid> source;
        public RegistryObject<FlowingFluid> flowing;
        public RegistryObject<LiquidBlock> block;
        public RegistryObject<Item> bucket;

        private FluidRegistration(String id) {
            this.id = id;
        }

        private ForgeFlowingFluid.Properties properties() {
            return new ForgeFlowingFluid.Properties(type, source, flowing)
                    .slopeFindDistance(4)
                    .levelDecreasePerBlock(1)
                    .block(block)
                    .bucket(bucket);
        }
    }

    private FarmFluids() {
    }
}
