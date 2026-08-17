package com.animania.common.command;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Executable compatibility coverage for the guarded vanilla conversion command. */
class AnimaniaCommandTest {
    @Test
    void legacyFamiliesMapToModernVanillaCounterparts() {
        assertEquals("minecraft:cow", id(AnimaniaConversion.vanillaTypeIdFor(
                new net.minecraft.resources.ResourceLocation("animania_farm", "cow_angus"))));
        assertEquals("minecraft:sheep", id(AnimaniaConversion.vanillaTypeIdFor(
                new net.minecraft.resources.ResourceLocation("animania_farm", "lamb_dorper"))));
        assertEquals("minecraft:pig", id(AnimaniaConversion.vanillaTypeIdFor(
                new net.minecraft.resources.ResourceLocation("animania_farm", "piglet_duroc"))));
        assertEquals("minecraft:chicken", id(AnimaniaConversion.vanillaTypeIdFor(
                new net.minecraft.resources.ResourceLocation("animania_farm", "rooster_leghorn"))));
        assertEquals("minecraft:horse", id(AnimaniaConversion.vanillaTypeIdFor(
                new net.minecraft.resources.ResourceLocation("animania_farm", "mare_draft"))));
        assertEquals("minecraft:rabbit", id(AnimaniaConversion.vanillaTypeIdFor(
                new net.minecraft.resources.ResourceLocation("animania_extra", "doe_lop"))));
        assertEquals("minecraft:cat", id(AnimaniaConversion.vanillaTypeIdFor(
                new net.minecraft.resources.ResourceLocation("animania_catsdogs", "queen_tabby"))));
        assertEquals("minecraft:wolf", id(AnimaniaConversion.vanillaTypeIdFor(
                new net.minecraft.resources.ResourceLocation("animania_catsdogs", "male_collie"))));
        assertNull(AnimaniaConversion.vanillaTypeIdFor(
                new net.minecraft.resources.ResourceLocation("minecraft", "cow")));
    }

    private static String id(net.minecraft.resources.ResourceLocation value) {
        return value == null ? null : value.toString();
    }

    @Test
    void commandRemainsAConfirmationGate() throws Exception {
        String source = java.nio.file.Files.readString(java.nio.file.Path.of(
                "src/main/java/com/animania/common/command/AnimaniaCommand.java"));
        assertTrue(source.contains("CONFIRM_WINDOW_MILLIS"));
        assertTrue(source.contains("commands.animania.tovanilla.warning"));
        assertTrue(source.contains("replaceAfterSuccessfulSpawn(level, entity, replacement)"));
        int spawn = source.indexOf("if (!level.addFreshEntity(replacement)) return false;");
        int discard = source.indexOf("source.discard();", spawn);
        assertTrue(spawn >= 0 && discard > spawn,
                "conversion must discard the source only after replacement spawn succeeds");
    }
}
