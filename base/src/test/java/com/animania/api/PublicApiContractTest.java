package com.animania.api;

import com.animania.api.data.AnimalAge;
import com.animania.api.data.AnimalGender;
import com.animania.api.data.AnimalSnapshot;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.item.ItemStack;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class PublicApiContractTest {
    private static final ResourceLocation TYPE = new ResourceLocation("contract_addon", "test_animal");

    @Test
    void stableAnimalContractExposesEveryPublishedGameplayState() {
        Set<String> methods = Arrays.stream(IAnimaniaAnimal.class.getMethods())
                .map(java.lang.reflect.Method::getName).collect(Collectors.toSet());
        assertTrue(methods.containsAll(Set.of(
                "typeId", "getGender", "setGender", "age", "isAdult",
                "getVariantName", "setVariantName", "getHunger", "getThirst",
                "feed", "drink", "isSleeping", "setSleeping", "isPlaying", "setPlaying", "play",
                "canBreedWith", "isPregnant", "setPregnant", "pregnancyTicks", "gestationTicks",
                "isSterilized", "setSterilized", "mateUuid", "setMateUuid",
                "parentUuid", "setParentUuid", "snapshot", "asMob")));

        for (Class<?> facade : Set.of(
                com.animania.api.interfaces.IAgeable.class,
                com.animania.api.interfaces.IAnimaniaAnimal.class,
                com.animania.api.interfaces.IAnimaniaAnimalBase.class,
                com.animania.api.interfaces.IChild.class,
                com.animania.api.interfaces.IFoodEating.class,
                com.animania.api.interfaces.IGendered.class,
                com.animania.api.interfaces.IImpregnable.class,
                com.animania.api.interfaces.IMateable.class,
                com.animania.api.interfaces.IPlaying.class,
                com.animania.api.interfaces.ISleeping.class,
                com.animania.api.interfaces.ISterilizable.class,
                com.animania.api.interfaces.IVariant.class)) {
            assertTrue(IAnimaniaAnimal.class.isAssignableFrom(facade),
                    facade.getName() + " does not inherit the stable modern animal contract");
        }
    }

    @Test
    void animaniaTypeContractIsStable() {
        com.animania.api.interfaces.AnimaniaType type = () -> "contract_addon:test_animal";
        assertEquals("contract_addon:test_animal", type.getTypeName());
    }

    @Test void ageableContractIsModernFacade() { assertModernFacade(com.animania.api.interfaces.IAgeable.class); }
    @Test void animaniaAnimalContractIsModernFacade() { assertModernFacade(com.animania.api.interfaces.IAnimaniaAnimal.class); }
    @Test void animaniaAnimalBaseContractIsModernFacade() { assertModernFacade(com.animania.api.interfaces.IAnimaniaAnimalBase.class); }
    @Test void childContractIsModernFacade() { assertModernFacade(com.animania.api.interfaces.IChild.class); }
    @Test void foodEatingContractIsModernFacade() { assertModernFacade(com.animania.api.interfaces.IFoodEating.class); }
    @Test void genderedContractIsModernFacade() { assertModernFacade(com.animania.api.interfaces.IGendered.class); }
    @Test void impregnableContractIsModernFacade() { assertModernFacade(com.animania.api.interfaces.IImpregnable.class); }
    @Test void mateableContractIsModernFacade() { assertModernFacade(com.animania.api.interfaces.IMateable.class); }
    @Test void playingContractIsModernFacade() { assertModernFacade(com.animania.api.interfaces.IPlaying.class); }
    @Test void sleepingContractIsModernFacade() { assertModernFacade(com.animania.api.interfaces.ISleeping.class); }
    @Test void sterilizableContractIsModernFacade() { assertModernFacade(com.animania.api.interfaces.ISterilizable.class); }
    @Test void variantContractIsModernFacade() { assertModernFacade(com.animania.api.interfaces.IVariant.class); }

    @Test
    void ageableContractMapsAgeAndAdultState() {
        com.animania.api.interfaces.IAgeable animal = new Stub(AnimalGender.FEMALE);
        assertEquals(AnimalAge.ADULT, animal.age());
        assertTrue(animal.isAdult());
    }

    @Test
    void animaniaAnimalContractMapsStableType() {
        com.animania.api.interfaces.IAnimaniaAnimal animal = new Stub(AnimalGender.FEMALE);
        assertEquals(TYPE.toString(), animal.getAnimalType().getTypeName());
    }

    @Test
    void childContractMapsParentRelationship() {
        com.animania.api.interfaces.IChild animal = new Stub(AnimalGender.FEMALE);
        UUID parent = UUID.randomUUID();
        animal.setParentUuid(parent);
        assertEquals(parent, animal.parentUuid());
    }

    @Test
    void foodEatingContractMapsFeedingAndDrinking() {
        com.animania.api.interfaces.IFoodEating animal = new Stub(AnimalGender.FEMALE);
        assertTrue(animal.feed(null));
        assertTrue(animal.drink(null));
        assertEquals(76, animal.getHunger());
        assertEquals(81, animal.getThirst());
    }

    @Test
    void genderedContractMapsModernGender() {
        com.animania.api.interfaces.IGendered animal = new Stub(AnimalGender.MALE);
        assertEquals(AnimalGender.MALE, animal.getModernGender());
        animal.setGender(AnimalGender.FEMALE);
        assertEquals(AnimalGender.FEMALE, animal.getModernGender());
    }

    @Test
    void impregnableContractMapsPregnancyState() {
        com.animania.api.interfaces.IImpregnable animal = new Stub(AnimalGender.FEMALE);
        animal.setPregnant(true);
        assertTrue(animal.isPregnant());
        assertEquals(40, animal.pregnancyTicks());
        assertEquals(200, animal.gestationTicks());
    }

    @Test
    void mateableContractMapsMateAndEligibility() {
        com.animania.api.interfaces.IMateable female = new Stub(AnimalGender.FEMALE);
        com.animania.api.interfaces.IMateable male = new Stub(AnimalGender.MALE);
        UUID mate = UUID.randomUUID();
        female.setMateUuid(mate);
        assertEquals(mate, female.mateUuid());
        assertTrue(female.canBreedWith(male));
    }

    @Test
    void playingContractMapsPlayingState() {
        com.animania.api.interfaces.IPlaying animal = new Stub(AnimalGender.FEMALE);
        animal.setPlaying(true);
        assertTrue(animal.isPlaying());
        assertTrue(animal.play(null));
    }

    @Test
    void sleepingContractMapsSleepState() {
        com.animania.api.interfaces.ISleeping animal = new Stub(AnimalGender.FEMALE);
        animal.setSleeping(true);
        assertTrue(animal.isSleeping());
    }

    @Test
    void sterilizableContractMapsSterilizationState() {
        com.animania.api.interfaces.ISterilizable animal = new Stub(AnimalGender.FEMALE);
        assertFalse(animal.isSterilized());
        animal.setSterilized(true);
        assertTrue(animal.isSterilized());
    }

    @Test
    void variantContractMapsVariantState() {
        com.animania.api.interfaces.IVariant animal = new Stub(AnimalGender.FEMALE);
        assertEquals("default", animal.getVariantName());
        animal.setVariantName("black");
        assertEquals("black", animal.getVariantName());
    }

    private static void assertModernFacade(Class<?> facade) {
        assertTrue(com.animania.api.IAnimaniaAnimal.class.isAssignableFrom(facade),
                facade.getName() + " does not inherit the Java 17 gameplay contract");
    }

    @Test
    void breedingAndLegacyFacadeUseTheSameModernState() {
        Stub female = new Stub(AnimalGender.FEMALE);
        Stub male = new Stub(AnimalGender.MALE);
        assertTrue(female.canBreedWith(male));
        female.pregnant = true;
        assertFalse(female.canBreedWith(male));
        female.pregnant = false;
        male.sterilized = true;
        assertFalse(female.canBreedWith(male));
        assertEquals(TYPE.toString(), female.getAnimalType().getTypeName());

        UUID mate = UUID.randomUUID();
        UUID parent = UUID.randomUUID();
        female.setMateUuid(mate);
        female.setParentUuid(parent);
        assertEquals(mate, female.mateUuid());
        assertEquals(parent, female.parentUuid());
        assertEquals(AnimalAge.ADULT, female.age());
        assertTrue(female.isAdult());
    }

    @Test
    void legacyUtilityInterfacesRetainTheirPublishedMethodContracts() throws Exception {
        assertEquals(int.class, com.animania.api.interfaces.IBlinking.class
                .getMethod("getBlinkTimer").getReturnType());
        assertEquals(void.class, com.animania.api.interfaces.IBlinking.class
                .getMethod("setBlinkTimer", int.class).getReturnType());
        assertEquals(net.minecraft.world.entity.Entity.class, com.animania.api.interfaces.IConvertable.class
                .getMethod("convertToVanilla").getReturnType());

        Map<String, Class<?>> spawnable = Arrays.stream(com.animania.api.interfaces.ISpawnable.class.getMethods())
                .collect(Collectors.toMap(java.lang.reflect.Method::getName, java.lang.reflect.Method::getReturnType));
        assertEquals(net.minecraft.world.item.Item.class, spawnable.get("getSpawnEgg"));
        assertEquals(int.class, spawnable.get("getPrimaryEggColor"));
        assertEquals(int.class, spawnable.get("getSecondaryEggColor"));
        assertEquals(boolean.class, spawnable.get("usesEggColor"));
        assertTrue(new SpawnableStub().usesEggColor());

        Set<String> provider = Arrays.stream(com.animania.api.interfaces.IFoodProviderTE.class.getMethods())
                .map(java.lang.reflect.Method::getName).collect(Collectors.toSet());
        assertTrue(provider.containsAll(Set.of("canConsume", "consumeSolidOrLiquid", "consumeSolid", "consumeLiquid")));
        assertEquals(5, com.animania.api.interfaces.IFoodProviderTE.class.getDeclaredMethods().length);
        assertEquals(0, com.animania.api.interfaces.IFoodProviderBlock.class.getDeclaredMethods().length,
                "IFoodProviderBlock must remain the published marker interface");
    }

    @Test
    void blinkingContractRoundTripsTimer() {
        BlinkingStub blinking = new BlinkingStub();
        assertEquals(0, blinking.getBlinkTimer());
        blinking.setBlinkTimer(37);
        assertEquals(37, blinking.getBlinkTimer());
    }

    @Test
    void spawnableContractKeepsEggColourPolicy() {
        SpawnableStub spawnable = new SpawnableStub();
        assertTrue(spawnable.usesEggColor());
        assertEquals(0x112233, spawnable.getPrimaryEggColor());
        assertEquals(0x445566, spawnable.getSecondaryEggColor());
    }

    @Test
    void foodProviderContractConsumesSolidAndLiquidAmounts() {
        FoodProviderStub provider = new FoodProviderStub();
        assertTrue(provider.canConsume(Set.of(), new net.minecraftforge.fluids.FluidStack[0]));
        assertTrue(provider.canConsume(null, Set.of()));
        provider.consumeSolidOrLiquid(4, 2);
        provider.consumeSolid(3);
        provider.consumeLiquid(5);
        assertEquals(14, provider.consumed);
    }

    private static final class SpawnableStub implements com.animania.api.interfaces.ISpawnable {
        @Override public net.minecraft.world.item.Item getSpawnEgg() { return null; }
        @Override public int getPrimaryEggColor() { return 0x112233; }
        @Override public int getSecondaryEggColor() { return 0x445566; }
    }

    private static final class BlinkingStub implements com.animania.api.interfaces.IBlinking {
        private int timer;

        @Override public int getBlinkTimer() { return timer; }
        @Override public void setBlinkTimer(int ticks) { timer = ticks; }
    }

    private static final class FoodProviderStub implements com.animania.api.interfaces.IFoodProviderTE {
        private int consumed;

        @Override public boolean canConsume(java.util.Set<ItemStack> foodItems,
                net.minecraftforge.fluids.FluidStack[] fluids) {
            return foodItems != null || fluids != null;
        }

        @Override public boolean canConsume(net.minecraftforge.fluids.FluidStack fluid,
                java.util.Set<ItemStack> foodItems) {
            return fluid != null || foodItems != null;
        }

        @Override public void consumeSolidOrLiquid(int liquidAmount, int itemAmount) {
            consumed += liquidAmount + itemAmount;
        }

        @Override public void consumeSolid(int amount) { consumed += amount; }

        @Override public void consumeLiquid(int amount) { consumed += amount; }
    }

    private static final class Stub implements com.animania.api.interfaces.IAnimaniaAnimal,
            com.animania.api.interfaces.IAgeable, com.animania.api.interfaces.IAnimaniaAnimalBase,
            com.animania.api.interfaces.IChild, com.animania.api.interfaces.IFoodEating,
            com.animania.api.interfaces.IGendered, com.animania.api.interfaces.IImpregnable,
            com.animania.api.interfaces.IMateable, com.animania.api.interfaces.IPlaying,
            com.animania.api.interfaces.ISleeping, com.animania.api.interfaces.ISterilizable,
            com.animania.api.interfaces.IVariant {
        private AnimalGender gender;
        private String variant = "default";
        private int hunger = 75;
        private int thirst = 80;
        private boolean sleeping;
        private boolean playing;
        private boolean pregnant;
        private boolean sterilized;
        private UUID mate;
        private UUID parent;

        private Stub(AnimalGender gender) { this.gender = gender; }
        @Override public AnimalGender getGender() { return gender; }
        @Override public void setGender(AnimalGender value) { gender = value; }
        @Override public String getVariantName() { return variant; }
        @Override public void setVariantName(String value) { variant = value; }
        @Override public int getHunger() { return hunger; }
        @Override public int getThirst() { return thirst; }
        @Override public boolean isSleeping() { return sleeping; }
        @Override public void setSleeping(boolean value) { sleeping = value; }
        @Override public boolean isPlaying() { return playing; }
        @Override public void setPlaying(boolean value) { playing = value; }
        @Override public boolean isPregnant() { return pregnant; }
        @Override public void setPregnant(boolean value) { pregnant = value; }
        @Override public int pregnancyTicks() { return 40; }
        @Override public int gestationTicks() { return 200; }
        @Override public boolean isSterilized() { return sterilized; }
        @Override public void setSterilized(boolean value) { sterilized = value; }
        @Override public UUID mateUuid() { return mate; }
        @Override public void setMateUuid(UUID value) { mate = value; }
        @Override public UUID parentUuid() { return parent; }
        @Override public void setParentUuid(UUID value) { parent = value; }
        @Override public AnimalAge age() { return AnimalAge.ADULT; }
        @Override public boolean feed(ItemStack stack) { hunger++; return true; }
        @Override public boolean drink(ItemStack stack) { thirst++; return true; }
        @Override public boolean play(ItemStack stack) { playing = true; return true; }
        @Override public AnimalSnapshot snapshot() {
            return new AnimalSnapshot(TYPE, gender, age(), variant, getHunger(), getThirst(),
                    sleeping, pregnant, sterilized);
        }
        @Override public AgeableMob asMob() { return null; }
    }
}
