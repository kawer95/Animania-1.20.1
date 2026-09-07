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
        String client = Files.readString(Path.of("src/main/java/com/animania/client/AnimaniaClient.java"));
        String screen = Files.readString(Path.of("src/main/java/com/animania/client/config/AnimaniaConfigScreen.java"));
        assertClientOnlyConfigRegistration(entry, client, "base");
        assertTrue(client.contains("AnimaniaConfigScreen::new"));
        assertTrue(screen.contains("collectEntries(spec)"));
        assertTrue(screen.contains("new EditBox"));
        assertTrue(screen.contains("value.set(valueToSet)"));
        assertTrue(screen.contains("spec.save()"));
        assertTrue(screen.contains("Button.builder"));

        for (String addon : new String[] {"farm", "catsdogs", "extra"}) {
            Path addonRoot = Path.of("..").resolve(addon).resolve("src/main/java/com/animania");
            Path addonEntryPath = addonRoot.resolve(switch (addon) {
                case "farm" -> "farm/AnimaniaFarm.java";
                case "catsdogs" -> "catsdogs/AnimaniaCatsDogs.java";
                case "extra" -> "extra/AnimaniaExtra.java";
                default -> throw new IllegalStateException(addon);
            });
            String addonEntry = Files.readString(addonEntryPath);
            Path addonClientPath = addonEntryPath.resolveSibling(addonEntryPath.getFileName().toString()
                    .replace(".java", "Client.java"));
            String addonClient = Files.readString(addonClientPath);
            assertClientOnlyConfigRegistration(addonEntry, addonClient, addon);
            assertTrue(addonClient.contains("new com.animania.client.config.AnimaniaConfigScreen"), addon);
        }
    }

    private static void assertClientOnlyConfigRegistration(String commonEntry, String clientEntry, String module) {
        assertTrue(commonEntry.contains("Client.registerConfigScreen()"), module);
        assertTrue(!commonEntry.contains("Client::registerConfigScreen"),
                module + " must not expose a direct client method handle while Forge reflects the mod constructor");
        assertTrue(!commonEntry.contains("ConfigScreenHandler.ConfigScreenFactory"), module);
        assertTrue(!commonEntry.contains("AnimaniaConfigScreen"), module);
        assertTrue(clientEntry.contains("ConfigScreenHandler.ConfigScreenFactory"), module);
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
