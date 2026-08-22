package com.animania.common;

import com.animania.Animania;
import com.animania.common.block.AnimaniaContainerBlock;
import com.animania.common.block.AnimaniaInvisibleBlock;
import com.animania.common.block.AnimaniaMudBlock;
import com.animania.common.block.AnimaniaSaltLickBlock;
import com.animania.common.block.AnimaniaSaltLickBlockEntity;
import com.animania.common.block.AnimaniaStorageBlockEntity;
import com.animania.common.block.AnimaniaNestBlock;
import com.animania.common.block.AnimaniaThinBlock;
import com.animania.common.block.AnimaniaTroughBlock;
import com.animania.common.config.AnimaniaConfig;
import com.animania.common.item.AnimaniaSaltLickItem;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.ForgeRegistries;

public final class AnimaniaBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, Animania.MOD_ID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, Animania.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, Animania.MOD_ID);

    public static final RegistryObject<Block> TROUGH = trough();
    public static final RegistryObject<Block> NEST = nest();
    public static final RegistryObject<Block> SALT_LICK = saltLick();
    public static final RegistryObject<Block> MUD = simple("mud", MapColor.COLOR_BROWN);
    public static final RegistryObject<Block> STRAW = thin("straw", MapColor.COLOR_YELLOW, AnimaniaThinBlock.Kind.STRAW);
    public static final RegistryObject<Block> INVISIBLE_BLOCK = BLOCKS.register("invisiblock", () -> new AnimaniaInvisibleBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.NONE).sound(SoundType.WOOD)));
    public static final RegistryObject<Block> SEEDS = thin("block_seeds", MapColor.PLANT, AnimaniaThinBlock.Kind.SEEDS);

    public static final RegistryObject<BlockEntityType<TroughEntity>> TROUGH_BE = blockEntity("trough", TROUGH, TroughEntity::new);
    public static final RegistryObject<BlockEntityType<NestEntity>> NEST_BE = blockEntity("nest", NEST, NestEntity::new);
    public static final RegistryObject<BlockEntityType<AnimaniaSaltLickBlockEntity>> SALT_LICK_BE = BLOCK_ENTITIES.register("salt_lick",
            () -> BlockEntityType.Builder.of(AnimaniaSaltLickBlockEntity::new, SALT_LICK.get()).build(null));
    public static final RegistryObject<BlockEntityType<InvisibleTroughProxyEntity>> INVISIBLE_BE = BLOCK_ENTITIES.register("invisiblock",
            () -> BlockEntityType.Builder.of(InvisibleTroughProxyEntity::new, INVISIBLE_BLOCK.get()).build(null));

    private static RegistryObject<Block> simple(String name, MapColor color) {
        RegistryObject<Block> block = BLOCKS.register(name, () -> {
            BlockBehaviour.Properties properties = BlockBehaviour.Properties.of().mapColor(color).strength(1.0f).sound(SoundType.WOOD);
            if (name.equals("mud")) return new AnimaniaMudBlock(properties.friction(0.6f).sound(SoundType.SLIME_BLOCK));
            return new Block(properties);
        });
        ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
        return block;
    }

    private static RegistryObject<Block> thin(String name, MapColor color, AnimaniaThinBlock.Kind kind) {
        RegistryObject<Block> block = BLOCKS.register(name, () -> {
            BlockBehaviour.Properties properties = BlockBehaviour.Properties.of().strength(0.1f).sound(SoundType.GRASS);
            if (kind == AnimaniaThinBlock.Kind.SEEDS) {
                properties.mapColor(state -> switch (state.getValue(AnimaniaThinBlock.VARIANT)) {
                    case WHEAT -> MapColor.PLANT;
                    case PUMPKIN -> MapColor.COLOR_YELLOW;
                    case MELON, BEETROOT -> MapColor.TERRACOTTA_BROWN;
                });
            } else {
                properties.mapColor(color);
            }
            return new AnimaniaThinBlock(properties, kind);
        });
        ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
        return block;
    }

    private static RegistryObject<Block> trough() {
        RegistryObject<Block> block = BLOCKS.register("trough", () -> new AnimaniaTroughBlock(
                BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(1.2f).sound(SoundType.WOOD)));
        ITEMS.register("trough", () -> new BlockItem(block.get(), new Item.Properties()));
        return block;
    }

    private static RegistryObject<Block> nest() {
        RegistryObject<Block> block = BLOCKS.register("nest", () -> new AnimaniaNestBlock(
                BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_BROWN).strength(1.2f).sound(SoundType.WOOD),
                (pos, state) -> new NestEntity(pos, state)));
        ITEMS.register("nest", () -> new BlockItem(block.get(), new Item.Properties()));
        return block;
    }

    private static RegistryObject<Block> saltLick() {
        RegistryObject<Block> block = BLOCKS.register("salt_lick", () -> new AnimaniaSaltLickBlock(
                BlockBehaviour.Properties.of().mapColor(MapColor.SNOW).strength(1.2f).sound(SoundType.STONE)));
        ITEMS.register("salt_lick", () -> new AnimaniaSaltLickItem(block.get(), new Item.Properties()));
        return block;
    }

    private static RegistryObject<Block> container(String name, MapColor color) {
        RegistryObject<Block> block = BLOCKS.register(name, () -> new AnimaniaContainerBlock(BlockBehaviour.Properties.of().mapColor(color).strength(1.2f).sound(SoundType.WOOD),
                (pos, state) -> switch (name) {
                    case "trough" -> new TroughEntity(pos, state);
                    case "nest" -> new NestEntity(pos, state);
                    default -> throw new IllegalStateException("Unknown Animania container: " + name);
                }));
        ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
        return block;
    }

    private static <T extends AnimaniaStorageBlockEntity> RegistryObject<BlockEntityType<T>> blockEntity(
            String name, RegistryObject<Block> block, BlockEntityType.BlockEntitySupplier<T> factory) {
        return BLOCK_ENTITIES.register(name, () -> BlockEntityType.Builder.of(factory, block.get()).build(null));
    }

    public static final class TroughEntity extends AnimaniaStorageBlockEntity {
        /** Mirrors the 1.12 TileEntityTrough.TroughContent state machine. */
        public enum TroughContent { EMPTY, LIQUID, FOOD }

        public TroughEntity(net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
            super(TROUGH_BE.get(), pos, state, 1, 1000);
        }

        /** Food wins over a transient fluid packet, exactly as the legacy tick did. */
        public TroughContent content() {
            if (!getItem(0).isEmpty()) return TroughContent.FOOD;
            return fluidSnapshot().isEmpty() ? TroughContent.EMPTY : TroughContent.LIQUID;
        }

        @Override public int getMaxStackSize() { return 3; }
        @Override protected boolean allowsAutomation() {
            try { return AnimaniaConfig.ALLOW_TROUGH_AUTOMATION.get(); }
            catch (IllegalStateException ignored) { return true; }
        }
        @Override protected boolean isItemValid(int slot, ItemStack stack) {
            return slot == 0 && fluidSnapshot().isEmpty() && AnimaniaConfig.matchesTroughFood(stack);
        }
        @Override protected boolean isFluidValid(net.minecraftforge.fluids.FluidStack stack) {
            return getItem(0).isEmpty() && !stack.isEmpty()
                    && (stack.getFluid().isSame(net.minecraft.world.level.material.Fluids.WATER)
                    || stack.getFluid().isSame(AnimaniaFluids.SOURCE_SLOP.get()));
        }
    }

    public static final class NestEntity extends AnimaniaStorageBlockEntity {
        private String birdVariant = "";

        public NestEntity(net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
            super(NEST_BE.get(), pos, state, 1, 0);
        }

        @Override public int getMaxStackSize() { return 3; }
        @Override protected boolean allowsAutomation() { return false; }
        @Override protected boolean isItemValid(int slot, ItemStack stack) {
            ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
            return slot == 0 && id != null && (stack.is(net.minecraft.world.item.Items.EGG)
                    || id.equals(new ResourceLocation("animania_farm", "brown_egg"))
                    || id.equals(new ResourceLocation("animania_extra", "peacock_egg_blue"))
                    || id.equals(new ResourceLocation("animania_extra", "peacock_egg_white")));
        }

        public boolean insertEgg(ItemStack egg, String variant) {
            if (!isItemValid(0, egg) || egg.isEmpty()) return false;
            ItemStack stored = getItem(0);
            if (!stored.isEmpty() && (!ItemStack.isSameItemSameTags(stored, egg) || !birdVariant.equals(variant))) return false;
            if (stored.getCount() >= 3) return false;
            ItemStack result = egg.copy();
            result.setCount(stored.getCount() + 1);
            birdVariant = variant == null ? "" : variant;
            setItem(0, result);
            return true;
        }

        public String birdVariant() { return birdVariant; }

        @Override protected void saveAdditional(net.minecraft.nbt.CompoundTag tag) {
            super.saveAdditional(tag);
            if (!birdVariant.isEmpty()) tag.putString("BirdVariant", birdVariant);
        }

        @Override public void load(net.minecraft.nbt.CompoundTag tag) {
            super.load(tag);
            birdVariant = tag.contains("BirdVariant") ? tag.getString("BirdVariant")
                    : tag.contains("birdType") ? tag.getString("birdType") : "";
        }
    }

    /**
     * Capability proxy for the second half of the two-block trough.  The
     * controller direction is block-state data, so there is no duplicate
     * inventory or fluid state to desynchronise or lose on reload.
     */
    public static final class InvisibleTroughProxyEntity extends net.minecraft.world.level.block.entity.BlockEntity {
        public InvisibleTroughProxyEntity(net.minecraft.core.BlockPos pos,
                                          net.minecraft.world.level.block.state.BlockState state) {
            super(INVISIBLE_BE.get(), pos, state);
        }

        private TroughEntity controller() {
            if (level == null || !(getBlockState().getBlock() instanceof AnimaniaInvisibleBlock)) return null;
            net.minecraft.core.BlockPos controllerPos = ((AnimaniaInvisibleBlock) getBlockState().getBlock())
                    .controllerPos(worldPosition, getBlockState());
            return level.getBlockEntity(controllerPos) instanceof TroughEntity trough ? trough : null;
        }

        @Override
        public <T> net.minecraftforge.common.util.LazyOptional<T> getCapability(
                net.minecraftforge.common.capabilities.Capability<T> capability,
                net.minecraft.core.Direction side) {
            TroughEntity trough = controller();
            if (trough != null && (capability == net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER
                    || capability == net.minecraftforge.common.capabilities.ForgeCapabilities.FLUID_HANDLER)) {
                return trough.getCapability(capability, side);
            }
            return super.getCapability(capability, side);
        }
    }

    private AnimaniaBlocks() {
    }
}
