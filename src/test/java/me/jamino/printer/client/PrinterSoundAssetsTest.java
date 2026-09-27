package me.jamino.printer.client;

import com.google.gson.JsonParser;
import me.jamino.printer.registry.ModSounds;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PrinterSoundAssetsTest {
    @Test void guiSoundsReferenceVanillaEventsAndKeepPrinterSubtitles() throws Exception {
        var definitions = JsonParser.parseString(read("/assets/printer/sounds.json")).getAsJsonObject();
        var language = JsonParser.parseString(read("/assets/printer/lang/en_us.json")).getAsJsonObject();
        for (var sound : List.of(ModSounds.BUTTON, ModSounds.PREVIEW_LOAD, ModSounds.PRINT,
                ModSounds.PRINT_RETURN, ModSounds.PAPER_FEED, ModSounds.PAPER_EJECT)) {
            assertTrue(definitions.has(sound.getId().getPath()), sound.getId().toString());
        }
        for (var definition : definitions.entrySet()) {
            var value = definition.getValue().getAsJsonObject();
            String subtitle = value.get("subtitle").getAsString();
            assertTrue(subtitle.startsWith("subtitles.printer."));
            assertTrue(language.has(subtitle));
            assertFalse(value.getAsJsonArray("sounds").isEmpty());
            for (var entry : value.getAsJsonArray("sounds")) {
                var reference = entry.getAsJsonObject();
                assertEquals("event", reference.get("type").getAsString());
                var id = ResourceLocation.parse(reference.get("name").getAsString());
                assertEquals("minecraft", id.getNamespace(), "No custom audio assets or mod dependencies");
                assertTrue(BuiltInRegistries.SOUND_EVENT.containsKey(id), "Unknown vanilla sound: " + id);
            }
        }
    }

    @Test void modResourcesContainNoBundledAudio() throws Exception {
        var definitions = getClass().getResource("/assets/printer/sounds.json");
        assertNotNull(definitions);
        // Inspect the processed resources actually used by the tests, independent of working directory.
        var root = Path.of(definitions.toURI()).getParent();
        try (var paths = Files.walk(root)) {
            assertTrue(paths.noneMatch(path -> path.toString().toLowerCase(java.util.Locale.ROOT)
                    .matches(".*\\.(ogg|wav|mp3|flac)$")), "Use Minecraft sound references instead of bundled audio");
        }
    }

    private String read(String path) throws Exception {
        try (var input = getClass().getResourceAsStream(path)) {
            assertNotNull(input, path);
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
