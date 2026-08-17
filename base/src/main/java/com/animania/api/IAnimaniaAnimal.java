package com.animania.api;

import com.animania.api.data.AnimalGender;
import com.animania.api.data.AnimalAge;
import com.animania.api.data.AnimalSnapshot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.resources.ResourceLocation;
import javax.annotation.Nullable;
import java.util.UUID;

/** Public, implementation-independent contract exposed to addon and probe integrations. */
public interface IAnimaniaAnimal {
    /**
     * Stable registry identity of this animal.  Implementations that expose a
     * snapshot automatically get this method; addon implementations may
     * override it when their live registry object is not available yet.
     */
    @Nullable
    default ResourceLocation typeId() {
        AnimalSnapshot state = snapshot();
        return state == null ? null : state.type();
    }

    AnimalGender getGender();

    void setGender(AnimalGender gender);

    String getVariantName();

    void setVariantName(String variant);

    int getHunger();

    int getThirst();

    boolean isSleeping();

    default boolean isPlaying() {
        return false;
    }

    /** Optional taming/interaction state used by pet addons and probes. */
    default boolean isTamed() {
        return false;
    }

    default boolean isSitting() {
        return false;
    }

    default boolean isSaddled() {
        return false;
    }

    /** True while a female milk-producing animal may be milked. */
    default boolean isMilkReady() {
        return false;
    }

    /** True while an addon transport item is holding this entity. */
    default boolean isInBall() {
        return false;
    }

    boolean isPregnant();

    /** True when an adult female has completed its post-birth recovery. */
    default boolean isFertile() {
        return !isSterilized();
    }

    /** Remaining post-birth recovery time in server ticks. */
    default int fertilityCooldownTicks() {
        return 0;
    }

    /** Number of server ticks already spent in the current pregnancy. */
    default int pregnancyTicks() {
        return 0;
    }

    /** Gestation duration in server ticks for this species. */
    default int gestationTicks() {
        return 0;
    }

    boolean isSterilized();

    /** Explicit state mutators are server-authoritative in the base entity. */
    default void setSleeping(boolean sleeping) {
    }

    default void setPlaying(boolean playing) {
    }

    default void setPregnant(boolean pregnant) {
    }

    default void setFertile(boolean fertile) {
    }

    default void setSterilized(boolean sterilized) {
    }

    /** Stable relationship state retained from the 1.12 public API. */
    @Nullable
    default UUID mateUuid() {
        return null;
    }

    default void setMateUuid(@Nullable UUID mateUuid) {
    }

    /** Children retain their actual mother's UUID instead of following any nearby adult. */
    @Nullable
    default UUID parentUuid() {
        return null;
    }

    default void setParentUuid(@Nullable UUID parentUuid) {
    }

    /** Stable age view used by addon renderers and compatibility integrations. */
    default AnimalAge age() {
        return asMob().isBaby() ? AnimalAge.BABY : AnimalAge.ADULT;
    }

    default boolean isAdult() {
        return age() == AnimalAge.ADULT && getGender().isAdult();
    }

    /**
     * Care hooks intentionally have defaults so third-party addons compiled
     * against the 3.0 API remain source/binary compatible.  Base entities
     * override them with server-side hunger/thirst/play handling.
     */
    default boolean feed(ItemStack stack) {
        return false;
    }

    default boolean drink(ItemStack stack) {
        return false;
    }

    default boolean play(ItemStack stack) {
        return false;
    }

    default boolean canBreedWith(IAnimaniaAnimal other) {
        return other != null && other != this && getGender().isAdult() && other.getGender().isAdult()
                && getGender() != other.getGender() && !isPregnant() && !other.isPregnant()
                && !isSterilized() && !other.isSterilized();
    }

    AnimalSnapshot snapshot();

    AgeableMob asMob();
}
