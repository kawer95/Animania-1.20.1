package com.animania.common;

import com.animania.Animania;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraft.resources.ResourceLocation;
import java.util.function.Consumer;
import net.minecraftforge.fluids.ForgeFlowingFluid;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** Native 1.20.1 slop fluid replacing the 1.12 Forge NBT bucket serializer. */
public final class AnimaniaFluids {
    public static final DeferredRegister<FluidType> FLUID_TYPES =
            DeferredRegister.create(ForgeRegistries.Keys.FLUID_TYPES, Animania.MOD_ID);
    public static final DeferredRegister<Fluid> FLUIDS =
            DeferredRegister.create(ForgeRegistries.FLUIDS, Animania.MOD_ID);
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, Animania.MOD_ID);

    public static final RegistryObject<FluidType> SLOP_TYPE = FLUID_TYPES.register("slop",
            () -> new FluidType(FluidType.Properties.create()
                    .descriptionId("fluid.animania.slop")
                    .density(1000).viscosity(1000).canSwim(false)) {
                @Override
                public void initializeClient(Consumer<IClientFluidTypeExtensions> consumer) {
                    consumer.accept(new IClientFluidTypeExtensions() {
                        private static final ResourceLocation STILL = new ResourceLocation(Animania.MOD_ID, "block/slop_still");
                        private static final ResourceLocation FLOW = new ResourceLocation(Animania.MOD_ID, "block/slop_flow");
                        @Override public ResourceLocation getStillTexture() { return STILL; }
                        @Override public ResourceLocation getFlowingTexture() { return FLOW; }
                    });
                }
            });
    public static final RegistryObject<FlowingFluid> SOURCE_SLOP = FLUIDS.register("slop",
            () -> new ForgeFlowingFluid.Source(slopProperties()));
    public static final RegistryObject<FlowingFluid> FLOWING_SLOP = FLUIDS.register("flowing_slop",
            () -> new ForgeFlowingFluid.Flowing(slopProperties()));
    public static final RegistryObject<LiquidBlock> SLOP_BLOCK = BLOCKS.register("slop",
            () -> new LiquidBlock(SOURCE_SLOP, BlockBehaviour.Properties.copy(Blocks.WATER).noLootTable()));

    private static ForgeFlowingFluid.Properties slopProperties() {
        return new ForgeFlowingFluid.Properties(SLOP_TYPE, SOURCE_SLOP, FLOWING_SLOP)
            .slopeFindDistance(4)
            .levelDecreasePerBlock(1)
            .block(SLOP_BLOCK)
            .bucket(AnimaniaItems.SLOP_BUCKET);
    }

    private AnimaniaFluids() {
    }
}
