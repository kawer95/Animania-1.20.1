package com.animania.client.config;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** Config screen and Forge extension-point regression coverage. */
class AnimaniaConfigScreenTest {
    @Test
    void forgeRegistersAnEditableClientConfigScreenForEveryModernSpec() throws Exception {
        String entry = Files.readString(Path.of("src/main/java/com/animania/Animania.java"));
        String screen = Files.readString(Path.of("src/main/java/com/animania/client/config/AnimaniaConfigScreen.java"));
        assertTrue(entry.contains("ConfigScreenHandler.ConfigScreenFactory"));
        assertTrue(entry.contains("AnimaniaConfigScreen::new"));
        assertTrue(screen.contains("collectEntries(spec)"));
        assertTrue(screen.contains("new EditBox"));
        assertTrue(screen.contains("value.set(valueToSet)"));
        assertTrue(screen.contains("spec.save()"));
        assertTrue(screen.contains("Button.builder"));

        for (String addon : new String[] {"farm", "catsdogs", "extra"}) {
            Path addonRoot = Path.of("..").resolve(addon).resolve("src/main/java/com/animania");
            String addonEntry = Files.walk(addonRoot)
                    .filter(path -> path.getFileName().toString().startsWith("Animania"))
                    .filter(path -> path.getFileName().toString().endsWith(".java"))
                    .map(path -> {
                        try { return Files.readString(path); }
                        catch (java.io.IOException exception) { throw new java.io.UncheckedIOException(exception); }
                    })
                    .filter(source -> source.contains("registerConfig(ModConfig.Type.COMMON"))
                    .findFirst().orElseThrow();
            assertTrue(addonEntry.contains("ConfigScreenHandler.ConfigScreenFactory"), addon);
            assertTrue(addonEntry.contains("new com.animania.client.config.AnimaniaConfigScreen"), addon);
        }
    }

    @Test
    void everyAddonConfigOptionHasASimplifiedChineseLabel() throws Exception {
        assertChineseCoverage("farm", "animania_farm", "farm", "FarmConfig.java");
        assertChineseCoverage("catsdogs", "animania_catsdogs", "catsdogs", "CatsDogsConfig.java");
        assertChineseCoverage("extra", "animania_extra", "extra", "ExtraConfig.java");
    }

    private static void assertChineseCoverage(String module, String namespace, String category,
                                              String configFile) throws Exception {
        Path moduleRoot = Path.of("..").resolve(module).resolve("src/main");
        Path source = Files.walk(moduleRoot.resolve("java"))
                .filter(path -> path.getFileName().toString().equals(configFile))
                .findFirst().orElseThrow();
        Path language = moduleRoot.resolve("resources/assets").resolve(namespace).resolve("lang/zh_cn.json");
        String java = Files.readString(source);
        String translations = Files.readString(language);
        var matcher = Pattern.compile("(?:define|defineInRange|defineList|defineBiome)\\([^\\r\\n]*?\"([A-Za-z0-9]+)\"")
                .matcher(java);
        while (matcher.find()) {
            String key = "\"config." + namespace + "." + category + "." + matcher.group(1) + "\"";
            assertTrue(translations.contains(key), "missing zh_cn translation: " + key);
        }
    }
}
